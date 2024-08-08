package es.emi;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;

public class CpModelTest {

    public static void solver() {

        Loader.loadNativeLibraries();

        CpModel model = new CpModel();
        CpSolver solver = new CpSolver();
//        solver.
//
//        IntVar x = model.newIntVar(0, Long.MAX_VALUE, "x");
//        IntVar y = model.newIntVar(0, Long.MAX_VALUE, "y");
//
//        model.addLessOrEqual(x, 3);
//        model.addLessOrEqual((7 * y), (x - 17));

        System.out.println("Number of variables ");

    }
}
