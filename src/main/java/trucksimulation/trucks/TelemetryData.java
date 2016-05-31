package trucksimulation.trucks;

import java.util.Random;

import trucksimulation.routing.Position;

public class TelemetryData {
	
	private String id;
	private long timeStamp;
	private Position position;
	private double altitude;
	private int verticalAccuracy = 20;
	private int horizontalAccuracy = 5;
	private double speed;
	private double bearing;
	private double temperature = 20.0;
	private transient Random random = new Random();
	private transient boolean deteriorate;
	
	public TelemetryData(String id, boolean deteriorate) {
		this.id = id;
		this.deteriorate = deteriorate;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public double getAltitude() {
		return altitude;
	}
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		if(deteriorate) {
			this.position = deteriorate(position);
		} else {
			this.position = position;
		}
		
	}
	
	private Position deteriorate(Position pos) {
		double lat = pos.getLat() + getDeterioration();
		double lon = pos.getLon() + getDeterioration();
		Position newPos = new Position(lat, lon);
		horizontalAccuracy = (int) Math.round(newPos.getDistance(pos));
		return newPos;
	}
	
	/**
	 * Returns a uniformly distributed value which can be added to the gps coordinates.
	 * @return
	 */
	private double getDeterioration() {
		return (random.nextDouble() - 0.5)/10000;
	}
	
	public int getVerticalAccuracy() {
		return verticalAccuracy;
	}
	public void setVerticalAccuracy(int verticalAccuracy) {
		this.verticalAccuracy = verticalAccuracy;
	}
	public int getHorizontalAccuracy() {
		return horizontalAccuracy;
	}
	public void setHorizontalAccuracy(int horizontalAccuracy) {
		this.horizontalAccuracy = horizontalAccuracy;
	}
	public double getBearing() {
		return bearing;
	}
	public void setBearing(double bearing) {
		this.bearing = bearing;
	}
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

}
