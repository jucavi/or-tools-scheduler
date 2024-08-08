package es.emi;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Calendar {

    private List<TimeSlot> slots;

    public Calendar(List<TimeSlot> slots) {
        this.slots = slots;
    }

    public List<TimeSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<TimeSlot> slots) {
        this.slots = slots;
    }

    public void addSlot(TimeSlot slot) {
        this.slots.add(slot);
    }

    public List<TimeSlot> getProductiveNSlots(ZonedDateTime start, int n) {

        List<TimeSlot> timeSlots = getTimeSlots(start);

        if (timeSlots.isEmpty() || n > slots.size()) {
            return List.of();
        }

        if (slots.get(0).getStartTime().isBefore(start)) {
            slots.set(0, new TimeSlot(start, slots.get(0).getEndTime()));
        }

        return timeSlots.subList(0, n);
    }

    public List<TimeSlot> getNonProductiveNSlots(ZonedDateTime start, int n) {

        List<TimeSlot> timeSlots = getTimeSlots(start);
        List<TimeSlot> nonProductiveNSlots = new ArrayList<>();

        if (timeSlots.isEmpty()) {
            return List.of();
        }

        if (timeSlots.get(0).getStartTime().isBefore(start)) {
            nonProductiveNSlots.add(new TimeSlot(start, timeSlots.get(0).getStartTime()));
        }

        Iterator<TimeSlot> iterator = timeSlots.iterator();
        TimeSlot first = iterator.next();

        while (iterator.hasNext()) {
            TimeSlot next = iterator.next();

            nonProductiveNSlots.add(new TimeSlot(first.getEndTime(), next.getStartTime()));
            first = next;
        }

        if (n > nonProductiveNSlots.size()) {
            return List.of();
        }

        return nonProductiveNSlots.subList(0, n);
    }

    private List<TimeSlot> getTimeSlots(ZonedDateTime start) {
        return slots.stream()
                .filter(c -> c.getStartTime().minusSeconds(1).isAfter(start)
                        || (c.getStartTime().minusSeconds(1).isBefore(start)
                        && c.getEndTime().plusSeconds(1).isAfter(start)))
                .toList();
    }

}
