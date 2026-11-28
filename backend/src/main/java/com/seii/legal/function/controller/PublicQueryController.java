package com.seii.legal.function.controller;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.PublicQueryDtos;
import com.seii.legal.function.service.PublicQueryAppService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class PublicQueryController {

    private final PublicQueryAppService publicQueryAppService;

    @GetMapping("/user/list")
    public AppResult<PublicQueryDtos.UserListResponse> listUsers(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) IdentityType identity,
            @RequestParam(required = false) String keyword) {
        return publicQueryAppService.listUsers(credential, identity, keyword);
    }

    @GetMapping("/guide/list")
    public AppResult<PublicQueryDtos.GuideOptionListResponse> listGuides(
            @RequestHeader("Authorization") String credential) {
        return publicQueryAppService.listGuides(credential);
    }
}
