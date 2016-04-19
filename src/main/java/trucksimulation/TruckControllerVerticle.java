package trucksimulation;

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
		startTrucks(40); //FIXME: separate cpu intense path finding into worker verticle
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
	
	
	private void startTrucks(int num) {
		Random rand = new Random();
		
		mongo.find("cities", new JsonObject(), res -> {
			int numTrucks = num;
			if(res.result().size() < num) {
				numTrucks = res.result().size();
			}
			int i = 0;
			while(i < numTrucks) {
				int idx1 = rand.nextInt(res.result().size());
				int idx2 = rand.nextInt(res.result().size());
				JsonObject start = res.result().get(idx1);
				JsonObject goal = res.result().get(idx2);
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
			startMoving();			
		});
	}

}
