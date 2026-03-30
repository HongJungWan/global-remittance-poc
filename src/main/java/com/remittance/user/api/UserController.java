package com.remittance.user.api;

import com.remittance.user.api.dto.UpdateProfileRequest;
import com.remittance.user.api.dto.UserResponse;
import com.remittance.user.application.UserService;
import com.remittance.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UUID userId) {
        User user = userService.getUser(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request.displayName());
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
