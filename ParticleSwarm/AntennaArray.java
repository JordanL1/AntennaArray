import java.util.*;

/** Antenna array design problem */
public class AntennaArray {
    /** Minimum spacing permitted between antennae. */
    public static final double MIN_SPACING = 0.25;


    public static void main(String[] args) {
        AntennaArray antennaArray = new AntennaArray(3, 90);
        
        Swarm particleSwarm = new Swarm(antennaArray, 20);
        double[] solution = particleSwarm.swarmSearch(100);

        System.out.println("Solution is valid: " + antennaArray.is_valid(solution));
        System.out.println("Solution cost: " + antennaArray.evaluate(solution));
        System.out.println("Solution: ");

        for (int i = 0; i < solution.length; i++) {
            System.out.println(solution[i]);
        }
    }

    /**
     * Construct an antenna design problem.
     * @param n_ant Number of antennae in our array.
     * @param steering_ang Desired direction of the main beam in degrees.
     */
    public AntennaArray(int n_ant,double steering_ang) {
        n_antennae = n_ant;
        steering_angle = steering_ang;
    }

    public double[] randomSearch(int iterations) {
        double[] bestSolution = generateRandomSolution();
        double bestCost = evaluate(bestSolution);

        for (int i = 0; i < iterations; i++) {
            double[] solution = generateRandomSolution();
            double cost = evaluate(solution);

            if (cost < bestCost) {
                bestCost = cost;
                bestSolution = solution;
            }
        }

        return bestSolution;
    }

    public double[] generateRandomSolution() {
        Random rand = new Random();
        boolean isValid = false;
        double upper = n_antennae / 2.0;
        double[] solution = new double[n_antennae];
        solution[n_antennae-1] = upper;

        while(!isValid) {
            for (int i = 0; i < n_antennae-1; i++) {
                solution[i] = rand.nextDouble() * upper;
            }

            isValid = is_valid(solution);
        }

        Arrays.sort(solution);
        return solution;
    }

  /**
   * Rectangular bounds on the search space.
   * @return Vector b such that b[i][0] is the minimum permissible value of the
   * ith solution component and b[i][1] is the maximum.
   */
    public double[][] bounds() {
        double[][] bnds = new double[n_antennae][2];
        double[] dim_bnd = {0.0,((double)n_antennae)/2.0};
        for(int i = 0;i<n_antennae;++i)
            bnds[i] = dim_bnd;
        return bnds;
    }
    /**
     * Check whether an antenna design lies within the problem's feasible
     * region.
     * A design is a vector of n_antennae anntena placements.
     * A placement is a distance from the left hand side of the antenna array.
     * A valid placement is one in which
     *   1) all antennae are separated by at least MIN_SPACING
     *   2) the aperture size (the maximum element of the array) is exactly
     *      n_antennae/2.
     */
    public boolean is_valid(double[] design) {
        if(design.length != n_antennae) return false;
        double[] des = new double[design.length];
        System.arraycopy(design,0,des,0,design.length);
        Arrays.sort(des);

        //Aperture size is exactly n_antennae/2
        if(Math.abs(des[des.length - 1] - ((double)n_antennae) / 2.0)>1e-10)
            return false;
        //All antennae lie within the problem bounds
        for(int i = 0;i<des.length-1;++i)
            if(des[i] < bounds()[i][0] || des[i] > bounds()[i][1] )
                return false;
        //All antennae are separated by at least MIN_SPACING
        for(int i = 0;i<des.length-1;++i)
            if(des[i+1] - des[i] < MIN_SPACING)
                return false;
        return true;
    }
    /**
     * Evaluate an antenna design returning peak SSL.
     * Designs which violate problem constraints will be penalised with extremely
     * high costs.
     * @param design A valid antenna array design.
     */
    public double evaluate(double[] design) {
        if(design.length != n_antennae)
            throw new RuntimeException(
                    "AntennaArray::evaluate called on design of the wrong size. Expected: " + n_antennae +
                    ". Actual: " +
                    design.length
            );
        if(!is_valid(design)) return Double.MAX_VALUE;

        class PowerPeak {
            public double elevation;
            public double power;

            public PowerPeak(double e,double p){
                elevation = e;
                power = p;
            }
        }

        //Find all the peaks in power
        List<PowerPeak> peaks = new ArrayList<PowerPeak>();
        PowerPeak prev = new PowerPeak(0.0,Double.MIN_VALUE);
        PowerPeak current = new PowerPeak(0.0,array_factor(design,0.0));
        for(double elevation = 0.01; elevation <= 180.0; elevation += 0.01){
            PowerPeak next = new PowerPeak(elevation,array_factor(design,elevation));
            if(current.power >= prev.power && current.power >= next.power)
                peaks.add(current);
            prev = current;
            current = next;
        }
        peaks.add(new PowerPeak(180.0,array_factor(design,180.0)));

        Collections.sort(peaks,(PowerPeak l,PowerPeak r) -> l.power > r.power ? -1 : 1);

        //No side-lobes case
        if(peaks.size()<2) return Double.MIN_VALUE;
        //Filter out main lobe and then return highest lobe level
        final double distance_from_steering = Math.abs(peaks.get(0).elevation - steering_angle);
        for(int i=1;i<peaks.size();++i)
            if(Math.abs(peaks.get(i).elevation - steering_angle) < distance_from_steering)
                return peaks.get(0).power;
        return peaks.get(1).power;
    }

    private int n_antennae;
    private double steering_angle;

    private double array_factor(double[] design,double elevation) {
        double steering = 2.0*Math.PI*steering_angle/360.0;
        elevation = 2.0*Math.PI*elevation/360.0;
        double sum = 0.0;
        for(double x : design){
            sum += Math.cos(2 * Math.PI * x * (Math.cos(elevation) - Math.cos(steering)));
        }
        return 20.0*Math.log(Math.abs(sum));
    }
}
