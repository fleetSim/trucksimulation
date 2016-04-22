package trucksimulation.models;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.TargetExceededException;

public class Truck {
	
	private long id;
	private Freight freight;
	private Route route;
	private TelemetryData data;
	
	private double speed = 5.0;
	private Position pos;
	private Position targetPos;
	private int interval = 1;
	
	// always point to the next target point
	private int curRouteSegment = 0;
	private int curSegmentPoint = 0;
	
	private static List<Truck> trucks = new ArrayList<>();
	private static long nextTruckId = 100;
	
	public Truck(long id) {
		this.id = id;
	}
	
	public static Truck buildTruck() {
		Truck t = new Truck(nextTruckId++);
		trucks.add(t);
		return t;
	}
	
	public void move() {
		if(speed == 0) {
			System.out.println("Speed is 0. but why?");
		}
		if(pos.equals(targetPos)) {
			throw new IllegalStateException("Already arrived at target.");
		}
		try {
			pos = pos.moveTowards(targetPos, speed * interval);
		} catch (TargetExceededException e) {
			//FIXME: jumping to start of next segment is not a good solution.
			// when the interval is increased, then this slows down the speed on each boundary and causes wrong results
			// instead the distance difference should be used to progress further behind the current target
			pos = targetPos;
			proceedToNextPoint();
		}
	}
	
	public JsonObject asGeoJsonFeature() {
		JsonObject feature = new JsonObject().put("id", id);
		JsonObject properties = new JsonObject().put("name", toString()).put("id", id);
		JsonObject geometry = new JsonObject()
				.put("type", "Point")
				.put("coordinates", new JsonArray().add(pos.getLon()).add(pos.getLat()));
		feature.put("type", "Feature").put("geometry",  geometry).put("properties", properties);
		return feature;
	}
	
	private void proceedToNextPoint() {	
		System.out.println("proceeding to next point");
		Instruction currentSegment = route.getSegment(curRouteSegment);
		PointList points = currentSegment.getPoints();
		if(points.getSize() > curSegmentPoint + 1) {
			curSegmentPoint++;
			targetPos = new Position(points.getLat(curSegmentPoint), points.getLon(curSegmentPoint));
		} else {
			curRouteSegment++;
			curSegmentPoint = 0;
			if(curRouteSegment == route.getSegmentCount() -1) {
				currentSegment = route.getSegment(curRouteSegment);
				points = currentSegment.getPoints();
				targetPos = new Position(points.getLat(curSegmentPoint), points.getLon(curSegmentPoint));
			} else if(curRouteSegment < route.getSegmentCount()) {
				currentSegment = route.getSegment(curRouteSegment);
				points = currentSegment.getPoints();
				targetPos = new Position(points.getLat(curSegmentPoint), points.getLon(curSegmentPoint));
				double nextSpeed = currentSegment.getDistance() / currentSegment.getTime() * 1000;
				if(nextSpeed > 0) {
					speed = nextSpeed;
				}
				
			} else {
				throw new IllegalStateException("Truck has reached its target. Please assign a new route before proceeding.");
			}
		}
		
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Freight getFreight() {
		return freight;
	}
	public void setFreight(Freight freight) {
		this.freight = freight;
	}
	public Route getRoute() {
		return route;
	}
	public void setRoute(Route route) {
		this.route = route;
		this.pos = route.getStartPosition();
		this.proceedToNextPoint();
	}
	public TelemetryData getData() {
		return data;
	}
	public void setData(TelemetryData data) {
		this.data = data;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Position getPos() {
		return pos;
	}

	public void setPos(Position pos) {
		this.pos = pos;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public static List<Truck> getTrucks() {
		return trucks;
	}

	public static void setTrucks(List<Truck> trucks) {
		Truck.trucks = trucks;
	}

	public static long getNextTruckId() {
		return nextTruckId;
	}

	public static void setNextTruckId(long nextTruckId) {
		Truck.nextTruckId = nextTruckId;
	}

}
