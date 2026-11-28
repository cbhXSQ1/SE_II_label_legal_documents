package com.seii.legal.function.support;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;

public interface TaskQueryStrategy {

    IdentityType supportedIdentity();

    PageResult<AnnotationTask> queryMyTasks(String accountId, int page, int size);

    default PageResult<AnnotationTask> queryMyTasks(String accountId, SystemType system, int page, int size) {
        return queryMyTasks(accountId, page, size);
    }
}
