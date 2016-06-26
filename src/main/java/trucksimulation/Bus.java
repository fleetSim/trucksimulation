package trucksimulation;

public enum Bus {
	START_SIMULATION("simulation.start"),
	STOP_SIMULATION("simulation.stop"),
	SIMULATION_STATUS("simulation.status"),
	SIMULATION_ENDED("simulation.ended"),
	TRUCK_STATE("truck.state"),
	BOX_MSG_DETER("truck.box.deteriorated"),
	BOX_MSG("truck.box"),
	CALC_ROUTE("route.calculate"),
	AMQP_PUB("amqp.publish");
	
	private String address;
	Bus(String address) {
		this.address = address;
	}
	
	public String address() {
		return address;
	}
}
