package trucksimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckControllerVerticle extends AbstractVerticle {
	
	private static final int MIN_DISTANCE = 50000;
	private static final Logger LOGGER = LoggerFactory.getLogger(TruckControllerVerticle.class);
	private MongoClient mongo;
	private int intervalMS;
	/**
	 * Maps simulation id's to the running state of the simulation (true when running).
	 */
	private LocalMap<String, Boolean> simulationStatus;
	/**
	 * Maps simulation id's to a list of periodic timers that are currently running as part of the simulation.
	 */
	@Deprecated
	private Map<String, ArrayList<Long>> simTimerMap = new HashMap<String, ArrayList<Long>>();
	
	private HashMap<String, Simulation> simulations = new HashMap<String, Simulation>();
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		intervalMS = config().getJsonObject("simulation", new JsonObject()).getInteger("interval_ms", 1000);
		
		SharedData sd = vertx.sharedData();
		simulationStatus = sd.getLocalMap("simStatusMap");
		
		//createSimulationData();
		
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
		simulations.put(simId, new Simulation(simId, vertx));
		
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
					assignRoute(truck, truckJson.getString("route"));
				}
			}
		});		
	}


	/**
	 * Resolves references to the trucks route and checks if traffic incidents are on the route.
	 * 
	 * @param simId
	 * @param truck
	 */
	private void assignRoute(Truck truck, String routeId) {
		Gson gson = Serializer.get();
		JsonObject routeQuery = new JsonObject().put("_id", routeId);
		
		mongo.findOne("routes", routeQuery, new JsonObject(), r -> {
			Route route = gson.fromJson(r.result().toString(), Route.class);
			truck.setRoute(route);
		});
	}
	
	
	/**
	 * Loads all traffic incidents which belong to the simulation and assigns incidents to
	 * trucks which are affected by those incidents.
	 * 
	 * @param simId the simulation id
	 */
	@SuppressWarnings("unchecked")
	private void loadTrafficIncidents(String simId) {
		Gson gson = Serializer.get();
		JsonObject query = new JsonObject().put("simulation", simId);
		
		Simulation simulation = simulations.get(simId);
		simulation.getAllRoutesLoaded().setHandler(h -> {
			mongo.find("traffic", query, trafficResult -> {
				if(trafficResult.result() != null) {
					simulation.setIncidentCount(trafficResult.result().size());
					
					for(JsonObject incidentJson : trafficResult.result()) {
						JsonObject intersectionQuery = buildIntersectionQuery(incidentJson, simId);
						FindOptions idFieldOnly = new FindOptions().setFields(new JsonObject().put("_id", true));
						
						mongo.findWithOptions("routes", intersectionQuery, idFieldOnly, routes -> {
							if(routes.result() != null && !routes.result().isEmpty()) {
								TrafficIncident trafficIncident = gson.fromJson(incidentJson.toString(), TrafficIncident.class);
								List<String> routeIds = routes.result().stream().map(c -> c.getString("_id")).collect(Collectors.toList());
								simulation.addTrafficIncident(trafficIncident, routeIds);
							}
						});
					}
				}
			});
		});
	}
	
	private JsonObject buildIntersectionQuery(JsonObject traffic, String simId) {
		//TODO: filter by simulation id
		JsonObject startGeometry = new JsonObject().put("$geometry", traffic.getJsonObject("start"));
		JsonObject endGeometry = new JsonObject().put("$geometry", traffic.getJsonObject("end"));
		JsonObject intersectsStartAndEnd = new JsonObject().put("$geoIntersects", startGeometry).put("$geoIntersects", endGeometry);
		JsonObject query = new JsonObject().put("segments", intersectsStartAndEnd);
		return query;
	}
	
	private void registerTimer(String simulationId, long timerId) {
		if(simTimerMap.get(simulationId) == null) {
			simTimerMap.put(simulationId, new ArrayList<Long>());
		}
		simTimerMap.get(simulationId).add(timerId);
	}
	
	private void stopSimulation(Message<JsonObject> msg) {
		String simId = msg.body().getString("_id");
		setRunningStatus(simId, false);
		if(simTimerMap.containsKey(simId)) {
			List<Long> timers = simTimerMap.get(simId);
			for(long timer : timers) {
				vertx.cancelTimer(timer);
			}
			simTimerMap.remove(simId);
		}
	}
	
	private void setRunningStatus(String simulationId, boolean status) {
		simulationStatus.put(simulationId, status);
	}
	
	private boolean isSimulationRunning(String simulationId) {
		Boolean isRunning = simulationStatus.get(simulationId);
		return isRunning != null && isRunning.booleanValue() == true;
	}
	
	private void createSimulationData() {
		Gson gson = new Gson();		
		Position factoryStuttgart = new Position(48.772510, 9.165465);
		Position berlin = new Position(52.413296, 13.421140);
		Position hamburg = new Position(53.551085, 9.993682);
		Position munich = new Position(48.135125, 11.581981);
		
		String to = gson.toJson(factoryStuttgart);
		String from = gson.toJson(berlin);
		
		JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));
		
		// calculate routes
		vertx.eventBus().send("routes.calculate", msg, (AsyncResult<Message<String>> rpl) -> {
			if(rpl.succeeded()) {
				JsonObject route = new JsonObject(rpl.result().body());
				
				mongo.insert("routes", route, res -> {
					if(res.succeeded()) {
						LOGGER.info("Inserted new route " + res.result());
					} else {
						LOGGER.error("Route insertion failed: ", res.cause());
					}
				});
			}
		});
	}

	/**
	 * 
	 * @param num number of trucks to simulate
	 */
	private void startDemoSimulation(int num) {
		mongo.find("cities", new JsonObject(), res -> {
				int numTrucks = num;
				if(res.result().size() < num) {
					numTrucks = res.result().size();
				}
				createTrucksAndRoutes(numTrucks, res.result());
		});
	}
	
	private void createTrucksAndRoutes(int num, List<JsonObject> cities) {
		Random rand = new Random();
		Gson gson = new Gson();

		while(num > 0) {
			int idx1 = rand.nextInt(cities.size());
			int idx2 = rand.nextInt(cities.size());
			JsonObject start = cities.get(idx1);
			JsonObject goal = cities.get(idx2);
			Position startPos = new Position(start.getDouble("lat"), start.getDouble("lon"), start.getString("name"));
			Position goalPos = new Position(goal.getDouble("lat"), goal.getDouble("lon"), goal.getString("name"));
			if(startPos.getDistance(goalPos) > MIN_DISTANCE) {
				String from = gson.toJson(startPos);
				String to = gson.toJson(goalPos);
				JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));
				
				vertx.eventBus().send("routes.calculate", msg, (AsyncResult<Message<String>> rpl) -> {
					if(rpl.succeeded()) {
						Route r = gson.fromJson(rpl.result().body(), Route.class);
						Truck t = Truck.buildTruck();
						t.setRoute(r);
						startMoving(t);
						LOGGER.info("Starting truck " + t.getId());
					}
				});
				num--;
			}
		}
	}
	
	/**
	 * Sets a periodic timer which moves the truck in each interval.
	 * 
	 * @param t truck to be moved
	 * @return id of the timer, so that it can be cancelled
	 */
	private long startMoving(Truck t) {
		long tId = vertx.setPeriodic(intervalMS, timerId -> {
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

}
