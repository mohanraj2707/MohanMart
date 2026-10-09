package com.mohan.mohanmart.dao;

import com.mohan.mohanmart.dao.impl.CartDAOImpl;
import com.mohan.mohanmart.dao.impl.OrderDAOImpl;
import com.mohan.mohanmart.dao.impl.OrderItemDAOImpl;
import com.mohan.mohanmart.dao.impl.ProductDAOImpl;
import com.mohan.mohanmart.dao.impl.ReviewDAOImpl;
import com.mohan.mohanmart.dao.impl.UserDAOImpl;

/**
 * Factory pattern implementation providing JDBC DAO instances for MohanMart domain entities.
 * Decouples service and controller layers from concrete JDBC DAO classes.
 */
public final class DaoFactory {

    private DaoFactory() {
        // Prevent instantiation
    }

    /**
     * @return {@link UserDAO} instance
     */
    public static UserDAO getUserDAO() {
        return new UserDAOImpl();
    }

    /**
     * @return {@link ProductDAO} instance
     */
    public static ProductDAO getProductDAO() {
        return new ProductDAOImpl();
    }

    /**
     * @return {@link CartDAO} instance
     */
    public static CartDAO getCartDAO() {
        return new CartDAOImpl();
    }

    /**
     * @return {@link OrderDAO} instance
     */
    public static OrderDAO getOrderDAO() {
        return new OrderDAOImpl();
    }

    /**
     * @return {@link OrderItemDAO} instance
     */
    public static OrderItemDAO getOrderItemDAO() {
        return new OrderItemDAOImpl();
    }

    /**
     * @return {@link ReviewDAO} instance
     */
    public static ReviewDAO getReviewDAO() {
        return new ReviewDAOImpl();
    }
}
