package trucksimulation.routing;

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

	/**
	 * Moves the point towards its target for the specified distance in meters.
	 *
	 * @param target
	 *            the target position
	 * @param distance
	 *            the distance in meters
	 * @return a new position between the last position and the target position
	 * @throws TargetExceededException
	 *             when the distance to move is larger than the available
	 *             distance between the position and the target
	 */
	public Position moveTowards(Position target, double distance) throws TargetExceededException {
		double distance2NextPos = getDistance(target);
		if (distance > distance2NextPos) {
			throw new TargetExceededException("Moving into targets direction would exceed the target point.",
					distance - distance2NextPos);
		}
		return moveTowards(target, distance, distance2NextPos);
	}

	public Position moveTowards(Position target, double distance2move, double distance2NextPos) {
		double latNew, lonNew;
		double stepsize = distance2NextPos / distance2move;
		latNew = lat + (target.lat - lat) / stepsize;
		lonNew = lon + (target.lon - lon) / stepsize;
		return new Position(latNew, lonNew);
	}

	/**
	 * 
	 * @param other
	 *            the other position to which the distance should be measured
	 * @return distance in meters (approximated using a 2d sphere)
	 */
	public double getDistance(Position other) {
		double latA = Math.toRadians(lat);
		double lonA = Math.toRadians(lon);
		double latB = Math.toRadians(other.lat);
		double lonB = Math.toRadians(other.lon);
		double cosAng = (Math.cos(latA) * Math.cos(latB) * Math.cos(lonB - lonA)) + (Math.sin(latA) * Math.sin(latB));
		if(cosAng > 1) {
			cosAng = 1; // might be > 1 due to rounding errors
		}
		double ang = Math.acos(cosAng);
		double dist = ang * EARTH_RADIUS;
		return dist;
	}

	/**
	 * Returns the bearing from the current position to the next position in
	 * degrees. Uses formula from
	 * http://www.movable-type.co.uk/scripts/latlong.html<br>
	 * 
	 * <pre>
	 * θ = atan2( sin Δλ ⋅ cos φ2 , cos φ1 ⋅ sin φ2 − sin φ1 ⋅ cos φ2 ⋅ cos Δλ )
	 * </pre>
	 * 
	 * where φ1,λ1 is the start point, φ2,λ2 the end point (Δλ is the
	 * difference in longitude)
	 * 
	 * @param nextPosition
	 * @return bearing in degrees
	 */
	public double getBearing(Position nextPosition) {
		double lat1, lat2, lon1, lon2;
		lat1 = Math.toRadians(lat); // φ1
		lon1 = Math.toRadians(lon); // λ1
		lat2 = Math.toRadians(nextPosition.getLat()); // φ2
		lon2 = Math.toRadians(nextPosition.getLon()); // λ2

		double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
		double brng = Math.toDegrees(Math.atan2(y, x));
		if (brng < 0) {
			brng = 360 + brng;
		}
		return brng;
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
