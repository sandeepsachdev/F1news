package com.f1news.service;

import com.f1news.model.dto.RaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class RaceNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RaceNotificationScheduler.class);

    private final F1DataService f1DataService;
    private final WebPushService webPushService;

    public RaceNotificationScheduler(F1DataService f1DataService, WebPushService webPushService) {
        this.f1DataService = f1DataService;
        this.webPushService = webPushService;
    }

    /**
     * Runs every day at 09:00 UTC to check if a race is tomorrow.
     */
    @Scheduled(cron = "${notification.scheduler.cron:0 0 9 * * *}", zone = "UTC")
    public void checkAndNotifyUpcomingRace() {
        log.info("Running race notification check...");

        Optional<RaceDto> nextRace = f1DataService.getNextRace();
        if (nextRace.isEmpty()) {
            log.info("No upcoming races found");
            return;
        }

        RaceDto race = nextRace.get();
        LocalDate raceDate = LocalDate.parse(race.date());
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        if (raceDate.equals(tomorrow)) {
            log.info("Race tomorrow: {} — sending notifications", race.raceName());
            String title = "🏁 Race Day Tomorrow!";
            String body = race.raceName() + " at " + race.circuitName() + ", " + race.country()
                    + " — " + formatTime(race.time());
            webPushService.sendNotificationToAll(title, body, "/");
        } else {
            log.info("Next race is on {} — {} days away", race.date(),
                    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), raceDate));
        }
    }

    private String formatTime(String time) {
        if (time == null || time.isBlank() || time.equalsIgnoreCase("TBC")) return "time TBC";
        return time.replace("Z", " UTC");
    }
}
