package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;

/**
 * 策略模式：按登录身份分派“我的任务”数据查询。
 */
public interface TaskQueryStrategy {

    IdentityType supportedIdentity();

    PageResult<AnnotationTask> queryMyTasks(String accountId, int page, int size);
}
