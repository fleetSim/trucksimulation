package trucksimulation.routing;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serializes a position to a geojson feature.
 *
 */
public class PositionAdapter implements JsonSerializer<Position> {

	@Override
	public JsonElement serialize(Position src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject feature = new JsonObject();
		JsonObject geometry = new JsonObject();
		
		JsonArray coordinates = new JsonArray();
		coordinates.add(src.getLon());
		coordinates.add(src.getLat());
		
		geometry.addProperty("type", "Point");
		geometry.add("coordinates", coordinates);
		
		feature.addProperty("type", "Feature");
		feature.add("geometry",  geometry);
		
		return feature;
	}

}
