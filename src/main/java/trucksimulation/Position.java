package trucksimulation;

public class Position {
	
	private static final int EARTH_RADIUS = 6378137;
	private double lat;
	private double lon;
	private String name;
	
	public Position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public Position(double lat, double lon, String name) {
		this(lat, lon);
		this.name = name;
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
	
	public String getName() {
		return name;
	}



	public void setName(String name) {
		this.name = name;
	}



	@Override
	public String toString() {
		return String.format("(%f,\t %f)", lat, lon);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Position other = (Position) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat)) {
			return false;
		}
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon)) {
			return false;
		}
		return true;
	}
	
	

}
