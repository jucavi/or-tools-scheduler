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

public class MultiMachineSchedulingB {

    public static void solver(int numMachines, int numPieces, int[] cycleTimes, ZonedDateTime[][][] productiveIntervals, int maxEnd) {

        // Reference point
        final ZonedDateTime REFERENCE_POINT = ZonedDateTime.of(LocalDate.of(2024, 8, 7), LocalTime.of(0, 0), ZoneId.of("UTC"));
        Loader.loadNativeLibraries();

        // Convert productive intervals and filter them
        int[][][] productiveIntervalsInSeconds = preprocessIntervals(numMachines, cycleTimes, productiveIntervals, REFERENCE_POINT);

        // Initialize model
        CpModel model = new CpModel();
        IntervalVar[][] tasks = new IntervalVar[numMachines][numPieces];
        IntVar[][] startTimes = new IntVar[numMachines][numPieces];
        IntVar[][] endTimes = new IntVar[numMachines][numPieces];
        BoolVar[][] isTaskActive = new BoolVar[numMachines][numPieces];

        // Initialize decision variables
        initializeVariables(model, numMachines, numPieces, cycleTimes, maxEnd, tasks, startTimes, endTimes, isTaskActive);

        // Add constraints
        addConstraints(model, numMachines, numPieces, productiveIntervalsInSeconds, tasks, startTimes, endTimes, isTaskActive);

        // Add objective: minimize makespan
        addObjective(model, maxEnd, endTimes);

        // Parallel processing
        CpSolver solver = new CpSolver();
        solver.getParameters().setNumWorkers(Runtime.getRuntime().availableProcessors());
        solver.getParameters().setLogSearchProgress(true);

        // Solve
        CpSolverStatus status = solver.solve(model);

        // Display results
        displayResults(status, solver, numMachines, numPieces, startTimes, endTimes, isTaskActive, REFERENCE_POINT);
    }

    private static int[][][] preprocessIntervals(int numMachines, int[] cycleTimes, ZonedDateTime[][][] productiveIntervals, ZonedDateTime referencePoint) {
        // Filter intervals based on cycle time
        int[][][] productiveIntervalsInSeconds = new int[numMachines][][];
        for (int m = 0; m < numMachines; m++) {
            List<int[]> validIntervals = new ArrayList<>();
            for (ZonedDateTime[] interval : productiveIntervals[m]) {
                int start = (int) ChronoUnit.SECONDS.between(referencePoint, interval[0]);
                int end = (int) ChronoUnit.SECONDS.between(referencePoint, interval[1]);
                if (end - start >= cycleTimes[m]) {
                    validIntervals.add(new int[]{start, end});
                }
            }
            productiveIntervalsInSeconds[m] = validIntervals.toArray(new int[0][]);
        }
        return productiveIntervalsInSeconds;
    }

    private static void initializeVariables(CpModel model, int numMachines, int numPieces, int[] cycleTimes, int maxEnd,
                                            IntervalVar[][] tasks, IntVar[][] startTimes, IntVar[][] endTimes, BoolVar[][] isTaskActive) {
        for (int m = 0; m < numMachines; m++) {
            for (int i = 0; i < numPieces; i++) {
                startTimes[m][i] = model.newIntVar(0, maxEnd, "start_machine_" + m + "_piece_" + i);
                endTimes[m][i] = model.newIntVar(0, maxEnd, "end_machine_" + m + "_piece_" + i);
                isTaskActive[m][i] = model.newBoolVar("isActive_machine_" + m + "_piece_" + i);
                tasks[m][i] = model.newOptionalIntervalVar(startTimes[m][i], LinearExpr.constant(cycleTimes[m]), endTimes[m][i], isTaskActive[m][i], "task_machine_" + m + "_piece_" + i);
            }
        }
    }

    private static void addConstraints(CpModel model, int numMachines, int numPieces, int[][][] productiveIntervalsInSeconds,
                                       IntervalVar[][] tasks, IntVar[][] startTimes, IntVar[][] endTimes, BoolVar[][] isTaskActive) {
        for (int m = 0; m < numMachines; m++) {
            for (int i = 0; i < numPieces; i++) {
                IntVar[] inIntervalConstraints = new IntVar[productiveIntervalsInSeconds[m].length];
                for (int j = 0; j < productiveIntervalsInSeconds[m].length; j++) {
                    BoolVar isInInterval = model.newBoolVar("isInInterval_machine_" + m + "_piece_" + i + "_interval_" + j);
                    model.addGreaterOrEqual(startTimes[m][i], productiveIntervalsInSeconds[m][j][0]).onlyEnforceIf(isInInterval);
                    model.addLessOrEqual(endTimes[m][i], productiveIntervalsInSeconds[m][j][1]).onlyEnforceIf(isInInterval);
                    inIntervalConstraints[j] = isInInterval;
                }
                model.addEquality(LinearExpr.sum(inIntervalConstraints), isTaskActive[m][i]);
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

        // No overlap on the same machine
        for (int m = 0; m < numMachines; m++) {
            model.addNoOverlap(tasks[m]);
        }
    }

    private static void addObjective(CpModel model, int maxEnd, IntVar[][] endTimes) {
        IntVar makespan = model.newIntVar(0, maxEnd, "makespan");
        IntVar[] allEndTimes = Arrays.stream(endTimes).flatMap(Arrays::stream).toArray(IntVar[]::new);
        model.addMaxEquality(makespan, allEndTimes);
        model.minimize(makespan);
    }

    private static void displayResults(CpSolverStatus status, CpSolver solver, int numMachines, int numPieces,
                                       IntVar[][] startTimes, IntVar[][] endTimes, BoolVar[][] isTaskActive, ZonedDateTime referencePoint) {

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found:");
            for (int m = 0; m < numMachines; m++) {
                for (int i = 0; i < numPieces; i++) {
                    if (solver.booleanValue(isTaskActive[m][i])) {
                        ZonedDateTime start = referencePoint.plusSeconds(solver.value(startTimes[m][i]));
                        ZonedDateTime end = referencePoint.plusSeconds(solver.value(endTimes[m][i]));
                        System.out.printf("Machine %d, Task %d: Start at %s, End at %s%n", m, i, start, end);
                    }
                }
            }

        } else {
            System.out.println("No feasible solution found.");
        }
    }
}
