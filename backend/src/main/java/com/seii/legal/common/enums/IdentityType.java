package com.seii.legal.common.enums;

public enum IdentityType {
    ADMIN("管理员", SystemType.ADMIN),
    TEACHING_ADMIN("教学系统管理员", SystemType.TEACHING),
    TASK_MANAGER("任务管理者", SystemType.ANNOTATION),
    TEACHER("教师", SystemType.TEACHING),
    ANNOTATOR("标注员", SystemType.ANNOTATION),
    STUDENT("学生", SystemType.TEACHING),
    ARBITER("仲裁员", SystemType.ANNOTATION),
    EXPERT("领域专家", SystemType.ANNOTATION);

    private final String displayName;
    private final SystemType defaultSystem;

    IdentityType(String displayName, SystemType defaultSystem) {
        this.displayName = displayName;
        this.defaultSystem = defaultSystem;
    }

    public String displayName() {
        return displayName;
    }

    public SystemType defaultSystem() {
        return defaultSystem;
    }
}
