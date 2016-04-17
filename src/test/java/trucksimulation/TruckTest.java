package trucksimulation;

import static org.junit.Assert.*;

import org.junit.Test;

public class TruckTest {

	@Test
	public void testMovement() throws InterruptedException {
		Truck t1 = Truck.buildTruck();
		Route r = new Route(55.926081, 11.665394, 55.676097, 12.568337); // Nykøbing Sjaelland to Copenhagen
		t1.setRoute(r);
		
		long journeyTime = 0;
		
		while(true) {
			try {
				t1.move();
				System.out.println("new pos: " + t1.getPos());
				Thread.sleep(1);
				journeyTime++;
			} catch (IllegalStateException ex) {
				ex.printStackTrace();
				break;
			}
		}
		System.out.println("Journey took " + journeyTime + " seconds.");
		System.out.println("estimated journey time was " + t1.getRoute().getPathWrapper().getTime()/1000);
	}

}
