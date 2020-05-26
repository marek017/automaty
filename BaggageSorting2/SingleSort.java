import java.lang.System;

import josx.platform.rcx.*;

class DC extends Thread {
    private static boolean lastDestinationA = true;
    private static String lastBelt = "LEFT";
    private static Timeout tockCount;
    private static Motor c;

    public DC(Motor m, Timeout t){
        tockCount = t;
        c = m;
    }

    static synchronized int passBag(boolean bagColor, String localBelt) {
        boolean destA = bagColor==true ? true : false;
        int done = 0;
        if ((lastDestinationA && !destA) || (!lastDestinationA && destA)){
            done = ((lastBelt.equals("LEFT") && lastDestinationA) ||
                    (lastBelt.equals("RIGHT") && !lastDestinationA))? 52 : 105;
            tockCount.Tick(done);
            Motor.C.reverseDirection();
            done = 0;
        }else{
            done = ((lastBelt.equals("LEFT") && !localBelt.equals("LEFT") && !lastDestinationA) ||
                    (lastBelt.equals("RIGHT") && !localBelt.equals("RIGHT") && lastDestinationA))? 82 : 28;
        }
        lastBelt = localBelt;
        lastDestinationA = destA;
        return done;
    }
}

class Timeout extends Thread {
    public Timeout(){};

    synchronized boolean Tick(int tockCount) {
        try {
            while(tockCount > 0){
                Thread.sleep(100);
                tockCount = tockCount - 1;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}

class Feeder extends Thread {
    static final int BLOCKED = 70, YELLOW = 60, BLACK = 45;

    Motor myMotor;
    Sensor mySensor;
    DC DistributionController;
    Timeout time;

    public Feeder(Motor m, Sensor s, DC d, Timeout t){
        myMotor = m;
        mySensor = s;
        DistributionController = d;
        time = t;
    }

    public void run() {
        try {
            final boolean closeToA = (myMotor == Motor.A);
            final int myMask = closeToA ? Poll.SENSOR1_MASK : Poll.SENSOR2_MASK;
            String belt = closeToA ? "LEFT" : "RIGHT";

            Poll e = new Poll();
            int done = (int) System.currentTimeMillis(); // When last bag is through

            int waitTillRelease = 0;

            myMotor.forward();

            while (true) {
                // Await arrival of a bag
                mySensor.activate();
                while (mySensor.readValue() > BLOCKED) {
                    e.poll(myMask, 0);
                }

                Thread.sleep(800);           // Wait for colour to be valid

                boolean bagColor = (mySensor.readValue() <= BLACK);   // Determine destination
                mySensor.passivate();

                Thread.sleep(2000);

                myMotor.stop();
                waitTillRelease = DC.passBag(bagColor, belt);
                if(waitTillRelease > 0){
                    time.Tick(waitTillRelease);
                }
                myMotor.forward();
                Thread.sleep(1200);                 // Follow to end of feed belt
            }
        } catch (Exception e) {
        }
    }
}

public class SingleSort {

    static final int BELT_SPEED = 5;          // Do not change

    public static void main(String[] arg) {
        Motor.A.setPower(BELT_SPEED);
        Motor.B.setPower(BELT_SPEED);
        Motor.C.setPower(BELT_SPEED);
        Motor.C.forward();

        Timeout t = new Timeout();
        DC f3 = new DC(Motor.C, t);
        Thread f1 = new Feeder(Motor.A, Sensor.S1, f3, t);
        f1.start();
        Thread f2 = new Feeder(Motor.B, Sensor.S2, f3, t);
        f2.start();
        try {
            Button.RUN.waitForPressAndRelease();
        } catch (Exception e) {
        }
        System.exit(0);
    }
}