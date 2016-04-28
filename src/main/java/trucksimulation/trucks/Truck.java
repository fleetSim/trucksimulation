package trucksimulation.trucks;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.TargetExceededException;

public class Truck {
	
	private long id;
	private Freight freight;
	private Route route;
	private TelemetryBox telemetryBox = new TelemetryBox();
	private TelemetryData data;
	
	private double speed = 5.0;
	private Position pos;
	private Position targetPos;
	private int interval = 1;
	private long ts = 0;
	
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
	
	public void move(double moveSpeed) {
		if(pos.equals(targetPos)) {
			throw new IllegalStateException("Already arrived at target.");
		}
		try {
			pos = pos.moveTowards(targetPos, moveSpeed * interval);
		} catch (TargetExceededException e) {
			pos = targetPos;
			proceedToNextPoint();
			move(e.getExceededBy());
		}
		ts += interval * 1000;
		data = telemetryBox.update(pos, ts);
	}
	
	public void move() {
		move(speed);		
	}
	
	public JsonObject asGeoJsonFeature() {
		return pos.asGeoJsonFeature();
	}
	
	private void proceedToNextPoint() {	
		RouteSegment currentSegment = route.getSegment(curRouteSegment);
		if(currentSegment.getSize() > curSegmentPoint + 1) {
			curSegmentPoint++;
			targetPos = currentSegment.getPoint(curSegmentPoint);
		} else {
			curRouteSegment++;
			curSegmentPoint = 0;
			if(curRouteSegment == route.getSegmentCount() -1) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
			} else if(curRouteSegment < route.getSegmentCount()) {
				currentSegment = route.getSegment(curRouteSegment);
				targetPos = currentSegment.getPoint(curSegmentPoint);
				double nextSpeed = currentSegment.getSpeed();
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
		this.pos = route.getStart();
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

	public JsonObject getJsonData() {
		JsonObject msg = new JsonObject();
		msg.put("position", data.getPosition().asGeoJsonFeature());
		msg.put("speed", data.getSpeed());
		msg.put("ts", data.getTimeStamp());
		msg.put("horizontalAccuracy", data.getHorizontalAccuracy());
		msg.put("truckId", id);
		msg.put("bearing", data.getBearing());
		return msg;
	}

}
