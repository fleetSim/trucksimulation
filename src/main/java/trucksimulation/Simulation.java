package trucksimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.Route;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

/**
 * Simulation representation.
 */
public class Simulation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TruckControllerVerticle.class);
	
	private String id;
	private Vertx vertx;
	private Map<String, Truck> route2truckMap = new HashMap<>();
	private List<Long> timerIds = new ArrayList<>();
	
	private int truckCount;
	private int incidentCount;
	private Future<Boolean> allRoutesLoaded = Future.future();
	private Future<Boolean> allIncidentsAssigned = Future.future();
	
	public Simulation(String simulationId) {
		this.id = simulationId;
		
	}
	
	public Simulation(String simulationId, Vertx vertx) {
		this(simulationId);
		this.vertx = vertx;
	}
	
	/**
	 * Starts the simulation as soon as all routes and incidents have been loaded.
	 * 
	 * Make sure to set the expected number of trucks and incidents and all incident,
	 * truck and route instances, otherwise the simulation won't start.
	 * 
	 * @see #setTruckCount(int)
	 * @see #addTruck(Truck)
	 * @see #setIncidentCount(int)
	 * @see #addRoute(String, Route)
	 * @see #addTrafficIncident(TrafficIncident, List)
	 */
	public void start() {
		CompositeFuture.all(allRoutesLoaded, allIncidentsAssigned).setHandler(h -> {
			for(Truck truck : route2truckMap.values()) {
				//FIXME: multiple trucks may drive the same route, needs different data structure
				startMoving(truck);
			}
		});
	}
	
	/**
	 * Sets a periodic timer which moves the truck in each interval.
	 * 
	 * @param t truck to be moved
	 * @return id of the timer, so that it can be cancelled
	 */
	private long startMoving(Truck t) {
		long tId = vertx.setPeriodic(1000, timerId -> {
			try {
				t.move();
				vertx.eventBus().publish("trucks", t.getJsonData());
			} catch(DestinationArrivedException ex) {
				LOGGER.info("Truck has arrived at destination: #" + t.getId());
				vertx.cancelTimer(timerId);
			} catch (Exception ex) {
				LOGGER.error("Unexpected error, stopping truck #" + t.getId(), ex);
				vertx.cancelTimer(timerId);
			}
		});
		return tId;
	}
	
	public void stop() {
		if(vertx == null) {
			throw new IllegalStateException("Simulation obj must be initialized with vertx instance.");
		}
		for(long timerId : timerIds) {
			vertx.cancelTimer(timerId);
		}
	}
	
	
	public void addTruck(Truck truck) {
		this.route2truckMap.put(truck.getRouteId(), truck);
	}
	
	public void removeTruck(Truck truck) {
		//FIXME: need different data structure
		this.route2truckMap.remove(truck.getRouteId());
	}
	
	/**
	 * Assigns a traffic incident object to all trucks which drive on one of the specified routes.
	 * The caller must make sure that the mapping from incident to route is correct as no
	 * additional checks are performed in the simulation object.
	 * 
	 * Operation is performed as soon as all routes have been assigned to the simulation trucks.
	 * 
	 * @see #getAllRoutesLoaded()
	 * @see #addRoute(String, Route)
	 * 
	 * @param incident
	 * @param truckIds ids of all trucks which are affected by the traffic incident
	 */
	public void addTrafficIncident(TrafficIncident incident, List<String> routeIds) {
		allRoutesLoaded.setHandler(h -> {
			for(String routeId : routeIds) {
				Truck truck = route2truckMap.get(routeId);
				if(truck != null) {
					truck.addTrafficIncident(incident);
				} else {
					throw new IllegalArgumentException("Truck with route id " + routeId + " not found in simulation.");
				}
			}
			incidentCount--;
			if(incidentCount == 0) {
				allIncidentsAssigned.complete();
			}
		});
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public void addRoute(String routeId, Route route) {
		route2truckMap.get(routeId).setRoute(route);
		truckCount--;
		if(truckCount == 0) {
			allRoutesLoaded.complete();
		}
	}

	public List<Long> getTimerIds() {
		return timerIds;
	}

	public void setTimerIds(List<Long> timerIds) {
		this.timerIds = timerIds;
	}
	
	public Future<Boolean> getAllRoutesLoaded() {
		return allRoutesLoaded;
	}

	public int getTruckCount() {
		return truckCount;
	}

	public void setTruckCount(int truckCount) {
		this.truckCount = truckCount;
	}

	public int getIncidentCount() {
		return incidentCount;
	}

	public void setIncidentCount(int incidentCount) {
		this.incidentCount = incidentCount;
	}

}
