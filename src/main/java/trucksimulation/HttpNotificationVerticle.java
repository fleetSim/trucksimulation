package trucksimulation;

import java.net.MalformedURLException;
import java.net.URL;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Listens on the eventbus for messages from trucks and forwards them to a http server.
 *
 */
public class HttpNotificationVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationVerticle.class);

	@Override
	public void start() throws Exception {
		try {
			postToHttpListener();
		} catch(MalformedURLException ex) {
			LOGGER.error("http receiver url is not valid. No notifications will be posted. " + ex.getMessage());
			vertx.undeploy(this.deploymentID());
		}
	}

	private void postToHttpListener() throws MalformedURLException {
		URL url = new URL(config().getJsonObject("simulation", new JsonObject()).getString("receiverUrl"));
		String host = url.getHost();
		int port = url.getPort();
		String path = url.getPath();
		
		HttpClientOptions opts = new HttpClientOptions().setDefaultHost(host).setDefaultPort(port).setConnectTimeout(500);
		HttpClient client = vertx.createHttpClient(opts);
		vertx.eventBus().consumer("trucks", (Message<JsonObject> msg) -> {
			client.post(path, response -> {
				LOGGER.trace("notified " + url.toString());
			}).putHeader("content-type", "application/json").end(msg.body().toString());

		});
	}

}
