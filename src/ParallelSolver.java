import java.util.ArrayList;

public class ParallelSolver extends Thread {
    private ArrayList<Solutions> solutions;
    private ArrayList<Integer> vnodeOrder;
    private int solutionIndex;
    private Graph ip, otn, vn;
    private OverlayMapping ipOtn;
    private ArrayList<Integer> locationConstraints[];
    
    public ParallelSolver() {
        solutions = null;
        solutionIndex = -1;
        this.vnodeOrder = null;
        this.ip = null;
        this.otn = null;
        this.ipOtn = null;
        this.locationConstraints = null;
    }

    public ParallelSolver(ArrayList<Solutions> solutions, int solutionIndex,
            ArrayList<Integer> vnodeOrder, Graph ip, Graph otn, Graph vn,
            ArrayList<Integer> locationConstraints[], OverlayMapping ipOtn) {
        this.solutions = solutions;
        this.solutionIndex = solutionIndex;
        this.vnodeOrder = vnodeOrder;
        this.ip = ip;
        this.otn = otn;
        this.vn = vn;
        this.ipOtn = ipOtn;
        this.locationConstraints = locationConstraints;
    }

    @Override
    public void run() {
        Solutions solution = null;
        CreateInitialSolution cis = new CreateInitialSolution(ip, otn, ipOtn,
                vnodeOrder);
        System.out.println(vn);
        solution = cis.getInitialSolution(vn, locationConstraints);
        this.solutions.set(solutionIndex, solution);
    }
}
