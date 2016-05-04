package trucksimulation;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;

import io.vertx.core.json.JsonObject;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.routing.RouteSegmentAdapter;

public class RouteTest {

	@Test
	public void testDenmark() {
		Route r = new Route(new Position(55.926081, 11.665394), new Position(55.676097, 12.568337)); // Nykøbing Sjaelland to Copenhagen
		PathWrapper route = r.getPathWrapper();
		
		Gson g = new Gson();
		System.out.println(g.toJson(r));
		
		
		// points, distance in meters and time in millis of the full path
		PointList pointList = route.getPoints();
		double distance = route.getDistance();
		long timeInMs = route.getTime();
		
		System.out.println("Distance (m): " + distance);
		System.out.println("Points: " + pointList.getSize());
		System.out.println("Time in minutes: " + timeInMs/1000/60);

		InstructionList il = route.getInstructions();
		// iterate over every turn instruction
		for(Instruction instruction : il) {
			double dist = instruction.getDistance(); //m
			long time = instruction.getTime();
			double speedOnSegment = dist/(time/1000);
			System.out.println("Length: " + Math.round(dist) + "m.\t Speed on segment = " + Math.round(speedOnSegment*10)/10.0 + " m/s");
			Iterator<GHPoint3D> iter = instruction.getPoints().iterator();
			while(iter.hasNext()) {
				GHPoint3D pt = iter.next();
				System.out.println(pt.lat + ", \t " + pt.lon);
			}
		}

		// or get the json
		List<Map<String, Object>> iList = il.createJson();

		// or get the result as gpx entries:
		List<GPXEntry> list = il.createGPXList();
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
