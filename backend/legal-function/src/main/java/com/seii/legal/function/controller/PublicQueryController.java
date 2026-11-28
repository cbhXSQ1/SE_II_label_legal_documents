package com.seii.legal.function.controller;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.result.AppResult;
import com.seii.legal.function.dto.publicquery.GuideListResult;
import com.seii.legal.function.dto.publicquery.UserListResult;
import com.seii.legal.function.service.PublicQueryAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class PublicQueryController {

    private final PublicQueryAppService publicQueryAppService;

    public PublicQueryController(PublicQueryAppService publicQueryAppService) {
        this.publicQueryAppService = publicQueryAppService;
    }

    @GetMapping("/user/list")
    public AppResult<UserListResult> listUsers(
            @RequestHeader("Authorization") String credential,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) IdentityType identity,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicQueryAppService.listUsers(credential, keyword, identity, page, size);
    }

    @GetMapping("/guide/list")
    public AppResult<GuideListResult> listGuides(@RequestHeader("Authorization") String credential) {
        return publicQueryAppService.listGuides(credential);
    }
}
