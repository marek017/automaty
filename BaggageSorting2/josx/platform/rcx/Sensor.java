/**
 * DTU, Course 02224, Real-Time Systems
 * 
 * Simulator for Baggage Sorting System  --- RCX Sensor
 *
 * @author hhl
 * @version 2.0 April 06, 2018
 * 
 */

package josx.platform.rcx;

public class Sensor {

  public static Sensor S1 = new Sensor(1);
  public static Sensor S2 = new Sensor(2);
  public static Sensor S3 = new Sensor(3);

  int no;

  boolean active = false;
  int     value  = 75;

  Sensor(int _no) { no = _no; }

  public synchronized void activate() {
	    active = true;
	    notifyAll();
	  }

  public synchronized void passivate() {
	    active = false;
	  }

  public synchronized int readValue() {
	  return active ? value : 60 + (value/2); 
  }

  public synchronized void setValue(int val) {
    if (val!= value) Poll.change(1<<(no-1));
    value = val;
  }

  /* Extra inspection methods for simulation */
  
  public synchronized boolean isActivated() {
	  return active;
  }
  
  public synchronized void awaitActive() throws InterruptedException {
	  while (!active) wait();
  }
  
  
}






