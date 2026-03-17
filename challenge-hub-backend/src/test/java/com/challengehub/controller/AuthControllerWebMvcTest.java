package com.challengehub.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.challengehub.dto.response.AuthResponse;
import com.challengehub.security.JwtAuthenticationFilter;
import com.challengehub.security.JwtProperties;
import com.challengehub.security.RateLimitingFilter;
import com.challengehub.service.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getCookieName()).thenReturn("ch_refresh_token");
        when(jwtProperties.getCookieSameSite()).thenReturn("Lax");
        when(jwtProperties.getCookiePath()).thenReturn("/api/v1/auth");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtProperties.isCookieSecure()).thenReturn(false);
    }

    @Test
    void registerShouldReturnCreatedResponse() throws Exception {
        AuthResponse response = AuthResponse.register(
                "e748f5a2-0f6a-46d3-bf77-5e5860f0fcb5",
                "tester",
                "tester@example.com",
                "USER",
                Instant.parse("2026-03-13T10:27:12Z"));
        when(authService.register(any())).thenReturn(new AuthService.AuthResult(response, "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("""
                        {
                          "username": "tester",
                          "email": "tester@example.com",
                          "password": "StrongPass1"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("e748f5a2-0f6a-46d3-bf77-5e5860f0fcb5"))
                .andExpect(jsonPath("$.data.username").value("tester"))
                .andExpect(jsonPath("$.data.created_at").value("2026-03-13T10:27:12Z"));
    }
}
