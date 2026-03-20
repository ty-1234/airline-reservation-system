package com.airline.reservation.service;

import com.airline.reservation.entity.Notification;
import com.airline.reservation.entity.User;
import com.airline.reservation.enums.NotificationType;
import com.airline.reservation.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void send(User user, String subject, String message) {
        save(user, NotificationType.EMAIL, subject, message);
        save(user, NotificationType.IN_APP, subject, message);
    }

    private void save(User user, NotificationType type, String subject, String message) {
        Notification notification = new Notification();
        notification.setUserId(user.getId());
        notification.setType(type);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setSent(true);
        notificationRepository.save(notification);
    }
}
