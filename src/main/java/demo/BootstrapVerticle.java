package demo;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.Bus;
import trucksimulation.Serializer;
import trucksimulation.SimulationControllerVerticle;
import trucksimulation.routing.Position;
import trucksimulation.routing.RouteCalculationVerticle;

/**
 * Verticle for performing bootstrapping tasks such as
 * setting up the database and loading initial data.
 *
 */
public class BootstrapVerticle extends AbstractVerticle {
	
	private static final int MIN_GAP_BETWEEN_INCIDENTS = 5000;
	private static final int MAX_TRAFFIC_LENGTH = 5000;
	private static final int MIN_TRAFFIC_LENGTH = 1000;
	private static final int CITY_SAMPLE_SIZE = 1000;
	private MongoClient mongo;
	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationControllerVerticle.class);
	
	@Override
	public void start() throws Exception {
		int cores = Runtime.getRuntime().availableProcessors();
		LOGGER.info("Deploying {0} RouteCalculationVerticles", cores - 1);
		DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setInstances(cores - 1).setConfig(config());
		
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		
		vertx.deployVerticle(RouteCalculationVerticle.class.getName(), routeMgrOptions, w -> {
			if (w.failed()) {
				LOGGER.error("Deployment of RouteManager failed." + w.cause());
			} else {
				createDemoSimulation();
				indexRoutes();
				indexTraffic();
				indexTrucks();
			}
		});
			
	}
	
	
	private void indexRoutes() {
		JsonObject key = new JsonObject().put("segments", "2dsphere").put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "routes") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "segments-simulation")));
		
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for routes: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}
	
	private void indexTraffic() {
		JsonObject key = new JsonObject().put("start", "2dsphere").put("end", "2dsphere").put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "traffic") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "start-end-simulation")));
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for traffic: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}
	
	private void indexTrucks() {
		JsonObject key = new JsonObject().put("simulation", 1);
		JsonObject indexCmd = new JsonObject() //
				.put("createIndexes", "trucks") //
				.put("indexes", new JsonArray().add(new JsonObject().put("key", key).put("name", "simulation")));
		mongo.runCommand("createIndexes", indexCmd, res-> {
			if(res.succeeded()) {
				LOGGER.info("created index for trucks: " + res.result());
			} else {
				LOGGER.error(res.cause());
			}
		});
	}


	private void createDemoSimulation() {
		// create a few routes
		Position factoryStuttgart = new Position(48.772510, 9.165465);
		Position berlin = new Position(52.413296, 13.421140);
		Position hamburg = new Position(53.551085, 9.993682);
		Position munich = new Position(48.135125, 11.581981);
		
		mongo.insert("simulations", new JsonObject().put("_id", "demo").put("description", "small demo simulation with traffic incidents"), sim -> {
			createSimulationData(berlin, factoryStuttgart, "demo", true, null);
			createSimulationData(hamburg, factoryStuttgart, "demo", true, null);
			createSimulationData(munich, factoryStuttgart, "demo", true, null);
		});
		
		JsonObject demoBig = new JsonObject().put("_id", "demoBig")
				.put("description", "large demo simulation in endless mode without traffic incidents")
				.put("endless", true);
		mongo.insert("simulations", demoBig, h -> {
			createRandomSimulationData("demoBig");
		});
	}
	
	/**
	 * Retrieve a random sample of cities from mongodb and use city pairs to create routes, trucks and traffic.
	 * Method is not indempotent and will increase the total number of trucks/routes with each call.
	 * 
	 * @param simId name of the simulation
	 */
	private void createRandomSimulationData(String simId) {
		
		vertx.eventBus().send(Bus.CITY_SAMPLE.address(), new JsonObject().put("size", CITY_SAMPLE_SIZE), (AsyncResult<Message<JsonArray>> res) -> {
			JsonArray cities = res.result().body();
			for(int i = 0; i + 1 < cities.size(); i += 2) {
				final int requestNo = i/2+1;
				JsonArray startPos = ((JsonObject) cities.getJsonObject(i)).getJsonObject("pos").getJsonArray("coordinates");
				JsonArray destPos = ((JsonObject) cities.getJsonObject(i+1)).getJsonObject("pos").getJsonArray("coordinates");
				Position start = new Position(startPos.getDouble(1), startPos.getDouble(0));
				Position dest = new Position(destPos.getDouble(1), destPos.getDouble(0));
				
				vertx.setTimer(1+i*200, h -> { // throttle to avoid timeouts due to large queue of pending requests
					LOGGER.info("Creating new simulation data {0} of {1}", requestNo, cities.size()/2);
					Future<Void> f = Future.future();
					createSimulationData(start, dest, simId, false, f);
					if(requestNo == cities.size()/2) {
						f.setHandler(c -> {
							vertx.close();
						});
					}
				});

			}
		});


	
	}
	
	/**
	 */
	private void createSimulationData(Position start, Position dest, String simId, boolean createTrafficIncidents, Future<Void> f) {
		Gson gson = Serializer.get();		
		String to = gson.toJson(dest);
		String from = gson.toJson(start);
		JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));
		
		// calculate routes
		vertx.eventBus().send(Bus.CALC_ROUTE.address(), msg, (AsyncResult<Message<String>> rpl) -> {
			if(rpl.succeeded()) {
				JsonObject route = new JsonObject(rpl.result().body());
				route.put("simulation", simId);
				
				mongo.insert("routes", route, res -> {
					if(res.succeeded()) {
						LOGGER.info("Inserted new route " + res.result());
						JsonObject truck = new JsonObject();
						truck.put("route", res.result());
						truck.put("simulation", simId);
						mongo.insert("trucks", truck, t -> {
							LOGGER.info("created truck " + t.result());
							if(f != null) f.complete();
						});
						
						if(createTrafficIncidents) {
							List<JsonObject> incidents = incidentsOnRoute(route, 3);
							for(JsonObject incident : incidents) {
								incident.put("simulation", simId);
								mongo.insert("traffic", incident, t -> {
									LOGGER.info("created traffic incident " + t.result());
								});
							}	
						}
					} else {
						LOGGER.error("Route insertion failed: ", res.cause());
						if(f != null) f.complete();
					}
				});
			} else {
				if(f != null) f.complete();
			}
		});
	}
	
	
	private List<JsonObject> incidentsOnRoute(JsonObject route, int max) {
		JsonArray geometries = route.getJsonObject("segments").getJsonArray("geometries");
		JsonArray startCoord, endCoord;
		List<JsonObject> incidents = new ArrayList<>();
		long gapBetweenIncidents = Long.MAX_VALUE;
		
		for(Object geo : geometries) {
			JsonObject geometry = (JsonObject) geo;
			
			if(geometry.getDouble("distance") > MIN_TRAFFIC_LENGTH && geometry.getDouble("distance") < MAX_TRAFFIC_LENGTH && incidents.size() < max && gapBetweenIncidents > MIN_GAP_BETWEEN_INCIDENTS) {
				JsonArray coordinates = geometry.getJsonArray("coordinates");
				startCoord = coordinates.getJsonArray(0);
				endCoord = coordinates.getJsonArray(coordinates.size() - 1);
				JsonObject incident = new JsonObject();
				JsonObject startPos = new JsonObject().put("type", "Point").put("coordinates", startCoord);
				JsonObject endPos = new JsonObject().put("type", "Point").put("coordinates", endCoord);
				
				incident.put("start", startPos);
				incident.put("end", endPos);
				incident.put("speed", 1.0);
				incidents.add(incident);
				
				gapBetweenIncidents = 0;
			} else {
				gapBetweenIncidents += geometry.getDouble("distance");
			}
		}
		return incidents;
	}	
	
}
