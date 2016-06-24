package trucksimulation.routing;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Serializes a position to a geojson feature.
 *
 */
public class PositionAdapter implements JsonSerializer<Position>, JsonDeserializer<Position> {

	@Override
	public JsonElement serialize(Position src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject geometry = new JsonObject();
		JsonArray coordinates = new JsonArray();
		coordinates.add(src.getLon());
		coordinates.add(src.getLat());
		geometry.addProperty("type", "Point");
		geometry.add("coordinates", coordinates);
		return geometry;
	}

	/**
	 * Handles geojson geometries as well as geojson points.
	 */
	@Override
	public Position deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
			throws JsonParseException {
		JsonObject jsonObj = json.getAsJsonObject();
		JsonArray coordinates;
		if(jsonObj.has("geometry")) {
			coordinates = jsonObj.getAsJsonObject("geometry").getAsJsonArray("coordinates");
		} else if(jsonObj.has("coordinates")) {
			coordinates = jsonObj.getAsJsonArray("coordinates");
		} else {
			// fallback to default deserialization
			Gson gson = new Gson();
			return gson.fromJson(json, Position.class);
		}
		double lon = coordinates.get(0).getAsDouble();
		double lat = coordinates.get(1).getAsDouble();
		Position pos = new Position(lat, lon);
		return pos;
	}

}
