package es.emi;

import java.time.Duration;
import java.time.ZonedDateTime;

public class TimeSlot {

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    public TimeSlot(ZonedDateTime startTime, ZonedDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getDuration() {
        return Duration.between(startTime, endTime).getSeconds();
    }

    @Override
    public String toString() {
        return String.format("Slot: [%s - %s]", startTime, endTime);
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getStartTimeInSeconds() {
        return startTime.toEpochSecond();
    }

    public Long getEndTimeInSeconds() {
        return endTime.toEpochSecond();
    }
}
