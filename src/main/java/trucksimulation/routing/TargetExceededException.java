package trucksimulation.routing;

public class TargetExceededException extends Exception {

	private static final long serialVersionUID = 1L;
	private double exceededBy = 0;

	public TargetExceededException() {
	}

	public TargetExceededException(String message) {
		super(message);
	}
	
	public TargetExceededException(String message, double exceededMeters) {
		super(message);
		this.exceededBy = exceededMeters;
	}

	public double getExceededBy() {
		return exceededBy;
	}

	public void setExceededBy(double exceededBy) {
		this.exceededBy = exceededBy;
	}

	public TargetExceededException(Throwable cause) {
		super(cause);
	}

	public TargetExceededException(String message, Throwable cause) {
		super(message, cause);
	}

	public TargetExceededException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
