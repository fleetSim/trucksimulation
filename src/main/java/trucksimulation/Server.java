package trucksimulation;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import trucksimulation.trucks.Truck;

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
	    
		//vertx.eventBus().registerDefaultCodec(Truck.class, truckCodec );
	}
	
	private void setUpBusBridge(final Router router) {
		BridgeOptions opts = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress("trucks"));
	    SockJSHandler ebHandler = SockJSHandler.create(vertx).bridge(opts);
	    router.route("/eventbus/*").handler(ebHandler);
	}
	
	private void setUpRoutes(Router router) {
		//router.route("/api/v1/simulations/{simId}");
		router.get("/api/v1/simulations/:simId/trucks").handler(this::getTrucks);
		router.get("/api/v1/simulations/:simId/routes").handler(this::getRoutes);
		router.post("/api/v1/simulations/:simId/start").handler(this::startSimulation);
		router.post("/api/v1/simulations/:simId/stop").handler(this::stopSimulation);
		router.get("/api/v1/simulations/:simId").handler(this::getSimulation);
		router.get("/api/v1/simulations").handler(this::getSimulations);
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
		JsonObject query = new JsonObject().put("_id", ctx.request().getParam("simId"));
		mongo.findOne("simulations", query, new JsonObject(), res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
	private void startSimulation(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("_id", ctx.request().getParam("simId"));
		vertx.eventBus().send("simulation.start", query, h -> {
			if(h.succeeded()) {
				JsonResponse.build(ctx).end(new JsonObject().put("status", "started").toString());
			} else {
				JsonResponse.build(ctx).setStatusCode(500).end(new JsonObject().put("status", "failed").toString());
			}
		});
	}
	
	private void stopSimulation(RoutingContext ctx) {
		String simulationId = ctx.request().getParam("simId");
		JsonObject query = new JsonObject().put("_id", simulationId);
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
	
	private void getRoutes(RoutingContext ctx) {
		JsonObject query = new JsonObject().put("simId", ctx.request().getParam("simId"));
		mongo.find("routes", query, res -> {
			if(res.failed()) {
				ctx.fail(res.cause());
			} else {
				JsonResponse.build(ctx).end(res.result().toString());
			}
		});
	}
	
}
