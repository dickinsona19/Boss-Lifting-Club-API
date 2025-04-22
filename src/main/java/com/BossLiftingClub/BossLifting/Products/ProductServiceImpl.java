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
            long unitAmount = (long) (product.getPrice() * 100);
            long totalAmount = unitAmount * quantity;

            String taxRateId = "txr_1RF33tGHcVHSTvgIzTwKENXt"; // Stripe Tax Rate ID

            // 1️⃣ Create invoice item
            Map<String, Object> invoiceItemParams = new HashMap<>();
            invoiceItemParams.put("customer", stripeCustomerId);
            invoiceItemParams.put("amount", totalAmount);
            invoiceItemParams.put("currency", "usd");
            invoiceItemParams.put("description", product.getName());
            invoiceItemParams.put("tax_rates", List.of(taxRateId));
            InvoiceItem.create(invoiceItemParams);

            // 2️⃣ Create and finalize invoice — Stripe will auto-charge default card here
            Map<String, Object> invoiceParams = new HashMap<>();
            invoiceParams.put("customer", stripeCustomerId);
            invoiceParams.put("collection_method", "charge_automatically"); // 💳 charges default card
            Invoice invoice = Invoice.create(invoiceParams);
            invoice = invoice.finalizeInvoice(); // 💥 This triggers the charge

            // 3️⃣ Transfer 4% to connected account — do this inside webhook after payment
            long feeAmount = (long) (totalAmount * 0.04);
            Map<String, Object> transferParams = new HashMap<>();
            transferParams.put("amount", feeAmount);
            transferParams.put("currency", "usd");
            transferParams.put("destination", "acct_1RDvRj4gikNsBARu");
            transferParams.put("transfer_group", invoice.getId());

            // ⚠️ Only call this after payment confirmation in webhook
            // Transfer.create(transferParams);

            return invoice.getHostedInvoiceUrl();

        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }
}
