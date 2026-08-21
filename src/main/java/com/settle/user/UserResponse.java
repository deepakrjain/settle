package com.settle.user;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String email;
    private String displayName;
    private LocalDateTime createdAt;
    
    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setDisplayName(user.getDisplayName());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
