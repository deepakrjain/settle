package com.settle.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        User registeredUser = userService.register(
            request.getEmail(), 
            request.getPassword(), 
            request.getDisplayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(registeredUser));
    }

    @GetMapping("/me")
    public ResponseEntity<String> getMe() {
        // Placeholder for Phase 2 authentication wiring
        return ResponseEntity.ok("Current user details will be here");
    }
}
