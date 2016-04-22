package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class StarterVerticle extends AbstractVerticle {
	
	@Override
	  public void start() {
	    vertx.deployVerticle(new TruckControllerVerticle(), e -> {
	    	if(e.failed()) {
	    		e.cause().printStackTrace();
	    	}
	    });
	    
	    vertx.deployVerticle(new Server(), e -> {
	    	if(e.failed()) {
	    		e.cause().printStackTrace();
	    	}
	    });
	  }
}
