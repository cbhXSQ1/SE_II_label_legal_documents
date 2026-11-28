package com.seii.legal.common.enums;

public enum SystemType {
    ADMIN("管理后台"),
    ANNOTATION("普通标注系统"),
    TEACHING("教学系统");

    private final String displayName;

    SystemType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
