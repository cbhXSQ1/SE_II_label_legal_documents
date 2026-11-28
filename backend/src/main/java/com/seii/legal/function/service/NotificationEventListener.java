package com.seii.legal.function.service;

import com.seii.legal.rule.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务提交后统一投递站内消息与邮件；邮件异常独立吞并，不回滚业务主事务。
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainNotification(DomainNotificationEvent event) {
        if (event == null || event.receiverIds() == null || event.receiverIds().isEmpty()) {
            return;
        }
        for (String receiverId : event.receiverIds()) {
            if (receiverId == null || receiverId.isBlank()) {
                continue;
            }
            notificationService.sendInApp(receiverId, event.content(), event.type());
            if (event.sendMail()) {
                try {
                    notificationService.sendMail(receiverId, event.subject(), event.content());
                } catch (Exception ignored) {
                    // 遵循总纲约定：学校邮件服务不可用时保留站内消息，不影响主流程
                }
            }
        }
    }
}
