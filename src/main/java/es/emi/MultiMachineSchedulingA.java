package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiMachineSchedulingA {

    public static void solver(int numMachines, int numPieces, int[] cycleTimes, ZonedDateTime[][][] productiveIntervals, int maxEnd) {

        // TODO: Reference point must be the minimum start date time
        final ZonedDateTime REFERENCE_POINT = ZonedDateTime.of(LocalDate.of(2024, 8, 7), LocalTime.of(0, 0), ZoneId.of("UTC"));

        Loader.loadNativeLibraries();

        // Convert ZonedDateTime to integer seconds from a reference (min start time interval starts from 0)
        int[][][] productiveIntervalsInSeconds = new int[numMachines][][];
        for (int m = 0; m < numMachines; m++) {
            List<int[]> validIntervals = new ArrayList<>();
            for (ZonedDateTime[] interval : productiveIntervals[m]) {
                int start = (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[0]);
                int end = (int) ChronoUnit.SECONDS.between(REFERENCE_POINT, interval[1]);
                if (end - start >= cycleTimes[m]) {
                    validIntervals.add(new int[]{start, end});
                }
            }
            productiveIntervalsInSeconds[m] = validIntervals.toArray(new int[0][]);
        }

        // Initializing model
        CpModel model = new CpModel();

        // Decision variables
        IntervalVar[][] tasks = new IntervalVar[numMachines][];
        IntVar[][] startTimes = new IntVar[numMachines][];
        IntVar[][] endTimes = new IntVar[numMachines][];
        BoolVar[][] isTaskActive = new BoolVar[numMachines][];

        // Initialize intervals variables for each machine and piece
        for (int m = 0; m < numMachines; m++) {
            tasks[m] = new IntervalVar[numPieces];
            startTimes[m] = new IntVar[numPieces];
            endTimes[m] = new IntVar[numPieces];
            isTaskActive[m] = new BoolVar[numPieces];

            for (int i = 0; i < numPieces; i++) {
                startTimes[m][i] = model.newIntVar(0, maxEnd, "start_machine_" + m + "_piece_" + i);
                endTimes[m][i] = model.newIntVar(0, maxEnd, "end_machine_" + m + "_piece_" + i);
                isTaskActive[m][i] = model.newBoolVar("isActive_machine_" + m + "_piece_" + i);

                tasks[m][i] = model.newOptionalIntervalVar(
                        startTimes[m][i],
                        LinearExpr.constant(cycleTimes[m]),
                        endTimes[m][i],
                        isTaskActive[m][i], "task_machine_" + m + "_piece_" + i);
            }
        }

        // Ensure each piece is assigned to exactly one machine
        for (int i = 0; i < numPieces; i++) {
            BoolVar[] assignment = new BoolVar[numMachines];
            for (int m = 0; m < numMachines; m++) {
                assignment[m] = isTaskActive[m][i];
            }
            model.addEquality(LinearExpr.sum(assignment), 1);
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

        // Hint for preferred machine assignment based on cycle times
        for (int i = 0; i < numPieces; i++) {
            int minCycleTime = Integer.MAX_VALUE;
            int preferredMachine = -1;

            for (int m = 0; m < numMachines; m++) {
                if (cycleTimes[m] < minCycleTime) {
                    minCycleTime = cycleTimes[m];
                    preferredMachine = m;
                }
            }

            model.addHint(isTaskActive[preferredMachine][i], 1);
        }

        // No overlap between pieces on the same machine
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

                        // Denormalize dates
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

