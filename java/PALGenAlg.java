import java.io.*;
import java.util.Random;

/**
 * Created by abratin on 12/6/14.
 */
public class PALGenAlg {
    private String[] population;
    private double[] fitnessBias;
    private double[] fitness;
    private int individualsDone = 0;
    private int popFront = 0;
    private int popTail = 0;
    int fpsIndex = 0;
    double prevEnemyScore = 0;

    private static final String serverCall = "../xpilots -map ../maps/simple.xp -noQuit -switchBase 1 -robots 2 ";
    private static final String botCall = "java Bratin4 ";
    private static final int[] fps = {100, 80, 60, 40, 20, 16};
//    private static final int[] ports = {1000, 1100, 1200, 1300, 1400, 1500};
    private Process[] servers;
    private Process server;
    private int popSize;
    private int geneSize;
    private int punctuateRate;
    private int maxIterations;
    private String popFile;
    private static final String defaultPopFile = "StartPopulation.txt";
    private static final String bestChromosomeFilename = "best chromosome.txt";
    private static final int printRate = 10;
    private static final int mutateRate = 300;
    private static final int defaultPopSize = 100;
    private static final int defaultGeneSize = 32;
    private static final int defaultMaxIterations = (int) Math.pow(10,7);
    private static final int defaultPunctuateRate = 500;

    Random rand = new Random();
    private int count = 0;
//    private static final String chromosomeFilename = "chromosome.txt";
//    private static final String bestChromosomeFilename = "best chromosome.txt";


    public PALGenAlg(int popSizeNew, int geneSizeNew, int maxIterationsNew, int PunctuateRateNew) {
        popSize = popSizeNew;
        geneSize = geneSizeNew;
        maxIterations = maxIterationsNew;
        punctuateRate = PunctuateRateNew;
    }

    public PALGenAlg(int popSizeNew, int geneSizeNew, int maxIterationsNew) {
        popSize = popSizeNew;
        geneSize = geneSizeNew;
        maxIterations = maxIterationsNew;
        punctuateRate = defaultPunctuateRate;
    }

    public PALGenAlg(int popSizeNew, int geneSizeNew) {
        popSize = popSizeNew;
        geneSize = geneSizeNew;
        maxIterations = defaultMaxIterations;
        punctuateRate = defaultPunctuateRate;
    }

    public PALGenAlg(int popSizeNew) {
        popSize = popSizeNew;
        geneSize = defaultGeneSize;
        maxIterations = defaultMaxIterations;
        punctuateRate = defaultPunctuateRate;
    }

    public PALGenAlg() {
        popSize = defaultPopSize;
        geneSize = defaultGeneSize;
        maxIterations = defaultMaxIterations;
        punctuateRate = defaultPunctuateRate;
    }

    private void initialize() {
        population = new String[popSize];
        fitness = new double[popSize];
        fitnessBias = new double[popSize];
        for (int i = 0; i < fitnessBias.length; i++) {
            fitnessBias[i] = 1;
        }
    }

    public String runGA() {
        return runGA(false, defaultPopFile);
    }

    public String runGA(boolean readPopFromFile) {
        return runGA(readPopFromFile, defaultPopFile);
    }

    public String runGA(boolean readPopFromFile, String filename) {
        if(readPopFromFile) {
            popFile = filename;
            readPop(popFile);
        } else {
            initialize();
            randomStart();
        }
        while(individualsDone < maxIterations) {
            if(fpsIndex == fps.length) {
                fpsIndex = 1;
            }
            if(individualsDone % punctuateRate == 0) {
                punctuate();
            } else {
                createChoosingList();
                individualsDone++;
            }
        }
        server.destroy();

        return population[bestChild()];
    }

    private void punctuate() {
        startServer(0);
        for (int j = 0; j < popSize; j++) {
            double fitnessNew = calculateFitness(population[j]);
            double fitnessBiasNew = fitnessNew/fitness[j];
            fitness[j] = fitnessNew;
            fitnessBias[j] = fitnessBiasNew;
        }
        if (count % printRate == 0) {
            printStats();
        }
        fpsIndex++;
        startServer(fpsIndex);
    }

    private void startServer(int index) {
        try {
            if(server!=null) {
                server.destroy();
            }
            String call = serverCall + "-fps " + Integer.toString(fps[index]);

            server = Runtime.getRuntime().exec(call);
//            BufferedReader input = new BufferedReader(new InputStreamReader(server.getInputStream()));
//            String line = "";
//            while ((line = input.readLine()) != null) {
//                System.out.println(line);
//            }
//            server.waitFor();
        } catch (Exception e) {
        }
    }


    private void printStats() {
        System.out.println("Iteration: " + count);
        int bestIndex = bestChild();
        System.out.print("Best child: [");
        for (int i = 0; i < geneSize; i++) {
            System.out.print(population[bestIndex]);
        }
        System.out.print("] \n");
        System.out.println("Best Fitness: " + fitness[bestIndex]);
        System.out.println("Average Fitness: " + averageFitness());
        writeChromosome(population[bestChild()], bestChromosomeFilename);
    }

    private void readPop(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = null;
            StringBuilder  stringBuilder = new StringBuilder();
            String ls = System.getProperty("line.separator");

            while( ( line = reader.readLine() ) != null ) {
                stringBuilder.append( line );
                stringBuilder.append( ls );
            }
            String[] pop = stringBuilder.toString().split(ls);
            popSize = pop.length;
            geneSize = pop[0].length();
            initialize();
            population = pop;
            reader.close();
        } catch (IOException e) {
            System.out.println("Error could not read file. Starting GA with random Pop");
            initialize();
            randomStart();
        }
    }

    private void randomStart() {
        for (int i = 0; i < popSize; i++) {
            StringBuilder s = new StringBuilder();
            for (int j = 0; j < geneSize; j++) {
                if(rand.nextInt(1000)>500) {
                    s.append(1);
                } else {
                    s.append(0);
                }
            }
            population[i] = s.toString();
        }
    }

    public double calculateFitness(String chromosome) {
        try {
            System.out.println("Chromosome: " + chromosome);
            String line = "";
            String call = "java BriJadam -training true -chromosome " + chromosome;
            Process bot = Runtime.getRuntime().exec("java BriJadam -training true -chromosome " + chromosome);
            BufferedReader input = new BufferedReader(new InputStreamReader(bot.getInputStream()));
            double enemyScore = -1;
            double botScore = -1;
            while ((line = input.readLine()) != null) {
                if(line.contains("Bot Score")) {
                    System.out.println(line);
                    botScore = Double.parseDouble(line.substring(11,line.length()));
                } else if(line.contains("Enemy Score")) {
                    System.out.println(line);
                    enemyScore = Double.parseDouble(line.substring(13,line.length()));
                    if(enemyScore == Double.NaN) {
                        enemyScore = 0;
                    }
                }
            }
            bot.waitFor();
            double enemyScoreNew = enemyScore - prevEnemyScore;
            prevEnemyScore = enemyScore;
            System.out.println("Fitness: " + Double.toString(botScore - enemyScoreNew));
            return botScore - enemyScoreNew;
        } catch (Exception e) {
            return 0;
        }
    }

    private int bestChild() {
        int bestIndex = 0;
        double bestFitness = fitness[0];
        for (int i = 1; i <popSize; i++) {
            if(fitness[i] > bestFitness) {
                bestFitness = fitness[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private double averageFitness() {
        double sum = 0.0;
        for (int i = 0; i < popSize; i++) {
            sum+= fitness[i];
        }
        return sum/popSize;
    }

    private void writeChromosome(String chromosome, String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        writer.write(chromosome);
        writer.close();
    }

    public void addChrom(String x, double fit, double fitBias)  {

        if (popTail == popFront && count != 0) {
            System.out.println("ERROR");
        }
        population[popTail] = x;
        fitness[popTail] = fit;
        fitnessBias[popTail] = fitBias;
        count++;
        if (popTail == (fitness.length - 1)) {
            popTail = 0;
        } else {
            popTail++;
        }
    }

    public void deleteChrom() {
        count--;
        if(popFront==((fitness.length-1))) {
            popFront = 0;
        } else {
            popFront++;
        }
    }

    public void createChoosingList() {
        double[] choosingList = new double[popSize];

        double min=fitness[0];
        for(int i=1;i<popSize;i++) {
            if(fitness[i]<min) {
                min = fitness[i];
            }
        }
        if(min < 0) {
            choosingList[0] = 0;
        } else {
            choosingList[0] = min;
        }
        for (int i = 1; i < popSize; i++) {
            if(min < 0) {
                choosingList[i] = fitness[i] + choosingList[i-1] - min;
            } else {
                choosingList[i] = fitness[i] + choosingList[i-1];
            }

        }
        double max=choosingList[0];
        for(int i=1;i<popSize;i++) {
            if(choosingList[i] > max) {
                max = choosingList[i];
            }
        }
        double randFit =rand.nextDouble()*max;
        int parent1 = checkIndex(randFit, choosingList);
        int parent2 = parent1;
        while (parent2 == parent1) {
            randFit =rand.nextDouble()*max;
            parent2 = checkIndex(randFit, choosingList);
        }
        crossOver(parent1,parent2);



    }

    private int checkIndex(double randFit, double[] choosingList) {
        for (int i = 0; i < popSize; i++) {
            if(randFit < choosingList[i]) {
                return i;
            }
        }
        return geneSize - 1;
    }

    public void crossOver(int index1,int index2) {
        String chrom1 = population[index1];
        String chrom2 = population[index2];
        int crossPoint = rand.nextInt(chrom1.length());
        String baby1 = "";
        String baby2 = "";
        baby1 = chrom1.substring(0, crossPoint);
        baby2 = chrom2.substring(0, crossPoint);
        baby1 = baby1 + chrom2.substring(crossPoint);
        baby2 = baby2 + chrom1.substring(crossPoint);

        String baby = "";

        if (rand.nextInt(1000) >= 1000 / 2) {
            baby = baby1;
        } else {
            baby = baby2;
        }

        baby = mutate(baby);
        deleteChrom();
        double fit = calculateFitness(population[popTail]);
        double fitBias = (fitness[index1] + fitness[index2]) / 2;
        fit = fit * fitBias;

        addChrom(baby, fit, fitBias);
    }

    private String mutate(String child) {
        for (int i = 0; i < child.length(); i++) {
            if (rand.nextInt(mutateRate) == 1) {
                if (child.charAt(i) == '0') {
                    String bit = "";
                    if (child.charAt(i) == 0) {
                        bit = "1";
                    } else {
                        bit = "0";
                    }
                    if (i == 0) {
                        child = bit + child.substring(1);
                    } else if (i < child.length() - 1) {
                        child = child.substring(0, i) + bit + child.substring(i + 1);
                    } else {
                        child = child.substring(0, i) + bit;
                    }
                }
            }
        }
        return child;
    }
}
