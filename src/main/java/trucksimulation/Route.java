package trucksimulation;

import java.io.File;
import java.util.Locale;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Instruction;

public class Route {
	
	private double latFrom;
	private double lonFrom;
	
	private double latTo;
	private double lonTo;
	private PathWrapper pathWrapper;
	
	
	public Route(Position start, Position goal) {
		this.latFrom = start.getLat();
		this.lonFrom = start.getLon();
		this.latTo = goal.getLat();
		this.lonTo = goal.getLon();
		calcRoute();
	}
	
	
	public void calcRoute() {
		// create one GraphHopper instance
		GraphHopper hopper = new GraphHopper().forServer();
		//hopper.importOrLoad();
		
		String userHome = System.getProperty("user.home");
		hopper.setOSMFile(new File(userHome, "denmark-latest.osm.pbf").getAbsolutePath());
		hopper.setGraphHopperLocation(new File(userHome, ".graphhopper").getAbsolutePath());
		hopper.setEncodingManager(new EncodingManager("car"));
		hopper.importOrLoad();

		// simple configuration of the request object, see the GraphHopperServlet classs for more possibilities.
		GHRequest req = new GHRequest(latFrom, lonFrom, latTo, lonTo).
		    setWeighting("fastest").
		    setVehicle("car").
		    setLocale(Locale.US);
		GHResponse rsp = hopper.route(req);

		// first check for errors
		if(rsp.hasErrors()) {
		   throw new IllegalArgumentException("Could not calculate route from " + latFrom);
		}
		pathWrapper = rsp.getBest();		
	}
	
	
	public Position getStartPosition() {
		return new Position(pathWrapper.getPoints().getLat(0), pathWrapper.getPoints().getLon(0));
	}
	
	public Instruction getSegment(int index) {
		return pathWrapper.getInstructions().get(index);
	}
	
	public int getSegmentCount() {
		return pathWrapper.getInstructions().getSize();
	}


	public double getLatFrom() {
		return latFrom;
	}

	public void setLatFrom(double latFrom) {
		this.latFrom = latFrom;
	}

	public double getLonFrom() {
		return lonFrom;
	}

	public void setLonFrom(double lonFrom) {
		this.lonFrom = lonFrom;
	}

	public double getLatTo() {
		return latTo;
	}

	public void setLatTo(double latTo) {
		this.latTo = latTo;
	}

	public double getLonTo() {
		return lonTo;
	}

	public void setLonTo(double lonTo) {
		this.lonTo = lonTo;
	}

	public PathWrapper getPathWrapper() {
		return pathWrapper;
	}

}
