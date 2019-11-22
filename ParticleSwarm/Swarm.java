public class Swarm {
    private AntennaArray antennaArray;
    private Particle[] particles;
    private double[] gbest;
    private double gbestCost;

    public Swarm(AntennaArray antArr, int numParticles) {
        antennaArray = antArr;
        particles = new Particle[numParticles];

        gbest = antennaArray.generateRandomSolution();
        gbestCost = antennaArray.evaluate(gbest);

        // Create particles
        for (int i = 0; i < numParticles; i++) {
            particles[i] = new Particle(antennaArray);
        }
    }

    public double[] swarmSearch(int iterations) {
        // 
        for (int i = 0; i < iterations; i++) {

            for (int j = 0; j < particles.length; j++) {
                double[] pbest = particles[j].updateParticle(gbest);
                double pbestCost = antennaArray.evaluate(pbest);

                if (pbestCost < gbestCost) {
                    gbest = pbest;
                    gbestCost = pbestCost;
                }
            }

            System.out.println(gbestCost);

        }

        return gbest;
    }
}