package com.challengehub.controller;

import com.challengehub.dto.request.LoginRequest;
import com.challengehub.dto.request.RegisterRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.AuthResponse;
import com.challengehub.security.JwtProperties;
import com.challengehub.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.register(request);
        attachRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.status(201).body(ApiResponse.success(result.response()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.login(request);
        attachRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "ch_refresh_token", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        attachRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) String accessToken,
            @CookieValue(name = "ch_refresh_token", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        authService.logout(accessToken, refreshToken);
        clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Dang xuat thanh cong"));
    }

    private void attachRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(jwtProperties.getCookiePath())
                .maxAge(jwtProperties.getRefreshTokenExpirationMs() / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(jwtProperties.getCookiePath())
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
