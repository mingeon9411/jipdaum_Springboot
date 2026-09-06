package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtp;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtpRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.token.BlacklistedTokenRepository;
import com.jipdaum_spring.dto.auth.FindIdVerifyRequest;
import com.jipdaum_spring.dto.auth.FindPasswordVerifyRequest;
import com.jipdaum_spring.exception.AuthException;
import com.jipdaum_spring.security.CurrentUserProvider;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import com.jipdaum_spring.security.captcha.HCaptchaService;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 아이디/비밀번호 찾기(보안질문 본인확인) 핵심 분기 — 답이 틀리면 반드시 막히고,
 * 맞으면 각각 아이디/재설정 토큰을 돌려주는지만 확인한다(UserAuthServiceTest와 별도 파일 —
 * 그 파일은 다른 작업이 진행 중이라 건드리지 않는다).
 */
@ExtendWith(MockitoExtension.class)
class UserAuthServiceSecurityQaTest {

    @Mock private JipdaumUserRepository jipdaumUserRepository;
    @Mock private EmailOtpRepository emailOtpRepository;
    @Mock private BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock private JipdaumUserProvisioner jipdaumUserProvisioner;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HCaptchaService hCaptchaService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private JavaMailSender mailSender;
    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;

    private UserAuthService newService() {
        return new UserAuthService(
                jipdaumUserRepository, emailOtpRepository, blacklistedTokenRepository,
                couponRepository, userCouponRepository, jipdaumUserProvisioner, jwtTokenProvider,
                passwordEncoder, hCaptchaService, currentUserProvider, mailSender);
    }

    private JipdaumUser userWithSecurityAnswer(String storedHash) {
        JipdaumUser user = new JipdaumUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "email", "find@jipdaum.com");
        ReflectionTestUtils.setField(user, "username", "find_user");
        ReflectionTestUtils.setField(user, "nickname", "찾는사람");
        ReflectionTestUtils.setField(user, "securityQuestion", "가장 좋아하는 음식은?");
        ReflectionTestUtils.setField(user, "securityAnswer", storedHash);
        return user;
    }

    @Test
    void 아이디_찾기_보안답이_맞으면_username을_반환한다() {
        UserAuthService service = newService();
        JipdaumUser user = userWithSecurityAnswer("hashed-answer");
        EmailOtp otp = new EmailOtp(1L, "find@jipdaum.com", "123456");
        ReflectionTestUtils.setField(otp, "createdAt", LocalDateTime.now());

        when(jipdaumUserRepository.findByEmail("find@jipdaum.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("떡볶이", "hashed-answer")).thenReturn(true);
        when(emailOtpRepository.findFirstByUserIdAndEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(1L, "find@jipdaum.com", "123456"))
                .thenReturn(Optional.of(otp));

        var response = service.verifyFindId(new FindIdVerifyRequest("find@jipdaum.com", "123456", "떡볶이"));

        assertThat(response.username()).isEqualTo("find_user");
    }

    @Test
    void 아이디_찾기_보안답이_틀리면_코드를_소모하지_않고_거부한다() {
        UserAuthService service = newService();
        JipdaumUser user = userWithSecurityAnswer("hashed-answer");

        when(jipdaumUserRepository.findByEmail("find@jipdaum.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.verifyFindId(new FindIdVerifyRequest("find@jipdaum.com", "123456", "오답")))
                .isInstanceOf(AuthException.class);

        // 답이 틀린 시점에 바로 막혀야 한다 — OTP 조회/소모 쪽으로 넘어가면 안 된다.
        org.mockito.Mockito.verifyNoInteractions(emailOtpRepository);
    }

    @Test
    void 비밀번호_찾기_닉네임_이메일_보안답이_모두_맞아야_재설정_토큰을_받는다() {
        UserAuthService service = newService();
        JipdaumUser user = userWithSecurityAnswer("hashed-answer");

        when(jipdaumUserRepository.findByNicknameAndEmail("찾는사람", "find@jipdaum.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("떡볶이", "hashed-answer")).thenReturn(true);
        when(jwtTokenProvider.generatePasswordResetToken("find@jipdaum.com")).thenReturn("reset-token-abc");

        var response = service.verifyFindPasswordIdentity(
                new FindPasswordVerifyRequest("찾는사람", "find@jipdaum.com", "떡볶이"));

        assertThat(response.resetToken()).isEqualTo("reset-token-abc");
    }

    @Test
    void 비밀번호_찾기_닉네임이_다르면_계정을_못_찾아_거부한다() {
        UserAuthService service = newService();
        when(jipdaumUserRepository.findByNicknameAndEmail("다른닉네임", "find@jipdaum.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyFindPasswordIdentity(
                new FindPasswordVerifyRequest("다른닉네임", "find@jipdaum.com", "떡볶이")))
                .isInstanceOf(AuthException.class);
    }
}
