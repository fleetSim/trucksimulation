package trucksimulation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.Route;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.TelemetryData;
import trucksimulation.trucks.Truck;
import trucksimulation.trucks.TruckEventListener;

/**
 * Simulation representation.
 */
public class Simulation implements TruckEventListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationControllerVerticle.class);
	
	private String id;
	private boolean endlessMode = false;
	private Vertx vertx;
	private Map<String, HashSet<Truck>> route2trucksMap = new HashMap<>();
	private List<Truck> trucks = new ArrayList<>();
	private List<Long> timerIds = new ArrayList<>();
	private Map<String, Integer> intervalCounters = new HashMap<>();
	private Map<TrafficIncident, List<String>> incident2RoutesMap = new HashMap<>();
	private int truckCount;
	private int incidentCount;
	private Future<Boolean> allRoutesLoaded = Future.future();
	private Future<Boolean> allIncidentsAssigned = Future.future();
	private LocalDateTime startTime;
	/**
	 * interval in which the trucks' positions should be updated in the simulation.
	 */
	private long intervalMs = 1000;
	/**
	 * Interval in which messages should be published by the box.
	 * Box data will be sent every {@link #publishInterval} * {@link #intervalMs} ms.
	 */
	private int publishInterval = 5;

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
		allIncidentsAssigned.setHandler(h -> {
			LOGGER.info("simulation initialisation completed, starting simulation.");
			startTime = LocalDateTime.now(ZoneOffset.UTC);
			for(Truck truck : trucks) {
				truck.setTrafficEventListener(this);
				long timerId = startMoving(truck);
				timerIds.add(timerId);
			}
		});
	}
	
	/**
	 * Sets a periodic timer which moves the truck in each interval.
	 * 
	 * @param truck truck to be moved
	 * @return id of the timer, so that it can be cancelled
	 */
	private long startMoving(Truck truck) {
		intervalCounters.put(truck.getId(), 0);
		long tId = vertx.setPeriodic(intervalMs, timerId -> {
			try {
				truck.move();
				publishBoxData(truck);
			} catch(DestinationArrivedException ex) {
				LOGGER.info("Truck has arrived at destination: #" + truck.getId());
				if(endlessMode) {
					// resets the truck so that it drives the same route again
					// TODO: truck should drive back, not beam to different position
					truck.setRoute(truck.getRoute());
				} else {
					cancelTimer(timerId);
				}
			} catch (Exception ex) {
				LOGGER.error("Unexpected error, stopping truck #" + truck.getId(), ex);
				cancelTimer(timerId);
			}
		});
		return tId;
	}
	
	private void cancelTimer(long timerId) {
		vertx.cancelTimer(timerId);
		this.timerIds.remove(timerId);
		if(!isRunning()) {
			LOGGER.info("Simulation " + this.id + " has ended, all trucks arrived");
			vertx.eventBus().publish(Bus.SIMULATION_ENDED.address(), new JsonObject().put("id", this.id));
		}
	}
	
	/**
	 * Publishes the correct simulation data when called and deteriorated data
	 * every {@link #publishInterval} calls.
	 * @param truck
	 */
	private void publishBoxData(Truck truck) {
		TelemetryData correctData = truck.getTelemetryBox().getTelemetryData();
		Gson gson = Serializer.get();
		JsonObject correctDataJson = new JsonObject(gson.toJson(correctData)).put("truckId", truck.getId());
		vertx.eventBus().publish(Bus.BOX_MSG.address(), correctDataJson);
		
		int ctr = intervalCounters.get(truck.getId()) + 1;
		intervalCounters.put(truck.getId(), ctr);
		if(ctr % publishInterval == 0) {
			intervalCounters.put(truck.getId(), 0);
			TelemetryData inexactData = truck.getTelemetryBoxInexact().getTelemetryData();
			JsonObject dataJson = new JsonObject(gson.toJson(inexactData)).put("truckId", truck.getId());
			vertx.eventBus().publish(Bus.BOX_MSG_DETER.address(), dataJson);
		}
	}
	
	@Override
	public void handleTrafficEvent(Truck truck, EventType type) {
		JsonObject truckStateMessage = new JsonObject() //
				.put("truckId", truck.getId()) //
				.put("ts", System.currentTimeMillis()) //
				.put("eventType", type.name());
		vertx.eventBus().publish(Bus.TRUCK_STATE.address(), truckStateMessage);
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
	 * <p>Assigns a traffic incident object to all trucks which drive on one of the specified routes.
	 * The caller must make sure that the mapping from incident to route is correct as no
	 * additional checks are performed in the simulation object.</p>
	 * 
	 * <p>An assingment to all affected trucks cannot be performed immediately, hence we store 
	 * the incident to routes mapping in the {@link #incident2RoutesMap}.
	 * When all routes have been added, and all incidents have been stored in the map then we can perform
	 * the actual assignment for incidents to trucks.</p>
	 * 
	 * @see #getAllRoutesLoaded()
	 * @see #addRoute(String, Route)
	 * 
	 * @param incident
	 * @param routeIds ids of all routes which are affected by the traffic incident
	 */
	public void addTrafficIncident(TrafficIncident incident, List<String> routeIds) {
		this.incident2RoutesMap.put(incident, routeIds);
		incidentCount--;
		
		// perform the actual assingment when the last incident has been assigned (incidentCount)
		// and all routes are loaded (allRoutesLoaded)
		if(incidentCount == 0) {
			allRoutesLoaded.setHandler(h -> {
				for(Entry<TrafficIncident, List<String>> entry : incident2RoutesMap.entrySet()) {
					List<String> routeIDs = entry.getValue();
					TrafficIncident in = entry.getKey();
					
					for(String routeId : routeIDs) {
						Set<Truck> trucks = route2trucksMap.get(routeId);
						if(trucks != null) {
							trucks.forEach(t -> t.addTrafficIncident(in));
						} else {
							throw new IllegalArgumentException("No trucks with route id " + routeId + " could be found in simulation " + id);
						}
					}				
				}
				LOGGER.info("Assignment of traffic incidents completed in simulation " + id);
				allIncidentsAssigned.complete();
			});
		}
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
		} else {
			LOGGER.warn("Attempted to add route, but simulation has no trucks.");
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

	public int getPublishInterval() {
		return publishInterval;
	}

	public void setPublishInterval(int publishInterval) {
		this.publishInterval = publishInterval;
	}

	public List<Truck> getTrucks() {
		return trucks;
	}

	/**
	 * Returns the time at which the simulation actually started 
	 * (which usually differs from the time when the start method has been called due to async loading of data).
	 * 
	 * @return start time
	 */
	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public boolean isEndlessMode() {
		return endlessMode;
	}

	public void setEndlessMode(boolean endlessMode) {
		this.endlessMode = endlessMode;
	}

}
