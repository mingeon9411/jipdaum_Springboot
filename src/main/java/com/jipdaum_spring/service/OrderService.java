package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.coupon.Coupon;
import com.jipdaum_spring.domain.coupon.CouponRepository;
import com.jipdaum_spring.domain.coupon.UserCoupon;
import com.jipdaum_spring.domain.coupon.UserCouponRepository;
import com.jipdaum_spring.domain.order.*;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.product.ProductOptionRepository;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.common.MessageResponse;
import com.jipdaum_spring.dto.order.CancelOrderResponse;
import com.jipdaum_spring.dto.order.CreateOrderRequest;
import com.jipdaum_spring.dto.order.CreateOrderResponse;
import com.jipdaum_spring.dto.order.OrderHistoryResponse;
import com.jipdaum_spring.dto.order.OrderItemRequest;
import com.jipdaum_spring.dto.order.OrderTrackingResponse;
import com.jipdaum_spring.dto.order.PaymentReadyRequest;
import com.jipdaum_spring.dto.order.PaymentReadyResponse;
import com.jipdaum_spring.dto.order.PaymentVerifyRequest;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CartRepository cartRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TrackingProvider trackingProvider;

    private JipdaumUser getCurrentUser() {
        return currentUserProvider.getCurrentUser();
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        JipdaumUser user = getCurrentUser();

        record Resolved(Product product, ProductOption option, int qty, int unitPrice) {}
        List<Resolved> resolved = new ArrayList<>();

        for (OrderItemRequest item : request.items()) {
            int qty = item.quantityOrDefault();

            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
            ProductOption option = item.optionId() != null
                    ? productOptionRepository.findById(item.optionId()).orElse(null) : null;

            if (product.getBasePrice() == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "'" + product.getName() + "' 가격 정보가 올바르지 않습니다.");

            if (option != null) {
                if (option.getStockCount() == null)
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "'" + product.getName() + "' 재고 정보가 올바르지 않습니다.");
                if (option.getStockCount() < qty)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "'" + product.getName() + "' 재고가 부족합니다.");
            }

            int extraPrice = option != null && option.getExtraPrice() != null ? option.getExtraPrice() : 0;
            int unitPrice = product.getBasePrice() + extraPrice;
            resolved.add(new Resolved(product, option, qty, unitPrice));
        }

        int totalAmount = resolved.stream().mapToInt(r -> r.unitPrice() * r.qty()).sum();

        // 쿠폰 처리
        String couponCode = request.couponCode() != null ? request.couponCode().strip().toUpperCase() : "";
        int discountAmount = 0;
        Coupon appliedCoupon = null;
        UserCoupon appliedUserCoupon = null;

        if (!couponCode.isEmpty()) {
            Optional<UserCoupon> ucOpt = userCouponRepository
                    .findByUserAndCoupon_CodeAndIsUsedFalse(user, couponCode);
            if (ucOpt.isPresent()) {
                appliedUserCoupon = ucOpt.get();
                appliedCoupon = appliedUserCoupon.getCoupon();
            } else {
                appliedCoupon = couponRepository.findByCodeAndIsPersonalFalse(couponCode).orElse(null);
            }
            if (appliedCoupon == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 쿠폰입니다.");
            if (!appliedCoupon.isValid())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
            discountAmount = appliedCoupon.calcDiscount(totalAmount);
        }

        int finalAmount = Math.max(0, totalAmount - discountAmount);

        Order order = Order.builder()
                .user(user).totalAmount(finalAmount).discountAmount(discountAmount)
                .coupon(appliedCoupon).status("PENDING").shippingAddr(request.shippingAddr())
                .build();
        orderRepository.save(order);

        for (Resolved r : resolved) {
            orderItemRepository.save(OrderItem.builder()
                    .order(order).product(r.product()).option(r.option())
                    .quantity(r.qty()).orderedPrice(r.unitPrice()).build());
        }

        // usageLimit/1인당 사용 여부는 DB 레벨 조건부 UPDATE로 원자적으로 확정한다.
        // 동시 요청이 같은 쿠폰을 함께 통과시키는 race condition을 막기 위함이며,
        // 실패 시 예외로 트랜잭션 전체(주문 생성 포함)를 롤백시킨다.
        if (appliedCoupon != null) {
            int updated = couponRepository.incrementUsedCountIfAvailable(appliedCoupon.getId());
            if (updated == 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "쿠폰 사용 가능 횟수를 초과했습니다.");
        }
        if (appliedUserCoupon != null) {
            int updated = userCouponRepository.markUsedIfAvailable(appliedUserCoupon.getId(), LocalDateTime.now());
            if (updated == 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다.");
        }

        // 주문된 상품을 장바구니에서 자동 제거
        for (Resolved r : resolved) {
            cartRepository.findByUserAndProductAndOption(user, r.product(), r.option())
                    .ifPresent(cartRepository::delete);
        }

        return new CreateOrderResponse(order.getId(), order.getTotalAmount());
    }

    @Transactional
    public PaymentReadyResponse readyPayment(PaymentReadyRequest request) {
        JipdaumUser user = getCurrentUser();
        String method = request.method().toUpperCase();

        Order order = orderRepository.findByIdAndUser(request.orderId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!"PENDING".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 준비 중인 주문이 아닙니다.");
        if (paymentRepository.findByOrder(order).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 결제가 진행된 주문입니다.");

        Payment payment = Payment.builder()
                .order(order).user(user).method(method).status("PENDING").amount(order.getTotalAmount())
                .build();
        paymentRepository.save(payment);

        return new PaymentReadyResponse(payment.getMerchantUid(), payment.getAmount());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public MessageResponse verifyPayment(PaymentVerifyRequest request) {
        JipdaumUser user = getCurrentUser();

        Payment payment = paymentRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (payment.getUser() == null || !payment.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 결제만 확인할 수 있습니다.");

        if ("SUCCESS".equals(payment.getStatus()))
            return new MessageResponse("이미 처리된 결제입니다.");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneApiSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map portoneData;
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    "https://api.portone.io/payments/" + request.paymentId(),
                    HttpMethod.GET, entity, Map.class);
            portoneData = res.getBody();
        } catch (Exception e) {
            log.warn("PortOne 결제 검증 API 호출 실패 - paymentId={}", request.paymentId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne 결제 검증에 실패했습니다.");
        }

        String portoneStatus = portoneData != null ? (String) portoneData.get("status") : null;
        Map amountMap = portoneData != null ? (Map) portoneData.get("amount") : null;
        Integer total = amountMap != null ? ((Number) amountMap.get("total")).intValue() : null;

        if (!"PAID".equals(portoneStatus))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다.");
        if (total == null || !total.equals(payment.getAmount()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");

        payment.complete(request.paymentId());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.complete();
        order.assignTracking("CJ대한통운", generateTrackingNumber(order.getId()));
        orderRepository.save(order);

        // 실제 돈이 오간 뒤이므로 재고가 이미 바닥났더라도 주문 자체는 되돌리지 않는다(환불 절차 없음).
        // 다만 이후 주문의 재고 확인이 정확하도록 원자적으로 차감하고, 소진된 경우 운영 로그로 남긴다.
        for (OrderItem item : order.getItems()) {
            if (item.getOption() == null) continue;
            int updated = productOptionRepository.decrementStockIfAvailable(item.getOption().getId(), item.getQuantity());
            if (updated == 0) {
                log.warn("재고 부족 상태에서 결제가 완료됨 - orderId={}, optionId={}, qty={}",
                        order.getId(), item.getOption().getId(), item.getQuantity());
            }
        }

        return new MessageResponse("결제가 완료되었습니다.");
    }

    @Transactional
    public CancelOrderResponse cancelOrder(Long orderId) {
        JipdaumUser user = getCurrentUser();
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!"PENDING".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입금대기 상태의 주문만 취소할 수 있습니다.");
        order.cancel();
        orderRepository.save(order);
        return new CancelOrderResponse("주문이 취소되었습니다.", orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getOrderHistory() {
        JipdaumUser user = getCurrentUser();
        return orderRepository.findByUserOrderByOrderDateDesc(user).stream()
                .map(OrderHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse getTracking(Long orderId) {
        JipdaumUser user = getCurrentUser();
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if ("PENDING".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송 조회가 가능한 주문이 아닙니다.");
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "아직 발급된 운송장 정보가 없습니다.");

        return trackingProvider.getTracking(order);
    }

    // 실제 택배사 API 연동 전까지 쓰는 임시 운송장 번호 포맷 (CJ대한통운 12자리 형식을 흉내냄)
    private String generateTrackingNumber(Long orderId) {
        return String.format("68%010d", orderId);
    }
}