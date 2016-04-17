package trucksimulation;

public class TargetExceededException extends Exception {

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
