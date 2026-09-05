package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCoupon;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.EmailOtpRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUserRepository;
import com.jipdaum_spring.domain.token.BlacklistedTokenRepository;
import com.jipdaum_spring.dto.auth.RegisterRequest;
import com.jipdaum_spring.dto.auth.RegisterResponse;
import com.jipdaum_spring.security.CurrentUserProvider;
import com.jipdaum_spring.security.JipdaumUserProvisioner;
import com.jipdaum_spring.security.captcha.HCaptchaService;
import com.jipdaum_spring.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

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

    @Test
    void 회원가입에_성공하면_웰컴_쿠폰이_계정에_지급된다() {
        UserAuthService service = new UserAuthService(
                jipdaumUserRepository, emailOtpRepository, blacklistedTokenRepository,
                couponRepository, userCouponRepository, jipdaumUserProvisioner, jwtTokenProvider,
                passwordEncoder, hCaptchaService, currentUserProvider, mailSender);

        JipdaumUser newUser = new JipdaumUser();
        ReflectionTestUtils.setField(newUser, "id", 1L);
        ReflectionTestUtils.setField(newUser, "email", "new@jipdaum.com");

        Coupon welcome10 = new Coupon();
        welcome10.setCode("WELCOME10");
        welcome10.setDiscountType("PERCENT");
        welcome10.setDiscountValue(10);
        Coupon welcome1man = new Coupon();
        welcome1man.setCode("WELCOME1MAN");
        welcome1man.setDiscountType("FIXED");
        welcome1man.setDiscountValue(10000);

        when(hCaptchaService.verify(anyString())).thenReturn(true);
        when(jipdaumUserRepository.existsByNickname("새회원")).thenReturn(false);
        when(jipdaumUserRepository.existsByEmail("new@jipdaum.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jipdaumUserRepository.findAllByUsernameIgnoreCase(anyString())).thenReturn(List.of());
        when(jipdaumUserRepository.findByEmail("new@jipdaum.com")).thenReturn(Optional.of(newUser));
        when(couponRepository.findAllByCodeInAndIsActiveTrue(any())).thenReturn(List.of(welcome10, welcome1man));

        RegisterResponse response = service.register(new RegisterRequest(
                "새회원", "new@jipdaum.com", "password123", "password123", "captcha-token"));

        ArgumentCaptor<List<UserCoupon>> savedCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(userCouponRepository).saveAll(savedCaptor.capture());
        List<UserCoupon> saved = savedCaptor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(uc -> {
            assertThat(uc.getUser()).isEqualTo(newUser);
            assertThat(uc.getIsUsed()).isFalse();
        });
        assertThat(response.coupons()).hasSize(2);
        assertThat(response.coupons()).extracting("code")
                .containsExactlyInAnyOrder("WELCOME10", "WELCOME1MAN");
    }
}
