package trucksimulation;

public class Position {
	
	private static final int EARTH_RADIUS = 6378137;
	private double lat;
	private double lon;
	
	public Position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public Position moveTowards(Position target, double speed) throws TargetExceededException {
		double distance = getDistance(target);
		if(speed > distance) {
			throw new TargetExceededException("Moving into targets direction in this speed would exceed the target point.");
		}
		return moveTowards(target, speed, distance);
	}
	
	public Position moveTowards(Position target, double speed, double distance) {
		double latNew, lonNew;
		double stepsize = distance / speed;
		latNew = lat + (target.lat - lat) / stepsize;
		lonNew = lon + (target.lon - lon) / stepsize;
		return new Position(latNew, lonNew);
	}
	
	/**
	 * 
	 * @param other
	 * @return distance in meters (approximated)
	 */
	public double getDistance(Position other) {
		    double latA = Math.toRadians(lat);
		    double lonA = Math.toRadians(lon);
		    double latB = Math.toRadians(other.lat);
		    double lonB = Math.toRadians(other.lon);
		    double cosAng = (Math.cos(latA) * Math.cos(latB) * Math.cos(lonB-lonA)) +
		                    (Math.sin(latA) * Math.sin(latB));
		    double ang = Math.acos(cosAng);
		    double dist = ang * EARTH_RADIUS;
		    return dist;
	}
	
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	@Override
	public String toString() {
		return String.format("(%f,\t %f)", lat, lon);
	}
	
	

}
