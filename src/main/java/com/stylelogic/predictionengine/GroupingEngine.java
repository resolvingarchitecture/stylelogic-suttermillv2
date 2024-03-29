package com.stylelogic.predictionengine;

import org.jdbi.v3.core.Jdbi;
import ra.common.Tuple3;

import java.io.IOException;

import java.util.*;

import static com.stylelogic.predictionengine.DAO.LOG;

public class GroupingEngine implements EngineControls //extends Frame
{
	MiningData mine;
//	GroupInfo[] groups;
//	UserInfo[] users;
	float totalHappiness;
	KMeanSquared kMean;
	Jdbi jdbi;
	private int itemCount;
	private final int kValue=80; // Fixed Number of Groups
	LinkedList taskList;

	ProcessingDaemon processingDaemon;
	GroupingDaemon groupingDaemon;
	LazyWriter lazyWriter;
	TaskListener listener;

	DAO dao;

	int groupingDaemonTimeout = 600;
	int processingDaemonTimeout = 10;
	int lazyWriterTimeout = 6;
	int taskListenerTimeout = 1;

	final static byte MIN_RATING_COUNT = 15;
	int	grouping_iterations = 5;

	private GUI gui;
	private	boolean	halt = false, active=false;
	private int	processRequests=0;

	public GroupingEngine() {}

	public boolean init() {
		long endTime0, endTime1, endTime2, endTime3;
		long startTime = System.currentTimeMillis();;
		taskList = new LinkedList();
		gui = new GUI(this);
		gui.addMessage("Initializing Prediction Engine...");

		mine = new MiningData();
		dao = new DAO(mine);
		jdbi = dao.getJdbi();

		gui.addMessage("Initializing Indicies");
		dao.loadIndicies();
		for (int i=0; i<kValue;i++)
		{
//			dbPortal.RemoveOldGroup(i);
			dao.storeNewGroup(i);
		}

		gui.addMessage("Loading User Ratings");
		dao.loadUserRatings();
//		dao.loadUserRatings(0,mine.getUserCount());
		endTime0 = System.currentTimeMillis();;
		kMean = new KMeanSquared(mine);

		System.out.println("Initializing GroupingAlgorithm");
		gui.addMessage("Loading Group Ratings & Reviews");
		int count = 0;
		List<Tuple3> userGroupMemberships = dao.getUserGroupMemberships();
		for(Tuple3 t : userGroupMemberships) {
			if (count++ % 100==0) LOG.info(".");
			int uIndex = mine.getUserIndex((Integer)t.first);
			int gIndex = mine.getGroupIndex((Integer)t.second);
			float happiness = (Integer)t.third / 100;
			if (mine.users[uIndex] != null) {
				mine.groups[gIndex].addUser(uIndex,mine.users[uIndex].ratings, happiness);
				mine.users[uIndex].setGroup(gIndex, happiness);
			}
		}

		processingDaemon = new ProcessingDaemon(this, gui);
		processingDaemon.start();
		groupingDaemon = new GroupingDaemon(this, gui);
		groupingDaemon.start();
		lazyWriter = new LazyWriter(this, gui, dao);
		lazyWriter.start();
		listener = new TaskListener(this, gui);
		listener.start();

		gui.setActive(true);

		System.out.println("Initilization complete");
		System.out.println("Waiting for users...");
		gui.addMessage("Initilization complete");
		active=true;
		return true;
	}

    public void showGroupErrorStats() {
		gui.addMessage(kMean.showGroupErrorStats());
	}

	public void showGroupDistanceStats() {
		gui.addMessage(kMean.showGroupDistanceStats());
	}

	public boolean haltProcessing()
	{
		halt =true;
		return (active);
	}

    public void start()
    {
		byte[] b = new byte[1];
		byte c;
		int count = 0;

		System.out.println("\n(h)alt processing\n(s)ave groups\ngroup (d)istance stats\ngroup (e)rror stats:");
		do
		{
			c=0;
			if (++count == 10*1800) {
				System.out.println("\n(h)alt processing\n(s)ave groups\ngroup (d)istance stats\ngroup (e)rror stats:");
				count=0;
			}
			try
			{
				while (System.in.available() > 0)
				{
					System.in.read(b);
					c=b[0];
					System.in.read(b);
					System.in.read(b);
				}
			} catch(IOException ex) {
				System.err.println("IOException: " + ex.getMessage());
			}
			if (c > 0) System.out.print((char) c);
			switch (c) {
				case 100: showGroupDistanceStats();break;
				case 101: showGroupErrorStats();break;
				case 115: storeGroups();break;
			}
			try {
				Thread.sleep(100);
			} catch(InterruptedException ex){
				System.err.println("InterruptException: " + ex.getMessage());
			}
		} while (c != 104 && !halt);
		shutDown();
	}

	public void shutDown() {
		int i=0;

		gui.addMessage("Shutting Down All Daemons");

		groupingDaemon.setActiveState(false);
		groupingDaemon.interrupt();
		listener.setActiveState(false);
		listener.interrupt();
		System.out.println("Shutting Down Task Listener & Grouping Daemon-");
		while (groupingDaemon.isAlive() || listener.isAlive()) {
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Task Listener Daemon Stopped");
		gui.addMessage("Grouping Daemon Stopped");

		processingDaemon.setActiveState(false);
		processingDaemon.interrupt();
		System.out.println("Shutting Down Processing  Daemon-");
		while (processingDaemon.isAlive()) {
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Processing Daemon Stopped");

		lazyWriter.setActiveState(false);
		lazyWriter.interrupt();
		System.out.println("Shutting Down Lazy Writer Daemon-");
		while (processingDaemon.isAlive() || groupingDaemon.isAlive() || lazyWriter.isAlive() || listener.isAlive()) {
			if ((i++ % 50) == 0) System.out.print("*");
		}
		gui.addMessage("Lazy Writer Daemon Stopped");

		gui.dispose();
		System.out.println("Prediction Engine shutdown.");
	}

	public void gracefulShutdown() {
		shutDown();
	}

	public void refresh() {
		float h = 0;
		totalHappiness = kMean.compileHappiness();
	}

	public void setGroupingIterations(int n) {
		grouping_iterations = n;
	}

	public void startGroupingEngine() {
		groupingDaemon.interrupt();
	}

	public void setGroupingIntermission(int seconds) {
		groupingDaemonTimeout = seconds;
	}

	public void incrementProcessRequests(int count) {
		processRequests += count;
	}

	public void resetProcessRequests(int count) {
		processRequests = 0;
	}

	public int getProcessRequests() {
		return processRequests;
	}

	public void storeUsers()	{
		lazyWriter.task[1] = true;
		System.out.println("Prepare to Store Users");
	}
	public void storeGroups()	{
		lazyWriter.task[0] = true;
		System.out.println("Prepare to Store Groups");
	}

}





