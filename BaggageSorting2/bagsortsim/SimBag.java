/**
 * DTU, Course 02224, Real-Time Systems
 * 
 * Simulator for Baggage Sorting System --- bag simulation
 *
 * @author hhl
 */

package bagsortsim;

import josx.platform.rcx.Sensor;
import josx.platform.rcx.Motor;

import java.awt.Color;

public class SimBag {

	/* The speeds are deltas for each step
	 * Given length of stretch and upper/lower durations
	 */ 
	final static float feedspeed = 20.0f / BagSortSim.freq;

	final static float cspeedmin = (63.0f/2.8f) / BagSortSim.freq;

	final static float cspeedmax = (63.0f/2.4f) / BagSortSim.freq;

	final static float dspeedmin = (56.0f/2.2f) / BagSortSim.freq;

	final static float dspeedmax = (56.0f/2.1f) / BagSortSim.freq;

	final static float espeedmin = (28.0f/1.2f) / BagSortSim.freq;

	final static float espeedmax = (28.0f/1.0f) / BagSortSim.freq;

	final static float fspeedmax = (64.0f/2.6f) / BagSortSim.freq;

	/* No of steps needed to introduce delay of (upper bound - lower bound) */
	final static int fmaxdelaysteps = 
		(int) Math.floor((3.1f - 2.6f) * BagSortSim.freq);

	float pos; 	// Logical position of bag centre
	//   On Feedbelt: mm's travelled from checkin
	//   On Distribution Belt.  mm from feed belt centre in "long" direction.

	// Feed belt positions
	// final static float entryPos =           -10.0f;
	final static float startA   =             0.0f;
    final static float endA	    =  startA  + 32.0f;
    final static float startB   =  endA;
    final static float endB     =  startB  + 48.0f;
    final static float endFeed	=  endB    + 23.0f;
    final static float offFeed  =  endFeed + 12.0f;
    final static float graceDist=             1.0f;  // Section b extension where stop is still OK.
    
    // Distribution belt positions
    final static float startD   =            92.0f; // Start of section d (long path)
    final static float endD     =           148.0f; // End of section d (long path)
    final static float startDr  =            28.0f; // Start of section d (reverse) 
    final static float endDr    =           -28.0f; // End of section d (reverse)
    final static float startEs  =           -28.0f; // Start of section e (short path)
    final static float startEl  =           148.0f; // Start of section e (long path)
    final static float endEs    =           -56.0f; // End of section e (short path)
    final static float endEl    =           176.0f; // End of section e (long path)
    final static float startF   =            28.0f; // Start of section f
    final static float middleF  =            60.0f; // Middle of section f (joint)
    final static float endF     =            92.0f; // End of section f
    
    int color;
	int colorVal; 
	
	int checkin;

	Sensor sensor;

	Motor feedmotor;

	BagSortSim sim;

	int direction;				// Current Direction 
	int sign;					// Sign == -1: Forward = Right
	// Sign ==  1: Forward = Left

	boolean shortroute = false;

	boolean atFeed = true;			// Still at Feed Belt
	boolean done = false;			// Has left the sorting system
	boolean collision = false;		        // Collision detected

	float speed;

	int fticks = 0; // No. of delay steps at middle of section (f)

	public SimBag(int chkin, int col) {
		color = col;
		checkin = chkin;
		if (checkin == 1) {
			sensor = Sensor.S1;
			feedmotor = Motor.A;
			sign = -1;
		}
		if (checkin == 2) {
			sensor = Sensor.S2;
			feedmotor = Motor.B;
			sign = 1;
		}

		sim = BagSortSim.getSimulator();
		pos = -(float) sim.getSeparation();
		
		// Calculate observed color
		colorVal = (color==BagSortSim.YELLOW ? 55 : 40);
		colorVal += Math.round(Math.random()*10 - 5.0); 
	}

	public synchronized boolean isDone() {
		return done;
	}

	boolean stopError = false;
	boolean backwardError = false;

	
	void checkfeed() {
		int feedDir = feedmotor.getDirection();
		
		if (feedDir < 0) {
			// report on first occasion only
			if (!backwardError) sim.error("Feed belt moves backwards");
			backwardError = true;
		} else
			backwardError = false;
			
		if (feedDir== 0 && pos > endB + graceDist) {
			// report on first occasion only
			if (!stopError) sim.error("Feed belt stopped while bag turning");
			stopError = true;
		} else
			stopError = false;
	}

	void checkturn() {
		if (direction != Motor.C.getDirection()) {
			sim.error("Distribution belt stopped or reversed while bag turning");
		}
		direction = Motor.C.getDirection();
	}

	void checkdistr() {
		if (direction !=0  && direction != Motor.C.getDirection()) {
			sim.warning("Distribution belt stopped or reversed when bag present");
		}
		direction = Motor.C.getDirection();
	}

	int sensorValue(Float pos) {
        /*
		 * Simulate passing of sensor (assuming sensor activated)
		 */
		if (pos >= 4.0f && pos <= 28.0f) {
			return colorVal;
		} else if (0.0f <= pos && pos <= 4.0f) {
			return 70 - Math.round((70-colorVal)*(pos/4.0f));
		} else if (28.0f < pos && pos <= 32.0f) {
			return 70 - Math.round((70-colorVal)*((pos-28.0f)/4.0f));
		}
		
		return 75;
	}
	
	
	public synchronized void step() {
		if (done) {
			return;
		}

		float oldpos = pos;

		if (atFeed) {
			checkfeed();
			
			if (pos < endA) {
				// In section (a)
				pos = pos + feedmotor.getDirection() * feedspeed;
				if (oldpos < endA && pos >=endA)
					sim.clearCheckin(checkin);
				if (startA < pos && pos < endA) {
					// Still in (a)
					sensor.setValue(sensorValue(pos-startA));
				} else
					sensor.setValue(75);
				
			} else if (pos < endB) {
				// In section (b)
				pos = pos + feedmotor.getDirection() * feedspeed;
				if (pos >= endB) {
					// Entered section (c) - leave feed belt and fix (c)-speed.
					sim.decrFeedCount(checkin);
					direction = Motor.C.getDirection();
					if (direction == 0)
						sim.error("Distribution belt not moving at start of turn");
					speed = Math.random() < 0.5 ? cspeedmin : cspeedmax;
				}

			} else if (pos < offFeed) {
				// In section (c), still partially at feed belt.
				checkturn();
				pos = pos + speed;  // Use determined speed here
				if (pos >= offFeed) {
					// Entered Distribution Belt.  No return to Feed Belt possible.
					atFeed = false;
					pos = pos - offFeed;  // Any surplus is transferred to distribution belt
					pos = pos * sign * direction;
				}
			} 

		} else {

			/*
			 * The remaining cases cover the Distribution Belt.  
			 * 
			 */	
			checkdistr();
			if (fticks > 0) 
				fticks--;  			// Introduce delay at joint (middle of (f))
			else
				pos = pos + speed * sign * Motor.C.getDirection();

			if (pos <= endEs || pos >= endEl) {
				done = true;
				/* Find destination and update statistics */
				int atB = (getPosition().x >140.0f ? 1 : 0);
				sim.stat.done[atB][color].incr();
				sim.window.newinfo();
			}

			if ((oldpos > startEs && pos <= startEs) ||
					(oldpos < startEl && pos >= startEl)) {
				// Entering section (e)
				speed = Math.random() < 0.5 ? espeedmin : espeedmax;
			}
			
			if ((oldpos < endDr && pos >= endDr) ||
					(oldpos >  startDr && pos <=  startDr) ||
					(oldpos <  startD && pos >=  startD) ||
					(oldpos > endD && pos <= endD)) {
				// Entering section (d)
				speed = Math.random() < 0.5 ? dspeedmin : dspeedmax;
			}
			
			if ((oldpos < startF && pos >= startF) ||
					(oldpos > endF && pos <= endF)) {
				// Entering section (f)
				speed = fspeedmax;  // Variation at (f) handled at joint
			}
			
			if ( (oldpos < middleF) != (pos < middleF) ) {
				// Passing joint af (f).  Introduce delay.
				fticks = Math.random() < 0.7 ? 0 : fmaxdelaysteps;
			}
		}
	}

	public synchronized void setCollision() {
		collision = true;
	}

	public synchronized BagPos getPosition() {
		Color col = (color == BagSortSim.BLACK ? Color.black : Color.yellow);
		BagPos bagpos = new BagPos(0.0f, 0.0f, 0.0f, col);
		bagpos.x = (checkin == 1) ? 80.0f : 200.0f;
		if (atFeed) {
			bagpos.y = pos + 41.0f;
		} else {
			bagpos.angle = (float) Math.PI / 2;
			bagpos.y = 156.0f;
			bagpos.x += -sign * pos;
		}
		if (collision) {
			bagpos.color = Color.red;
			collision = false;
		}
		return bagpos;
	}

}
