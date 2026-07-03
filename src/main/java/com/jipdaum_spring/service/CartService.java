package com.jipdaum_spring.service;

import com.jipdaum_spring.domain.order.Cart;
import com.jipdaum_spring.domain.order.CartRepository;
import com.jipdaum_spring.domain.product.Product;
import com.jipdaum_spring.domain.product.ProductOption;
import com.jipdaum_spring.domain.product.ProductOptionRepository;
import com.jipdaum_spring.domain.product.ProductRepository;
import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import com.jipdaum_spring.dto.cart.AddToCartRequest;
import com.jipdaum_spring.dto.cart.CartItemResponse;
import com.jipdaum_spring.dto.cart.CartMutationResult;
import com.jipdaum_spring.dto.cart.DeleteCartRequest;
import com.jipdaum_spring.dto.cart.UpdateCartRequest;
import com.jipdaum_spring.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final CurrentUserProvider currentUserProvider;

    private JipdaumUser getCurrentUser() {
        return currentUserProvider.getCurrentUser();
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart() {
        JipdaumUser user = getCurrentUser();
        return cartRepository.findByUser(user).stream().map(CartItemResponse::from).toList();
    }

    @Transactional
    public CartMutationResult addToCart(AddToCartRequest request) {
        JipdaumUser user = getCurrentUser();
        int quantity = request.quantityOrDefault();

        Product product = productRepository.findById(request.product())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        ProductOption option = request.option() != null
                ? productOptionRepository.findById(request.option())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "옵션을 찾을 수 없습니다."))
                : null;

        Optional<Cart> existing = cartRepository.findByUserAndProductAndOption(user, product, option);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            cartRepository.save(cart);
            return new CartMutationResult("장바구니 수량이 추가되었습니다.", false);
        }

        cartRepository.save(Cart.builder().user(user).product(product).option(option).quantity(quantity).build());
        return new CartMutationResult("장바구니에 담겼습니다.", true);
    }

    @Transactional
    public void updateCart(UpdateCartRequest request) {
        JipdaumUser user = getCurrentUser();
        Cart cart = cartRepository.findByIdAndUser(request.itemId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));
        cart.setQuantity(request.quantity());
        cartRepository.save(cart);
    }

    @Transactional
    public void deleteCart(DeleteCartRequest request) {
        JipdaumUser user = getCurrentUser();
        Cart cart = cartRepository.findByIdAndUser(request.itemId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."));
        cartRepository.delete(cart);
    }
}