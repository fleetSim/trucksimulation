package trucksimulation.trucks;

public interface TruckEventListener {
	
	int ENTER_TRAFFIC = 0;
	int LEAVE_TRAFFIC = 1;
	
	void handleTrafficEvent(Truck truck, int type);

}
