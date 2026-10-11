package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String tokenType = "Bearer";

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}
