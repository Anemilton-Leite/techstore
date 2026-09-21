package com.techstore.api.service;

import com.techstore.api.dto.auth.AuthResponse;
import com.techstore.api.dto.auth.LoginRequest;
import com.techstore.api.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
