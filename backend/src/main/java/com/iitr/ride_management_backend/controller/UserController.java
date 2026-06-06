package com.iitr.ride_management_backend.controller;

import com.iitr.ride_management_backend.domain.User;
import com.iitr.ride_management_backend.dto.UpdateProfileRequest;
import com.iitr.ride_management_backend.dto.UserResponse;
import com.iitr.ride_management_backend.service.AuthService;
import com.iitr.ride_management_backend.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public UserController(CurrentUserService currentUserService, AuthService authService) {
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me(currentUserService.currentUser());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserService.currentUser();
        return authService.updateProfile(user, request);
    }
}
