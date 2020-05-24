/**
 * DTU, Course 02224, Real-Time Systems
 * 
 * Simulator for Baggage Sorting System  --- RCX Motor
 *
 * @author hhl
 * @version 1.0 April 26, 2006
 * 
 */
package josx.platform.rcx;  // Simulated

import bagsortsim.BagSortSim;

public class Motor {

	final static int noA = 0;
	final static int noB = 1;
	final static int noC = 2;

	final static char[] name = {'A','B','C'};

	public static Motor A = new Motor(noA);
	public static Motor B = new Motor(noB);
	public static Motor C = new Motor(noC);
  
  public static BagSortSim sim = BagSortSim.getSimulator();

  int no = 0;

  int speed = 0;
  int dir = 0;

  Motor(int _no) { no = _no; }
  
  public final char getId() {
	  return name[no];
  }
  

  public synchronized void setPower(int n) {
    speed = n;
    if (speed != 5) sim.error("Speed of motor " + name[no] + 
    		" is set to " + n + ", not 5");
 }

  public synchronized void forward() {
	    dir = 1;
	  }

	public synchronized boolean isForward() {
  		return dir == 1;
  	}

  public synchronized void backward() {
    dir = -1;
  }

	public synchronized boolean isBackward() {
  		return dir == -1;
  	}

  public synchronized void stop() {
    dir = 0;
  }

  public synchronized void flt() {
    dir = 0;
    sim.warning("Effect of flt() is not simulated.");
  }

  public synchronized void reverseDirection() {
	  if (dir != 0) {
		  dir = -dir;
	  } else
		  sim.warning("Direction reversed while motor " + getId()
				  + " is stopped." + " (Has no effect)");
  }

  /* Extra inspection methods for simulator */
  
  public synchronized int getDirection() {
	  if (speed == 0) return 0;
	  return dir;
  }

}


