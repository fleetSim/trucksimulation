package trucksimulation.trucks;

import trucksimulation.routing.Position;

/**
 * Represents the telemetry box which is mounted to a truck.
 * By default, boxes do not deteriorate data, unless explicitly set with {@link #setDeteriorate(boolean)}.
 *
 */
public class TelemetryBox {
	
	private String id;
	private TelemetryData prevData;
	private TelemetryData curData;
	private boolean deteriorate;

	public TelemetryBox(String id) {
		this.id = id;
	}

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
		curData = new TelemetryData(id);
		curData.setTimeStamp(timestamp);
		curData.setPosition(pos);
		curData.setSpeed(getSpeed());
		if(prevData != null) {
			curData.setBearing(prevData.getPosition().getBearing(pos));
		}
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

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean isDeteriorating() {
		return deteriorate;
	}

	public void setDeteriorate(boolean deteriorate) {
		this.deteriorate = deteriorate;
	}
	
	

}
