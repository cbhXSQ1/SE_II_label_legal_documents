package com.seii.legal.function.event;

import com.seii.legal.rule.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 观察者监听：在事务提交后调用 NotificationService 发送站内消息及可选邮件。
 * 遵循总纲约定：邮件服务不可用时吞掉异常，保留站内消息并避免影响主事务结果。
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainNotification(DomainNotificationEvent event) {
        if (event == null || event.receiverIds() == null || event.receiverIds().isEmpty()) {
            return;
        }
        for (String receiverId : event.receiverIds()) {
            if (receiverId == null || receiverId.isBlank()) {
                continue;
            }
            try {
                notificationService.sendInApp(receiverId, event.content(), event.type());
            } catch (Exception ex) {
                log.warn("站内消息发送失败 receiverId={} type={}", receiverId, event.type(), ex);
            }
            if (event.sendMail()) {
                try {
                    boolean ok = notificationService.sendMail(receiverId, event.subject(), event.content());
                    if (!ok) {
                        log.warn("邮件发送返回失败（已保留站内消息） receiverId={} subject={}", receiverId, event.subject());
                    }
                } catch (Exception ex) {
                    log.warn("邮件服务异常（已保留站内消息） receiverId={} subject={}", receiverId, event.subject(), ex);
                }
            }
        }
    }
}
