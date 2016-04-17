package trucksimulation;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.graphhopper.PathWrapper;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;

public class RouteTest {

	@Test
	public void testDenmark() {
		Route r = new Route(55.926081, 11.665394, 55.676097, 12.568337); // Nykøbing Sjaelland to Copenhagen
		PathWrapper route = r.getPathWrapper();
		
		
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

}
