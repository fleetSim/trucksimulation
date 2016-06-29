package trucksimulation.routing;

import java.io.File;
import java.util.Map;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class GraphHopperBuilder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphHopperBuilder.class);

	/**
	 * 
	 * @param osmFile path to the osmFile to use
	 * @param workingDir if multiple GH instances are used, then each should have a different workingDir
	 * @return graphhopper instance
	 */
	public static GraphHopper get(String osmFile, String workingDir) {
		GraphHopper hopper;
		// create one GraphHopper instance
		Map<String, String> env = System.getenv();
		if(Boolean.valueOf(env.get("LOW_MEMORY"))) {
			LOGGER.info("Using Graphhopper for mobile due to LOW_MEMORY env.");
			hopper = new GraphHopper().forMobile();
			hopper.setCHPrepareThreads(1);
		} else {
			hopper = new GraphHopper().forServer();
		}
		hopper.setOSMFile(osmFile);
		hopper.setGraphHopperLocation(workingDir);
		hopper.setEncodingManager(new EncodingManager("car"));
		hopper.importOrLoad();
		return hopper;
	}

}
