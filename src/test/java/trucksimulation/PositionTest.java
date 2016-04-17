package trucksimulation;

import static org.junit.Assert.*;

import org.junit.Test;

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
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
			double distanceOld = distance;
			distance = start.getDistance(target);
			System.out.println("Moved " + (distanceOld - distance) + " meters. Remaining distance: " + distance);
		}
		
	}

}
