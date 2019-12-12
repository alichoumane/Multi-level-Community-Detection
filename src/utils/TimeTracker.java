package utils;

/**
 * computes wall clock elapsed time from the instant of its call until calling {@link TimeTracker#stop()}
 *
 */
public class TimeTracker {

	private long startTime = 0;
	private long stopTime = -1;
	
	public TimeTracker() {
		restart();
	}
	public void restart() {
		startTime = System.currentTimeMillis();
	}
	
	public long stop() {
		stopTime = System.currentTimeMillis();
		return stopTime - startTime;
	}
	
	public long getTimeInMillis() {
		if(stopTime==-1)return -1;
		return stopTime - startTime;
	}
	
	public double getTimeInSec() {
		double m = getTimeInMillis();
		return m/1000;
	}
	
	public double getTimeInMin() {
		double s = getTimeInSec();
		return s/60;
	}
	
	@Override
	public String toString() {
		double min = getTimeInMin();
		int minutes = (int)min;
		double sec = (min - minutes)*60;
		return minutes+"min "+(int)sec+"sec";
	}
}
