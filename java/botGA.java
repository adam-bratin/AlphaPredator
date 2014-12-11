import java.io.*;

/**
 * Created by abratin on 11/3/14.
 */
public class botGA extends PALGenAlg {
    private static final int bitlength = 370;
    private static final int popSize = 100;
    private static final int maxIter = (int) Math.pow(10,7);

    private static final String chromosomeFilename = "chromosome.txt";



    public static void main(String[] args) {
        PALGenAlg ga = new PALGenAlg(popSize, bitlength, maxIter);
        String bestChromosome = ga.runGA();
        try {
            PrintWriter writer = new PrintWriter(chromosomeFilename);
            writer.write(bestChromosome);
            writer.close();
        } catch (Exception e) {
            System.out.println("Error no File for chromosome.");
        }
    }
}
