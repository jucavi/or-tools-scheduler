package es.emi;

import java.util.HashMap;
import java.util.Map;

public class MachineScheduledJobs {

    private Map<Integer, Calendar> schedule = new HashMap<Integer, Calendar>();

    public Map<Integer, Calendar> getSchedule() {
        return schedule;
    }

    public void setSchedule(Map<Integer, Calendar> schedule) {
        this.schedule = schedule;
    }

    public void addCalendar(Integer id, Calendar calendar) {
        this.schedule.put(id, calendar);
    }

    public void addSlot(Integer id, TimeSlot timeSlot) {

        if (schedule.containsKey(id)) {
            this.schedule.get(id).addSlot(timeSlot);
        }
    }

}
