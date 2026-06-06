package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.dto.AuthResponse;
import com.iitr.ride_management_backend.dto.LoginRequest;
import com.iitr.ride_management_backend.dto.RegisterDriverRequest;
import com.iitr.ride_management_backend.dto.RegisterPassengerRequest;
import com.iitr.ride_management_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/passenger")
    public AuthResponse registerPassenger(@Valid @RequestBody RegisterPassengerRequest request) {
        return authService.registerPassenger(request);
    }

    @PostMapping("/register/driver")
    public AuthResponse registerDriver(@Valid @RequestBody RegisterDriverRequest request) {
        return authService.registerDriver(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
