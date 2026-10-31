package com.sitewatch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterForm {
    @NotBlank(message = "Choose a username.")
    @Size(min = 3, max = 30, message = "Username must be 3 to 30 characters.")
    @Pattern(regexp = "[A-Za-z0-9_.-]*", message = "Use only letters, numbers, dot, dash and underscore.")
    private String username;

    @Email(message = "Enter a valid email address.")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Choose a password.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters.")
    private String password;
}
