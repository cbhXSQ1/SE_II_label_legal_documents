package com.seii.legal.function.dto;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入账号") String account,
            @NotBlank(message = "请输入密码") String password
    ) {
    }

    public record SelectIdentityRequest(
            @NotNull(message = "请选择系统") SystemType system,
            @NotNull(message = "请选择身份") IdentityType identity
    ) {
    }

    public record ApplyAccountRequest(
            @NotBlank(message = "请填写姓名") String name,
            @NotBlank(message = "请填写单位") String unit,
            @NotNull(message = "请选择申请系统") SystemType system,
            @NotNull(message = "请选择申请身份") IdentityType identity,
            String studentNo,
            String teacherNo,
            String note
    ) {
    }

    public record IdentityOptionDto(
            SystemType system,
            IdentityType identity,
            String systemName,
            String identityName,
            String description
    ) {
    }

    public record SessionResponse(
            String credential,
            String accountId,
            String name,
            String unit,
            SystemType system,
            IdentityType identity,
            String systemName,
            String identityName,
            boolean single,
            IdentityOptionDto identityOption,
            List<IdentityOptionDto> options
    ) {
    }

    public record ApplyAccountResponse(String applicationId) {
    }

    public record AuthOptionsResponse(
            String name,
            List<IdentityOptionDto> options,
            SystemType currentSystem,
            IdentityType currentIdentity
    ) {
    }
}
