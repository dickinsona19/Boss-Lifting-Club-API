package com.BossLiftingClub.BossLifting.Products;

import com.google.api.client.util.Value;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public List<Products> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Products addProduct(Products product) {
        try {
            // Create product in Stripe
            Map<String, Object> productParams = new HashMap<>();
            productParams.put("name", product.getName());
            productParams.put("description", product.getDefinition());

            Product stripeProduct = Product.create(productParams);

            // Create a Price for the product
            Map<String, Object> priceParams = new HashMap<>();
            priceParams.put("unit_amount", (long)(product.getPrice() * 100)); // Convert to cents
            priceParams.put("currency", "usd");
            priceParams.put("product", stripeProduct.getId());

            Price stripePrice = Price.create(priceParams);

            product.setStripeProductId(stripeProduct.getId());

            return productRepository.save(product);

        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }

    @Override
    public Products updateProduct(Long id, Products updatedProduct) {
        return productRepository.findById(id).map(product -> {
            product.setName(updatedProduct.getName());
            product.setDefinition(updatedProduct.getDefinition());
            product.setPrice(updatedProduct.getPrice());
            product.setCategory(updatedProduct.getCategory());
            product.setImageUrl(updatedProduct.getImageUrl());
            return productRepository.save(product);
        }).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public void deleteProduct(Long id) {
        Products product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        try {
            com.stripe.model.Product stripeProduct = com.stripe.model.Product.retrieve(product.getStripeProductId());
            stripeProduct.delete();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to delete product from Stripe: " + e.getMessage());
        }

        productRepository.deleteById(id);
    }
}
