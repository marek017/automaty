/**
 * DTU, Course 02224, Real-Time Systems
 * 
 * Simulator for Baggage Sorting System
 *
 * @author hhl
 *
 */

package bagsortsim;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import josx.platform.rcx.Sensor;

import java.awt.Color;


public class BagSortSim {

	public final static int BLACK  = 0;
	public final static int YELLOW = 1;

	public final static String version = "2.1";	

	final static int delta         =   10;     // simulation step time [milliseconds]
	final static int refreshperiod =    5;     // no. of deltas per GUI repaint

	final static float freq = 1000F / delta;   // Steps per second

	final static int tickPeriod = 1000;        // [milliseconds]
	final static int clearCount = 3;           // No. of ticks messages are displayed

	static BagSortSim simulator = null;

	Thread engine;

	public Vector<SimBag> bags = new Vector<SimBag>();  // Vector is thread-safe

	public Statistics stat = new Statistics();

	public BagSortWin window;

	boolean[] occupied  = {false, false};
	int[]     feedcount = {0, 0};

	boolean feedCheck   = false; // Should we check for empty feed belt?
	boolean activeCheck = false; // Should we check for active sensor?
	int separation = 10;         // Length of pre-sensor section (min. bag separation) [mm]

	int ticsLeft = 0;   // Remaining no. of tics for displayed message 

	BagSortSim() {
		window = new BagSortWin(this);
		engine = new Thread(new SimEngine(this,bags, window));
		window.newparams();  // Be sure to show initial values correctly
	}	

	public void start() {
		engine.start();
	}


	public synchronized boolean checkin(int chkin, int col) {
		if (chkin < 1 || chkin > 2) {
			error("Checkin counter out of range: " + chkin);
			return false;
		}

		Sensor sensor = (chkin == 1) ? Sensor.S1 : Sensor.S2;
		
		if (col < BLACK || col > YELLOW) {
			error("Checkin color out of range: " + col);
			return false;
		}

		if (feedCheck && feedcount[chkin - 1] > 0) {
			message("Feed Belt " + chkin + " not empty (bag ignored)");
			return false;
		}

		
		if (activeCheck && !sensor.isActivated()) {
			message("Sensor S" + chkin + " not activated (bag ignored)");
			return false;
		}

		if (occupied[chkin - 1]) {
			message("Checkin " + chkin + " not clear (bag ignored)");
			return false;
		}

		bags.add(new SimBag(chkin, col));
		occupied[chkin - 1] = true;
		feedcount[chkin - 1]++;

		return true;
	}

	public synchronized void setFeedCheck(boolean on) {
		if (feedCheck!= on) {
			feedCheck = on;
			window.newparams();
			if (feedCheck && (feedcount[0] > 1)) {
				warning("Feed Belt no. 1 overloaded");
			}
			if (feedCheck && (feedcount[1] > 1)) {
				warning("Feed Belt no. 2 overloaded");
			}
		}
	}

	public synchronized boolean getFeedCheck() {
		return feedCheck;
	}

	public synchronized void setActiveCheck(boolean on) {
		if (activeCheck!= on) {
			activeCheck = on;
			window.newparams();
		}
	}

	public synchronized boolean getActiveCheck() {
		return activeCheck;
	}

	public synchronized void setSeparation(int mm) {
		if (mm < 0 || mm > 80) {
			warning("Separation out of range [0..80] (not set)");
			return;
		}
		if (separation != mm) {
			separation = mm;
			window.newparams();
		}

	}
		
	public synchronized int getSeparation() {
		return separation;
	}
		
	/* Part of interface used by bags */
	
	public synchronized void clearCheckin(int chkin) {
		occupied[chkin - 1] = false;
	}

	public synchronized void decrFeedCount(int chkin) {
		assert(feedcount[chkin - 1] > 0);
		feedcount[chkin - 1]--;
	}


	/* Messages, warnings and errors go to standard output */

	public void message(String msg) {
		System.out.println(msg);
		window.message(msg);
		ticsLeft = clearCount;
	}

	public void warning(String msg) {
		System.out.println("WARNING: " + msg);
		window.message("WARNING: " + msg);
		ticsLeft = clearCount;
		stat.warnings.incr();  window.newinfo();
	}

	public void tick() {
		if (ticsLeft > 0) {
			ticsLeft--;
			if (ticsLeft==0) window.message("");
		}
	}

	public void error(String msg) {
		System.out.println("ERROR: " + msg);
		window.message("ERROR: " + msg);
		ticsLeft = clearCount;
		stat.errors.incr();  window.newinfo();
	}

	/* Singleton factory */
	public static BagSortSim getSimulator() {
		if (simulator == null) {
			simulator = new BagSortSim();
		}
		return simulator;
	}



}

class BagPos {

	public float x, y; // Position of centre

	public float angle;

	public Color color; // BagSortSim constants

	public BagPos(float _x, float _y, float _angle, Color _color) {
		x = _x;
		y = _y;
		angle = _angle;
		color = _color;
	}

}

class ShutDown extends WindowAdapter {

	public void windowClosing(WindowEvent we) {
		System.exit(0);
	}
}

@SuppressWarnings("serial")
class CheckIn extends Panel {

	BagSortSim sim;

	int no;

	Button a = new Button("A");

	Button b = new Button("B");

	public CheckIn(int _no, BagSortSim _sim) {
		no = _no;
		sim = _sim;
		a.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sim.checkin(no, BagSortSim.BLACK);
			}
		});
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sim.checkin(no, BagSortSim.YELLOW);
			}
		});

		a.setBackground(Color.BLACK);
		a.setForeground(Color.WHITE);
		b.setBackground(Color.YELLOW);
		b.setForeground(Color.BLACK);

		add(a);
		add(b);
	}

}

@SuppressWarnings("serial")
class ParamPanel extends Panel {

	BagSortSim sim;

	Checkbox checkactiveon = new Checkbox("Respect activation",true);
	
	Checkbox checkfeedon   = new Checkbox("Max 1 on feed belt",true);
	
	Label sepText = new Label("Min bag separation: 00 mm");
	
	public ParamPanel(BagSortSim _sim) {
		sim = _sim;

		checkactiveon.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				sim.setActiveCheck(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		
		checkfeedon.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				sim.setFeedCheck(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		
		setBackground(Color.WHITE);
		setLayout(new GridLayout(3,1));
		add(checkactiveon);
		add(checkfeedon);
		add(sepText);
	}
	
	public void newparams() {
		// AWT components are thread-safe and hence may be set from here
		
		checkactiveon.setState(sim.getActiveCheck());  
		checkactiveon.repaint();
		
		checkfeedon.setState(sim.getFeedCheck());  
		checkfeedon.repaint();
		
		sepText.setText("Min bag separation: " + sim.getSeparation() + " mm");
		sepText.repaint();
	}

}

@SuppressWarnings("serial")
class InfoPanel extends Panel {

	final int A = 0;
	final int B = 1;

	BagSortSim sim;

	Label ay = new Label("000") { 
		public void paint(Graphics g) {
			setText("" + sim.stat.done[A][BagSortSim.YELLOW].get());
			super.paint(g);
		}
	};

	Label ab = new Label("000") { 
		public void paint(Graphics g) {
			setText("" + sim.stat.done[A][BagSortSim.BLACK].get());
			super.paint(g);
		}
	};

	Label by = new Label("000") { 
		public void paint(Graphics g) {
			setText("" + sim.stat.done[B][BagSortSim.YELLOW].get());
			super.paint(g);
		}
	};

	Label bb = new Label("000") { 
		public void paint(Graphics g) {
			setText("" + sim.stat.done[B][BagSortSim.BLACK].get());
			super.paint(g);
		}
	};

	Label warn = new Label("000") { 
		public void paint(Graphics g) {
			int count = sim.stat.warnings.get();
			if (count > 0) {
				setBackground(Color.blue);
				setForeground(Color.white);
			}
			setText("" + count);
			super.paint(g);
		}
	};

	Label err = new Label("000") { 
		public void paint(Graphics g) {
			int count = sim.stat.errors.get();
			if (count > 0) {
				setBackground(Color.red);
				setForeground(Color.white);
			}
			setText("" + count);
			super.paint(g);
		}
	};

	Label collis = new Label("              ") { 
		public void paint(Graphics g) {
			int count = sim.stat.collisions.get();
			if (count > 0) {
				setBackground(Color.red);
				setForeground(Color.white);
				setText("Collisions");
			}
			super.paint(g);
		}
	};

	Label separator1 = new Label("");
	Label separator2 = new Label("");

	public InfoPanel(BagSortSim sim) {
		this.sim = sim;
		setLayout(new FlowLayout());

		separator1.setPreferredSize(new Dimension(60,1));
		separator2.setPreferredSize(new Dimension(60,1));

		add(ab);  ab.setBackground(Color.black);   ab.setForeground(Color.white);
		ab.setAlignment(Label.RIGHT);
		add(ay);  ay.setBackground(Color.yellow);  ay.setForeground(Color.black);
		ay.setAlignment(Label.RIGHT);
		add(separator1);
		add(new Label("Warnings:"));
		add(warn); // warn.setBackground(Color.blue);  warn.setForeground(Color.white); 
		warn.setAlignment(Label.RIGHT);
		add(collis);
		collis.setAlignment(Label.CENTER);
		add(new Label("  Errors:"));
		add(err); // err.setBackground(Color.red);  err.setForeground(Color.white); 
		err.setAlignment(Label.RIGHT);
		add(separator2);
		add(bb);  bb.setBackground(Color.black);   bb.setForeground(Color.white);
		bb.setAlignment(Label.RIGHT);
		add(by);  by.setBackground(Color.yellow);  ay.setForeground(Color.black);
		by.setAlignment(Label.RIGHT);
	}	

	public void newinfo() {
		// There must be a smarter way to get all fields updated 
		// (without resizing them). 
		ab.repaint(); ay.repaint(); warn.repaint(); err.repaint();
		bb.repaint(); by.repaint(); collis.repaint();
	}



}



@SuppressWarnings("serial")
class SimCanvas extends Canvas {

	BagSortSim sim;

	BufferedImage buf;

	Graphics2D g;

	Rectangle area;

	boolean bufferok = false;

	Shape feed1 = new Rectangle2D.Float(68.0f, 8.0f, 24.0f, 136.0f);

	Shape feed2 = new Rectangle2D.Float(188.0f, 8.0f, 24.0f, 136.0f);

	Shape distr = new Rectangle2D.Float(20.0f, 144.0f, 240.0f, 24.0f);

	Shape sens1 = new Rectangle2D.Float(92.0f, 48.0f, 32.0f, 16.0f);

	Shape sens2 = new Rectangle2D.Float(156.0f, 48.0f, 32.0f, 16.0f);

	Shape led1  = new Ellipse2D.Float(92.0f,54.0f,4.0f,4.0f);

	Shape led2  = new Ellipse2D.Float(184.0f,54.0f,4.0f,4.0f);

	public SimCanvas(BagSortSim _sim) {
		sim = _sim;
	}

	public void update(Graphics g1) {
		Graphics2D g2 = (Graphics2D) g1;

		if (!bufferok) {
			Dimension dim = getSize();
			area = new Rectangle(dim);
			buf = (BufferedImage) createImage(600, 400);
			g = buf.createGraphics();
			bufferok = true;
			g.scale(2.0, 2.0);
			g.setFont(new Font("Monospaced", Font.PLAIN, 8));
		}

		g.clearRect(0, 0, area.width, area.height);

		g.setColor(new Color(200,200,200));
		g.fill(feed1);
		g.fill(feed2);
		g.fill(distr);

		g.setColor(Color.black);
		g.draw(feed1);
		g.draw(feed2);
		g.draw(distr);

		g.setColor(Color.blue);
		g.fill(sens1);
		g.fill(sens2);
		g.setColor(Color.white);	

		g.drawString("" + Sensor.S1.readValue(), 102.0f, 59.0f);
		g.drawString("" + Sensor.S2.readValue(), 166.0f, 59.0f);

		g.setColor(Sensor.S1.isActivated() ? Color.red : Color.black); 
		g.fill(led1);
		g.setColor(Sensor.S2.isActivated() ? Color.red : Color.black);
		g.fill(led2);

		SimBag[] baglist = new SimBag[0];
		baglist = sim.bags.toArray(baglist);

		for (int k = 0; k < baglist.length; k++) {
			BagPos pos = baglist[k].getPosition();
			g.setColor(pos.color);
			if (pos.angle == 0.0) {
				g.fill(new Rectangle2D.Float(pos.x - 8.0f, pos.y - 16.0f,
						16.0f, 32.0f));
			} else {
				g.fill(new Rectangle2D.Float(pos.x - 16.0f, pos.y - 8.0f,
						32.0f, 16.0f));
			}
		}

		g2.drawImage(buf, 0, 0, this);

	}

}

@SuppressWarnings("serial")
class BagSortWin extends Frame {

	BagSortSim sim;

	Canvas canvas;

	InfoPanel info;

	TextField console;
	
	ParamPanel params;

	public BagSortWin(BagSortSim _sim) {
		sim = _sim;
		addWindowListener(new ShutDown());
		setTitle("Bag Sorting Simulator  " +  BagSortSim.version);

		params = new ParamPanel(sim);
		
		Panel top = new Panel();
		top.add(new Label("Checkin 1 ->"));
		top.add(new CheckIn(1,sim));
		//	top.add(new Label("                                       "));
		top.add(new Label(" "));
		top.add(params);
		top.add(new Label(" "));
		top.add(new CheckIn(2,sim));
		top.add(new Label("<- Checkin 2"));

		Panel center = new Panel();
		canvas = new SimCanvas(sim);
		canvas.setSize(560, 355);
		center.add(canvas);

		Panel bottom = new Panel();
		bottom.setLayout(new BorderLayout());

		info = new InfoPanel(sim);

		bottom.add("North",info);

		console = new TextField(60);
		console.setBackground(Color.WHITE);
		bottom.add("South",console);

		/* Main window */

		setBackground(new Color(230,230,230));
		setLayout(new BorderLayout());
		add("North", top);
		add("Center",center);
		add("South", bottom);
		pack();
		setVisible(true);
	}

	public void refresh() {
		canvas.repaint();
	}

	public void newinfo() {
		info.newinfo();
	}
	
	public void newparams() {
		params.newparams();
	}
	

	public void message(String msg) {
		// OK to call GUI components from other threads in AWT
		console.setText(msg);
	}

}

/**
 *  Protected counter class
 */
class Counter {

	int count = 0;

	public synchronized int get() {
		return count;
	}

	public synchronized void incr() {
		count++;
	}
}


class Statistics {

	public Counter[][] done = {{new Counter(), new Counter()},
			{new Counter(), new Counter()}};

	public Counter warnings = new Counter();
	public Counter errors = new Counter();
	public Counter collisions = new Counter();

}



class SimEngine implements Runnable {

	BagSortSim sim;

	long next;
	long nextTick;

	Vector<SimBag> bags;

	BagSortWin win;

	public SimEngine(BagSortSim _sim, Vector<SimBag> _bags, BagSortWin _win) {
		sim = _sim;
		bags = _bags;
		win = _win;
	};

	/* Brute force collision check */
	void checkCollision() {

		SimBag[] baglist = new SimBag[0];

		/* Get bag list (atomically) */
		baglist = bags.toArray(baglist);
		int n = baglist.length;

		/* Get corresponding positions */
		BagPos[] pos = new BagPos[n];
		for (int k = 0; k < n; k++) 
			pos[k] = baglist[k].getPosition();

		/* Pairwise comparison */
		for (int i = 0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				if (close(pos[i], pos[j])) {
					baglist[i].setCollision();
					baglist[j].setCollision();
					sim.stat.collisions.incr();  sim.window.newinfo();
				}
			}
		}

	}

	boolean close(BagPos p1, BagPos p2) {
		/* Bags are considered close (potentially bumping), if their centers are not 
		 * separated by a bag length
		 */
		//		return Math.max(Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y)) < 32.0;
		return (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y) < 32.0*32.0;
	}

	public void run() {
		try {

			int i = 0;

			next = System.currentTimeMillis();
			nextTick = next + BagSortSim.tickPeriod;

			while (true) {
				next = next + BagSortSim.delta;
				long now = System.currentTimeMillis();
				if (next > now)
					Thread.sleep(next - now);

				SimBag[] baglist = new SimBag[0];
				baglist = bags.toArray(baglist);

				for (int k = 0; k < baglist.length; k++) {
					SimBag bag = baglist[k];
					bag.step();
					if (bag.isDone())
						bags.removeElement(bag);
				}

				checkCollision();

				if (System.currentTimeMillis() > nextTick) {
					sim.tick();
					nextTick += BagSortSim.tickPeriod;
				}

				if (++i >= BagSortSim.refreshperiod) {
					win.refresh();
					i = 0;
				}

			}

		} catch (Exception e) {
			sim.error("Exception in simulation engine: " + e);
			e.printStackTrace();
		}

	}
}

