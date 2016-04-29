/**
 * 
 */
package trucksimulation.trucks;

/**
 * Signals that a vehicle has arrived its destination and hence can't move 
 * further towards the current destination.
 */
public class DestinationArrivedException extends IllegalStateException {

	public DestinationArrivedException() {
	}

	public DestinationArrivedException(String s) {
		super(s);
	}

	public DestinationArrivedException(Throwable cause) {
		super(cause);
	}

	public DestinationArrivedException(String message, Throwable cause) {
		super(message, cause);
	}

}
