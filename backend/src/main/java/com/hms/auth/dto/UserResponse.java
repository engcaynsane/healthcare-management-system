package com.hms.auth.dto;

import java.util.List;

public record UserResponse(Long id, String username, String fullName, String email, String phone,
                           boolean active, Long branchId, String branchName, List<String> roles) {
}