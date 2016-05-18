package trucksimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private Map<String, HashSet<Truck>> route2trucksMap = new HashMap<>();
	private List<Truck> trucks = new ArrayList<>();
	
	private List<Long> timerIds = new ArrayList<>();
	
	private int truckCount;
	private int incidentCount;
	private Future<Boolean> allRoutesLoaded = Future.future();
	private Future<Boolean> allIncidentsAssigned = Future.future();
	/**
	 * interval in which the trucks' positions should be updated in the simulation.
	 */
	private long intervalMs = 1000;
	
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
		LOGGER.info("Simulation start requested for simulation " + id);
		CompositeFuture.all(allRoutesLoaded, allIncidentsAssigned).setHandler(h -> {
			LOGGER.info("simulation initialisation completed, starting simulation.");
			for(Truck truck : trucks) {
				long timerId = startMoving(truck);
				timerIds.add(timerId);
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
		long tId = vertx.setPeriodic(intervalMs, timerId -> {
			try {
				t.move();
				vertx.eventBus().publish("trucks", t.getJsonData());
			} catch(DestinationArrivedException ex) {
				LOGGER.info("Truck has arrived at destination: #" + t.getId());
				vertx.cancelTimer(timerId);
				this.timerIds.remove(timerId);
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
		this.trucks.add(truck);
		if(!route2trucksMap.containsKey(truck.getRouteId())) {
			route2trucksMap.put(truck.getRouteId(), new HashSet<>());
		}
		this.route2trucksMap.get(truck.getRouteId()).add(truck);
	}
	
	public void removeTruck(Truck truck) {
		trucks.remove(truck);
		route2trucksMap.remove(truck.getRouteId());
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
				Set<Truck> trucks = route2trucksMap.get(routeId);
				if(trucks != null) {
					trucks.forEach(t -> t.addTrafficIncident(incident));
				} else {
					throw new IllegalArgumentException("No trucks with route id " + routeId + " could be found in simulation " + id);
				}
			}
			incidentCount--;
			if(incidentCount == 0) {
				LOGGER.info("Assignment of traffic incidents completed in simulation " + id);
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
	
	public boolean isRunning() {
		return timerIds.size() >= 1;
	}

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public void addRoute(String routeId, Route route) {
		Set<Truck> trucks = route2trucksMap.get(routeId);
		if(trucks != null) {
			trucks.forEach(t -> {
				t.setRoute(route);
				truckCount--;
				if(truckCount == 0) {
					LOGGER.info("Assignment of truck routes completed in simulation " + id);
					allRoutesLoaded.complete();
				}
			});
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
		if(incidentCount == 0) {
			LOGGER.info("No traffic incidents expected for simulation " + id);
			allIncidentsAssigned.complete();
		}
		this.incidentCount = incidentCount;
	}

	public long getIntervalMs() {
		return intervalMs;
	}

	public void setIntervalMs(long intervalMs) {
		this.intervalMs = intervalMs;
	}

	public List<Truck> getTrucks() {
		return trucks;
	}

}
