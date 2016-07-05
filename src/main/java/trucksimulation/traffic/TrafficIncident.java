package trucksimulation.traffic;

import trucksimulation.routing.Position;

public class TrafficIncident {
	private Position start;
	private Position end;
	private double direction;
	private double speed = 1.0;
	private boolean active = true;
	private boolean reported = true;
	
	
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isReported() {
		return reported;
	}
	public void setReported(boolean reported) {
		this.reported = reported;
	}
	public Position getStart() {
		return start;
	}
	public void setStart(Position start) {
		this.start = start;
	}
	public Position getEnd() {
		return end;
	}
	public void setEnd(Position end) {
		this.end = end;
	}
	public double getDirection() {
		return direction;
	}
	public void setDirection(double direction) {
		this.direction = direction;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public String toString() {
		return String.format("Traffic from %s to %s with speed %f", start, end, speed);
	}
}
