package org.graphstream.algorithm.myrmex.centroid;

public class Mass {
	private int theMass;

	public Mass(int initial) {
		assert initial >= 0;
		this.theMass = initial;
	}

	public int getMass() {
		return theMass;
	}

	public void transferTo(int mass, Mass target) {
		target.increment(decrement(mass));
	}

	protected void increment(int mass) {
		theMass += mass;
	}

	protected int decrement(int mass) {
		int t = theMass;
		theMass = Math.max(0, theMass - mass);
		
		return t - theMass;
	}
	
	protected void set(int mass) {
		theMass = mass;
	}
}
