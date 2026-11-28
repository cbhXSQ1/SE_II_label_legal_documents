package com.seii.legal.function.support;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;

/**
 * 功能层内部轻量工具：统一分页默认值与枚举展示名。
 */
public final class FunctionHelpers {

    private FunctionHelpers() {
    }

    public static int normalizePage(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    public static int normalizeSize(Integer size) {
        return (size == null || size < 1) ? 20 : Math.min(size, 200);
    }

    public static String identityName(IdentityType identity) {
        if (identity == null) {
            return "";
        }
        return switch (identity) {
            case ADMIN -> "系统管理员";
            case TEACHING_ADMIN -> "教学系统管理员";
            case TASK_MANAGER -> "任务管理者";
            case TEACHER -> "教师";
            case ANNOTATOR -> "标注员";
            case STUDENT -> "学生";
            case ARBITER -> "仲裁员";
            case EXPERT -> "领域专家";
        };
    }

    public static SystemType defaultSystem(IdentityType identity) {
        if (identity == null) {
            return SystemType.ANNOTATION;
        }
        return switch (identity) {
            case ADMIN -> SystemType.ADMIN;
            case TEACHING_ADMIN, TEACHER, STUDENT -> SystemType.TEACHING;
            case TASK_MANAGER, ANNOTATOR, ARBITER, EXPERT -> SystemType.ANNOTATION;
        };
    }
}
