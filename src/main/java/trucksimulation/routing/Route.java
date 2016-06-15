package trucksimulation.routing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Instruction;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class Route {
	
	private Position start;	
	private Position goal;
	private RouteSegment[] segments;
	private double timeMs;
	private double distanceMeters;
	private transient PathWrapper pathWrapper;
	private transient String osmPath;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Route.class);
	
	public Route() {
		
	}
	
	public Route(Position start, Position goal, String osmPath) {
		this.start = start;
		this.goal = goal;
		this.osmPath = osmPath;
		init();
	}
	
	public Route(Position start, Position goal) {
		this(start, goal, new File("osm", "denmark-latest.osm.pbf").getAbsolutePath());
	}
	
	private void init() {
		calcRoute();
		loadRouteFromWrapper();
	}
	
	private void calcRoute() {
		// create one GraphHopper instance
		GraphHopper hopper = new GraphHopper().forServer();
		
		String userHome = System.getProperty("user.home");
		hopper.setOSMFile(osmPath);
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
		this.timeMs = pathWrapper.getTime();
		this.distanceMeters = pathWrapper.getDistance();
		
		List<RouteSegment> tmpSegList = new ArrayList<>(pathWrapper.getInstructions().size());
		for(int s = 0; s < pathWrapper.getInstructions().size(); s++) {
			Instruction inst = pathWrapper.getInstructions().get(s);
			if(inst.getPoints().size() > 1) {
				double dist = inst.getDistance();
				double time = inst.getTime();
				double lats[] = new double[inst.getPoints().size()];
				double lons[] = new double[inst.getPoints().size()];
				for(int i = 0; i < inst.getPoints().size(); i++) {
					lats[i] = inst.getPoints().getLat(i);
					lons[i] = inst.getPoints().getLon(i);
				}
				RouteSegment segment = new RouteSegment(lats, lons, time, dist);
				tmpSegList.add(segment);
			} else {
				//TODO: append point to previous segment if position is different
				// from previous point
				LOGGER.warn("Dropped point from instruction list.");
			}
		}
		segments = tmpSegList.toArray(new RouteSegment[0]);
		// wrapper can be garbage collected, it is no longer needed
		pathWrapper = null;
	}
	
	
	public RouteSegment getSegment(int index) {
		return segments[index];
	}
	
	public int getSegmentCount() {
		return segments.length;
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
	
	/**
	 * 
	 * @return approximate time in milliseconds that is needed to drive the route.
	 */
	public double getTimeMs() {
		return timeMs;
	}

	public void setTimeMs(double time) {
		this.timeMs = time;
	}

	public double getDistanceMeters() {
		return distanceMeters;
	}

	public void setDistanceMeters(double distance) {
		this.distanceMeters = distance;
	}

	public RouteSegment[] getSegments() {
		return segments;
	}
	
	/**
	 * Sets the route's segments and updates start and goal accordingly.
	 * @param segments
	 */
	public void setSegments(RouteSegment... segments) {
		if(segments == null) {
			throw new IllegalArgumentException("segments must not be null");
		}
		this.segments = segments;
		this.start = segments[0].getPoint(0);
		RouteSegment lastSeg = segments[segments.length-1];
		this.goal = lastSeg.getPoint(lastSeg.getSize()-1);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", start, goal);
	}

}
