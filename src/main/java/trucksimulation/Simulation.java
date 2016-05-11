package trucksimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import trucksimulation.routing.Route;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.trucks.Truck;

/**
 * Representation of runtime information for a simulation.
 *
 */
public class Simulation {
	
	public Simulation(String simulationId) {
		this.id = simulationId;
		
	}
	
	public Simulation(String simulationId, Vertx vertx) {
		this(simulationId);
		this.vertx = vertx;
	}
	
	private String id;
	private Vertx vertx;
	private Map<String, Truck> route2truckMap = new HashMap<>();
	private List<Long> timerIds = new ArrayList<>();
	
	private int truckCount;
	private int incidentCount;
	private Future allRoutesLoaded = Future.future();
	private Future allIncidentsAssigned = Future.future();
	
	public void start() {
		CompositeFuture.all(allRoutesLoaded, allIncidentsAssigned).setHandler(h -> {
			//TODO: start simulation, store timer ids
		});
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
		this.route2truckMap.remove(truck.getRouteId());
	}
	
	/**
	 * Assigns a traffic incident object to all trucks that are specified in the lsit of ids.
	 * The caller must make sure that the mapping from incident to trucks is correct as no
	 * additional checks are performed in the simulation object.
	 * 
	 * Throws an IllegalStateException if trucks have not been added yet and an IllegalArgumentException
	 * if the list contains an ID which cannot be found in the list of trucks.
	 * 
	 * Incidents can only be assigned when
	 * 
	 * @param incident
	 * @param truckIds ids of all trucks which are affected by the traffic incident
	 */
	public void addTrafficIncident(TrafficIncident incident, List<String> routeIds) {
		if(route2truckMap.isEmpty()) {
			throw new IllegalStateException("Trucks have not been added to the simulation yet.");
		}
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
	
	public Future getAllRoutesLoaded() {
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
