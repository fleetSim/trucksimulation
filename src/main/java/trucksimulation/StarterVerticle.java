package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import trucksimulation.routing.RouteManager;

public class StarterVerticle extends AbstractVerticle {
	
	@Override
	  public void start() {

	    
	    vertx.deployVerticle(new RouteManager(), new DeploymentOptions().setWorker(true).setMultiThreaded(false), w -> {
		    vertx.deployVerticle(new TruckControllerVerticle(), e -> {
		    	if(e.failed()) {
		    		e.cause().printStackTrace();
		    	}
		    });
	    });
	    
	    vertx.deployVerticle(new Server(), e -> {
	    	if(e.failed()) {
	    		e.cause().printStackTrace();
	    	}
	    });
	  }
}
