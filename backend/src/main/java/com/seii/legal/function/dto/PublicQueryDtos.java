package com.seii.legal.function.dto;

import java.util.List;

public final class PublicQueryDtos {

    private PublicQueryDtos() {
    }

    public record UserOption(
            String id,
            String name,
            String identity
    ) {
    }

    public record UserListResponse(List<UserOption> users) {
    }

    public record GuideOption(
            String version,
            String importedAt,
            String operator,
            String scope
    ) {
    }

    public record GuideOptionListResponse(List<GuideOption> versions) {
    }
}
