package trucksimulation.traffic;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;


/**
 * 
 * Parameters which can be passed in to the traffic service in the URLs query part.<br>
 * Allowed params are:
 * <ul>
 * 	<li>lat (double, required)</li>
 *  <li>lon (double, required)</li>
 *  <li>maxDistance (int, optional, in meters)</li>
 * </ul>
 *
 *
 */
public class TrafficQueryParams {
	
	private double lat;
	private double lon;
	private int maxDistance = 10000;
	
	public TrafficQueryParams(MultiMap paramMap) {
		String latStr = paramMap.get("lat");
		String lonStr = paramMap.get("lon");
		String maxDistStr = paramMap.get("maxDistance");
		
		loadLatLon(latStr, lonStr);
		loadMaxDist(maxDistStr);

	}

	private void loadMaxDist(String maxDistStr) {
		if(maxDistStr != null) {
			try {
				maxDistance = Integer.parseInt(maxDistStr);
			} catch(NumberFormatException ex) {
				// default will be used
			}
		}
	}

	private void loadLatLon(String latStr, String lonStr) {
		if(latStr == null || lonStr == null) {
			throw new IllegalArgumentException("lat and lon must be provided");
		}
		
		try {
			lat = Double.parseDouble(latStr);
			lon = Double.parseDouble(lonStr);
		} catch(NumberFormatException ex) {
			throw new IllegalArgumentException("lat and lon must be valid double numbers");
		}
	}
	
	public JsonArray getLonLatArr() {
		return new JsonArray().add(lon).add(lat);
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

	public int getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(int maxDistance) {
		this.maxDistance = maxDistance;
	}

}
