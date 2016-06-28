package amqp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rabbitmq.RabbitMQClient;
import trucksimulation.Bus;


/**
 * Forwards messages to an AMQP queue.
 */
public class AmqpBridgeVerticle extends AbstractVerticle {
	
	private static final String QUEUE_NAME = "simulation";
	private static final Logger LOGGER = LoggerFactory.getLogger(AmqpBridgeVerticle.class);
	private JsonObject amqpConf;
	private RabbitMQClient client;

	
	@Override
	public void start() throws Exception {
		amqpConf = config().getJsonObject("amqp");
		client = RabbitMQClient.create(vertx, amqpConf);
		client.start(this::declareQueue);
	}
	
	private void declareQueue(AsyncResult<Void> startResult) {
		if(startResult.succeeded()) {
			client.queueDeclare(QUEUE_NAME, false, false, false, declareResult -> {
				if(declareResult.succeeded()) {
					vertx.eventBus().consumer(Bus.AMQP_PUB.address(), this::publish);
					vertx.eventBus().consumer(Bus.TRUCK_STATE.address(), this::publish);
				} else {
					LOGGER.error("Could not declare queue {0}", QUEUE_NAME, declareResult.cause());
				}
			});
		} else {
			LOGGER.error("Could not connect to RabbitMQ Server. Check configuration.", startResult.cause());
		}

	}
	
	
	private void publish(Message<JsonObject> msg) {
		JsonObject body = msg.body();
		JsonObject message = new JsonObject().put("body", body.toString());
		
		client.basicPublish("", QUEUE_NAME, message, pubResult -> {
			if(pubResult.succeeded()) {
				msg.reply(pubResult.result());
			} else {
				pubResult.cause().printStackTrace();
				LOGGER.error(pubResult.cause());
				msg.fail(500, pubResult.cause().getMessage());
			}
			
		});		
	}
}
