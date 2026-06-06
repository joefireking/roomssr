package com.apartment.hub.task;

import com.apartment.hub.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillScheduledTask {

    private final BillService billService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkOverdueBills() {
        log.info("Starting scheduled overdue bill check");
        try {
            billService.checkOverdue();
            log.info("Scheduled overdue bill check completed");
        } catch (Exception e) {
            log.error("Scheduled overdue bill check failed", e);
        }
    }
}
