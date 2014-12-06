import java.io.*;

/**
 * Created by abratin on 11/3/14.
 */
public class botGA extends GenAlg {
    private static final int bitlength = 4*14;
    private static final int popSize = 16;
    private static final int maxIter = (int) Math.pow(10,6);

    private static final String chromosomeFilename = "chromosome.txt";



    public static void main(String[] args) {
        GenAlg ga = new GenAlg(popSize, bitlength, maxIter);
        int[] bestChromosome = ga.runGA();
        try {
            PrintWriter writer = new PrintWriter(chromosomeFilename);
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < bestChromosome.length; i++) {
                s.append(Integer.toString(bestChromosome[i]));
            }
            writer.write(s.toString());
            writer.close();
        } catch (Exception e) {
            System.out.println("Error no File for chromosome.");
        }
    }
}
