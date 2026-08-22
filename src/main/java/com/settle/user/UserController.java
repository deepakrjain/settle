package com.settle.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for user registration and profile retrieval")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account with BCrypt password hashing")
    @ApiResponse(responseCode = "201", description = "User successfully registered")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or email already exists")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        User registeredUser = userService.register(
            request.getEmail(),
            request.getPassword(),
            request.getDisplayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(registeredUser));
    }

    @Operation(summary = "Get current user profile", description = "Returns profile details for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized or missing JWT token")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
