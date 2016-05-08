package trucksimulation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.RouteSegmentAdapter;
import trucksimulation.routing.RouteSegmentArrayAdapter;

public interface Serializer {
	
	static Gson get() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RouteSegment.class, new RouteSegmentAdapter());
		builder.registerTypeAdapter(RouteSegment[].class, new RouteSegmentArrayAdapter());
		Gson gson = builder.create();
		return gson;
	}

}
