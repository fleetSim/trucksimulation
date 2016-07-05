package trucksimulation.traffic;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import trucksimulation.routing.Position;

/**
 * Model for the traffic API which reports deteriorated traffic incidents.
 * 
 * Based on https://msdn.microsoft.com/en-us/library/hh441730.aspx
 *
 */
public class TrafficModel {
	private transient TrafficIncident incident;
	private long incidentId;
	private Position startPoint;
	private Position endPoint;
	private boolean verified;
	private double speed;
	private LocalDateTime lastModified;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private LocalDateTime reported;
	private double distance;
	
	public TrafficModel() {
		
	}
	
	public TrafficModel(TrafficIncident incident) {
		this.incident = incident;
		this.startPoint = incident.getStart();
		this.endPoint = incident.getEnd();
		this.speed = incident.getSpeed();
		this.lastModified = LocalDateTime.now(ZoneOffset.UTC);
		if(incident.isActive()) {
			this.setReported(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));
			this.startTime = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10);
			this.endTime = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(60);
		} else {
			this.setReported(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(60));
			this.startTime = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(60);
			this.endTime = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
		}
		
	}
	
	public long getIncidentId() {
		return incidentId;
	}
	public void setIncidentId(long incidentId) {
		this.incidentId = incidentId;
	}
	public TrafficIncident getIncident() {
		return incident;
	}
	public void setIncident(TrafficIncident incident) {
		this.incident = incident;
	}
	public Position getStartPoint() {
		return startPoint;
	}
	public void setStartPoint(Position startPoint) {
		this.startPoint = startPoint;
	}
	public Position getEndPoint() {
		return endPoint;
	}
	public void setEndPoint(Position endPoint) {
		this.endPoint = endPoint;
	}
	public boolean isVerified() {
		return verified;
	}
	public void setVerified(boolean verified) {
		this.verified = verified;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public LocalDateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(LocalDateTime lastModified) {
		this.lastModified = lastModified;
	}

	public LocalDateTime getReported() {
		return reported;
	}

	public void setReported(LocalDateTime reported) {
		this.reported = reported;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
}
