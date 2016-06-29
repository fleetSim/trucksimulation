package trucksimulation;

import java.io.File;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.graphhopper.GraphHopper;

import trucksimulation.routing.GraphHopperBuilder;
import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckTest {
	
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
	public void testMovement() throws InterruptedException {	
		Truck t1 = Truck.buildTruck();
		Route r = Route.getRoute(hopper, new Position(42.450656, 1.485264), new Position(42.541762, 1.457115));
		t1.setRoute(r);
		
		long journeyTime = 0;
		
		while(true) {
			try {
				t1.move();
				Thread.sleep(1);
				journeyTime++;
			} catch (DestinationArrivedException ex) {
				break;
			}
		}
		System.out.println("Journey took " + journeyTime + " seconds.");
		System.out.println("estimated journey time was " + t1.getRoute().getTimeMs()/1000);
	}
	
	@Test
	public void testPauseMode() {
		Truck t1 = Truck.buildTruck();
		Route r = Route.getRoute(hopper, new Position(42.541762, 1.457115), new Position(42.450656, 1.485264));
		t1.setRoute(r);
		Assert.assertFalse(t1.isInPauseMode());
		t1.move();
		Position pos = t1.getPos();
		t1.pause(1); // pause for 60 seconds
		Assert.assertTrue(t1.isInPauseMode());
		
		for(int i = 0; i < 60; i++) {
			t1.move();
			Assert.assertEquals("Truck should not change position in pause mode.", pos, t1.getPos());
		}
		Assert.assertFalse(t1.isInPauseMode());
		t1.move();
		Assert.assertNotEquals(pos,  t1.getPos());
	}

}
