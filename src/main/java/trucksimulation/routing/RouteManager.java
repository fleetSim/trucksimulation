package trucksimulation.routing;

import java.io.File;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RouteManager extends AbstractVerticle {
	
	private String osmFile;
	private static final Logger LOGGER = LoggerFactory.getLogger(RouteManager.class);
	
	@Override
	public void start() throws Exception {
		JsonObject simConf = config().getJsonObject("simulation", new JsonObject());
		osmFile = simConf.getString("osmFile", new File("osm", "denmark-latest.osm.pbf").getAbsolutePath());
		
		vertx.eventBus().consumer("routes.calculate", this::calcRoute);
	}
	
	private void calcRoute(Message<JsonObject> msg) {
		JsonObject from = msg.body().getJsonObject("from");
		JsonObject to = msg.body().getJsonObject("to");
		Gson gson = new Gson();
		Position fromPos = gson.fromJson(from.toString(), Position.class);
		Position toPos = gson.fromJson(to.toString(), Position.class);
		try {
			Route route = new Route(fromPos, toPos, osmFile);
			msg.reply(gson.toJson(route));
		} catch(Exception ex) {
			LOGGER.error("Route could not be calculated", ex);
			msg.fail(500, ex.getMessage());
		}
		
		
	}

}
