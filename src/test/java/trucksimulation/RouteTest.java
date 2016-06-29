package trucksimulation;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.graphhopper.GraphHopper;

import io.vertx.core.json.JsonObject;
import trucksimulation.routing.GraphHopperBuilder;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;

public class RouteTest {
	
	private static GraphHopper hopper;
	
	@BeforeClass
	public static void initGraphHopper() {
		// use different graphhopper cache directory to avoid conflicts
		String userHome = System.getProperty("user.home");
		String ghCacheLocation = new File(userHome, ".graphhopper-test").getAbsolutePath();
		String osmFile = new File("osm", "andorra-latest.osm.pbf").toString();
		hopper = GraphHopperBuilder.get(osmFile, ghCacheLocation);
	}

	@Test
	public void testAndorra() {
		Route r = Route.getRoute(hopper, new Position(42.450656, 1.485264), new Position(42.541762, 1.457115));
		
		Gson g = new Gson();
		System.out.println(g.toJson(r));
		
		// points, distance in meters and time in millis of the full path
		double distance = r.getDistanceMeters();
		int segmentCount = r.getSegmentCount();
		double timeInMs = r.getTimeMs();
		
		assertTrue(distance > 0);
		assertTrue(segmentCount > 0);
		assertTrue(timeInMs > 0);
		
		System.out.println("Distance (m): " + distance);
		System.out.println("Segments: " + segmentCount);
		System.out.println("Time in minutes: " + timeInMs/1000/60);
	}
	
	@Test
	public void testSegmentSerialization() {
		Gson gson = Serializer.get();
		double[] lats = {10.0,11.0,12.0};
		double[] lons = {9.0, 8.0, 8.0};
		RouteSegment testSegment = new RouteSegment(lats, lons, 200.0, 4000.0);
		String json = gson.toJson(testSegment);
		//should be valid json
		new JsonObject(json);
		
		RouteSegment seg = gson.fromJson(json, RouteSegment.class);
		assertTrue(testSegment.getLats()[0] == seg.getLats()[0]);
		assertTrue(testSegment.getLons()[0] == seg.getLons()[0]);
	}
	
	@Test
	public void testRouteSerialization() {
		double[] lats1 = {11.0,12.0,13.0};
		double[] lons1 = {55.0,54.2,55.1};
		RouteSegment seg1 = new RouteSegment(lats1, lons1, 5000, 10000);
		double[] lats2 = {10.0,11.0,12.0};
		double[] lons2 = {9.0, 8.0, 8.0};
		RouteSegment seg2 = new RouteSegment(lats2, lons2, 200.0, 4000.0);
		
		Route r = new Route();
		RouteSegment[] segments = {seg1, seg2};
		r.setSegments(segments);
		
		System.out.println(Serializer.get().toJson(r));
	}

}
