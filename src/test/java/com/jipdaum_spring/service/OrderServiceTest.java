package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.domain.order.*;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.product.ProductOptionRepository;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.dto.order.CreateOrderRequest;
import com.jipdaum_spring.dto.order.CreateOrderResponse;
import com.jipdaum_spring.dto.order.OrderItemRequest;
import com.jipdaum_spring.dto.order.PaymentVerifyRequest;
import com.jipdaum_spring.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private OrderService orderService;

    private JipdaumUser user;

    @BeforeEach
    void setUp() {
        user = new JipdaumUser();
        ReflectionTestUtils.setField(user, "id", 1L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(user);

        // save()는 호출된 엔티티를 그대로 돌려주는 것으로 충분하다.
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 100L);
            return order;
        });
        lenient().when(productOptionRepository.decrementStockIfAvailable(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(1);
    }

    private Product productWithPrice(Integer basePrice) {
        return Product.builder().name("상품").basePrice(basePrice).build();
    }

    private ProductOption optionOf(Product product, Integer extraPrice, Integer stockCount) {
        ProductOption option = new ProductOption();
        ReflectionTestUtils.setField(option, "id", 10L);
        ReflectionTestUtils.setField(option, "product", product);
        ReflectionTestUtils.setField(option, "extraPrice", extraPrice);
        ReflectionTestUtils.setField(option, "stockCount", stockCount);
        return option;
    }

    @Test
    void 옵션이_있으면_기본가격에_추가금액을_더해_총액을_계산한다() {
        Product product = productWithPrice(10000);
        ProductOption option = optionOf(product, 2000, 5);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionRepository.findById(10L)).thenReturn(Optional.of(option));

        CreateOrderRequest request = new CreateOrderRequest(
                "서울시 어딘가", null, List.of(new OrderItemRequest(1L, 10L, 2)));

        CreateOrderResponse response = orderService.createOrder(request);

        // (10000 + 2000) * 2 = 24000
        assertThat(response.totalAmount()).isEqualTo(24000);
    }

    @Test
    void 상품_가격정보가_없으면_500대신_명확한_에러를_던진다() {
        Product product = productWithPrice(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        CreateOrderRequest request = new CreateOrderRequest(
                "서울시 어딘가", null, List.of(new OrderItemRequest(1L, null, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("가격 정보가 올바르지 않습니다");
    }

    @Test
    void 옵션_재고정보가_없으면_명확한_에러를_던진다() {
        Product product = productWithPrice(10000);
        ProductOption option = optionOf(product, 0, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionRepository.findById(10L)).thenReturn(Optional.of(option));

        CreateOrderRequest request = new CreateOrderRequest(
                "서울시 어딘가", null, List.of(new OrderItemRequest(1L, 10L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("재고 정보가 올바르지 않습니다");
    }

    @Test
    void 재고보다_많은_수량을_주문하면_실패한다() {
        Product product = productWithPrice(10000);
        ProductOption option = optionOf(product, 0, 1);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productOptionRepository.findById(10L)).thenReturn(Optional.of(option));

        CreateOrderRequest request = new CreateOrderRequest(
                "서울시 어딘가", null, List.of(new OrderItemRequest(1L, 10L, 5)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    void 쿠폰_사용가능횟수를_다른_요청이_먼저_소진했으면_주문이_실패한다() {
        Product product = productWithPrice(10000);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Coupon coupon = new Coupon();
        ReflectionTestUtils.setField(coupon, "id", 5L);
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(1000);
        coupon.setIsActive(true);

        when(userCouponRepository.findByUserAndCoupon_CodeAndIsUsedFalse(user, "WELCOME"))
                .thenReturn(Optional.empty());
        when(couponRepository.findByCodeAndIsPersonalFalse("WELCOME")).thenReturn(Optional.of(coupon));
        // 원자적 조건부 UPDATE가 0건 갱신 -> 다른 트랜잭션이 먼저 usageLimit을 소진한 상황
        when(couponRepository.incrementUsedCountIfAvailable(5L)).thenReturn(0);

        CreateOrderRequest request = new CreateOrderRequest(
                "서울시 어딘가", "welcome", List.of(new OrderItemRequest(1L, null, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("쿠폰 사용 가능 횟수를 초과했습니다");
    }

    @Test
    void 본인_결제가_아니면_결제확인이_거부된다() {
        JipdaumUser otherUser = new JipdaumUser();
        ReflectionTestUtils.setField(otherUser, "id", 999L);

        Payment payment = Payment.builder()
                .order(null).user(otherUser).method("CARD").status("PENDING").amount(10000)
                .build();

        when(paymentRepository.findByMerchantUidForUpdate("uid-1")).thenReturn(Optional.of(payment));

        PaymentVerifyRequest request = new PaymentVerifyRequest("pay-1", "uid-1");

        assertThatThrownBy(() -> orderService.verifyPayment(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("본인 결제만 확인할 수 있습니다");
    }
}
