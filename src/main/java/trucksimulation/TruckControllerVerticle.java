package trucksimulation;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.trucks.Truck;

public class TruckControllerVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TruckControllerVerticle.class);
	private MongoClient mongo;
	private int intervalMS;
	/**
	 * Maps simulation id's to the running state of the simulation (true when running).
	 */
	private LocalMap<String, Boolean> simulationStatus;
	
	private HashMap<String, Simulation> simulations = new HashMap<String, Simulation>();
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		intervalMS = config().getJsonObject("simulation", new JsonObject()).getInteger("interval_ms", 1000);
		
		SharedData sd = vertx.sharedData();
		simulationStatus = sd.getLocalMap("simStatusMap");
		
		vertx.eventBus().consumer("simulation.start", this::startSimulation);
		vertx.eventBus().consumer("simulation.stop", this::stopSimulation);
	}	
	
	
	/**
	 * Loads all trucks from the db which belong to this simulation and starts moving them
	 * as soon as their corresponding routes are loaded.
	 * @param msg
	 */
	private void startSimulation(Message<JsonObject> msg) {
		JsonObject simulationQuery = msg.body();
		String simId = simulationQuery.getString("_id");
		if(isSimulationRunning(simId)) {
			msg.fail(400, "Simulation is already running.");
			return;
		}
		Simulation simulation = new Simulation(simId, vertx);
		simulation.setIntervalMs(intervalMS);
		simulations.put(simId, simulation);
		
		JsonObject trucksQuery = new JsonObject().put("simulation", simId);
		mongo.find("trucks", trucksQuery, res -> {
			if(res.failed()) {
				msg.fail(500, res.cause().getMessage());
			} else {
				msg.reply("ok");
				setRunningStatus(simId, true);
				simulations.get(simId).setTruckCount(res.result().size());
				for(JsonObject truckJson : res.result()) {
					Truck truck = new Truck(truckJson.getString("_id"));
					truck.setRouteId(truckJson.getString("route"));
					simulation.addTruck(truck);
					assignRoute(simId, truck);
				}
				loadTrafficIncidents(simId);
				simulation.start();
			}
		});
	}
	
	/**
	 * Stops the simulation if it is running in this verticle and updates the running status in the shared status map {@link #simulationStatus}.
	 * 
	 * @param msg a JsonObject with an "_id" field containing the id string fo the simulation.
	 */
	private void stopSimulation(Message<JsonObject> msg) {
		String simId = msg.body().getString("_id");
		if(simulations.containsKey(simId)) {
			LOGGER.info("Stopping simulation " + simId + " in verticle " + this.deploymentID());
			Simulation simulation = simulations.get(simId);
			simulation.stop();
			simulations.remove(simId);
			setRunningStatus(simId, false);
		}
	}


	/**
	 * Resolves reference to the truck's route and assigns route objects to the truck.
	 * 
	 * @param simId
	 * @param truck
	 */
	private void assignRoute(String simulationId, Truck truck) {
		Gson gson = Serializer.get();
		JsonObject routeQuery = new JsonObject().put("_id", truck.getRouteId());
		
		mongo.findOne("routes", routeQuery, new JsonObject(), r -> {
			Route route = gson.fromJson(r.result().toString(), Route.class);
			simulations.get(simulationId).addRoute(truck.getRouteId(), route);
		});
	}
	
	
	/**
	 * Loads all traffic incidents which belong to the simulation and assigns incidents to
	 * trucks which are affected by those incidents.
	 * 
	 * @param simId the simulation id
	 */
	private void loadTrafficIncidents(String simId) {
		Gson gson = Serializer.get();
		JsonObject query = new JsonObject().put("simulation", simId);
		Simulation simulation = simulations.get(simId);

		mongo.find("traffic", query, trafficResult -> {
			if (trafficResult.result() != null) {
				simulation.setIncidentCount(trafficResult.result().size());

				for (JsonObject incidentJson : trafficResult.result()) {
					JsonObject intersectionQuery = buildIntersectionQuery(incidentJson, simId);
					FindOptions idFieldOnly = new FindOptions().setFields(new JsonObject().put("_id", true));

					mongo.findWithOptions("routes", intersectionQuery, idFieldOnly, routes -> {
						if (routes.result() != null && !routes.result().isEmpty()) {
							TrafficIncident trafficIncident = gson.fromJson(incidentJson.toString(),
									TrafficIncident.class);
							List<String> routeIds = routes.result().stream().map(c -> c.getString("_id"))
									.collect(Collectors.toList());
							simulation.addTrafficIncident(trafficIncident, routeIds);
						}
					});
				}
			}
		});
	}
	
	private JsonObject buildIntersectionQuery(JsonObject traffic, String simId) {
		JsonObject startGeometry = new JsonObject().put("$geometry", traffic.getJsonObject("start"));
		JsonObject endGeometry = new JsonObject().put("$geometry", traffic.getJsonObject("end"));
		JsonObject intersectsStartAndEnd = new JsonObject().put("$geoIntersects", startGeometry).put("$geoIntersects", endGeometry);
		JsonObject query = new JsonObject().put("segments", intersectsStartAndEnd).put("simulation", simId);
		return query;
	}
	
	/**
	 * Sets the status of the simulation so that all local verticles can see it.
	 * 
	 * @param simulationId
	 * @param status
	 */
	private void setRunningStatus(String simulationId, boolean status) {
		simulationStatus.put(simulationId, status);
	}
	
	private boolean isSimulationRunning(String simulationId) {
		Boolean isRunning = simulationStatus.get(simulationId);
		return isRunning != null && isRunning.booleanValue() == true;
	}	

	
}
