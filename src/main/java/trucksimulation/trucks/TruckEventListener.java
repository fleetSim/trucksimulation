package trucksimulation.trucks;

public interface TruckEventListener {
	enum EventType {
		ENTER_TRAFFIC,
		LEAVE_TRAFFIC,
		DESTINATION_ARRIVED
	}
	
	void handleTrafficEvent(Truck truck, EventType type);

}
