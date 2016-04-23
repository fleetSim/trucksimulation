package trucksimulation.trucks;

import java.util.Random;

import trucksimulation.routing.Position;

public class TelemetryData {
	
	private long timeStamp;
	private Position position;
	private double altitude;
	private int verticalAccuracy = 20;
	private int horizontalAccuracy = 5;
	private double speed;
	private double bearing;
	private Random random = new Random();
	
	
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
		this.position = deteriorate(position);
	}
	
	private Position deteriorate(Position pos) {
		double lat = pos.getLat() * getDeteriorationfactor();
		double lon = pos.getLon() * getDeteriorationfactor();
		Position newPos = new Position(lat, lon);
		horizontalAccuracy = (int) Math.round(newPos.getDistance(pos));
		return newPos;
	}
	
	private double getDeteriorationfactor() {
		return 1 + (random.nextDouble() - 0.5) / 600000;
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

}
