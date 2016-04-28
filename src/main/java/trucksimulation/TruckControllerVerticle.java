package trucksimulation;

import java.util.List;
import java.util.Random;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.Truck;

public class TruckControllerVerticle extends AbstractVerticle {
	
	private static final int INTERVAL_MS = 1000;
	private static final int MIN_DISTANCE = 50000;
	private MongoClient mongo;
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, new JsonObject().put("db_name", "test"));
		startSimulation(10);
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
						System.out.println("Starting truck " + t.getId());
					}
				});
				num--;
			}
		}
	}
	
	private void startMoving(Truck t) {
			vertx.setPeriodic(INTERVAL_MS, timerId -> {
				try {
					t.move();
					vertx.eventBus().publish("trucks", t.getJsonData());
				} catch(Exception ex) {
					ex.printStackTrace();
					vertx.cancelTimer(timerId);
				}							
			});
			

	}

}
