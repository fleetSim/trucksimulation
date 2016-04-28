package trucksimulation.routing;

import java.io.File;

import com.google.gson.Gson;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class RouteManager extends AbstractVerticle {
	
	private String osmFile;
	
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
			ex.printStackTrace();
			msg.fail(500, ex.getMessage());
		}
		
		
	}

}
