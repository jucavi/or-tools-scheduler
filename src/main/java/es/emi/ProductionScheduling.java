package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.sql.Time;
import java.util.*;

public class ProductionScheduling {

    public static void solveProductionScheduling(List<Machine> machines, int numPieces) {

        Loader.loadNativeLibraries();

        CpModel model = new CpModel();

        int numMachines = machines.size();

        // Convert productive intervals to seconds from a fixed reference point (start of the day)
        List<List<Long>> productiveStartTimes = new ArrayList<>(machines.size());
        List<List<Long>> productiveEndTimes = new ArrayList<>(machines.size());

        // initialize empty productive times per machine
        for (Machine machine : machines) {
            int slots = machine.getCalendarSize();

            productiveStartTimes.add(new ArrayList<>(slots));
            productiveEndTimes.add(new ArrayList<>(slots));
        }

        for (int i = 0; i < numMachines; i++) {

            List<TimeSlot> slots = machines.get(i).getCalendar().getSlots();

            // all calendar intervals
            for (TimeSlot slot : slots) {
                productiveStartTimes.get(i).add(slot.getStartTimeInSeconds());
                productiveEndTimes.get(i).add(slot.getEndTimeInSeconds());
            }
        }

        long minStartTime = productiveStartTimes.stream()
                .flatMap(Collection::stream)
                .mapToLong(c -> c)
                .min().orElse(0);

        long maxEndTime = productiveStartTimes.stream()
                .flatMap(Collection::stream)
                .mapToLong(c -> c)
                .max().orElse(Long.MAX_VALUE);


        // VARIABLES
        List<List<IntVar>> endTimesVar = new ArrayList<>(numMachines);
        List<List<IntVar>> startTimesVar = new ArrayList<>(numMachines);
        List<IntVar> assignmentsVar = new ArrayList<>(numPieces);
        IntVar makeSpanObjective = model.newIntVar(0L, maxEndTime, "makespan");

        // initialize schedule time variables
        for (Machine machine : machines) {
            int slots = machine.getCalendarSize();

            startTimesVar.add(new ArrayList<>(slots));
            endTimesVar.add(new ArrayList<>(slots));
        }


        // variable piece assigned to machine flag
        //  0 1 2 3 4 5 pieces as index
        // [0,1,1,1,1,2] machines as values
        // piece_0 assigned to machine_0
        // .....
        // piece_5 assigned to machine_2
        for (int i = 0; i < numPieces; i++) {
            assignmentsVar.add(model.newIntVar(0, numMachines - 1, "assignment_" + i));
        }


        // CONSTRAINS
        for (int i = 0; i < numMachines; i++) {

            Machine machine = machines.get(i);
            int timeSlotsSize = machine.getCalendarSize();

            for (int j = 0; j < timeSlotsSize; j++) {

                // Create start and end time variables and store it
                IntVar startIJ = model.newIntVar(minStartTime, maxEndTime, "startTime_" + i + "_" + j);
                startTimesVar.get(i).add(startIJ);

                IntVar endIJ = model.newIntVar(minStartTime, maxEndTime, "endTime_" + i + "_" + j);
                endTimesVar.get(i).add(endIJ);

                Long start = productiveStartTimes.get(i).get(j);
                Long end = productiveEndTimes.get(i).get(j);

                // Add constraints for start and end times
                // Ensure the pieces are produced within the productive intervals
                model.addGreaterThan(endIJ, startIJ);
                model.addGreaterOrEqual(startIJ, start);
                model.addLessOrEqual(endIJ, end);
                // Set end time as start time plus cycle time
                IntVar speedTime = model.newConstant(machine.getSpeed());
                model.addLessOrEqual(endIJ, LinearExpr.sum(new IntVar[]{startIJ, speedTime}));

//                model.addLessOrEqual(endIJ, makeSpanObjective);

                // Print data
                System.out.println("Productive interval: [" + start + ", " + end + "]");
                System.out.println("Cycle time: " + machine.getSpeed());

            }
        }

        // Flatten the list of startTimesVar for get all
        List<IntVar> allStartTimesVars = startTimesVar.stream()
                .flatMap(Collection::stream)
                .toList();

//        // IntVar for the sum of startTimesVar sizes
        IntVar sumStartTimesVars = model.newIntVar(0, allStartTimesVars.size(), "sumStartTimesVars");

        // Define constraints
        // Constraint to ensure that the sum of the sizes of startTimesVar per machine is not greater than numPieces
        model.addLessOrEqual(sumStartTimesVars, numPieces);

        // Create an IntVar to count the number of start times variables
        IntVar actualCount = model.newIntVar(0, allStartTimesVars.size(), "actualCount");

        // Constraint to ensure that the actualCount does not exceed numPieces
        model.addLessOrEqual(actualCount, numPieces);

        // Ensures that each piece is assigned to exactly one machine
        for (int j = 0; j < numPieces; j++) {
            List<BoolVar> pieceAssignments = new ArrayList<>(numMachines);
            for (int i = 0; i < numMachines; i++) {
                BoolVar assignment = model.newBoolVar("assignment_" + j + "_" + i);
                pieceAssignments.add(assignment);
            }
            model.addExactlyOne(pieceAssignments.toArray(new BoolVar[0]));
        }

        // Objective: Minimize the make span Ok
//        for (int i = 0; i < numMachines; i++) {
//            Machine machine = machines.get(i);
//            List<TimeSlot> slots = machine.getCalendar().getSlots();
//
//            for (int j = 0; j < slots.size(); j++) {
//                model.addLessOrEqual(endTimesVar.get(i).get(j), makeSpanObjective);
//            }
//        }

        model.minimize(makeSpanObjective);
        System.out.println("MakeSpanObjective: " + makeSpanObjective);

        for (int i = 0; i < numMachines; i++) {
            for (int j = 0; j < startTimesVar.get(i).size(); j++) {
                IntVar startVar = startTimesVar.get(i).get(j);
                IntVar endVar = endTimesVar.get(i).get(j);

                System.out.println("Machine " + i + ", Slot " + j);
                System.out.println("Start time variable: " + startVar);
                System.out.println("End time variable: " + endVar);
            }
        }

        // Solving the model
        CpSolver solver = new CpSolver();
        solver.getParameters().setLogSearchProgress(true);
        CpSolverStatus status = solver.solve(model);




        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Makespan: " + solver.value(makeSpanObjective));
            for (int i = 0; i < numMachines; i++) {
                for (int j = 0; j < numPieces; j++) {
                    if (solver.value(assignmentsVar.get(j)) == i) {
                        System.out.println("Machine " + i + ": Piece " + j +
                                ", Start time: " + solver.value(startTimesVar.get(i).get(j)) +
                                ", End time: " + solver.value(endTimesVar.get(i).get(j)));
                    }
                }
            }
        } else {
            System.out.println("No solution found.");
        }

        System.out.println("\nAdvanced usage:");
        System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
        System.out.println("Problem response stats " + solver.responseStats());

    }
}