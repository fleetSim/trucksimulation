package trucksimulation;

import java.util.List;
import java.util.Random;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckControllerVerticle extends AbstractVerticle {
	
	private static final int MIN_DISTANCE = 50000;
	private static final Logger LOGGER = LoggerFactory.getLogger(TruckControllerVerticle.class);
	private MongoClient mongo;
	private int intervalMS;
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
		
		intervalMS = config().getJsonObject("simulation", new JsonObject()).getInteger("interval_ms", 1000);
		
		startSimulation(1000);
	}	
	
	/**
	 * 
	 * @param num number of trucks to simulate
	 */
	private void startSimulation(int num) {
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
	
	private void startMoving(Truck t) {
			vertx.setPeriodic(intervalMS, timerId -> {
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
			

	}

}
