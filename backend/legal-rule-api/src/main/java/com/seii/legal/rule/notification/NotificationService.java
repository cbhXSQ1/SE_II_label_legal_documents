package com.seii.legal.rule.notification;

public interface NotificationService {
    String sendInApp(String receiverId, String content, String type);

    boolean sendMail(String receiver, String subject, String content);
}
