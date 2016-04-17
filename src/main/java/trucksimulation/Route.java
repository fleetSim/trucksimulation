package trucksimulation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

public class Route {
	
	private double latFrom;
	private double lonFrom;
	
	private double latTo;
	private double lonTo;
	private PathWrapper pathWrapper;
	
	
	public Route(double startLat, double startLon, double endLat, double endLon) {
		this.latFrom = startLat;
		this.lonFrom = startLon;
		this.latTo = endLat;
		this.lonTo = endLon;
		calcRoute();
	}
	
	
	public void calcRoute() {
		// create one GraphHopper instance
		GraphHopper hopper = new GraphHopper().forServer();
		//hopper.importOrLoad();
		
		String userHome = System.getProperty("user.home");
		hopper.setOSMFile("/home/rocco/Downloads/denmark-latest.osm.pbf");
		hopper.setGraphHopperLocation("/home/rocco/.graphhopper");
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
		   // handle them!
		   // rsp.getErrors()
		   return;
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
