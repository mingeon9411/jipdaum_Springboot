package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.jipdaumuser.EmailOtp;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtpRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.token.BlacklistedToken;
import com.jipdaum_spring.domain.token.BlacklistedTokenRepository;
import com.jipdaum_spring.dto.auth.*;
import com.jipdaum_spring.exception.AuthException;
import com.jipdaum_spring.exception.FieldValidationException;
import com.jipdaum_spring.security.CurrentUserProvider;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import com.jipdaum_spring.security.captcha.HCaptchaVerifier;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Django Users 앱(회원가입/로그인/로그아웃/닉네임 중복확인/이메일 OTP)을 포팅한 서비스.
 * 비밀번호 강도 검사는 Django의 validate_password(공통 비밀번호 사전 등)를 완전히 복제하지 않고
 * 최소 8자 + 전체 숫자 금지 + 이메일/닉네임과 동일 금지로 단순화했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {

    private static final long OTP_TTL_SECONDS = 300;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JipdaumUserRepository jipdaumUserRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JipdaumUserProvisioner jipdaumUserProvisioner;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final HCaptchaVerifier hCaptchaVerifier;
    private final CurrentUserProvider currentUserProvider;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String nickname = request.nickname().trim();
        String email = request.email().trim();

        if (nickname.length() < 2) {
            throw new FieldValidationException("nickname", "닉네임은 최소 2자 이상이어야 합니다.");
        }
        if (nickname.length() > 30) {
            throw new FieldValidationException("nickname", "닉네임은 최대 30자 이하여야 합니다.");
        }
        if (jipdaumUserRepository.existsByNickname(nickname)) {
            throw new FieldValidationException("nickname", "이미 사용 중인 닉네임입니다.");
        }
        if (jipdaumUserRepository.existsByEmail(email)) {
            throw new FieldValidationException("email", "이미 사용 중인 이메일입니다.");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new FieldValidationException("password_confirm", "비밀번호가 일치하지 않습니다.");
        }
        validatePasswordStrength(request.password(), email, nickname);

        String username = generateUsername(email);
        String passwordHash = passwordEncoder.encode(request.password());
        jipdaumUserProvisioner.createLocalUser(username, passwordHash, email, nickname);

        return new RegisterResponse(nickname, email);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (!hCaptchaVerifier.verify(request.recaptchaToken())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 인증에 실패했습니다. 다시 시도해주세요.");
        }

        String identifier = request.username().trim();
        JipdaumUser user = jipdaumUserRepository.findByUsername(identifier)
                .orElseGet(() -> identifier.contains("@")
                        ? jipdaumUserRepository.findByEmail(identifier).orElse(null)
                        : null);

        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않거나 비활성화된 계정입니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        return new LoginResponse(accessToken, refreshToken, UserSummaryResponse.from(user));
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = request.refresh();
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "리프레시 토큰이 누락되었습니다.");
        }
        if (!JwtTokenProvider.TYPE_REFRESH.equals(jwtTokenProvider.getType(refreshToken))
                || !jwtTokenProvider.validate(refreshToken)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다.");
        }

        Date expiration = jwtTokenProvider.getExpiration(refreshToken);
        LocalDateTime expiresAt = expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        blacklistedTokenRepository.save(new BlacklistedToken(refreshToken, expiresAt));
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String rawNickname) {
        String nickname = rawNickname == null ? "" : rawNickname.trim();
        if (nickname.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요.");
        }
        if (nickname.length() < 2) {
            return new NicknameCheckResponse(false, "닉네임은 최소 2자 이상이어야 합니다.");
        }
        if (nickname.length() > 30) {
            return new NicknameCheckResponse(false, "닉네임은 최대 30자 이하여야 합니다.");
        }
        if (jipdaumUserRepository.existsByNickname(nickname)) {
            return new NicknameCheckResponse(false, "이미 사용 중인 닉네임입니다.");
        }
        return new NicknameCheckResponse(true, "사용 가능한 닉네임입니다.");
    }

    @Transactional
    public void sendEmailOtp(EmailOtpSendRequest request) {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        String email = request.email().trim();
        if (email.isEmpty()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "이메일을 입력해주세요.");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        emailOtpRepository.deleteAllByUserIdAndUsedFalse(user.getId());
        emailOtpRepository.save(new EmailOtp(user.getId(), email, code));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("[집다움] 이메일 인증 코드");
            message.setText("인증 코드: " + code + "\n5분 이내에 입력해주세요.");
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("이메일 OTP 발송 실패 (email={})", email, e);
            throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다. 이메일 주소를 확인해주세요.");
        }
    }

    @Transactional
    public void verifyEmailOtp(EmailOtpVerifyRequest request) {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        String email = request.email().trim();
        String code = request.code().trim();

        EmailOtp otp = emailOtpRepository
                .findFirstByUserIdAndEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(user.getId(), email, code)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."));

        if (otp.getCreatedAt().plusSeconds(OTP_TTL_SECONDS).isBefore(LocalDateTime.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 재발송해주세요.");
        }

        otp.setUsed(true);
        emailOtpRepository.save(otp);

        user.setEmailVerified(true);
        jipdaumUserRepository.save(user);
    }

    private String generateUsername(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : "user";
        String sanitized = local.replaceAll("[^a-zA-Z0-9_]", "_");
        String base = sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
        String truncatedBase = base.length() > 17 ? base.substring(0, 17) : base;

        String candidate = base;
        int counter = 1;
        while (!jipdaumUserRepository.findAllByUsernameIgnoreCase(candidate).isEmpty()) {
            candidate = truncatedBase + "_" + counter;
            counter++;
        }
        return candidate;
    }

    private void validatePasswordStrength(String password, String email, String nickname) {
        if (password.length() < 8) {
            throw new FieldValidationException("password", "비밀번호는 최소 8자 이상이어야 합니다.");
        }
        if (password.chars().allMatch(Character::isDigit)) {
            throw new FieldValidationException("password", "비밀번호가 숫자로만 되어 있어 사용할 수 없습니다.");
        }
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        if (password.equalsIgnoreCase(local) || password.equalsIgnoreCase(nickname)) {
            throw new FieldValidationException("password", "비밀번호가 이메일 또는 닉네임과 너무 유사합니다.");
        }
    }
}
