package com.seii.legal.function.service;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 策略路由：按 IdentityType 分发到对应的 TaskQueryStrategy。
 */
@Component
public class TaskQueryDispatcher {

    private final Map<IdentityType, TaskQueryStrategy> strategyMap;

    public TaskQueryDispatcher(List<TaskQueryStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(TaskQueryStrategy::supportedIdentity, Function.identity()));
    }

    public PageResult<AnnotationTask> dispatch(IdentityType identity, String accountId, int page, int size) {
        TaskQueryStrategy strategy = strategyMap.get(identity);
        if (strategy == null) {
            return PageResult.empty(page, size);
        }
        return strategy.queryMyTasks(accountId, page, size);
    }
}
