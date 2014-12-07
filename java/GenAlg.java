import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.*;

public class GenAlg{

    /**
     * Created by abratin on 10/26/14.
     */
    private int popSize;
    private int geneSize;
    private int maxIterations;
    private String popFile;
    private static final String defaultPopFile = "StartPopulation.txt";
    private static final int printRate = 10;
    private static final int mutateRate = 300;
    private static final int defaultPopSize = 100;
    private static final int defaultGeneSize = 32;
    private static final int defaultMaxIterations = 10000;
    private static final int fps = 32;
    private int[][] population;
    private double[] fitness;
    Random rand = new Random();
    private int count = 0;
    private int popCount = 0;
    private static File file = new File("score.txt");
    private static final String chromosomeFilename = "chromosome.txt";
    private static final String bestChromosomeFilename = "best chromosome.txt";

    public GenAlg(int popSizeNew, int geneSizeNew, int maxIterationsNew) {
        popSize = popSizeNew;
        geneSize = geneSizeNew;
        maxIterations = maxIterationsNew;
    }

    public GenAlg(int popSizeNew, int geneSizeNew) {
        popSize = popSizeNew;
        geneSize = geneSizeNew;
        maxIterations = defaultMaxIterations;
    }

    public GenAlg(int popSizeNew) {
        popSize = popSizeNew;
        geneSize = defaultGeneSize;
        maxIterations = defaultMaxIterations;
    }

    public GenAlg() {
        popSize = defaultPopSize;
        geneSize = defaultGeneSize;
        maxIterations = defaultMaxIterations;
    }

    private void initialze() {
        population = new int[popSize][geneSize];
        fitness = new double[popSize];
    }

    public int[] runGA() {
        return runGA(false, defaultPopFile);
    }

    public int[] runGA(boolean readPopFromFile) {
        return runGA(readPopFromFile, defaultPopFile);
    }

    public int[] runGA(boolean readPopFromFile, String filename) {
        popFile = filename;
        if (readPopFromFile) {
            readPop(popFile);
        } else {
            initialze();
            randomStart();
            for (int i = 0; i < maxIterations; i++) {
                count++;
                for (int j = 0; j < popSize; j++) {
                    fitness[j] = calculateFitness(population[j]);
                }
                if(count % printRate == 0) {
                    printStats();
                }

//                int[][] populationNew = new int[popSize][geneSize];
//                for (int j = 0; j < popSize; j+=2) {
//                    int[] parents = getRandomParents();
//                    int[] child = generationalCrossover(parents);
//                    populationNew[j] = child;
//                }
//                population = populationNew;
                int[] parents = getRandomParents();
                nonGenerationalCrossover(parents);
                writeChromosome(population[bestChild()], bestChromosomeFilename);
            }
        }
        return population[bestChild()];
    }

    private void printStats() {
        System.out.println("Iteration: " + count);
        int bestIndex = bestChild();
        System.out.print("Best child: [");
        for (int i = 0; i < geneSize; i++) {
            System.out.print(population[bestIndex][i]);
        }
        System.out.print("] \n");
        System.out.println("Best Fitness: " + fitness[bestIndex]);
        System.out.println("Average Fitness: " + averageFitness());
    }

//    public double calculateFitness(int[] chromosome) {
//        int oneCount = 0;
//        for (int i = 0; i < geneSize; i++) {
//            if (chromosome[i] == 1) {
//                oneCount++;
//            }
//        }
//        return Math.pow(oneCount, 2);
//    }

    public double calculateFitness(int[] chromosome) {
        writeChromosome(chromosome, chromosomeFilename);
        popCount++;
        try {
            String line = "";
            Process server = Runtime.getRuntime().exec("../xpilots -map ../maps/simple.xp -robots 2 -FPS" + Integer.toString(fps) + "-switchBase 1");
            Process bot = Runtime.getRuntime().exec("java Bratin_Program4 -training true");
            BufferedReader input = new BufferedReader(new InputStreamReader(bot.getInputStream()));
            double enemyScore = -1;
            double botScore = -1;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
                if(line.contains("Bot Score")) {
                    botScore = Double.parseDouble(line.substring(11,line.length()));
                } else if(line.contains("Enemy Score")) {
                    enemyScore = Double.parseDouble(line.substring(13,line.length()));
                    if(enemyScore == Double.NaN) {
                        enemyScore = 0;
                    }
                }
            }
            bot.waitFor();
            return botScore - enemyScore;
        } catch (Exception e) {
            return 0;
        }
    }

    private void writeChromosome(int[] chromosome, String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < chromosome.length; i++) {
            s.append(Integer.toString(chromosome[i]));
        }
        writer.write(s.toString());
        writer.close();
    }

    private double[] readScore() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            double scorebot = Double.parseDouble(reader.readLine());
            double scoreEnemy = Double.parseDouble(reader.readLine());
            return new double[] {scorebot, scoreEnemy};
        } catch (Exception e) {
            return new double[] {0, 0};
        }
    }

    private double averageFitness() {
        double sum = 0.0;
        for (int i = 0; i < popSize; i++) {
            sum+= fitness[i];
        }
        return sum/popSize;
    }

    private void writePop() {
        try {
            PrintWriter writer = new PrintWriter(popFile);
            for (int i = 0; i < popSize; i++) {
                StringBuilder s = new StringBuilder();
                for (int j = 0; j < geneSize; j++) {
                    s.append(population[i][j]);
                }
                writer.println(s.toString());
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error: unable to write to file");
        }
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
            population = new int[popSize][geneSize];
            fitness = new double[popSize];
            for (int i = 0; i <popSize; i++) {
                for (int j = 0; j < geneSize; j++) {
                    population[i][j] = Character.getNumericValue(pop[i].charAt(j));
                }
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error could not read file. Starting GA with random Pop");
            initialze();
            randomStart();
        }

    }

    private void randomStart() {
        for (int i = 0; i < popSize; i++) {
            for (int j = 0; j < geneSize; j++) {
                if(rand.nextInt(1000)>500) {
                    population[i][j] = 1;
                } else {
                    population[i][j] = 0;
                }
            }
        }
    }

    private boolean checkFitness() {
        for (int i = 0; i < popSize; i++) {
            if(fitness[i] == geneSize) {
                return true;
            }
        }
        return false;
    }

    private int[] getRandomParents() {
        double[] parentPicker = new double[popSize];
        double minFitness = findMinFitness();
        parentPicker[0] = fitness[0] - minFitness;
        for (int i = 1; i < popSize; i++) {
            parentPicker[i] = parentPicker[i-1] + fitness[i] - minFitness;
        }
        System.out.println(parentPicker[popSize-1]);
        int randFit = rand.nextInt((int) parentPicker[popSize-1]);
        int parent1 = checkIndex(randFit, parentPicker);
        int parent2 = parent1;
        while (parent2 == parent1) {
            randFit = rand.nextInt((int) parentPicker[popSize-1]);
            parent2 = checkIndex(randFit, parentPicker);
        }
        return new int[]{parent1, parent2};
    }

    private double findMinFitness() {
        double minFitness = fitness[0];
        for (int i = 0; i < popSize; i++) {
            if(fitness[i] < minFitness) {
                minFitness = fitness[i];
            }
        }
        return minFitness;
    }

    private int checkIndex(int randFit, double[] parentPicker) {
        for (int i = 0; i < popSize; i++) {
            if(randFit < parentPicker[i]) {
                return i;
            }
        }
        return geneSize - 1;
    }

    private int[] generationalCrossover(int[] parents) {
        int crossoverPoint = rand.nextInt(geneSize);
//        int[][] children = new int[2][geneSize];
        int[] child = new int[geneSize];
        int leftIndex = 0;
        int rightIndex = 0;
        if(rand.nextInt(10000) > 10000/2) {
            leftIndex = 0;
            rightIndex = 1;
        } else {
            leftIndex = 1;
            rightIndex = 0;
        }
        for (int i = 0; i < crossoverPoint; i++) {
            child[i] = population[parents[leftIndex]][i];

        }
        for (int i = crossoverPoint; i < geneSize; i++) {
            child[i] = population[parents[rightIndex]][i];
        }
        child = mutate(child);
        return child;
    }

    private void nonGenerationalCrossover(int[] parents) {
        int crossoverPoint = rand.nextInt(geneSize);
        int[][] children = new int[2][geneSize];
        int[] child = new int[geneSize];
        int leftIndex = 0;
        int rightIndex = 0;
        if(rand.nextInt(10000) > 10000/2) {
            leftIndex = 0;
            rightIndex = 1;
        } else {
            leftIndex = 1;
            rightIndex = 0;
        }
        for (int i = 0; i < crossoverPoint; i++) {
            children[0][i] = population[parents[leftIndex]][i];
            children[1][i] = population[parents[rightIndex]][i];
        }
        for (int i = crossoverPoint; i < geneSize; i++) {
            children[1][i] = population[parents[rightIndex]][i];
            children[1][i] = population[parents[leftIndex]][i];
        }
        for (int i = 0; i < children.length; i++) {
            children[i] = mutate(children[i]);
        }

        int[][] chromosomes = new int[][] {children[0], children[1], population[parents[0]], population[parents[1]]};
        double[] fitnessNew = new double[4];
        fitnessNew[2] = fitness[parents[0]];
        fitnessNew[3] = fitness[parents[1]];
        for (int i = 0; i < chromosomes.length; i++) {
            fitnessNew[i] = calculateFitness(chromosomes[i]);
        }
        int[][] chromosomesNew = findBestChildren(chromosomes, fitnessNew);
        for (int i = 0; i < chromosomesNew.length; i++) {
            population[parents[i]] = chromosomesNew[i];
        }
    }

    private int[][] findBestChildren(int[][] chromosomes, double[] fitnessNew) {
        popCount = 0;
        int index = 0;
       for (int i = (chromosomes.length - 1); i >= 0; i--) {
           for (int j = 1; j <= i; j++) {
               if (fitnessNew[j - 1] > fitnessNew[j]) {
                   int[] temp = chromosomes[j - 1];
                   chromosomes[j - 1] = chromosomes[j];
                   chromosomes[j] = temp;
               }
           }
       }
        return new int[][] {chromosomes[2], chromosomes[3]};
    }

    private int findChromosomeLocation(int[] chromosome1, int[][] chromosomes) {
        for (int i = 0; i < popSize; i++) {
            if(chromosome1.equals(chromosomes[i])) {
                return i;
            }
        }
        return -1;
    }

    private int[] mutate(int[] child) {
        for (int i = 0; i < child.length; i++) {
            if (rand.nextInt(mutateRate) == 1) {
                if(child[i] == 0) {
                    child[i] = 1;
                } else {
                    child[i] = 0;
                }
            }
        }
        return child;
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
}
