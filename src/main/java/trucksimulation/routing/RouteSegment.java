package trucksimulation.routing;

public class RouteSegment {
	private double[] lats;
	private double[] lons;
	private double time;
	private double distance;
	
	public RouteSegment() {
		
	}
	
	public RouteSegment(double[] lats, double[] lons, double time, double distance) {
		this.lats = lats;
		this.lons = lons;
		this.time = time;
		this.distance = distance;
	}

	public double[] getLats() {
		return lats;
	}

	public void setLats(double[] lats) {
		this.lats = lats;
	}

	public double[] getLons() {
		return lons;
	}

	public void setLons(double[] lons) {
		this.lons = lons;
	}
	
	public int getSize() {
		if(this.lats != null) {
			return this.lats.length;
		} else {
			return 0;
		}
	}
	
	public Position getPoint(int idx) {
		return new Position(lats[idx], lons[idx]);
	}

	/**
	 * 
	 * @return time in milliseconds
	 */
	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	/**
	 * 
	 * @return distance in meters
	 */
	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	/**
	 * Returns the driving speed which can be expected on this segment.
	 * @return speed in m/s
	 */
	public double getSpeed() {
		return getDistance() / getTime() * 1000;
	}

}
