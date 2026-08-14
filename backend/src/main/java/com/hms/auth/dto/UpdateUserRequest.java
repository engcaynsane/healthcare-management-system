package com.hms.auth.dto;

import jakarta.validation.constraints.Email;

import java.util.List;

public record UpdateUserRequest(@jakarta.validation.constraints.NotBlank String fullName,
                                @Email String email,
                                String phone,
                                Boolean active,
                                List<String> roleCodes,
                                Long branchId) {
}