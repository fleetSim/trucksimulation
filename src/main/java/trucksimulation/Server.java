package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import trucksimulation.traffic.TrafficManager;

public class Server extends AbstractVerticle {
	
	private static final String SIMULATIONS_COLLECTION = "simulations";
	private static final String TRUCKS_COLLECTION = "trucks";
	private static final String ROUTES_COLLECTION = "routes";
	private MongoClient mongo;
	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	@Override
	public void start() throws Exception {
		mongo = MongoClient.createShared(vertx, config().getJsonObject("mongodb", new JsonObject()));
	    Router router = Router.router(vertx);
	    setUpBusBridge(router);
	    setUpRoutes(router);
	    router.route().handler(StaticHandler.create());
	    vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("port", 8080));
	}
	
	private void setUpBusBridge(final Router router) {
		BridgeOptions opts = new BridgeOptions()//
				.addOutboundPermitted(new PermittedOptions().setAddress(Bus.BOX_MSG.address()))//
				.addOutboundPermitted(new PermittedOptions().setAddress(Bus.BOX_MSG_DETER.address()));
	    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
	    router.route("/eventbus/*").handler(ebHandler);
	}
	
	private void setUpRoutes(Router router) {
		TrafficManager trafficMgr = new TrafficManager(mongo);
		
		// regex caputes simId
		router.routeWithRegex("/api/v1/simulations\\/([^\\/]+)(\\/)?(.*)").handler(this::provideSimulationContext);
		router.get("/api/v1/simulations/:simId/routes/:routeId").handler(this::getRoute);
		router.get("/api/v1/simulations/:simId/routes").handler(this::getRoutes);
		router.get("/api/v1/simulations/:simId/trucks/:truckId").handler(this::getTruck);
		router.get("/api/v1/simulations/:simId/trucks").handler(this::getTrucks);
		router.get("/api/v1/simulations/:simId/trafficservice").handler(trafficMgr::getNearByTrafficModels);
		router.get("/api/v1/simulations/:simId/traffic").handler(trafficMgr::getTraffic);
		router.post("/api/v1/simulations/:simId/traffic").handler(trafficMgr::createTraffic);
		router.post("/api/v1/simulations/:simId/start").handler(this::startSimulation);
		router.post("/api/v1/simulations/:simId/stop").handler(this::stopSimulation);
		router.get("/api/v1/simulations/:simId").handler(this::getSimulation);
		router.get("/api/v1/simulations").handler(this::getSimulations);
	}	

	
	private void provideSimulationContext(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("_id", ctx.request().getParam("param0"));
		mongo.findOne(SIMULATIONS_COLLECTION, query, new JsonObject(), res -> {
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
		mongo.find(SIMULATIONS_COLLECTION, new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	private void getSimulation(RoutingContext ctx) {
		JsonObject simulation = ctx.get("simulation");
		String simId = simulation.getString("_id");
		vertx.eventBus().send(Bus.SIMULATION_STATUS.address(), simId, reply -> {
			if(reply.succeeded()) {
				Boolean isRunning = (Boolean) reply.result().body();
				simulation.put("isRunning", isRunning);
				JsonResponse.build(ctx).end(simulation.toString());
			} else {
				LOGGER.error("Could not get simulation status for simulation {0}", simId, reply.cause());
			}

		});
	}
	
	private void startSimulation(RoutingContext ctx) {
		JsonObject simulation = ctx.get("simulation");
		if(simulation == null) {
			throw new IllegalArgumentException("daum!");
		}
		vertx.eventBus().send(Bus.START_SIMULATION.address(), simulation, h -> {
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
		vertx.eventBus().publish(Bus.STOP_SIMULATION.address(), query);
		JsonResponse.build(ctx).end(new JsonObject().put("status", "stopped").toString());
	}
	
	private void getTrucks(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simulation", ctx.request().getParam("simId"));
		mongo.find(TRUCKS_COLLECTION, query, res -> {
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
		mongo.findOne(TRUCKS_COLLECTION, query, new JsonObject(), res -> {
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
		mongo.findWithOptions(ROUTES_COLLECTION, query, options, res -> {
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
		mongo.findOne(ROUTES_COLLECTION, query, new JsonObject(), res -> {
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
