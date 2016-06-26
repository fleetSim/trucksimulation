package amqp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import trucksimulation.Bus;


/**
 * Forwards messages to an AMQP queue.
 */
public class AmqpBridgeVerticle extends AbstractVerticle {
	
	private Channel channel;
	private static final String QUEUE_NAME = "simulation";
	private static final Logger LOGGER = LoggerFactory.getLogger(AmqpBridgeVerticle.class);
	private JsonObject amqpConf;

	
	@Override
	public void start() throws Exception {
		amqpConf = config().getJsonObject("amqp");
		connect();
		vertx.eventBus().consumer(Bus.AMQP_PUB.address(), this::publish);
		vertx.eventBus().consumer(Bus.TRUCK_STATE.address(), this::publish);
	}
	
	
	private void connect() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, TimeoutException {		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setUri(amqpConf.getString("uri", "amqp://localhost"));
		Connection conn = factory.newConnection();
		channel = conn.createChannel();
		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
	}
	
	
	private void publish(Message<JsonObject> msg) {
		JsonObject json = msg.body();
		byte[] body = json.toString().getBytes();
		try {
			channel.basicPublish("", QUEUE_NAME, null, body);
		} catch (IOException e) {
			LOGGER.error("could not submit message to AMQP broker", e);
		}
		
	}
}
