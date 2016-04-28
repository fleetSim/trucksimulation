package trucksimulation.routing;

import java.io.File;
import java.util.Locale;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Instruction;


public class Route {
	
	private Position start;	
	private Position goal;
	private RouteSegment[] segments;
	private double time;
	private double distance;
	private transient PathWrapper pathWrapper;
	
	
	public Route(Position start, Position goal) {
		this.start = start;
		this.goal = goal;
		init();
	}
	
	private void init() {
		calcRoute();
		loadRouteFromWrapper();
	}
	
	private void calcRoute() {
		// create one GraphHopper instance
		GraphHopper hopper = new GraphHopper().forServer();
		//hopper.importOrLoad();
		
		String userHome = System.getProperty("user.home");
		hopper.setOSMFile(new File("osm-maps", "denmark-latest.osm.pbf").getAbsolutePath());
		hopper.setGraphHopperLocation(new File(userHome, ".graphhopper").getAbsolutePath());
		hopper.setEncodingManager(new EncodingManager("car"));
		hopper.importOrLoad();

		// simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
		GHRequest req = new GHRequest(start.getLat(), start.getLon(), goal.getLat(), goal.getLon()).
		    setWeighting("fastest").
		    setVehicle("car").
		    setLocale(Locale.US);
		GHResponse rsp = hopper.route(req);

		// first check for errors
		if(rsp.hasErrors()) {
		   throw new IllegalArgumentException("Could not calculate route. Check coordinates.", rsp.getErrors().get(0));
		}
		pathWrapper = rsp.getBest();
	}
	
	private void loadRouteFromWrapper() {
		this.time = pathWrapper.getTime();
		this.distance = pathWrapper.getDistance();
		
		segments = new RouteSegment[pathWrapper.getInstructions().size()];
		for(int s = 0; s < pathWrapper.getInstructions().size(); s++) {
			Instruction inst = pathWrapper.getInstructions().get(s);
			double dist = inst.getDistance();
			double time = inst.getTime();
			double lats[] = new double[inst.getPoints().size()];
			double lons[] = new double[inst.getPoints().size()];
			for(int i = 0; i < inst.getPoints().size(); i++) {
				lats[i] = inst.getPoints().getLat(i);
				lons[i] = inst.getPoints().getLon(i);
			}
			RouteSegment segment = new RouteSegment(lats, lons, time, dist);
			segments[s] = segment;
		}
		// wrapper can be garbage collected, it is no longer needed
		pathWrapper = null;
	}
	
	
	public RouteSegment getSegment(int index) {
		return segments[index];
	}
	
	public int getSegmentCount() {
		return segments.length;
	}


	public PathWrapper getPathWrapper() {
		return pathWrapper;
	}


	public Position getStart() {
		return start;
	}


	public void setStart(Position start) {
		this.start = start;
	}


	public Position getGoal() {
		return goal;
	}


	public void setGoal(Position goal) {
		this.goal = goal;
	}
	
	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public RouteSegment[] getSegments() {
		return segments;
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", start, goal);
	}

}
