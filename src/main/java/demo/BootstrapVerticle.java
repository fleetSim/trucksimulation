package demo;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.Serializer;
import trucksimulation.TruckControllerVerticle;
import trucksimulation.routing.Position;
import trucksimulation.routing.RouteCalculationVerticle;

/**
 * Verticle for performing bootstrapping tasks such as
 * setting up the database and loading initial data.
 *
 */
public class BootstrapVerticle extends AbstractVerticle {
	
	private MongoClient mongo;
	private static final Logger LOGGER = LoggerFactory.getLogger(TruckControllerVerticle.class);
	
	@Override
	public void start() throws Exception {
		DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setConfig(config());
		
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		
		vertx.deployVerticle(new RouteCalculationVerticle(), routeMgrOptions, w -> {
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
		
		createSimulationData(berlin, factoryStuttgart, "demo");
		createSimulationData(hamburg, factoryStuttgart, "demo");
		createSimulationData(munich, factoryStuttgart, "demo");
	}
	
	/**
	 */
	private void createSimulationData(Position start, Position dest, String simId) {
		Gson gson = Serializer.get();		
		String to = gson.toJson(dest);
		String from = gson.toJson(start);
		JsonObject msg = new JsonObject().put("from", new JsonObject(from)).put("to", new JsonObject(to));
		
		mongo.insert("simulations", new JsonObject().put("_id", "demo").put("description", "demo simulation"), sim -> {
			
		});
		
		// calculate routes
		vertx.eventBus().send("routes.calculate", msg, (AsyncResult<Message<String>> rpl) -> {
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
						});
						
						List<JsonObject> incidents = incidentsOnRoute(route, 3);
						for(JsonObject incident : incidents) {
							incident.put("simulation", simId);
							mongo.insert("traffic", incident, t -> {
								LOGGER.info("created traffic incident " + t.result());
							});
						}

					} else {
						LOGGER.error("Route insertion failed: ", res.cause());
					}
				});
			}
		});
	}
	
	
	private List<JsonObject> incidentsOnRoute(JsonObject route, int max) {
		JsonArray geometries = route.getJsonObject("segments").getJsonArray("geometries");
		JsonArray startCoord, endCoord;
		List<JsonObject> incidents = new ArrayList<>();
		
		for(Object geo : geometries) {
			JsonObject geometry = (JsonObject) geo;
			if(geometry.getDouble("distance") > 800 && incidents.size() < max) {
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
			}
		}
		return incidents;
	}	
	
}
