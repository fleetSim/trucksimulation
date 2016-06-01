package trucksimulation;

import java.time.LocalDateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import trucksimulation.routing.Position;
import trucksimulation.routing.PositionAdapter;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.RouteSegmentAdapter;
import trucksimulation.routing.RouteSegmentArrayAdapter;
import trucksimulation.traffic.LocalDateTimeAdapter;

public interface Serializer {
	
	static Gson get() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(RouteSegment.class, new RouteSegmentAdapter());
		builder.registerTypeAdapter(RouteSegment[].class, new RouteSegmentArrayAdapter());
		builder.registerTypeAdapter(Position.class, new PositionAdapter());
		builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
		Gson gson = builder.create();
		return gson;
	}
	
	static GsonBuilder getBuilder() {
		GsonBuilder builder = new GsonBuilder();
		return builder;
	}

}
