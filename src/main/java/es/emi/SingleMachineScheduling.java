package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class SingleMachineScheduling {

    // Define the reference point for the conversion
    private static final ZonedDateTime REFERENCE_POINT = ZonedDateTime.of(LocalDate.of(2024, 8, 7), LocalTime.of(0, 0), ZoneId.of("UTC"));

    public static void main(String[] args) {
        Loader.loadNativeLibraries();

        // Define productive intervals using ZonedDateTime
        ZonedDateTime[][] productiveIntervals = {
                {ZonedDateTime.parse("2024-08-07T00:30:00Z"), ZonedDateTime.parse("2024-08-07T01:00:00Z")},
                {ZonedDateTime.parse("2024-08-07T02:00:00Z"), ZonedDateTime.parse("2024-08-07T03:30:00Z")},
                {ZonedDateTime.parse("2024-08-07T05:30:00Z"), ZonedDateTime.parse("2024-08-07T08:00:00Z")}
        };

        // Convert ZonedDateTime to integer seconds from the reference point
        int[][] productiveIntervalsInSeconds = Arrays.stream(productiveIntervals)
                .map(interval -> new int[]{
                        (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[0]),
                        (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[1])
                })
                .toArray(int[][]::new);

        // Data
        int numPieces = 3;
        int processingTime = 3600; // Time to process one piece in seconds

        // Create the model CP-SAT
        CpModel model = new CpModel();

        // Decision variables
        IntervalVar[] tasks = new IntervalVar[numPieces];
        IntVar[] startTimes = new IntVar[numPieces];
        IntVar[] endTimes = new IntVar[numPieces];

        // Assuming the maximum interval is within 12 hours (43200 seconds)
        int maxTime = 43200;

        for (int i = 0; i < numPieces; i++) {
            startTimes[i] = model.newIntVar(0, maxTime, "start_" + i);
            endTimes[i] = model.newIntVar(0, maxTime, "end_" + i);
            tasks[i] = model.newIntervalVar(startTimes[i], LinearExpr.constant(processingTime), endTimes[i], "task_" + i);
        }

        // Constraints
        for (int i = 0; i < numPieces; i++) {
            IntVar[] inIntervalConstraints = new IntVar[productiveIntervalsInSeconds.length];
            for (int j = 0; j < productiveIntervalsInSeconds.length; j++) {
                IntVar isInInterval = model.newBoolVar("isInInterval_" + i + "_interval_" + j);
                model.addGreaterOrEqual(startTimes[i], productiveIntervalsInSeconds[j][0]).onlyEnforceIf((Literal) isInInterval);
                model.addLessOrEqual(endTimes[i], productiveIntervalsInSeconds[j][1]).onlyEnforceIf((Literal) isInInterval);
                inIntervalConstraints[j] = isInInterval;
            }
            model.addEquality(LinearExpr.sum(inIntervalConstraints), 1);
        }

        // Ensure no overlap between tasks
        for (int i = 0; i < numPieces; i++) {
            for (int j = i + 1; j < numPieces; j++) {
                model.addNoOverlap(new IntervalVar[]{tasks[i], tasks[j]});
            }
        }

        // Objective: Minimize the makespan
        IntVar makespan = model.newIntVar(0, maxTime, "makespan");
        model.addMaxEquality(makespan, endTimes);
        model.minimize(makespan);

        // Solve the model
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        // Display results
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found:");
            for (int i = 0; i < numPieces; i++) {
                ZonedDateTime start = REFERENCE_POINT.plusSeconds(solver.value(startTimes[i]));
                ZonedDateTime end = REFERENCE_POINT.plusSeconds(solver.value(endTimes[i]));
                System.out.printf("Task %d: Start at %s, End at %s%n", i, start, end);
            }
            ZonedDateTime makespanEnd = REFERENCE_POINT.plusSeconds(solver.value(makespan));
            System.out.println("Makespan: " + makespanEnd);
        } else {
            System.out.println("No feasible solution found.");
        }
    }
}
