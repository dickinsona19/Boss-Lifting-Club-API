package com.BossLiftingClub.BossLifting.Products;

import java.util.List;

public interface ProductService {
    List<Products> getAllProducts();
    Products addProduct(Products products);
    Products updateProduct(Long id, Products products);
    void deleteProduct(Long id);
     String createInvoiceForUser(Long productId, String stripeCustomerId, int quantity);
}
