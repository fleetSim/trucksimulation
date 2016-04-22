package trucksimulation.routing;

public class TargetExceededException extends Exception {

	private static final long serialVersionUID = 1L;

	public TargetExceededException() {
	}

	public TargetExceededException(String message) {
		super(message);
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
