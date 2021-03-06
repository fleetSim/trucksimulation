package trucksimulation.trucks;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.TargetExceededException;
import trucksimulation.traffic.TrafficIncident;

public class Truck {
	
	private String id;
	private Route route;
	private String routeId;
	private TelemetryBox telemetryBox;
	private TelemetryBox telemetryBoxInexact;
	private List<TrafficIncident> incidents = new ArrayList<>();
	/** points to the current traffic incident if the truck is affected by one. null otherwise. */
	private TrafficIncident curIncident = null;
	private TruckEventListener trafficEventListener;
	private int idleCountdown = 0;
	private boolean needsNewRoute = false;
	
	private double speed = 5.0;
	private Position pos;
	private Position targetPos;
	private int interval = 1;
	private long ts = 0;
	
	private int curRouteSegment = 0;
	private int curSegmentPoint = 0;
	
	private static List<Truck> trucks = new ArrayList<>();
	private static long nextTruckId = 100;
	private static final Logger LOGGER = LoggerFactory.getLogger(Truck.class);
	
	/**
	 * Automatically assigns the initial timestamp to the current time in UTC.
	 * With each move, the time will be incremented by the interval (real time cannot be used, as simulation may be sped up)
	 * 
	 * @param id
	 */
	public Truck(String id) {
		this.id = id;
		telemetryBox = new TelemetryBox(id);
		telemetryBoxInexact = new TelemetryBox(id);
		telemetryBoxInexact.setDeteriorate(true);
		ts = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC) * 1000;
	}
	
	public static Truck buildTruck() {
		Truck t = new Truck("truck " + nextTruckId++);
		trucks.add(t);
		return t;
	}
	
	/**
	 * Instructs the truck to idel for the specified amount of minutes.<br>
	 * Consequent calls to {@link #move()} will not change the trucks position until the pause time is over.<br>
	 * 
	 * When called while a truck is already in pause mode, the current remaining pause time is replaced with the new value.
	 * 
	 * @param pauseTimeMinutes pause time in minutes
	 */
	public void pause(int pauseTimeMinutes) {
		LOGGER.info("truck `{0}` is having a break for {1} minute/s.", id, pauseTimeMinutes);
		this.idleCountdown = pauseTimeMinutes * 60;
	}
	
	public boolean isInPauseMode() {
		return this.idleCountdown > 0;
	}
	
	/**
	 * Moves the truck forward on its assigned route (unless it is in pause mode).
	 * 
	 * Throws a DestinationArrivedException if the truck is already at the last point of the route.
	 * 
	 * @see #pause(int)
	 */
	public void move() {
		if(isInPauseMode()) {
			idleCountdown--;
			if(!isInPauseMode()) {
				LOGGER.info("truck `{0}` completed its break.", id);
			}
		} else {
			move(speed);
		}
	}
	
	private void move(double moveSpeed) {
		if(hasArrived() && !needsNewRoute) {
			// use needsNewRoute flag to ensure exception is only called once per destination
			needsNewRoute = true;
			throw new DestinationArrivedException("Already arrived at destination. Set a new destination / route.");
		} else if (needsNewRoute) {
			// don't move until a new route has been set
			return;
		}
		try {
			pos = pos.moveTowards(targetPos, moveSpeed * interval);
		} catch (TargetExceededException e) {
			pos = targetPos;
			proceedToNextPoint();
			move(e.getExceededBy());
		}
		ts += interval * 1000;
		telemetryBox.update(pos, ts);
		telemetryBoxInexact.update(pos, ts);
		updateTrafficMode();
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
				if(nextSpeed > 0 && curIncident == null) {
					speed = nextSpeed;
				}
			} else {
				needsNewRoute = true;
				throw new DestinationArrivedException("Truck has reached its target. Please assign a new route before proceeding.");
			}
		}
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Route getRoute() {
		return route;
	}
	
	/**
	 * Assigns a route to the truck and positions the truck at the start of the route.
	 * 
	 * @param route
	 */
	public void setRoute(Route route) {
		this.route = route;
		this.pos = route.getStart();
		curRouteSegment = 0;
		curSegmentPoint = 0;
		needsNewRoute = false;
		this.proceedToNextPoint();
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

	public TelemetryBox getTelemetryBoxInexact() {
		return telemetryBoxInexact;
	}

	public TelemetryBox getTelemetryBox() {
		return telemetryBox;
	}

	/**
	 * Detects if the truck is entering or leaving a traffic incident.
	 * Uses a simple check for the current distance to the traffic incidents start and end point.
	 * It is assumed that only traffic incidents have been assigned to the truck that are actually
	 * on the trucks route. 
	  */
	private void updateTrafficMode() {
		Iterator<TrafficIncident> iter = incidents.iterator();
		while(iter.hasNext()) {
			TrafficIncident incident = iter.next();
			double distToStart = pos.getDistance(incident.getStart());
			double distToend = pos.getDistance(incident.getEnd());
			
			if(distToStart < speed * interval) {
				enterTraffic(incident);
			}
			if(distToend < speed * interval) {
				leaveTraffic(incident);
				iter.remove();
			}
		}
	}
	
	private void enterTraffic(TrafficIncident incident) {
		if(curIncident != null && !curIncident.equals(incident)) {
			throw new IllegalStateException("truck is already in a traffic incident. This is likely a bug in the updateTrafficMode method.");
		}
		this.speed = incident.getSpeed();
		curIncident = incident;
		LOGGER.info("truck `{0}` has entered traffic `{1}`", id, curIncident);
		if(trafficEventListener != null) {
			trafficEventListener.handleTrafficEvent(this, TruckEventListener.EventType.ENTER_TRAFFIC);
		}
	}
	
	private void leaveTraffic(TrafficIncident incident) {
		this.speed = route.getSegment(curRouteSegment).getSpeed();
		LOGGER.info("truck `{0}` has left traffic `{1}`", id, curIncident);
		curIncident = null;
		if(trafficEventListener != null) {
			trafficEventListener.handleTrafficEvent(this, TruckEventListener.EventType.LEAVE_TRAFFIC);
		}
		
	}


	public TrafficIncident getCurIncident() {
		return curIncident;
	}

	public void setCurIncident(TrafficIncident curIncident) {
		this.curIncident = curIncident;
	}

	public List<TrafficIncident> getTrafficIncidents() {
		return incidents;
	}
	
	public void addTrafficIncident(TrafficIncident incident) {
		if(incident == null) {
			throw new IllegalArgumentException("must not be null");
		}
		this.incidents.add(incident);
	}
	
	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public boolean hasArrived() {
		return this.pos.equals(route.getGoal());
	}
	
	public TruckEventListener getTrafficEventListener() {
		return trafficEventListener;
	}

	public void setTrafficEventListener(TruckEventListener trafficEventListener) {
		this.trafficEventListener = trafficEventListener;
	}	

}
