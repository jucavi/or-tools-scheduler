package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

public class SingleMachineScheduling {

    public static void main(String[] args) {

        // Cargar las librerías nativas de OR-Tools
        Loader.loadNativeLibraries();

        // Crear el modelo CP-SAT
        CpModel model = new CpModel();

        // Datos del problema
        int numPieces = 3;
        int processingTime = 2; // Tiempo que tarda en realizar una pieza
        int[][] productiveIntervals = {
                {0, 6},  // Intervalo 1: de 0 a 5
                {9, 11}  // Intervalo 2: de 6 a 10
        };

        // Variables de decisión
        IntervalVar[] tasks = new IntervalVar[numPieces];
        IntVar[] startTimes = new IntVar[numPieces];
        IntVar[] endTimes = new IntVar[numPieces];

        for (int i = 0; i < numPieces; i++) {
            startTimes[i] = model.newIntVar(0, 11, "start_" + i);
            endTimes[i] = model.newIntVar(0, 11, "end_" + i);
            tasks[i] = model.newIntervalVar(startTimes[i], LinearExpr.constant(processingTime), endTimes[i], "task_" + i);
        }

        // Restricciones
        for (int i = 0; i < numPieces; i++) {
            // Cada tarea debe comenzar y terminar dentro de algún intervalo productivo
            IntVar[] inIntervalConstraints = new IntVar[productiveIntervals.length];
            for (int j = 0; j < productiveIntervals.length; j++) {
                IntVar isInInterval = model.newBoolVar("isInInterval_" + i + "_interval_" + j);
                model.addGreaterOrEqual(startTimes[i], productiveIntervals[j][0]).onlyEnforceIf((Literal) isInInterval);
                model.addLessOrEqual(endTimes[i], productiveIntervals[j][1]).onlyEnforceIf((Literal) isInInterval);
                inIntervalConstraints[j] = isInInterval;
            }
            model.addEquality(LinearExpr.sum(inIntervalConstraints), 1);
        }

        // Asegurar que las tareas no se solapen
        for (int i = 0; i < numPieces; i++) {
            for (int j = i + 1; j < numPieces; j++) {
                model.addNoOverlap(new IntervalVar[]{tasks[i], tasks[j]});
            }
        }

        // Función objetivo: Minimizar el tiempo de finalización de la última tarea
        IntVar makespan = model.newIntVar(0, 14, "makespan");
        model.addMaxEquality(makespan, endTimes);
        model.minimize(makespan);

        // Crear el solver y resolver el modelo
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        // Mostrar resultados
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution found:");
            for (int i = 0; i < numPieces; i++) {
                System.out.printf("Task %d: Start at %d, End at %d%n", i, solver.value(startTimes[i]), solver.value(endTimes[i]));
            }
            System.out.println("Makespan: " + solver.value(makespan));
        } else {
            System.out.println("No feasible solution found.");
        }
    }
}
