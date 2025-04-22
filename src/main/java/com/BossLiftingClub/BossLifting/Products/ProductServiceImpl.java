package com.BossLiftingClub.BossLifting.Products;

import com.google.api.client.util.Value;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceItem;
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
        return productRepository.save(product); // Just save to DB
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

        productRepository.deleteById(id);
    }

    public String createInvoiceForUser(Long productId, String stripeCustomerId, int quantity) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        try {
            // Create invoice item
            Map<String, Object> invoiceItemParams = new HashMap<>();
            invoiceItemParams.put("customer", stripeCustomerId);
            invoiceItemParams.put("amount", (long) (product.getPrice() * 100) * quantity); // cents
            invoiceItemParams.put("currency", "usd");
            invoiceItemParams.put("description", product.getName());

            InvoiceItem.create(invoiceItemParams);

            // Create invoice
            Map<String, Object> invoiceParams = new HashMap<>();
            invoiceParams.put("customer", stripeCustomerId);
            invoiceParams.put("collection_method", "charge_automatically");

            Invoice invoice = Invoice.create(invoiceParams);
            invoice.finalizeInvoice();

            return invoice.getHostedInvoiceUrl(); // or invoice.getId() if you prefer

        } catch (StripeException e) {
            throw new RuntimeException("Stripe invoice error: " + e.getMessage());
        }
    }
}
