package com.edrs.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendReservationConfirmation(String userId, String confirmationNumber) {
        logger.info("MOCK EMAIL: Sending reservation confirmation to user {} for reservation {}", userId, confirmationNumber);
        logger.info("MOCK EMAIL CONTENT: Your reservation {} has been confirmed!", confirmationNumber);
        // In production, this would send an actual email via email service
    }

    public void sendCancellationConfirmation(String userId, String confirmationNumber) {
        logger.info("MOCK EMAIL: Sending cancellation confirmation to user {} for reservation {}", userId, confirmationNumber);
        logger.info("MOCK EMAIL CONTENT: Your reservation {} has been cancelled successfully.", confirmationNumber);
        // In production, this would send an actual email via email service
    }
}
