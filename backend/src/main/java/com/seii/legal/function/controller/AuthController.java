package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.AuthDtos;
import com.seii.legal.function.service.AuthAppService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @PostMapping("/login")
    public AppResult<AuthDtos.SessionResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authAppService.login(request);
    }

    @PostMapping("/select")
    public AppResult<AuthDtos.SessionResponse> select(
            @RequestHeader("Authorization") String credential,
            @Valid @RequestBody AuthDtos.SelectIdentityRequest request) {
        return authAppService.selectIdentity(credential, request);
    }

    @PostMapping("/logout")
    public AppResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String credential) {
        return authAppService.logout(credential);
    }

    @PostMapping("/apply")
    public AppResult<AuthDtos.ApplyAccountResponse> apply(@Valid @RequestBody AuthDtos.ApplyAccountRequest request) {
        return authAppService.apply(request);
    }

    @GetMapping("/options")
    public AppResult<AuthDtos.AuthOptionsResponse> options(@RequestHeader("Authorization") String credential) {
        return authAppService.options(credential);
    }
}
