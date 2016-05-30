package trucksimulation.routing;

import java.io.File;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.Serializer;

public class RouteCalculationVerticle extends AbstractVerticle {
	
	private String osmFile;
	private static final Logger LOGGER = LoggerFactory.getLogger(RouteCalculationVerticle.class);
	
	@Override
	public void start() throws Exception {
		JsonObject simConf = config().getJsonObject("simulation", new JsonObject());
		osmFile = simConf.getString("osmFile", new File("osm", "denmark-latest.osm.pbf").getAbsolutePath());
		LOGGER.info("Using osm file " + osmFile + " for route calculations.");
		
		vertx.eventBus().consumer("routes.calculate", this::calcRoute);
	}
	
	private void calcRoute(Message<JsonObject> msg) {
		JsonObject from = msg.body().getJsonObject("from");
		JsonObject to = msg.body().getJsonObject("to");
		Gson gson = Serializer.get();
		Position fromPos = gson.fromJson(from.toString(), Position.class);
		Position toPos = gson.fromJson(to.toString(), Position.class);
		
		try {
			Route route = new Route(fromPos, toPos, osmFile);
			msg.reply(gson.toJson(route));
		} catch(Exception ex) {
			LOGGER.error("Route could not be calculated. From " + from + " to " + to, ex);
			msg.fail(500, ex.getMessage());
		}
		
		
	}

}
