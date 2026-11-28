package com.seii.legal.data.repository;

import com.seii.legal.common.page.PageResult;
import com.seii.legal.data.entity.Notification;

public interface NotificationRepository {
    String save(Notification notification);

    PageResult<Notification> find(String receiverId, Boolean read, int page, int size);

    void markRead(String notificationId);
}
