package trucksimulation.trucks;

import trucksimulation.routing.Position;

public class TelemetryBox {
	
	private TelemetryData prevData;
	private TelemetryData curData;
	
	public TelemetryData getTelemetryData() {
		return curData;		
	}
	
	/**
	 * 
	 * @param pos
	 * @param timestamp time in ms
	 */
	public TelemetryData update(Position pos, long timestamp) {
		prevData = curData;
		curData = new TelemetryData();
		curData.setTimeStamp(timestamp);
		curData.setPosition(pos);
		curData.setSpeed(getSpeed());
		return curData;
	}
	
	/**
	 * Calculates the current speed using the distance between last and current position.
	 * Due to inexact position data, speeds can differ from the actual speed.
	 * 
	 * @return speed in m/s
	 */
	private double getSpeed() {
		if(prevData != null && prevData.getPosition() != null) {
			Position prevPos = prevData.getPosition();
			Position curPos = curData.getPosition();
			double dist = prevPos.getDistance(curPos);
			return dist / (curData.getTimeStamp() - prevData.getTimeStamp()) * 1000;	
		} else {
			return 0;
		}
	}
	
	

}
