package trucksimulation.trucks;

import java.util.ArrayList;

public class Freight {
	
	private ArrayList<FreightItem> items = new ArrayList<>();

	public ArrayList<FreightItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<FreightItem> items) {
		if(items == null) {
			throw new IllegalArgumentException("must not be null");
		}
		this.items = items;
	}
	
	public void addItem(FreightItem item) {
		items.add(item);
	}

}
