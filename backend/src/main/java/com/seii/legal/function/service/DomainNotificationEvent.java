package com.seii.legal.function.service;

import java.util.List;

/**
 * 观察者模式：领域通知事件，由功能层 AppService 发布，解耦核心事务与消息/邮件发送。
 */
public record DomainNotificationEvent(
        List<String> receiverIds,
        String type,
        String subject,
        String content,
        boolean sendMail
) {
}
