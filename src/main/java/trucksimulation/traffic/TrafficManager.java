package trucksimulation.traffic;

import com.google.gson.Gson;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import trucksimulation.JsonResponse;
import trucksimulation.Serializer;

public class TrafficManager {
	
	private MongoClient mongo;
	private static final Logger LOGGER = LoggerFactory.getLogger(TrafficManager.class);

	public TrafficManager(MongoClient mongo) {
		this.mongo = mongo;
	}
	
	public void getTraffic(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId"));
		mongo.find("traffic", query, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).putHeader("Connection", "close").end(res.result().toString());
			}
		});
	}
	
	
	/**
	 * Responds with reported traffic incident models that are closest to a given position.
	 * The query position must be passed in as http query parameter.
	 * 
	 * @see TrafficQueryParams 
	 * 
	 * @param ctx
	 */
	public void getNearByTrafficModels(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId")).put("reported", true);
		TrafficQueryParams params;
		
		try {
			params = new TrafficQueryParams(ctx.request().params());
		} catch(IllegalArgumentException ex) {
			JsonObject err = new JsonObject().put("error", ex.getMessage());
			JsonResponse.build(ctx).setStatusCode(400).end(err.toString());
			return;
		}
		JsonObject geoJsonPoint = new JsonObject().put("type", "Point").put("coordinates", params.getLonLatArr());
		JsonObject geoNearCommand = new JsonObject().put("geoNear", "traffic")
				.put("near", geoJsonPoint) //
				.put("maxDistance", params.getMaxDistance()) //
				.put("limit", 3) //
				.put("spherical", true) //
				.put("query", query);
		
		mongo.runCommand("geoNear", geoNearCommand, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				Gson gson = Serializer.get();
				JsonArray results = res.result().getJsonArray("results");
				TrafficModel[] reports = new TrafficModel[results.size()];
				
				for(int i = 0; i < reports.length; i++) {
					JsonObject result = results.getJsonObject(i);
					TrafficIncident incident = gson.fromJson(result.getJsonObject("obj").toString(), TrafficIncident.class);
					TrafficModel m = new TrafficModel(incident);
					m.setDistance(result.getDouble("dis"));
					reports[i] = m;
				}
				JsonResponse.build(ctx).end(gson.toJson(reports));
			}
		});
	}

	/**
	 * Inserts a new traffic document and returns the created document.
	 * @param ctx
	 */
	public void createTraffic(RoutingContext ctx) {
		JsonObject trafficIncident = ctx.getBodyAsJson();
		trafficIncident.put("simulation", ctx.request().getParam("simId"));
		
		mongo.insert("traffic", trafficIncident, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				String id = res.result();
				trafficIncident.put("_id", id);
				JsonResponse.build(ctx).setStatusCode(201).end(trafficIncident.toString());
			}
		});
	}
	

}
