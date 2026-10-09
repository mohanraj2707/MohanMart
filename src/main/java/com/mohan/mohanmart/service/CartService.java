package com.mohan.mohanmart.service;

import com.mohan.mohanmart.dto.CartItemDTO;
import com.mohan.mohanmart.exception.AppException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface for managing user shopping cart and calculations.
 */
public interface CartService {

    /**
     * Retrieves cart items and computes running totals for a buyer.
     *
     * @param userId Buyer ID
     * @return List of CartItemDTOs
     * @throws AppException if retrieval fails
     */
    List<CartItemDTO> getCart(Long userId) throws AppException;

    /**
     * Calculates total cart price.
     *
     * @param userId Buyer ID
     * @return Total price of cart
     * @throws AppException if calculation fails
     */
    BigDecimal calculateCartTotal(Long userId) throws AppException;

    /**
     * Adds an item to the shopping cart with stock validation.
     *
     * @param userId    Buyer ID
     * @param productId Product ID
     * @param quantity  Requested quantity
     * @throws AppException if stock is insufficient or validation fails
     */
    void addToCart(Long userId, Long productId, int quantity) throws AppException;

    /**
     * Updates the quantity of a cart item with stock check.
     *
     * @param cartItemId Cart item ID
     * @param quantity   New quantity
     * @throws AppException if stock is insufficient
     */
    void updateQuantity(Long cartItemId, int quantity) throws AppException;

    /**
     * Updates the quantity of a cart item after verifying ownership by the specified user.
     *
     * @param userId     Authenticated buyer ID
     * @param cartItemId Cart item ID
     * @param quantity   New quantity
     * @throws AppException if stock is insufficient or item belongs to another user
     */
    default void updateQuantity(Long userId, Long cartItemId, int quantity) throws AppException {
        updateQuantity(cartItemId, quantity);
    }

    /**
     * Removes an item from the cart.
     *
     * @param cartItemId Cart item ID
     * @throws AppException if operation fails
     */
    void removeFromCart(Long cartItemId) throws AppException;

    /**
     * Removes an item from the cart after verifying ownership by the specified user.
     *
     * @param userId     Authenticated buyer ID
     * @param cartItemId Cart item ID
     * @throws AppException if item belongs to another user or operation fails
     */
    default void removeFromCart(Long userId, Long cartItemId) throws AppException {
        removeFromCart(cartItemId);
    }

    /**
     * Empties the user's cart.
     *
     * @param userId Buyer ID
     * @throws AppException if operation fails
     */
    void clearCart(Long userId) throws AppException;
}
