package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCoupon;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtp;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtpRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.token.BlacklistedToken;
import com.jipdaum_spring.domain.token.BlacklistedTokenRepository;
import com.jipdaum_spring.dto.auth.*;
import com.jipdaum_spring.dto.coupon.MyCouponResponse;
import com.jipdaum_spring.exception.AuthException;
import com.jipdaum_spring.exception.FieldValidationException;
import com.jipdaum_spring.security.CurrentUserProvider;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import com.jipdaum_spring.security.captcha.HCaptchaService;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

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
    // 회원가입 시 자동 지급되는 웰컴 쿠폰 코드 — 15%/30% 할인 쿠폰 각 1장.
    // Django backend/coupons 시드 데이터(0002_seed_coupons)와 코드가 맞아야 한다.
    private static final List<String> WELCOME_COUPON_CODES =
            List.of("WELCOME15", "WELCOME30");
    // 아이디/비밀번호 찾기 본인확인용 보안질문 고정 목록 — 프론트
    // frontend/src/data/securityQuestions.js와 반드시 동일해야 한다.
    private static final List<String> SECURITY_QUESTIONS = List.of(
            "가장 좋아하는 음식은?",
            "첫 반려동물의 이름은?",
            "출신 초등학교는?",
            "가장 기억에 남는 여행지는?",
            "어머니의 성함은?"
    );

    private final JipdaumUserRepository jipdaumUserRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final JipdaumUserProvisioner jipdaumUserProvisioner;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final HCaptchaService hCaptchaService;
    private final CurrentUserProvider currentUserProvider;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!hCaptchaService.verify(request.recaptchaToken())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 인증에 실패했습니다. 다시 시도해주세요.");
        }

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
        if (!SECURITY_QUESTIONS.contains(request.securityQuestion())) {
            throw new FieldValidationException("security_question", "보안 질문을 목록에서 선택해주세요.");
        }

        String username = generateUsername(email);
        String passwordHash = passwordEncoder.encode(request.password());
        jipdaumUserProvisioner.createLocalUser(username, passwordHash, email, nickname);

        JipdaumUser newUser = jipdaumUserRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "회원 생성에 실패했습니다."));
        newUser.setSecurityQuestion(request.securityQuestion());
        newUser.setSecurityAnswer(passwordEncoder.encode(normalizeAnswer(request.securityAnswer())));
        jipdaumUserRepository.save(newUser);
        List<MyCouponResponse> issuedCoupons = issueWelcomeCoupons(newUser);

        return new RegisterResponse(nickname, email, issuedCoupons);
    }

    /** 가입 완료 직후 웰컴 쿠폰을 계정에 지급하고, 화면에 바로 보여줄 수 있게 지급 내역을 반환한다. */
    private List<MyCouponResponse> issueWelcomeCoupons(JipdaumUser user) {
        List<Coupon> coupons = couponRepository.findAllByCodeInAndIsActiveTrue(WELCOME_COUPON_CODES);
        LocalDateTime now = LocalDateTime.now();

        List<UserCoupon> issued = coupons.stream().map(coupon -> {
            UserCoupon uc = new UserCoupon();
            uc.setUser(user);
            uc.setCoupon(coupon);
            uc.setIsUsed(false);
            uc.setCreatedAt(now);
            return uc;
        }).toList();

        userCouponRepository.saveAll(issued);
        return issued.stream().map(MyCouponResponse::from).toList();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (!hCaptchaService.verify(request.recaptchaToken())) {
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

    /**
     * 회원 탈퇴. JIPDAUM_USER는 주문/리뷰 등 다수 FK의 참조 대상이라 하드 삭제 대신
     * is_active=0으로 비활성화한다(Django User 모델의 통상적인 탈퇴 처리 방식과 동일).
     * 진행 중인 refresh token도 즉시 블랙리스트에 올려 재발급을 막는다.
     */
    @Transactional
    public void withdraw(WithdrawRequest request) {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        user.setActive(false);
        jipdaumUserRepository.save(user);

        String refreshToken = request.refresh();
        if (StringUtils.hasText(refreshToken)
                && JwtTokenProvider.TYPE_REFRESH.equals(jwtTokenProvider.getType(refreshToken))
                && jwtTokenProvider.validate(refreshToken)) {
            Date expiration = jwtTokenProvider.getExpiration(refreshToken);
            LocalDateTime expiresAt = expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            blacklistedTokenRepository.save(new BlacklistedToken(refreshToken, expiresAt));
        }
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
        issueAndSendOtp(user.getId(), email);
    }

    @Transactional
    public EmailOtpVerifyResponse verifyEmailOtp(EmailOtpVerifyRequest request) {
        JipdaumUser user = currentUserProvider.getCurrentUser();
        String email = request.email().trim();
        String code = request.code().trim();

        consumeOtp(user.getId(), email, code);

        user.setEmailVerified(true);
        jipdaumUserRepository.save(user);
        return new EmailOtpVerifyResponse("이메일 인증이 완료되었습니다.", user.getSecurityQuestion() != null);
    }

    /** 6자리 코드를 발급/저장하고 메일로 보낸다 — 로그인 사용자용(sendEmailOtp)과 아이디 찾기(비로그인) 양쪽에서 재사용. */
    private void issueAndSendOtp(Long userId, String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        emailOtpRepository.deleteAllByUserIdAndUsedFalse(userId);
        emailOtpRepository.save(new EmailOtp(userId, email, code));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("[집다움] 이메일 인증 코드");
            helper.setText("""
                    <div style="font-family:'Malgun Gothic',sans-serif;max-width:420px;margin:0 auto;padding:32px 24px;">
                      <div style="display:flex;align-items:center;gap:14px;margin-bottom:28px;">
                        <img src="cid:jdLogo" alt="J.D" style="height:32px;" />
                        <div style="width:1px;height:28px;background:#d8d8d8;"></div>
                        <img src="cid:hanokLogo" alt="집다움" style="height:40px;" />
                      </div>
                      <p style="font-size:15px;color:#333;margin:0 0 8px;">안녕하세요, 집다움입니다.</p>
                      <p style="font-size:15px;color:#333;margin:0 0 20px;">요청하신 보안코드는 %s입니다.</p>
                      <p style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#1a1a1a;margin:0 0 20px;">%s</p>
                      <p style="font-size:13px;color:#888;margin:0;">인증코드는 5분간 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
                    </div>
                    """.formatted(code, code), true);
            helper.addInline("jdLogo", new ClassPathResource("mail/jd-logo.png"));
            helper.addInline("hanokLogo", new ClassPathResource("mail/hanok-logo.png"));
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.warn("이메일 OTP 발송 실패 (email={})", email, e);
            throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다. 이메일 주소를 확인해주세요.");
        }
    }

    /** 코드를 검증하고 사용 처리한다. 틀렸거나 만료됐으면 예외를 던진다. */
    private void consumeOtp(Long userId, String email, String code) {
        EmailOtp otp = emailOtpRepository
                .findFirstByUserIdAndEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(userId, email, code)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."));

        if (otp.getCreatedAt().plusSeconds(OTP_TTL_SECONDS).isBefore(LocalDateTime.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 재발송해주세요.");
        }

        otp.setUsed(true);
        emailOtpRepository.save(otp);
    }

    /** 답변 비교는 대소문자/앞뒤 공백 차이를 관대하게 허용한다. */
    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase();
    }

    private boolean matchesAnswer(String rawAnswer, String storedHash) {
        return storedHash != null && passwordEncoder.matches(normalizeAnswer(rawAnswer), storedHash);
    }

    /** 아이디/비밀번호 찾기 첫 화면에서 보안 질문 텍스트를 보여주기 위한 조회. 계정 유무를 과다 노출하지 않게 오류 문구를 통일한다. */
    @Transactional(readOnly = true)
    public SecurityQuestionResponse getSecurityQuestion(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        JipdaumUser user = jipdaumUserRepository.findByEmail(email).orElse(null);
        if (user == null || user.getSecurityQuestion() == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "가입되지 않은 이메일이거나 보안 질문이 설정되지 않았습니다. 로그인 후 마이페이지에서 먼저 설정해주세요.");
        }
        return new SecurityQuestionResponse(user.getSecurityQuestion());
    }

    /** 아이디 찾기 1단계: 이메일로 인증코드 발송. 미가입 이메일이어도 존재 여부를 노출하지 않도록 동일하게 성공 처리한다. */
    @Transactional
    public void sendFindIdCode(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();
        JipdaumUser user = jipdaumUserRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        issueAndSendOtp(user.getId(), email);
    }

    /** 아이디 찾기 2단계: 인증코드 + 보안답 확인 후 username(로그인 아이디)을 돌려준다. */
    @Transactional
    public FindIdResponse verifyFindId(FindIdVerifyRequest request) {
        String email = request.email().trim();
        JipdaumUser user = jipdaumUserRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."));

        // 보안답부터 확인 — 순서를 바꾸면 답을 틀렸을 때도 코드가 소모돼, 맞는 코드인데도
        // 재발송을 새로 받아야 하는 UX 버그가 생긴다.
        if (!matchesAnswer(request.securityAnswer(), user.getSecurityAnswer())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 질문 답변이 일치하지 않습니다.");
        }
        consumeOtp(user.getId(), email, request.code().trim());
        return new FindIdResponse(user.getUsername());
    }

    /**
     * 비밀번호 찾기 본인확인: 닉네임+이메일+보안답이 모두 일치해야 통과.
     * 통과 시 새 비밀번호를 그 자리에서 입력받기 위한 단기(10분) 토큰을 내려준다.
     */
    @Transactional(readOnly = true)
    public FindPasswordVerifyResponse verifyFindPasswordIdentity(FindPasswordVerifyRequest request) {
        JipdaumUser user = jipdaumUserRepository
                .findByNicknameAndEmail(request.nickname().trim(), request.email().trim())
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "일치하는 회원 정보가 없습니다."));

        if (!matchesAnswer(request.securityAnswer(), user.getSecurityAnswer())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 질문 답변이 일치하지 않습니다.");
        }
        return new FindPasswordVerifyResponse(jwtTokenProvider.generatePasswordResetToken(user.getEmail()));
    }

    /** 본인확인을 통과해 받은 단기 토큰으로 실제 비밀번호를 변경한다. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.resetToken();
        if (!JwtTokenProvider.TYPE_PW_RESET.equals(jwtTokenProvider.getType(token))
                || !jwtTokenProvider.validate(token)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "본인확인이 만료되었습니다. 처음부터 다시 시도해주세요.");
        }
        String email = jwtTokenProvider.getEmail(token);
        JipdaumUser user = jipdaumUserRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "회원을 찾을 수 없습니다."));

        validatePasswordStrength(request.newPassword(), user.getEmail(), user.getNickname());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        jipdaumUserRepository.save(user);
    }

    /** 로그인한 회원이 보안 질문을 최초 설정하거나 바꾼다(로그인 직후 안내 화면, 마이페이지 양쪽에서 재사용). */
    @Transactional
    public void updateSecurityQa(SecurityQaRequest request) {
        if (!SECURITY_QUESTIONS.contains(request.securityQuestion())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "보안 질문을 목록에서 선택해주세요.");
        }
        JipdaumUser user = currentUserProvider.getCurrentUser();
        user.setSecurityQuestion(request.securityQuestion());
        user.setSecurityAnswer(passwordEncoder.encode(normalizeAnswer(request.securityAnswer())));
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
