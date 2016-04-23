package trucksimulation;

import static org.junit.Assert.*;

import org.junit.Test;

import trucksimulation.routing.Position;
import trucksimulation.routing.TargetExceededException;

public class PositionTest {

	@Test
	public void testMovement() {
		Position start = new Position(55.674556955871516, 12.564518769782456);
		Position target = new Position(55.6756683962569, 12.567897980687722);
		
		double distance = start.getDistance(target);
		System.out.println(start.getDistance(target));
		
		while(start.getDistance(target) > 5) {
			try {
				start = start.moveTowards(target, 50);
			} catch (TargetExceededException e) {
				break;
			}
			double distanceOld = distance;
			distance = start.getDistance(target);
			System.out.println("Moved " + (distanceOld - distance) + " meters. Remaining distance: " + distance);
		}
		//should terminate
	}
	
	@Test
	public void testBearing() {
		Position pos1 = new Position(51, 11);
		Position pos2 = new Position(51, 10);
		double bearing = pos1.getBearing(pos1);
		assertEquals(0, Math.round(bearing));
		bearing = pos1.getBearing(pos2);
		assertTrue("bearing is wrong: " + bearing, bearing > 268 && bearing < 271);
	}

}
