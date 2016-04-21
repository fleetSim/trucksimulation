package trucksimulation;

import java.util.List;
import java.util.Random;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class TruckControllerVerticle extends AbstractVerticle {
	
	private static final int INTERVAL_MS = 500;
	private static final int MIN_DISTANCE = 50000;
	private MongoClient mongo;
	
	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, new JsonObject().put("db_name", "test"));
		startSimulation(40);
	}	
	
	/**
	 * 
	 * @param num number of trucks to simulate
	 */
	private void startSimulation(int num) {
		mongo.find("cities", new JsonObject(), res -> {
			vertx.executeBlocking(future -> {
				int numTrucks = num;
				if(res.result().size() < num) {
					numTrucks = res.result().size();
				}
				createTrucksAndRoutes(numTrucks, res.result());
				future.complete();
			}, done -> {
				System.out.println("Done calculating routes, starting simulation.");
				startMoving();	
			});
		});
	}
	
	private void createTrucksAndRoutes(int num, List<JsonObject> cities) {
		Random rand = new Random();
		int i = 0;			
		while(i < num) {
			int idx1 = rand.nextInt(cities.size());
			int idx2 = rand.nextInt(cities.size());
			JsonObject start = cities.get(idx1);
			JsonObject goal = cities.get(idx2);
			Position startPos = new Position(start.getDouble("lat"), start.getDouble("lon"), start.getString("name"));
			Position goalPos = new Position(goal.getDouble("lat"), goal.getDouble("lon"), goal.getString("name"));
			if(startPos.getDistance(goalPos) > MIN_DISTANCE) {
				try {
					Route r = new Route(startPos, goalPos);
					Truck t = Truck.buildTruck();
					t.setRoute(r);
					i++;
					System.out.println("#" + i + " " + start.getString("name") + " -> " + goal.getString("name"));
				} catch(IllegalArgumentException ex) {
					// cant calculate route
					ex.printStackTrace();
				}
			}
		}
	}
	
	private void startMoving() {
		for(Truck t : Truck.getTrucks()) {
			vertx.setPeriodic(INTERVAL_MS, timerId -> {
				try {
					t.move();
					vertx.eventBus().publish("trucks", t.asGeoJsonFeature());
				} catch(Exception ex) {
					vertx.cancelTimer(timerId);
				}							
			});
			
		}

	}

}
