package com.flowBoard.auth_service.dto;

import com.flowBoard.auth_service.entity.ROLE;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private ROLE role;
    private boolean isActive;
    private LocalDateTime createdAt;
}
