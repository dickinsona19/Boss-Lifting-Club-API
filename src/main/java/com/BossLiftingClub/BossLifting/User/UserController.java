package com.BossLiftingClub.BossLifting.User;

import com.BossLiftingClub.BossLifting.User.PasswordAuth.JwtUtil;
import com.stripe.model.Customer;
import com.stripe.model.billingportal.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    private final BarcodeService barcodeService;
    private final UserService userService;

    @Autowired
    public UserController(UserService userService, BarcodeService barcodeService) {
        this.userService = userService;
        this.barcodeService = barcodeService;
    }


    @Autowired
    private JwtUtil jwtUtil;
    @GetMapping("/{id}/media")
    public ResponseEntity<UserMediaDTO> getUserMedia(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(new UserMediaDTO(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping("/signin")
    public ResponseEntity<Map<String, Object>> signIn(@RequestBody Map<String, String> requestBody) {
        try {
            String phoneNumber = requestBody.get("phoneNumber");
            String password = requestBody.get("password");

            if (phoneNumber == null || password == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing phone number or password");
                return ResponseEntity.status(400).body(errorResponse);
            }

            User user = userService.signInWithPhoneNumber(phoneNumber, password);
            String token = jwtUtil.generateToken(phoneNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("user", user); // Use DTO to control serialization
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }


    // Get all users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    //Delete User given Phone Number
    @DeleteMapping("/delete-user")
    public ResponseEntity<Map<String, Object>> deleteUser(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        Optional<User> deletedUser = userService.deleteUserWithPhoneNumber(phoneNumber);

        Map<String, Object> response = new HashMap<>();
        if (deletedUser.isPresent()) {
            response.put("message", "User with ID " + deletedUser.get().getId() + " was deleted successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "User with phone number " + phoneNumber + " not found.");
            return ResponseEntity.status(404).body(response);
        }
    }

    // Get a single user by id
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create a new user
    @PostMapping
    public User createUser(@RequestBody User user) throws Exception {
        return userService.save(user);
    }

    // Update an existing user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userService.findById(id)
                .map(user -> {
                    user.setFirstName(userDetails.getFirstName());
                    user.setLastName(userDetails.getLastName());
                    user.setPassword(userDetails.getPassword());
                    user.setPhoneNumber(userDetails.getPhoneNumber());
                    user.setIsInGoodStanding(userDetails.getIsInGoodStanding());
                    // createdAt usually remains unchanged
                    try {
                        return ResponseEntity.ok(userService.save(user));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/password/{id}")
    public ResponseEntity<User> updateUserPassword(@PathVariable Long id, @RequestBody User userDetails) {
        return userService.findById(id)
                .map(user -> {
                    user.setPassword(userDetails.getPassword());
                    try {
                        return ResponseEntity.ok(userService.save(user));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<?> getUserByBarcode(@PathVariable String barcode) {
        Optional<User> user = userService.getUserByBarcodeToken(barcode);
        if (user.isPresent()) {
            return ResponseEntity.ok(new UserDTO(user.get()));
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "User not found with barcode: " + barcode));
        }
    }


    @GetMapping(value = "/barcode/image/{token}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBarcodeImage(@PathVariable String token) {
        try {
            byte[] image = barcodeService.generateBarcode(token, 300, 300);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("/fullSetupUser")
    public ResponseEntity<Map<String, String>> createUserAndCheckout(@RequestBody UserRequest userRequest) {
        try {
            // 1. Create a Stripe customer
            Map<String, Object> customerParams = new HashMap<>();
            customerParams.put("name", userRequest.getFirstName() + " " + userRequest.getLastName());
            customerParams.put("phone", userRequest.getPhoneNumber());
            Customer customer = Customer.create(customerParams);

            // 2. Create a Checkout session for activation fee and payment method setup
            Map<String, Object> sessionParams = new HashMap<>();
            sessionParams.put("customer", customer.getId());
            sessionParams.put("mode", "payment"); // For one-time charge
            sessionParams.put("payment_method_types", java.util.Arrays.asList("card"));
            sessionParams.put("success_url", "http://localhost:5173/success");
            sessionParams.put("cancel_url", "http://localhost:5173/cancel");

            // Add a one-time activation fee (e.g., $10)
            Map<String, Object> priceData = new HashMap<>();
            priceData.put("currency", "usd");
            priceData.put("unit_amount", 1000); // $10.00 in cents
            priceData.put("product_data", new HashMap<String, Object>() {{
                put("name", "Activation Fee");
            }});

            Map<String, Object> lineItem = new HashMap<>();
            lineItem.put("price_data", priceData);
            lineItem.put("quantity", 1);

            sessionParams.put("line_items", java.util.Arrays.asList(lineItem));

            // Save payment method for future use
            sessionParams.put("payment_intent_data", new HashMap<String, Object>() {{
                put("setup_future_usage", "off_session"); // Saves payment method
            }});

            Session session = Session.create(sessionParams); // Now using com.stripe.model.checkout.Session

            // 3. Return session ID to frontend
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", session.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        }
    }

    // Update user profile picture
    @PostMapping("/{id}/picture")
    public ResponseEntity<UserMediaDTO> updateProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        byte[] profilePicture = file.getBytes();
        return userService.updateProfilePicture(id, profilePicture)
                .map(user -> ResponseEntity.ok(new UserMediaDTO(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/referralCode/{referralCode}")
    public ResponseEntity<User> getUserByReferralCode(@PathVariable String referralCode) {
        User user = userService.getUserByReferralCode(referralCode); // Returns User or null
        if (user != null) {
            return ResponseEntity.ok(user); // 200 OK with user
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    @PutMapping("/referralCode/{referralCode}")
    public ResponseEntity<Void> updateUserReferralCode(
            @PathVariable String referralCode,
            @RequestBody String newReferralCode) {
        boolean updated = userService.updateReferralCode(referralCode, newReferralCode);
        if (updated) {
            return ResponseEntity.ok().build(); // 200 OK
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    @PostMapping("/{id}/waiver")
    public ResponseEntity<UserMediaDTO> saveWaiverSignature(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) throws Exception {
        String base64Signature = requestBody.get("signature");
        User user = userService.saveWaiverSignature(id, base64Signature);
        return ResponseEntity.ok(new UserMediaDTO(user));
    }

}

