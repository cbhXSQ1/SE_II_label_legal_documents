package com.seii.legal.function.event;

import java.util.List;

/**
 * 观察者模式：功能层内部领域通知事件载体，解耦业务主事务与通知派发。
 */
public record DomainNotificationEvent(
        List<String> receiverIds,
        String type,
        String subject,
        String content,
        boolean sendMail
) {
}
