package com.tly.scheduler;

import com.tly.service.impl.BookingServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约违规自动判定：开始后60分钟未核销 -> 违规
 */
@Component
public class BookingViolationScheduler {

    @Autowired
    private BookingServiceImpl bookingService;

    @Scheduled(fixedDelay = 60_000)
    public void markNoShowAsViolation() {
        bookingService.processNoShowViolations(200);
    }
}

