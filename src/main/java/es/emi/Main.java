package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

//        int numMachines = 3;
//        double[] cycleTimes = {3600};
//        int numPieces = 10;

        List<Machine> machines = new ArrayList<>(); // Time to produce one piece for each machine
        int numPieces = 1;

        Machine machine = new Machine(1, 3600, new Calendar(Arrays.asList(
                new TimeSlot(ZonedDateTime.parse("2024-08-07T00:00:00Z"), ZonedDateTime.parse("2024-08-07T01:00:00Z")),
                new TimeSlot(ZonedDateTime.parse("2024-08-07T02:00:00Z"), ZonedDateTime.parse("2024-08-07T03:30:00Z")),
                new TimeSlot(ZonedDateTime.parse("2024-08-07T05:30:00Z"), ZonedDateTime.parse("2024-08-07T08:00:00Z")))));

        machines.add(machine);

        ProductionScheduling.solveProductionScheduling(machines, numPieces);
//        testAddExactlyOneConstraint();
//        testProductionScheduling(machines, numPieces);
    }


//    public static void testAddExactlyOneConstraint() {
//        Loader.loadNativeLibraries();
//        CpModel model = new CpModel();
//
//        // Number of machines and pieces for the test
//        int numMachines = 3;
//        int numPieces = 8;
//
//        List<BoolVar> assignmentConstraints = new ArrayList<>(numPieces);
//
//        // Test variables for pieces
//        for (int j = 0; j < numPieces; j++) {
//            List<BoolVar> pieceAssignments = new ArrayList<>(numMachines);
//            for (int i = 0; i < numMachines; i++) {
//                BoolVar assignment = model.newBoolVar("assignment_" + j + "_" + i);
//                pieceAssignments.add(assignment);
//            }
//            model.addExactlyOne(pieceAssignments.toArray(new BoolVar[0]));
//            assignmentConstraints.addAll(pieceAssignments);
//        }
//
//        // Solving the test model
//        CpSolver solver = new CpSolver();
//        CpSolverStatus status = solver.solve(model);
//
//        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
//            System.out.println("Test model is feasible.");
//            for (int j = 0; j < numPieces; j++) {
//                for (int i = 0; i < numMachines; i++) {
//                    BoolVar assignment = assignmentConstraints.get(j * numMachines + i);
//                    System.out.println("Piece " + j + " assigned to machine " + i + ": " + (solver.value(assignment) == 1));
//                }
//            }
//        } else {
//            System.out.println("Test model is infeasible.");
//        }
//    }


    public static void testProductionScheduling(List<Machine> machines, int numPieces) {

        Loader.loadNativeLibraries();

        CpModel model = new CpModel();

        int numMachines = machines.size();

        // Simplified variable setup
        List<IntVar> startTimesVar = new ArrayList<>();
        List<IntVar> endTimesVar = new ArrayList<>();

        // Initialize variables with small example data
        for (int i = 0; i < numMachines; i++) {
            IntVar startIJ = model.newIntVar(0, 100, "startTime_" + i);
            IntVar endIJ = model.newIntVar(0, 100, "endTime_" + i);
            startTimesVar.add(startIJ);
            endTimesVar.add(endIJ);
        }

        // Define a simplified constraint
        for (int i = 0; i < numMachines; i++) {
            IntVar startIJ = startTimesVar.get(i);
            IntVar endIJ = endTimesVar.get(i);
            model.addLessOrEqual(endIJ, LinearExpr.sum(new IntVar[]{startIJ, model.newConstant(10)}));
        }

        // Define a simple sum constraint
        IntVar totalStartTime = model.newIntVar(0, numMachines * 100L, "totalStartTime");
        model.addEquality(totalStartTime, LinearExpr.sum(startTimesVar.toArray(new IntVar[0])));
        model.addLessOrEqual(totalStartTime, numPieces);

        // Objective: Minimize the total start time
        model.minimize(totalStartTime);

        // Solve the model
        CpSolver solver = new CpSolver();
        solver.getParameters().setLogSearchProgress(true);
        CpSolverStatus status = solver.solve(model);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found:");
            for (int i = 0; i < numMachines; i++) {
                System.out.println("Machine " + i + ": Start time: " + solver.value(startTimesVar.get(i)) + ", End time: " + solver.value(endTimesVar.get(i)));
            }
        } else {
            System.out.println("No solution found.");
        }
    }

}