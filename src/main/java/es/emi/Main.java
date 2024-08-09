package es.emi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Main {

    private static final ZonedDateTime REFERENCE_POINT = ZonedDateTime.of(LocalDate.of(2024, 8, 7), LocalTime.of(0, 0), ZoneId.of("UTC"));

    public static void main(String[] args) {

        int intervals = 100;
        int duration = 15*60; // seconds

        int numMachines = 12; // Number of machines
        int numPieces = 20;   // Number of pieces to produce
        // Cycle times for each machine
        int[] cycleTimes = {15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60, 15 * 60, 30 * 60, 60 * 60}; // Example cycle times for M1, M2, M3 (in seconds)
//        int[] cycleTimes = {3600, 5400, 7200}; // Example cycle times for M1, M2, M3 (in seconds)
//        int[] cycleTimes = {3600, 3600, 3600}; // Example cycle times for M1, M2, M3 (in seconds)
        // Must be the far date time (horizon)
        int maxEnd = 432000;

        // Define productive intervals for each machine using ZonedDateTime
//        ZonedDateTime[][][] productiveIntervals = {
//                {
//                        {ZonedDateTime.parse("2024-08-07T00:30:00Z"), ZonedDateTime.parse("2024-08-07T01:30:00Z")},
//                        {ZonedDateTime.parse("2024-08-07T02:00:00Z"), ZonedDateTime.parse("2024-08-07T03:30:00Z")},
////                }
//                },
//                {
//                        {ZonedDateTime.parse("2024-08-07T00:30:00Z"), ZonedDateTime.parse("2024-08-07T02:00:00Z")},
//                        {ZonedDateTime.parse("2024-08-07T05:30:00Z"), ZonedDateTime.parse("2024-08-07T08:00:00Z")},
//                },
//                {
//                        {ZonedDateTime.parse("2024-08-07T01:00:00Z"), ZonedDateTime.parse("2024-08-07T03:00:00Z")},
//                        {ZonedDateTime.parse("2024-08-07T04:30:00Z"), ZonedDateTime.parse("2024-08-07T06:00:00Z")},
//                }
//        };

        ZonedDateTime[][][] productiveIntervals = generateMachinesCalendars(REFERENCE_POINT, duration, intervals, numMachines);

        MultiMachineScheduling.solver(numMachines, numPieces, cycleTimes, productiveIntervals, maxEnd);

//        System.out.println(Arrays.deepToString(generateMachinesCalendars(REFERENCE_POINT, 15 * 60, 200, 3)));
    }


    public static ZonedDateTime[][][] generateMachinesCalendars(ZonedDateTime start, long duration, int intervals, int numMachines) {

        ZonedDateTime[][][] calendars = new ZonedDateTime[numMachines][1][intervals];

        for (int i = 0; i < numMachines; i++) {

            ZonedDateTime current = start;
            ZonedDateTime[][] calendar = new ZonedDateTime[intervals][2];

            for (int j = 0; j < intervals; j++) {
                calendar[j][0] = current;
                calendar[j][1] = current.plusSeconds(duration);

                current = current.plusSeconds(2L * duration);
            }

            calendars[i] = calendar;
        }

        return calendars;
    }
}