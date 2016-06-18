package trucksimulation;

import com.google.gson.Gson;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import trucksimulation.traffic.TrafficIncident;
import trucksimulation.traffic.TrafficManager;
import trucksimulation.traffic.TrafficModel;

public class Server extends AbstractVerticle {
	
	private MongoClient mongo;

	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
	    Router router = Router.router(vertx);
	    setUpBusBridge(router);
	    setUpRoutes(router);
	    router.route().handler(StaticHandler.create());
	    vertx.createHttpServer().requestHandler(router::accept).listen(8080);
	}
	
	private void setUpBusBridge(final Router router) {
		BridgeOptions opts = new BridgeOptions()//
				.addOutboundPermitted(new PermittedOptions().setAddress("trucks"))//
				.addOutboundPermitted(new PermittedOptions().setAddress("trucks.real"));
	    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
	    router.route("/eventbus/*").handler(ebHandler);
	}
	
	private void setUpRoutes(Router router) {
		TrafficManager trafficMgr = new TrafficManager(mongo);
		
		// regex caputes simId
		router.routeWithRegex("/api/v1/simulations\\/([^\\/]+)\\/(.*)").handler(this::provideSimulationContext);
		router.get("/api/v1/simulations/:simId/routes/:routeId").handler(this::getRoute);
		router.get("/api/v1/simulations/:simId/routes").handler(this::getRoutes);
		router.get("/api/v1/simulations/:simId/trucks/:truckId").handler(this::getTruck);
		router.get("/api/v1/simulations/:simId/trucks").handler(this::getTrucks);
		router.get("/api/v1/simulations/:simId/trafficservice").handler(trafficMgr::getTrafficModels);
		router.get("/api/v1/simulations/:simId/traffic").handler(trafficMgr::getTraffic);
		router.post("/api/v1/simulations/:simId/traffic").handler(trafficMgr::createTraffic);
		router.post("/api/v1/simulations/:simId/start").handler(this::startSimulation);
		router.post("/api/v1/simulations/:simId/stop").handler(this::stopSimulation);
		router.get("/api/v1/simulations/:simId").handler(this::getSimulation);
		router.get("/api/v1/simulations").handler(this::getSimulations);
	}	

	
	private void provideSimulationContext(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("_id", ctx.request().getParam("param0"));
		mongo.findOne("simulations", query, new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else if(res.result() == null) {
				JsonResponse.build(ctx).setStatusCode(404).end();
			} else {
				ctx.put("simulation", res.result());
				ctx.next();
			}
		});
	}
	
	private void getSimulations(RoutingContext ctx) {
		mongo.find("simulations", new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	private void getSimulation(RoutingContext ctx) {
		JsonObject simulation = ctx.get("simulation");
		vertx.eventBus().send("simulation.status", simulation.getString("_id"), reply -> {
					Boolean isRunning = (Boolean) reply.result().body();
					simulation.put("isRunning", isRunning);
					JsonResponse.build(ctx).end(simulation.toString());
		});
	}
	
	private void startSimulation(RoutingContext ctx) {
		JsonObject simulation = ctx.get("simulation");
		if(simulation == null) {
			throw new IllegalArgumentException("daum!");
		}
		vertx.eventBus().send("simulation.start", simulation, h -> {
			if(h.succeeded()) {
				JsonResponse.build(ctx).end(new JsonObject().put("status", "started").toString());
			} else {
				JsonResponse.build(ctx).setStatusCode(500).end(new JsonObject().put("status", "failed").toString());
			}
		});
	}
	
	private void stopSimulation(RoutingContext ctx) {
		JsonObject simulation = ctx.get("simulation");
		JsonObject query = new JsonObject().put("_id", simulation.getString("_id"));
		vertx.eventBus().publish("simulation.stop", query);
		JsonResponse.build(ctx).end(new JsonObject().put("status", "stopped").toString());
	}
	
	private void getTrucks(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId"));
		mongo.find("trucks", query, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	private void getTruck(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId"));
		query.put("_id", ctx.request().getParam("truckId"));
		mongo.findOne("trucks", query, new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else if (res.result() == null){
				JsonResponse.build(ctx).setStatusCode(404).end();
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	
	private void getRoutes(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId"));
		FindOptions options = new FindOptions();
		options.setFields(new JsonObject().put("segments", false));
		mongo.findWithOptions("routes", query, options, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	private void getRoute(RoutingContext ctx) {
		JsonObject query = new JsonObject() //
				.put("_id", ctx.request().getParam("routeId")) //
				.put("simulation", ctx.request().getParam("simId"));
		mongo.findOne("routes", query, new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else if (res.result() == null) {
				JsonResponse.build(ctx).setStatusCode(404).end();
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
}
