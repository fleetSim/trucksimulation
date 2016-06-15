package trucksimulation;

import org.junit.Test;

import trucksimulation.routing.Position;
import trucksimulation.routing.Route;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TruckTest {

	@Test
	public void testMovement() throws InterruptedException {
		Truck t1 = Truck.buildTruck();
		Route r = new Route(new Position(52.926081, 9.665394), new Position(52.676097, 9.568337));
		t1.setRoute(r);
		
		long journeyTime = 0;
		
		while(true) {
			try {
				t1.move();
				System.out.println("new pos: " + t1.getPos().toString());
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
