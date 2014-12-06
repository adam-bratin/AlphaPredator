import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Created by abratin on 11/2/14.
 */
public class Bratin_Program4 extends  javaAI {
    private static final String chromosomeFilename = "chromosome.txt";
    private static final String scoreFilename= "score.txt";

    private static final int maxTurn = 20;

    private static final int ruleCount = 14;
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

    private static final int wallFeelerDist = 1000;

    private boolean[] rulesFired;
    private static int[] rulePriority;

    private static boolean training = false;
    private static final int fps = 32;
    private static final int maxRunTime = fps * 20;
    private double score;
    private int count = 0;

    public Bratin_Program4(String args[], String chromosome, boolean trainingNew) {
        rulePriority = new int[ruleCount];
        if(chromosome.length() == ruleCount * 4) {
            calculateRulePriority(chromosome);
        } else {
            throw new IllegalArgumentException("Error: Wrong number of rules");
        }
        training = trainingNew;
         new Bratin_Program4(args, chromosome, trainingNew, true);
    }

    public Bratin_Program4 (String args[], String chromosome, boolean trainingNew, boolean startBot) {
        super(args);
    }
    private void startBot(String args[]){

    }

    private void calculateRulePriority(String chromosome) {
        int index = 0;
        for(int i = 0; i < chromosome.length(); i+=4) {
            rulePriority[index] = Integer.parseInt(chromosome.substring(i,i+4),2);
            index++;
        }

    }

    public static void main(String args[]) {
        String[] new_args;
        boolean train = false;
        if (args.length == 0) {
            new_args = new String[] {"-name", "Bratin"};
        } else {
            new_args = new String[] {"-name", "Bratin", "-join", "localhost"};
            boolean setTrain = false;
            for (int i = 0; i < args.length; i++) {
                if("-training".equals(args[i])) {
                    train = Boolean.valueOf(args[i + 1]);
                    setTrain = true;
                    if (setTrain) {
                        i = args.length;
                    }
                }
            }
        }
        String chromosome = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(chromosomeFilename));
            chromosome = reader.readLine();
            reader.close();
        } catch (Exception e) {
            System.out.println("Error no File for chromosome.");
        }
        Bratin_Program4 Bratin = new Bratin_Program4(new_args, chromosome, train);
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

    private int findMaxRule() {
        int maxRule = 0;
        int maxPriority = rulePriority[0];
        for (int i = 1; i < ruleCount; i++) {
            if(rulePriority[i] > maxPriority) {
                maxPriority = rulePriority[i];
                maxRule = i;
            } else if(rulePriority[i] == maxRule) {
                if(Math.random() > .5) {
                    maxPriority = rulePriority[i];
                    maxRule = i;
                }
            }
        }
        return maxRule;
    }

    private void rulecalc() {
        rulesFired = new boolean[ruleCount];
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
                rulesFired[i] = rule6(closestEnemyID, closestEnemyDist);
            } else if(i == 7) {
                rulesFired[i] = rule7(closestEnemyID, closestEnemyDist);;
            } else if(i == 8) {
                rulesFired[i] = rule8(shotDist, closestEnemyDist);
            } else if(i == 9) {
                rulesFired[i] = rule9(shotDist, closestEnemyDist);
            } else if(i == 10) {
                rulesFired[i] = rule10(closestEnemyDist);
            } else if(i == 11) {
                rulesFired[i] = rule11(closestEnemyDist);
            } else if(i == 12) {
                rulesFired[i] = rule12(feeler, shotDist, closestEnemyDist);
            } else if(i == 13) {
                rulesFired[i] = rule13();
            } else if(i == 14) {
                rulesFired[i] = rule14();
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
            turnToEnemy();
        } else if (maxRule == 7) {
            turnToEnemy();
        } else if (maxRule == 8) {
            turnFromBulletAndToEnemy();
        } else if (maxRule == 9) {
            turnFromBullet();
        } else if (maxRule == 10) {
            turnToEnemy();
        } else if (maxRule == 11) {
            turnToEnemy();
        } else if (maxRule == 12) {
            thrust(0);
        } else if (maxRule == 13) {
            turn90Deg("Right");
        } else if (maxRule == 14) {
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

    private boolean rule1(double feeler) {
        return (feeler <= veryClose);
    }

    private boolean rule2(double feeler) {
        return (feeler <= somewhatClose && feeler > veryClose);
    }

    private boolean rule3(double feeler) {
        return (feeler >= medium);
    }

    private boolean rule4(double shotDist) {
        return (shotDist > 0 && shotDist <= veryClose);
    }

    private boolean rule5(double shotDist) {
        return (shotDist > 0 && shotDist <= medium && shotDist > veryClose);
    }

    private boolean rule6(int closestEnemyID, double closestEnemyDist) {
        return (closestEnemyID == previousClosestEnemyID && closestEnemyDist < previousClosestEnemyDist);
    }

    private boolean rule7(int closestEnemyID, double closestEnemyDist) {
        return (closestEnemyID == previousClosestEnemyID && closestEnemyDist >= previousClosestEnemyDist);
    }

    private boolean rule8(double shotDist, double closestEnemyDist) {
        return (closestEnemyDist <= veryClose && shotDist <= veryClose);
    }

    private boolean rule9(double shotDist, double closestEnemyDist) {
        return (closestEnemyDist >= far && shotDist <= veryClose);
    }

    private boolean rule10(double closestEnemyDist) {
        return (closestEnemyDist <= medium);
    }

    private boolean rule11(double closestEnemyDist) {
        return (closestEnemyDist > medium);
    }

    private boolean rule12(double feeler, double shotDist, double closestEnemyDist) {
        return (shotDist > 0 && feeler == wallFeelerDist && closestEnemyDist == 0);
    }

    private boolean rule13() {
        return (wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), 90)) < medium);
    }

    private boolean rule14() {
        return (wallFeeler(wallFeelerDist, angleAdd((int) selfHeadingDeg(), -90)) < medium);
    }
}