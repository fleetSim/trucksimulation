package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class Server extends AbstractVerticle {

	@Override
	public void start() throws Exception {
	    Router router = Router.router(vertx);
	    setUpBusBridge(router);
	    router.route().handler(StaticHandler.create());
	    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}
	
	private void setUpBusBridge(final Router router) {
		BridgeOptions opts = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("trucks"));
	    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
	    router.route("/eventbus/*").handler(ebHandler);
	}
}
