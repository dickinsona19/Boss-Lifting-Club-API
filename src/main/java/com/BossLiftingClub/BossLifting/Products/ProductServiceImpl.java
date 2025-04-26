package com.BossLiftingClub.BossLifting.Products;

import com.google.api.client.util.Value;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceItem;
import com.stripe.model.Transfer;
import com.stripe.param.InvoiceCreateParams;
import com.stripe.param.InvoiceItemCreateParams;
import com.stripe.param.TransferCreateParams;
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
    @Override
    public String createInvoiceForUser(Long productId, String stripeCustomerId, int quantity) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        try {
            // Validate inputs
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            if (product.getPrice() <= 0) {
                throw new IllegalArgumentException("Product price must be greater than zero");
            }

            long unitAmountCents = (long) (product.getPrice() * 100); // Convert price to cents
            long totalAmountCents = unitAmountCents * quantity; // Total pre-tax amount in cents
            String taxRateId = "txr_1RF33tGHcVHSTvgIzTwKENXt"; // 7.5% tax rate

            // 1️⃣ Create invoice item
            InvoiceItemCreateParams invoiceItemParams = InvoiceItemCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setAmount(totalAmountCents)
                    .setCurrency("usd")
                    .setDescription(product.getName())
                    .setQuantity((long) quantity) // Explicitly set quantity for clarity
                    .addTaxRate(taxRateId)
                    .build();
            InvoiceItem invoiceItem = InvoiceItem.create(invoiceItemParams);

            // 2️⃣ Create invoice
            InvoiceCreateParams invoiceParams = InvoiceCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setCollectionMethod(InvoiceCreateParams.CollectionMethod.CHARGE_AUTOMATICALLY)
                    .setAutoAdvance(true) // Ensures invoice is finalized and paid automatically
                    .build();
            Invoice invoice = Invoice.create(invoiceParams);

            // 3️⃣ Finalize and pay invoice
            invoice = invoice.finalizeInvoice();
            if (invoice.getAmountDue() <= 0) {
                throw new RuntimeException("Invoice has no amount due: check invoice items or payment method");
            }

            // 4️⃣ Verify payment success
            if (!"paid".equals(invoice.getStatus()) || invoice.getCharge() == null) {
                throw new RuntimeException("Invoice payment failed: status is " + invoice.getStatus());
            }

            // 5️⃣ Transfer 4% to Connected Account
            long feeAmountCents = (long) (totalAmountCents * 0.04); // 4% of pre-tax amount
            if (feeAmountCents > 0) {
                // Check available balance
                Balance balance = Balance.retrieve();
                long availableBalance = balance.getAvailable().stream()
                        .filter(b -> "usd".equals(b.getCurrency()))
                        .mapToLong(Balance.Available::getAmount)
                        .sum();
                if (availableBalance < feeAmountCents) {
                    // Optionally queue transfer (implement as discussed previously)
                    throw new RuntimeException("Insufficient balance for transfer: available " + (availableBalance / 100.0) + " USD, needed " + (feeAmountCents / 100.0));
                }

                TransferCreateParams transferParams = TransferCreateParams.builder()
                        .setAmount(feeAmountCents)
                        .setCurrency("usd")
                        .setDestination("acct_1RDvRj4gikNsBARu")
                        .setSourceTransaction(invoice.getCharge()) // Tie transfer to the charge
                        .setTransferGroup(invoice.getId())
                        .build();
                Transfer transfer = Transfer.create(transferParams);
                System.out.println("Transferred " + (feeAmountCents / 100.0) + " USD to Connected Account for invoice " + invoice.getId());
            }

            // 6️⃣ Return hosted invoice URL
            return invoice.getHostedInvoiceUrl();

        } catch (StripeException e) {
            throw new RuntimeException("Stripe error: " + e.getMessage());
        }
    }
}
