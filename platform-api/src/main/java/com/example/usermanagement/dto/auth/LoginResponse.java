package com.example.usermanagement.dto.auth;

import com.example.usermanagement.entity.Role;

public class LoginResponse {

    private String accessToken;

    public LoginResponse(
            String accessToken
    ) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}