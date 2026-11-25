package com.vn.otech.billing.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BillingClock {
    private final ZoneId zoneId;

    public BillingClock(@Value("${app.monetization.time-zone:Asia/Ho_Chi_Minh}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(zoneId);
    }

    public LocalDate today() {
        return LocalDate.now(zoneId);
    }
}
