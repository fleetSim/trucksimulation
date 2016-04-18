package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class TruckControllerVerticle extends AbstractVerticle {
	
	@Override
	public void start() throws Exception {
		
		Position nykobing = new Position(55.926081, 11.665394);
		Position copenhagen = new Position(55.676097, 12.568337);
		Position aalborg = new Position(57.048820, 9.921747);
		Position odense = new Position(55.403756, 10.402370);
		
		Truck t1 = Truck.buildTruck();
		Route r = new Route(nykobing, copenhagen);
		t1.setRoute(r);
		
		Truck t2 = Truck.buildTruck();
		Route r2 = new Route(copenhagen, aalborg);
		t2.setRoute(r2);
		
		Truck t3 = Truck.buildTruck();
		Route r3 = new Route(odense, copenhagen);
		t3.setRoute(r3);
		
		vertx.setPeriodic(1000, timerId -> {
			t1.move();
			JsonObject msg = t1.asGeoJsonFeature();
			vertx.eventBus().publish("trucks", msg);
		});
		
		vertx.setPeriodic(1000, timerId -> {
			t2.move();
			JsonObject msg = t2.asGeoJsonFeature();
			vertx.eventBus().publish("trucks", msg);
		});
		
		vertx.setPeriodic(1000, timerId -> {
			t3.move();
			JsonObject msg = t3.asGeoJsonFeature();
			vertx.eventBus().publish("trucks", msg);
		});
		
	}

}
