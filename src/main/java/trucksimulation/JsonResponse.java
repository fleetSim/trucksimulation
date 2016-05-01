package trucksimulation;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public interface JsonResponse {

	static final String CONTENT_TYPE = "content-type";
	static final String APPLICATION_JSON = "application/json";

	public static HttpServerResponse build(RoutingContext ctx) {
		return ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON);
	}

}
