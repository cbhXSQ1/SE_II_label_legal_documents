package com.seii.legal.function.support;

import com.seii.legal.common.enums.IdentityType;
import com.seii.legal.common.enums.SystemType;
import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.AnnotationTask;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TaskQueryDispatcher {

    private final Map<IdentityType, TaskQueryStrategy> strategyMap = new EnumMap<>(IdentityType.class);

    public TaskQueryDispatcher(List<TaskQueryStrategy> strategies) {
        for (TaskQueryStrategy strategy : strategies) {
            strategyMap.putIfAbsent(strategy.supportedIdentity(), strategy);
        }
    }

    public PageResult<AnnotationTask> dispatch(IdentityType identity, String accountId, SystemType system,
                                               int page, int size) {
        if (identity == null) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        TaskQueryStrategy strategy = strategyMap.get(identity);
        if (strategy == null) {
            return new PageResult<>(List.of(), 0, page, size);
        }
        return strategy.queryMyTasks(accountId, system, page, size);
    }
}
