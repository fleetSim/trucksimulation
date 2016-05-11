package trucksimulation.routing;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import trucksimulation.Serializer;

/**
 * Serializer and deserializer for arrays of RouteSegments.
 * 
 * When represented as json, arrays are wrapped in a json object which represents a
 * geojson GeometryCollection or unwrapped when deserialized respectively.
 */
public class RouteSegmentArrayAdapter implements JsonSerializer<RouteSegment[]>, JsonDeserializer<RouteSegment[]> {

	/**
	 * Wraps the array in a json object which represents a geojson GeometryCollection.
	 * 
	 * @see http://geojson.org/geojson-spec.html
	 */
	@Override
	public JsonElement serialize(RouteSegment[] src, Type typeOfSrc, JsonSerializationContext context) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RouteSegment.class, new RouteSegmentAdapter());
		Gson gson = builder.create();
		JsonParser parser = new JsonParser();
		// gson.toJsonTree(src) does not seem to work 
		JsonArray arr = parser.parse(gson.toJson(src)).getAsJsonArray();
		JsonObject geojson  = new JsonObject();
		geojson.addProperty("type", "GeometryCollection");
		geojson.add("geometries", arr);
		return geojson;
	}

	/**
	 * Uses the geometries field of the GeometryCollection to perform standard deserialization.
	 * Other fields are ignored.
	 */
	@Override
	public RouteSegment[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if(json.isJsonObject()) {
			JsonObject geojson = json.getAsJsonObject();
			JsonArray arr = geojson.get("geometries").getAsJsonArray();
			GsonBuilder builder = Serializer.getBuilder();
			builder.registerTypeAdapter(RouteSegment.class, new RouteSegmentAdapter());
			return builder.create().fromJson(arr, RouteSegment[].class);	
		} else {
			throw new IllegalStateException("could not deserialize segments field, was expecting an object for " + typeOfT);
		}
	}
	

}
