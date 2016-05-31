package trucksimulation.trucks;

import org.junit.Assert;
import org.junit.Test;

import trucksimulation.routing.Position;

public class TelemetryDataTest {

	@Test
	public void testDeterioration() {
		Position orig = new Position(11.0, 50.0);
		TelemetryData data = new TelemetryData("s", true);
		double maxDist = 0;
		for(int i = 0; i< 500; i++) {
			data.setPosition(orig);
			Position detPos = data.getPosition();
			double dist = detPos.getDistance(orig);
			if(dist > maxDist) {
				maxDist = dist;
			}
			Assert.assertTrue("position shouldn't be too far away (distance was "+ dist + ").", dist < 50);
		}
		Assert.assertTrue(maxDist > 0);
		System.out.println("maximum distance to true position due to GPS data deterioration was " + maxDist + "m");
	}

}
