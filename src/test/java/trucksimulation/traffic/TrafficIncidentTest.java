package trucksimulation.traffic;

import org.junit.Assert;
import org.junit.Test;

import trucksimulation.routing.Route;
import trucksimulation.routing.RouteSegment;
import trucksimulation.trucks.DestinationArrivedException;
import trucksimulation.trucks.Truck;

public class TrafficIncidentTest {

	@Test
	public void testTruckDetectsTraffic() {
		Truck withTraffic = new Truck("traffic");
		withTraffic.setRoute(buildRoute());
		TrafficIncident incident = new TrafficIncident();
		incident.setStart(withTraffic.getRoute().getSegment(1).getPoint(1));
		incident.setEnd(withTraffic.getRoute().getSegment(1).getPoint(3));
		incident.setSpeed(2);
		withTraffic.addTrafficIncident(incident);
		
		boolean enteredIncident = false;
		boolean leftIncident = false;
		while(true) {
			try {
				withTraffic.move();
				if(withTraffic.getCurIncident() != null) {
					enteredIncident = true;
				}
				if(enteredIncident && withTraffic.getCurIncident() == null) {
					leftIncident = true;
				}
			} catch(DestinationArrivedException ex) {
				Assert.assertTrue(enteredIncident);
				Assert.assertTrue(leftIncident);
				break;
			}

		}
	}
	
	@Test
	public void testTruckSlowsDown() {
		Truck noTraffic = new Truck("noTraffic");
		Truck withTraffic = new Truck("traffic");
		
		noTraffic.setRoute(buildRoute());
		withTraffic.setRoute(buildRoute());
		
		TrafficIncident incident = new TrafficIncident();
		incident.setStart(withTraffic.getRoute().getSegment(1).getPoint(1));
		incident.setEnd(withTraffic.getRoute().getSegment(1).getPoint(3));
		incident.setSpeed(2);
		
		withTraffic.addTrafficIncident(incident);
		
		while(true) {
			try {
				withTraffic.move();	
				noTraffic.move();
			} catch(DestinationArrivedException ex) {
				Assert.assertTrue(noTraffic.hasArrived());
				Assert.assertFalse("truck which was stuck in the traffic jam should have been slower than truck without traffic jam on same route.",
						withTraffic.hasArrived());
				break;
			}
		}
		
	}
	
	
	
	private Route buildRoute() {
		RouteSegment seg1 = new RouteSegment();
		seg1.setLats(1.0, 1.1, 1.11, 1.2, 1.3);
		seg1.setLons(1.0, 1.0, 1.4, 1.5, 1.5);
		seg1.setDistance(30);
		seg1.setTime(1000);
		RouteSegment seg2 = new RouteSegment();
		seg2.setLats(1.35, 1.4, 1.5, 1.55);
		seg2.setLons(1.55, 1.4, 1.3, 1.3);
		seg2.setDistance(35);
		seg2.setTime(1000);
		RouteSegment seg3 = new RouteSegment();
		seg3.setLats(1.65, 1.7, 1.9, 2.0);
		seg3.setLons(1.25, 1.2, 1.3, 1.3);
		seg2.setDistance(30);
		seg2.setTime(1000);
		Route r = new Route();
		r.setSegments(seg1, seg2, seg3);
		return r;
	}

}
