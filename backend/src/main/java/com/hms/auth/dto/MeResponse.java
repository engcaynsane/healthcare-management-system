package com.hms.auth.dto;

import java.util.List;

public record MeResponse(Long id, String username, String fullName, String email, String phone,
                         Long branchId, String branchName, List<String> roles, List<String> permissions) {
}