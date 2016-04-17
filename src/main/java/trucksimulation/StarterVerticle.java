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
	    Router router = Router.router(vertx);
	    router.route().handler(StaticHandler.create());
	    
	    BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("trucks"));
	    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));
	    vertx.createHttpServer().requestHandler(router::accept).listen(8080);	    
	    vertx.setPeriodic(100, t -> vertx.eventBus().publish("trucks", "news from the server!"));
	    
	    vertx.deployVerticle(new TruckControllerVerticle(), e -> {
	    	
	    });
	  }

}
