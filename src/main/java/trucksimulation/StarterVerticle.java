package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import trucksimulation.routing.RouteManager;

public class StarterVerticle extends AbstractVerticle {
	
	
	@Override
	  public void start() {

	    DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setConfig(config());
	    DeploymentOptions deplOptions = new DeploymentOptions().setConfig(config());
		
	    vertx.deployVerticle(new RouteManager(), routeMgrOptions, w -> {
	    	if(w.failed()) {
	    		w.cause().printStackTrace();
	    	}
		    vertx.deployVerticle(new TruckControllerVerticle(), deplOptions, e -> {
		    	if(e.failed()) {
		    		e.cause().printStackTrace();
		    	}
		    });
	    });
	    
	    vertx.deployVerticle(new Server(), deplOptions,  e -> {
	    	if(e.failed()) {
	    		e.cause().printStackTrace();
	    	}
	    });
	  }
}
