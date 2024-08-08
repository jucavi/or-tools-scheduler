package es.emi;


import java.util.List;

public class Machine {

    private final Integer id;
    private final Integer speed;
    private final Calendar calendar;


    public Machine(Integer id, Integer speed, Calendar calendar) {
        this.id = id;
        this.speed = speed;
        this.calendar = calendar;
    }

    public Integer getId() {
        return id;
    }

    public Integer getSpeed() {
        return speed;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public List<TimeSlot> getSlots() {
        return calendar.getSlots();
    }

    public Integer getCalendarSize() {
        return getSlots().size();
    }
}