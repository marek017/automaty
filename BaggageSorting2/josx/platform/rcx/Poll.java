/**
 * DTU, Course 02224, Real-Time Systems
 * 
 * Simulator for Baggage Sorting System  --- RCS Poll mechanism
 *
 * @author hhl
 * @version 1.0 April 26, 2006
 * 
 */

package josx.platform.rcx;


/**
 * RCX access classes.
 *
 * Adopted from josx.platform.rcx for BagSortSim /hhl
 */

/**
 * Provides blocking access to events from the RCX. Poll is a bit
 * of a misnomer (since you don't 'poll' at all) but it takes its
 * name from the Unix call of the same name.
 */

public class Poll {

  public static final short SENSOR1_MASK = 0x01;
  public static final short SENSOR2_MASK = 0x02;
  public static final short SENSOR3_MASK = 0x04;
  public static final short ALL_SENSORS  = 0x07;

  public static final short RUN_MASK     = 0x08;
  public static final short VIEW_MASK    = 0x10;
  public static final short PRGM_MASK    = 0x20;
  public static final short ALL_BUTTONS  = 0x38;
  public static final short BUTTON_MASK_SHIFT  = 3;
  
  public static final short SERIAL_MASK = 0x40;
  public static final short SERIAL_SHIFT = 6;


  private static Object monitor = new Object();
  
  private static short changed;  // The 'changed' mask.


  public final int poll(int mask, int millis) throws InterruptedException {
	  synchronized (monitor) {
		  int ret = mask & Poll.changed;
		  
		  // The inputs we're interested in may have already changed
		  // since we last looked so check before we wait.
		  while (ret == 0) {
			  monitor.wait(millis);
			  
			  // Work out what's changed that we're interested in.
			  ret = mask & Poll.changed;
		  }
		  
		  // Clear the bits that we're monitoring. If anyone else
		  // is also monitoring these bits its tough.
		  Poll.changed &= ~mask;
		  return ret;
	  }
  }

  public final void setThrottle(int throttle){
    // Dummy in simulation
  }

  public static void change(int mask) {
    synchronized (monitor) {
      changed |= mask;
      monitor.notifyAll();
    }
  }

}









