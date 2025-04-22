package com.BossLiftingClub.BossLifting.Products;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Products> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping
    public ResponseEntity<Products> addProduct(@RequestBody Products products) {
        return new ResponseEntity<>(productService.addProduct(products), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Products> updateProduct(@PathVariable Long id, @RequestBody Products products) {
        return ResponseEntity.ok(productService.updateProduct(id, products));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/purchaseProduct")
    public String purchaseProduct(
            @RequestParam Long productId,
            @RequestParam String stripeCustomerId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        return productService.createInvoiceForUser(productId, stripeCustomerId, quantity);
    }
}
