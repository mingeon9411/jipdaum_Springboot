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
import com.jipdaum_spring.domain.user.JipdaumUser;
import com.jipdaum_spring.domain.user.JipdaumUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

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
    private final JipdaumUserRepository jipdaumUserRepository;
    private final CartRepository cartRepository;

    private JipdaumUser getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<JipdaumUser> byEmail = jipdaumUserRepository.findByEmail(email);
        if (!email.contains("@")) {
            Optional<JipdaumUser> djangoUser = jipdaumUserRepository.findAllByUsernameIgnoreCase(email)
                    .stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().contains("@"))
                    .findFirst();
            if (djangoUser.isPresent()) return djangoUser.get();
        }
        return byEmail.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        String shippingAddr = (String) body.get("shipping_addr");
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");

        if (shippingAddr == null || shippingAddr.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지를 입력해주세요.");
        if (itemsData == null || itemsData.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문 상품이 없습니다.");

        record Resolved(Product product, ProductOption option, int qty, int unitPrice) {}
        List<Resolved> resolved = new ArrayList<>();

        for (Map<String, Object> item : itemsData) {
            Long productId = Long.parseLong(item.get("product_id").toString());
            Object optRaw = item.get("option_id");
            Long optionId = optRaw != null ? Long.parseLong(optRaw.toString()) : null;
            int qty = Integer.parseInt(item.getOrDefault("quantity", 1).toString());

            if (qty < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
            ProductOption option = optionId != null
                    ? productOptionRepository.findById(optionId).orElse(null) : null;

            if (option != null && option.getStockCount() < qty)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "'" + product.getName() + "' 재고가 부족합니다.");

            int unitPrice = product.getBasePrice() + (option != null ? option.getExtraPrice() : 0);
            resolved.add(new Resolved(product, option, qty, unitPrice));
        }

        int totalAmount = resolved.stream().mapToInt(r -> r.unitPrice() * r.qty()).sum();

        // 쿠폰 처리
        String couponCode = body.containsKey("coupon_code")
                ? body.get("coupon_code").toString().strip().toUpperCase() : "";
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
                .coupon(appliedCoupon).status("PENDING").shippingAddr(shippingAddr)
                .build();
        orderRepository.save(order);

        for (Resolved r : resolved) {
            orderItemRepository.save(OrderItem.builder()
                    .order(order).product(r.product()).option(r.option())
                    .quantity(r.qty()).orderedPrice(r.unitPrice()).build());
        }

        if (appliedCoupon != null) {
            appliedCoupon.setUsedCount(appliedCoupon.getUsedCount() + 1);
            couponRepository.save(appliedCoupon);
        }
        if (appliedUserCoupon != null) {
            appliedUserCoupon.setIsUsed(true);
            appliedUserCoupon.setUsedAt(LocalDateTime.now());
            userCouponRepository.save(appliedUserCoupon);
        }

        // 주문된 상품을 장바구니에서 자동 제거
        for (Resolved r : resolved) {
            cartRepository.findByUserAndProductAndOption(user, r.product(), r.option())
                    .ifPresent(cartRepository::delete);
        }

        return Map.of("order_id", order.getId(), "total_amount", order.getTotalAmount());
    }

    @Transactional
    public Map<String, Object> processPayment(Long orderId, Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));

        if (!"PENDING".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제할 수 없는 주문 상태입니다.");

        String method = body.get("method") != null ? body.get("method").toString().toUpperCase() : null;
        if (method == null || method.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 수단을 선택해주세요.");

        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .status("PAID")
                .amount(order.getTotalAmount())
                .build();
        paymentRepository.save(payment);

        order.complete();
        orderRepository.save(order);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_id", order.getId());
        m.put("payment_id", payment.getId());
        m.put("method", payment.getMethodDisplay());
        m.put("amount", payment.getAmount());
        m.put("paid_at", payment.getPaidAt());
        return m;
    }

    @Transactional
    public Map<String, Object> readyPayment(Map<String, Object> body) {
        JipdaumUser user = getCurrentUser();
        Long orderId = Long.parseLong(body.get("order_id").toString());
        String method = body.get("method") != null ? body.get("method").toString().toUpperCase() : null;
        if (method == null || method.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 수단을 선택해주세요.");

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!"PENDING".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 준비 중인 주문이 아닙니다.");
        if (paymentRepository.findByOrder(order).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 결제가 진행된 주문입니다.");

        Payment payment = Payment.builder()
                .order(order).user(user).method(method).status("PENDING").amount(order.getTotalAmount())
                .build();
        paymentRepository.save(payment);

        return Map.of("merchant_uid", payment.getMerchantUid(), "amount", payment.getAmount());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyPayment(Map<String, Object> body) {
        String paymentId = body.get("payment_id").toString();
        String merchantUid = body.get("merchant_uid").toString();

        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if ("SUCCESS".equals(payment.getStatus()))
            return Map.of("message", "이미 처리된 결제입니다.");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneApiSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map portoneData;
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    "https://api.portone.io/payments/" + paymentId,
                    HttpMethod.GET, entity, Map.class);
            portoneData = res.getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne 결제 검증에 실패했습니다: " + e.getMessage());
        }

        String portoneStatus = portoneData != null ? (String) portoneData.get("status") : null;
        Map amountMap = portoneData != null ? (Map) portoneData.get("amount") : null;
        Integer total = amountMap != null ? ((Number) amountMap.get("total")).intValue() : null;

        if (!"PAID".equals(portoneStatus))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다.");
        if (total == null || !total.equals(payment.getAmount()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");

        payment.complete(paymentId);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.complete();
        orderRepository.save(order);

        return Map.of("message", "결제가 완료되었습니다.");
    }

    @Transactional
    public Map<String, Object> cancelOrder(Long orderId) {
        JipdaumUser user = getCurrentUser();
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!"PENDING".equals(order.getStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입금대기 상태의 주문만 취소할 수 있습니다.");
        order.cancel();
        orderRepository.save(order);
        return Map.of("message", "주문이 취소되었습니다.", "order_id", orderId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrderHistory() {
        JipdaumUser user = getCurrentUser();
        return orderRepository.findByUserOrderByOrderDateDesc(user).stream().map(order -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", order.getId());
            m.put("order_date", order.getOrderDate());
            m.put("status", order.getStatus());
            m.put("status_display", order.getStatusDisplay());
            m.put("total_amount", order.getTotalAmount());
            m.put("shipping_addr", order.getShippingAddr());

            Payment payment = order.getPayment();
            m.put("payment_method", payment != null ? payment.getMethodDisplay() : null);
            m.put("payment_status", payment != null ? payment.getStatus() : null);
            m.put("paid_at", payment != null ? payment.getPaidAt() : null);

            m.put("items", order.getItems().stream().map(item -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("product_name", item.getProduct().getName());
                im.put("product_image", item.getProduct().getThumbnailUrl());
                im.put("quantity", item.getQuantity());
                im.put("ordered_price", item.getOrderedPrice());
                return im;
            }).toList());

            return (Map<String, Object>) m;
        }).toList();
    }
}
