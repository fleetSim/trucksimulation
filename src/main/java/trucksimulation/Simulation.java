package trucksimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Vertx;
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
	private Map<String, Truck> trucks = new HashMap<>();
	private List<Long> timerIds = new ArrayList<>();
	
	public void start() {
		
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
		this.trucks.put(truck.getId(), truck);
	}
	
	public void removeTruck(Truck truck) {
		this.trucks.remove(truck.getId());
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
	public void addTrafficIncident(TrafficIncident incident, List<String> truckIds) {
		if(trucks.isEmpty()) {
			throw new IllegalStateException("Trucks have not been added to the simulation yet.");
		}
		for(String truckId : truckIds) {
			Truck truck = trucks.get(truckId);
			if(truck != null) {
				truck.addTrafficIncident(incident);
			} else {
				throw new IllegalArgumentException("Truck with id " + truckId + " not found in simulation.");
			}
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

	public Map<String, Truck> getTrucks() {
		return trucks;
	}

	public void setTrucks(Map<String, Truck> trucks) {
		this.trucks = trucks;
	}

	public List<Long> getTimerIds() {
		return timerIds;
	}

	public void setTimerIds(List<Long> timerIds) {
		this.timerIds = timerIds;
	}

}
