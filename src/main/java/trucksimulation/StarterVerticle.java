package trucksimulation;

import amqp.AmqpBridgeVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.routing.RouteCalculationVerticle;

public class StarterVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(StarterVerticle.class);

	@Override
	public void start() {

		DeploymentOptions routeMgrOptions = new DeploymentOptions().setWorker(true).setInstances(2).setConfig(config());
		DeploymentOptions deplOptions = new DeploymentOptions().setConfig(config());

		vertx.deployVerticle(RouteCalculationVerticle.class.getName(), routeMgrOptions, w -> {
			if (w.failed()) {
				LOGGER.error("Deployment of RouteManager failed.", w.cause());
			}
			vertx.deployVerticle(new SimulationControllerVerticle(), deplOptions, e -> {
				if (e.failed()) {
					LOGGER.error("Deployment of TruckController failed.", e.cause());
				}
			});
		});

		vertx.deployVerticle(new Server(), deplOptions, e -> {
			if (e.failed()) {
				LOGGER.error("Deployment of server failed. ", e.cause());
			}
		});
		
		
		if(config().getJsonObject("simulation", new JsonObject()).getBoolean("postData", true)) {
			vertx.deployVerticle(new HttpNotificationVerticle(), deplOptions, h -> {
				if (h.failed()) {
					LOGGER.error("Deployment of http notification verticle failed. ", h.cause());
				}
			});
		}
		
		if(config().getJsonObject("amqp").getBoolean("enabled", false)) {
			vertx.deployVerticle(new AmqpBridgeVerticle(), deplOptions, h -> {
				if (h.failed()) {
					LOGGER.error("Deployment of AMQP bridge verticle failed. ", h.cause());
				}
			});
		}

	}
}
