package trucksimulation;

import java.io.File;

import org.junit.Test;

import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckTest {

	@Test
	public void testMovement() throws InterruptedException {
		// use different graphhopper cache directory to avoid conflicts
		String userHome = System.getProperty("user.home");
		String ghCacheLocation = new File(userHome, ".graphhopper-test").getAbsolutePath();
		
		Truck t1 = Truck.buildTruck();
		Route r = Route.getRoute(new File("osm", "andorra-latest.osm.pbf").toString(), ghCacheLocation,
				new Position(42.450656, 1.485264), new Position(42.541762, 1.457115));
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

}
