package com.seii.legal.function.controller;

import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.ApplyCommand;
import com.seii.legal.function.dto.ApplyResult;
import com.seii.legal.function.dto.LoginCommand;
import com.seii.legal.function.dto.OptionsView;
import com.seii.legal.function.dto.SelectCommand;
import com.seii.legal.function.dto.SessionView;
import com.seii.legal.function.service.AuthAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/auth")
public class AuthController {

    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    @PostMapping("/login")
    public AppResult<SessionView> login(@RequestBody LoginCommand command) {
        return authAppService.login(command);
    }

    @PostMapping("/select")
    public AppResult<SessionView> select(
            @RequestHeader("Authorization") String credential,
            @RequestBody SelectCommand command) {
        return authAppService.select(credential, command);
    }

    @PostMapping("/logout")
    public AppResult<Void> logout(@RequestHeader("Authorization") String credential) {
        return authAppService.logout(credential);
    }

    @PostMapping("/apply")
    public AppResult<ApplyResult> apply(@RequestBody ApplyCommand command) {
        return authAppService.apply(command);
    }

    @GetMapping("/options")
    public AppResult<OptionsView> options(@RequestHeader("Authorization") String credential) {
        return authAppService.options(credential);
    }
}
