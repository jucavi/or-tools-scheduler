package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class MultiMachineScheduling {

    private static final ZonedDateTime REFERENCE_POINT = ZonedDateTime.of(LocalDate.of(2024, 8, 7), LocalTime.of(0, 0), ZoneId.of("UTC"));

    public static void main(String[] args) {
        Loader.loadNativeLibraries();

        int numMachines = 3; // Number of machines
        int numPieces = 5;   // Number of pieces to produce
        int maxEnd = 43200;

        // Cycle times for each machine
        int[] cycleTimes = {3600, 5400, 7200}; // Example cycle times for M1, M2, M3 (in seconds)


        // Define productive intervals for each machine using ZonedDateTime
        ZonedDateTime[][][] productiveIntervals = {
                {
                        {ZonedDateTime.parse("2024-08-07T00:30:00Z"), ZonedDateTime.parse("2024-08-07T01:30:00Z")},
                        {ZonedDateTime.parse("2024-08-07T02:00:00Z"), ZonedDateTime.parse("2024-08-07T03:30:00Z")},
                },
                {
                        {ZonedDateTime.parse("2024-08-07T00:30:00Z"), ZonedDateTime.parse("2024-08-07T02:00:00Z")},
                        {ZonedDateTime.parse("2024-08-07T05:30:00Z"), ZonedDateTime.parse("2024-08-07T08:00:00Z")},
                },
                {
                        {ZonedDateTime.parse("2024-08-07T01:00:00Z"), ZonedDateTime.parse("2024-08-07T03:00:00Z")},
                        {ZonedDateTime.parse("2024-08-07T04:30:00Z"), ZonedDateTime.parse("2024-08-07T06:30:00Z")},
                }
        };


        // Convert ZonedDateTime to integer seconds from the reference point
        int[][][] productiveIntervalsInSeconds = Arrays.stream(productiveIntervals)
                .map(machineIntervals -> Arrays.stream(machineIntervals)
                        .map(interval -> new int[]{
                                (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[0]),
                                (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[1])
                        })
                        .toArray(int[][]::new))
                .toArray(int[][][]::new);


        // Create the model
        CpModel model = new CpModel();

        // Decision variables
        IntervalVar[][] tasks = new IntervalVar[numMachines][];
        IntVar[][] startTimes = new IntVar[numMachines][];
        IntVar[][] endTimes = new IntVar[numMachines][];
        BoolVar[][] isTaskActive = new BoolVar[numMachines][];

        // Initialize variables for each machine and piece
        for (int m = 0; m < numMachines; m++) {

            tasks[m] = new IntervalVar[numPieces];
            startTimes[m] = new IntVar[numPieces];
            endTimes[m] = new IntVar[numPieces];
            isTaskActive[m] = new BoolVar[numPieces];  // Create BoolVar array for each machine

            for (int i = 0; i < numPieces; i++) {
                startTimes[m][i] = model.newIntVar(0, maxEnd, "start_machine_" + m + "_piece_" + i);
                endTimes[m][i] = model.newIntVar(0, maxEnd, "end_machine_" + m + "_piece_" + i);
                isTaskActive[m][i] = model.newBoolVar("isActive_machine_" + m + "_piece_" + i); // Create BoolVar

                // Create the IntervalVar as an optional interval
                tasks[m][i] = model.newOptionalIntervalVar(
                        startTimes[m][i],
                        LinearExpr.constant(cycleTimes[m]), endTimes[m][i],
                        isTaskActive[m][i], "task_machine_" + m + "_piece_" + i + "_interval");
            }
        }

        // Ensure each piece is assigned to exactly one machine
        for (int i = 0; i < numPieces; i++) {

            BoolVar[] assignment = new BoolVar[numMachines];
            for (int m = 0; m < numMachines; m++) {
                assignment[m] = isTaskActive[m][i];
            }
            model.addEquality(LinearExpr.sum(assignment), 1); // Ensure the piece is assigned to exactly one machine
        }

        // Constraints for each machine
        for (int m = 0; m < numMachines; m++) {

            for (int i = 0; i < numPieces; i++) {
                IntVar[] inIntervalConstraints = new IntVar[productiveIntervalsInSeconds[m].length];

                for (int j = 0; j < productiveIntervalsInSeconds[m].length; j++) {

                    BoolVar isInInterval = model.newBoolVar("isInInterval_machine_" + m + "_piece_" + i + "_interval_" + j);

                    model.addGreaterOrEqual(startTimes[m][i], productiveIntervalsInSeconds[m][j][0]).onlyEnforceIf(isInInterval);
                    model.addLessOrEqual(endTimes[m][i], productiveIntervalsInSeconds[m][j][1]).onlyEnforceIf(isInInterval);

                    inIntervalConstraints[j] = isInInterval;
                }

                // Ensure the task is within exactly one productive interval if it's active
                model.addEquality(LinearExpr.sum(inIntervalConstraints), isTaskActive[m][i]);
            }
        }

        // **Preference constraints based on cycle times**
        for (int i = 0; i < numPieces; i++) {

            int minCycleTime = Integer.MAX_VALUE;
            int preferredMachine = -1;

            // Identify the machine with the minimum cycle time for this task
            for (int m = 0; m < numMachines; m++) {
                if (cycleTimes[m] < minCycleTime) {
                    minCycleTime = cycleTimes[m];
                    preferredMachine = m;
                }
            }

            // Add a hint to prefer the machine with the lower cycle time for this task
            model.addHint(isTaskActive[preferredMachine][i], 1);
        }

        // Ensure no overlap between tasks on the same machine
        for (int m = 0; m < numMachines; m++) {
            model.addNoOverlap(tasks[m]);
        }

        // Objective: Minimize the makespan
        IntVar makespan = model.newIntVar(0, maxEnd, "makespan");
        IntVar[] allEndTimes = Arrays.stream(endTimes)
                .flatMap(Arrays::stream)
                .toArray(IntVar[]::new);
        model.addMaxEquality(makespan, allEndTimes);
        model.minimize(makespan);

        // Solve the model
        CpSolver solver = new CpSolver();
        solver.getParameters().setLogSearchProgress(true);
        CpSolverStatus status = solver.solve(model);

        // Display results
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found:");
            for (int m = 0; m < numMachines; m++) {
                for (int i = 0; i < numPieces; i++) {
                    if (solver.booleanValue(isTaskActive[m][i])) {
                        ZonedDateTime start = REFERENCE_POINT.plusSeconds(solver.value(startTimes[m][i]));
                        ZonedDateTime end = REFERENCE_POINT.plusSeconds(solver.value(endTimes[m][i]));
                        System.out.printf("Machine %d, Task %d: Start at %s, End at %s%n", m, i, start, end);
                    }
                }
            }
            ZonedDateTime makespanEnd = REFERENCE_POINT.plusSeconds(solver.value(makespan));
            System.out.println("Makespan: " + makespanEnd);
        } else {
            System.out.println("No feasible solution found.");
        }
    }
}
