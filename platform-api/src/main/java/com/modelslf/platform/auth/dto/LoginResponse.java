package com.modelslf.platform.auth.dto;

import com.modelslf.platform.auth.domain.Role;

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