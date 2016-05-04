package trucksimulation.routing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RouteSegmentAdapter extends TypeAdapter<RouteSegment> {

	
	@Override
	public RouteSegment read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
	         reader.nextNull();
	         return null;
	    }
		reader.beginObject();
		RouteSegment segment = new RouteSegment();
		List<Double> lats = new ArrayList<>();
		List<Double> lons = new ArrayList<>();
		
		while(reader.hasNext()) {
			String key = reader.nextName();
			if(key.equals("time")) {
				segment.setTime(reader.nextDouble());
			} else if(key.equals("distance")) {
				segment.setDistance(reader.nextDouble());
			} else if(key.equals("coordinates")) {
				reader.beginArray();
				while(reader.hasNext()) {
					reader.beginArray();
					lons.add(reader.nextDouble());
					lats.add(reader.nextDouble());
					reader.endArray();
				}
				reader.endArray();
			} else {
				reader.skipValue();
			}
		}
		reader.endObject();
		segment.setLats(toArray(lats));
		segment.setLons(toArray(lons));
		return segment;
	}
	
	private double[] toArray(List<Double> list) {
		double[] arr = new double[list.size()];
		for(int i = 0; i < list.size(); i++) {
			arr[i] = list.get(i);
		}
		return arr;
	}

	@Override
	public void write(JsonWriter writer, RouteSegment value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}
		JsonObject geoLinestring = new JsonObject();
		geoLinestring.put("type", "LineString");
		geoLinestring.put("distance", value.getDistance());
		geoLinestring.put("time", value.getTime());
		geoLinestring.put("speed", value.getSpeed());
		JsonArray arr = new JsonArray();
		for(int idx = 0; idx < value.getSize(); idx++) {
			arr.add(new JsonArray(Arrays.asList(value.getLons()[idx], value.getLats()[idx])));
		}
		geoLinestring.put("coordinates", arr);
		writer.jsonValue(geoLinestring.toString());
	}

}
