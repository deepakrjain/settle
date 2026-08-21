package com.settle.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
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
    public ResponseEntity<UserResponse> getMe() {
        // The JwtAuthenticationFilter placed the userId (UUID) as the principal
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
