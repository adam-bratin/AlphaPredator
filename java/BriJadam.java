import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Created by abratin on 12/8/14.
 */
public class BriJadam  extends  javaAI {
    private static final String chromosomeFilename = "chromosome.txt";
    private static final String scoreFilename= "score.txt";
    private static final int chromosomeLength = 320;

    private static final int maxTurn = 20;

    private static final int ruleCount = 12;
    private static final int veryClose = 150;
    private static final int somewhatClose = 300;
    private static final int medium = 500;
    private static final int far = 700;

    private boolean previousTurn = false;
    private int previousTurnAngle;
    private int previousTurnProgress;
    private String turnType = "";

    private int previousClosestEnemyID;
    private double previousClosestEnemyDist;

    private static final int wallFeelerDist = 1024;

    private double[] rulesFired;
    private static int[] rulePriority;
    private static int[] fuzzySet;

    private static boolean training = false;
    private static final int fps = 32;
    private static final int maxRunTime = fps * 20;
    private double score;
    private int count = 0;

    public BriJadam(String args[], String chromosome, boolean trainingNew) {
        rulePriority = new int[ruleCount];
        if(chromosome.length() == ruleCount * 4) {
            calculateRulePriority(chromosome);
        } else {
            throw new IllegalArgumentException("Error: Wrong number of rules");
        }
        training = trainingNew;
        new BriJadam(args, chromosome, trainingNew, true);
    }

    public BriJadam (String args[], String chromosome, boolean trainingNew, boolean startBot) {
        super(args);
    }

    private void calculateRulePriority(String chromosome) {
        int index = 0;
        for(int i = 0; i < chromosome.length(); i+=4) {
            rulePriority[index] = Integer.parseInt(chromosome.substring(i,i+4),2);
            index++;
        }

    }

    public static void main(String args[]) {
        String[] new_args = null;
        String chromosome = "";
        boolean train = false;
        if (args.length > 0) {
            for (int i = 0; i < args.length; i+=2) {
                if(args[i].contentEquals("-chromosome")) {
                    chromosome = args[i+1];
                }
                if(args[i].contentEquals("-training") && args[i].contentEquals("true")) {
                    new_args = new String[] {"-name", "Bratin", "-join", "localhost"};
                }
                else if (args[i].contentEquals("-training")){
                    new_args = new String[] {"-name", "Bratin"};
                    train = true;
                }
            }
            if(new_args == null) {
                new_args = new String[]{"-name", "Bratin"};
            }
        } else {
            new_args = new String[] {"-name", "Bratin"};
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < chromosomeLength; i++) {
                s.append('1');
            }
            chromosome = s.toString();
        }
        parseChromosome(chromosome);
        BriJadam bot = new BriJadam(new_args, chromosome, train);
    }

    private static void parseChromosome(String chromosome) {
        int index = 0;
        int distIndex = 0;
        int bitLength = 10;
        fuzzySet = new int[10];
        int i;
        for (i = 0; i < fuzzySet.length*bitLength; i+=10) {
            if(distIndex == 0 || distIndex == 2 || distIndex == 5 || distIndex == 8) {
                fuzzySet[distIndex] = Integer.parseInt(chromosome.substring(i, i + bitLength), 2);
            } else {
                int prev;
                if(distIndex == 1) {
                    prev = 0;
                } else if(distIndex == 3 || distIndex == 4) {
                    prev = 2;
                } else if(distIndex == 6 || distIndex == 7) {
                    prev = 5;
                } else {
                    prev = 8;
                }
                fuzzySet[distIndex] = Integer.parseInt(chromosome.substring(i, i + bitLength), 2) + fuzzySet[prev];
                if(fuzzySet[distIndex] > wallFeelerDist) {
                    fuzzySet[distIndex] = wallFeelerDist;
                }
            }
            distIndex++;
        }
        for (int j = i; j < chromosome.length(); j+=5) {
            rulePriority[index] = Integer.parseInt(chromosome.substring(j, i + 5), 2);
            index++;
        }
    }

    @Override
    public void AI_loop() {
        count++;
        if(training) {
            headlessMode();
            keyHome();
        }
        if (count >= maxRunTime && training) {
            System.out.println("Bot Score: " + selfScore());
            System.out.println("Enemy Score : " + enemyScoreId(previousClosestEnemyID));
            quitAI();
            System.exit(0);
        }
//        System.out.println("Frame: " + count);
        setPower(3);
        setTurnSpeed(maxTurn);
        lockClose();
        rulecalc();
        int maxRule = findMaxRule();
        runRule(maxRule);
        determineShoot();
    }

    private void writeScore() {
        try {
            PrintWriter writer = new PrintWriter(scoreFilename);
            writer.write(Double.toString(selfScore()) + "\n");
            writer. write(Double.toString(enemyScoreId(previousClosestEnemyID)));
            writer.close();
        } catch (Exception e) {

        }
    }

    private void determineShoot() {
        int enemyAngle = angleDiff((int) selfHeadingDeg(), (int) lockHeadingDeg());
        if(Math.abs(enemyAngle) <= maxTurn) {
            fireShot();
        }
    }

    private double calcClose(double proximity) {
        //closeLevelDist = 0, closeEndDist = 1
        if (proximity <= fuzzySet[0]) {
            return 1;
        } else if (proximity > fuzzySet[0] && proximity <= fuzzySet[1]) {
            return 1 - (proximity - fuzzySet[0] / (fuzzySet[1] - fuzzySet[0]));
        } else {
            return 0;
        }
    }

    private double calcSomewhateClose(double proximity) {
        //somewhatCloseStart = 2, somewhatCloseTop = 3, somewhatCloseEnd = 4
        if (proximity < fuzzySet[2]) {
            return 0;
        } else if (proximity >= fuzzySet[2] && proximity <= fuzzySet[3]) {
            return (proximity - fuzzySet[2] / (fuzzySet[3] - fuzzySet[2]));
        } else if (proximity > fuzzySet[3] && proximity <= fuzzySet[4]) {
            return 1 - (proximity - fuzzySet[3] / (fuzzySet[4] - fuzzySet[3]));
        } else {
            return 0;
        }
    }

    private double calcMedium(double proximity) {
        //midStart = 5, midTop = 6, midEnd = 7
        if (proximity < fuzzySet[5]) {
            return 0;
        } else if (proximity >= fuzzySet[5] && proximity <= fuzzySet[6]) {
            return (proximity - fuzzySet[5] / (fuzzySet[6] - fuzzySet[5]));
        } else if (proximity > fuzzySet[6] && proximity <= fuzzySet[7]) {
            return 1 - (proximity - fuzzySet[6] / (fuzzySet[7] - fuzzySet[6]));
        } else {
            return 0;
        }
    }

    private double calcFar(double wallProximity) {
        //farStart = 8, farLevel = 9
        if (wallProximity < fuzzySet[8]) {
            return 0;
        } else if (wallProximity >= fuzzySet[8] && wallProximity < fuzzySet[9]) {
            return wallProximity - fuzzySet[8] / (fuzzySet[9] - fuzzySet[8]);
        } else {
            return 1;
        }
    }

    private int findMaxRule() {
        int maxRule = 0;
        double maxProb = rulePriority[0] * rulesFired[0];
        for (int i = 1; i < ruleCount; i++) {
            double fireProb = rulePriority[i] * rulesFired[i];
            if(fireProb > maxProb) {
                maxProb = fireProb;
                maxRule = i;
            } else if(fireProb == maxProb) {
                if(Math.random() > .5) {
                    maxProb = fireProb;
                    maxRule = i;
                }
            }
        }
        return maxRule;
    }

    private void rulecalc() {
        rulesFired = new double[ruleCount];
        for (int i = 0; i < ruleCount; i++) {
            double feeler = wallFeeler(wallFeelerDist, (int) selfHeadingDeg());
            double shotDist = shotDist(0);
            int closestEnemyID = closestShipId();
            double closestEnemyDist = enemyDistanceId(closestEnemyID);
            if(i==1) {
                rulesFired[i] = rule1(feeler);
            } else if(i == 2) {
                rulesFired[i] = rule2(feeler);
            } else if(i == 3) {
                rulesFired[i] = rule3(feeler);
            } else if(i == 4) {
                rulesFired[i] = rule4(shotDist);
            } else if(i == 5) {
                rulesFired[i] = rule5(shotDist);
            } else if(i == 6) {
                rulesFired[i] = rule6(shotDist, closestEnemyDist);
            } else if(i == 7) {
                rulesFired[i] = rule7(shotDist, closestEnemyDist);
            } else if(i == 8) {
                rulesFired[i] = rule8(closestEnemyDist);
            } else if(i == 9) {
                rulesFired[i] = rule9(closestEnemyDist);
            } else if(i == 10) {
                rulesFired[i] = rule10(feeler, shotDist, closestEnemyDist);
            } else if(i == 11) {
                rulesFired[i] = rule11();
            } else if(i == 12) {
                rulesFired[i] = rule12();
            }
            previousClosestEnemyID = closestEnemyID;
            previousClosestEnemyDist = closestEnemyDist;
        }
    }

    private void runRule(int maxRule) {
        turnLeft(0);
        turnRight(0);
        if (maxRule == 1) {
            turnBackwards();
        } else if (maxRule == 2) {
            turnBackwards();
        } else if (maxRule == 3) {
            turnToEnemy();
        } else if (maxRule == 4) {
            turnFromBullet();
        } else if (maxRule == 5) {
            turnFromBullet();
        } else if (maxRule == 6) {
            turnFromBulletAndToEnemy();
        } else if (maxRule == 7) {
            turnFromBullet();
        } else if (maxRule == 8) {
            turnToEnemy();
        } else if (maxRule == 9) {
            turnToEnemy();
        } else if (maxRule == 10) {
            thrust(0);
        } else if (maxRule == 11) {
            turn90Deg("Right");
        } else if (maxRule == 12) {
            turn90Deg("Left");
        }
    }

    private void turn90Deg(String turnType) {
        thrust(0);
        if (!previousTurn) {
            if (turnType.equalsIgnoreCase("Right")) {
                turnRight(1);
            } else {
                turnLeft(1);
            }
            previousTurn = true;
            previousTurnAngle = 90;
            previousTurnProgress = maxTurn;
        } else {
            continueTurn();
        }
    }

    private void turnBackwards() {
        thrust(0);
        if (!previousTurn) {
            double leftFeeller = wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), 90));
            double rightFeeller = wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), 90));
            if (leftFeeller < rightFeeller) {
                turnRight(1);
                turnType = "Right";
            } else if (leftFeeller > rightFeeller) {
                turnLeft(1);
                turnType = "Left";
            } else {
                if (Math.random() > .5) {
                    turnLeft(1);
                    turnType = "Left";
                } else {
                    turnRight(1);
                    turnType = "Right";
                }
            }
            previousTurn = true;
            previousTurnAngle = 180;
            previousTurnProgress = maxTurn;
        } else {
            continueTurn();
        }
    }

    private void continueTurn() {
        if(previousTurnProgress == previousTurnAngle) {
            thrust(1);
            previousTurn = false;
            previousTurnAngle = 0;
            previousTurnProgress = 0;
        } else {
            double turnLeft = previousTurnAngle - previousTurnProgress;
            if(turnLeft < maxTurn) {
                setTurnSpeed(turnLeft);
            }
            if (turnType.equalsIgnoreCase("Left")) {
                turnLeft(1);
                previousTurnProgress += maxTurn;
            } else {
                turnRight(1);
                previousTurnProgress += maxTurn;
            }
        }
    }

    private void turnToEnemy() {
        if(!previousTurn) {
            int enemyAngle = angleDiff((int) selfHeadingDeg(), (int) lockHeadingDeg());
            if (Math.abs(enemyAngle) < maxTurn) {
                setTurnSpeed(Math.abs(enemyAngle));
            }
            if (enemyAngle < 0) {
                turnRight(1);
            } else {
                turnLeft(1);
            }
        } else {
            continueTurn();
        }
    }

    private void turnFromBullet() {
        if (!previousTurn) {
            int bulletAngle = shotVelDir(0);
            if (bulletAngle == selfHeadingDeg() || bulletAngle == angleAdd((int) selfHeadingDeg(), 180)) {
                if (Math.random() > .5) {
                    turnLeft(1);
                } else {
                    turnRight(1);
                }
            }
            thrust(1);
        } else {
            continueTurn();
        }
    }

    private void turnFromBulletAndToEnemy() {
        if (!previousTurn) {
            int bulletAngle = shotVelDir(0);
            if (bulletAngle == selfHeadingDeg() || bulletAngle == angleAdd((int) selfHeadingDeg(), 180)) {
                int enemyAngle = angleDiff((int) selfHeadingDeg(), (int) lockHeadingDeg());
                if(enemyAngle!=0) {
                    turnToEnemy();
                } else {
                    if (Math.random() > .5) {
                        turnLeft(1);
                    } else {
                        turnRight(1);
                    }
                }
            }
            thrust(1);
        } else {
            continueTurn();
        }
    }

    private double rule1(double feeler) {
        return calcClose(feeler);
    }

    private double rule2(double feeler) {
        return Math.min(calcClose(feeler), calcSomewhateClose(feeler));
    }

    private double rule3(double feeler) {
        return calcMedium(feeler);
    }

    private double rule4(double shotDist) {
        return calcClose(shotDist);
    }

    private double rule5(double shotDist) {
        if(shotDist > 0) {
            return Math.min(calcMedium(shotDist), calcClose(shotDist));
        } else {
            return 0;
        }
    }

    private double rule6(double shotDist, double closestEnemyDist) {
        return Math.min(calcClose(shotDist), calcClose(closestEnemyDist));
    }

    private double rule7(double shotDist, double closestEnemyDist) {
        return Math.min(calcClose(shotDist), calcFar(closestEnemyDist));
    }

    private double rule8(double closestEnemyDist) {
        return calcMedium(closestEnemyDist);
    }

    private double rule9(double closestEnemyDist) {
        return calcFar(closestEnemyDist);
    }

    private double rule10(double feeler, double shotDist, double closestEnemyDist) {
        if(shotDist>0) {
            return Math.min(calcFar(feeler), calcFar(closestEnemyDist));
        } else {
            return 0;
        }
    }

    private double rule11() {
        return(calcMedium(wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), 90))));
    }

    private double rule12() {
        return (calcMedium(wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), -90))));
    }
}
