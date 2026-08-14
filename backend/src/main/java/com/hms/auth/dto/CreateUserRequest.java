package com.hms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(@NotBlank String username,
                                @NotBlank String fullName,
                                @Email String email,
                                String phone,
                                @NotBlank @Size(min = 6) String password,
                                List<String> roleCodes,
                                Long branchId) {
}