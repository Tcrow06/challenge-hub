package com.challengehub.service;

import com.challengehub.dto.request.LoginRequest;
import com.challengehub.dto.request.RegisterRequest;
import com.challengehub.dto.response.AuthResponse;

public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken);

    void logout(String accessToken, String refreshToken);

    record AuthResult(AuthResponse response, String refreshToken) {
    }
}
