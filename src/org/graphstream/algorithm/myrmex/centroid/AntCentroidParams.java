package org.graphstream.algorithm.myrmex.centroid;

import org.graphstream.algorithm.DefineParameter;
import org.graphstream.algorithm.myrmex.AntParams;

public class AntCentroidParams extends AntParams {

	public static enum MassMode {
		MASS, PASSAGES, ANT_COUNT
	}
	
	public final double arctanp10 = Math.atan(-10);
	public final double arctanm10 = Math.atan(10);
	protected int saturation = 15;
	protected float regulation = 0.4f;
	
	protected int minMassStolen = 3;
	protected int maxMassStolen = 10;
	protected int initialNodeMass = 100;
	protected boolean logarithmicMass = false;
	@DefineParameter(name = "ant.centroid.params.mass_mode")
	protected MassMode massMode = MassMode.MASS;
	
	protected float epsilon;
	
	public void defaults() {
		super.defaults();
		
		this.colonySpecies = CentromyrmexColony.class.getName();
		this.dropPheromoneOn = AntParams.DropOn.EDGES;
		this.minMassStolen = 3;
		this.maxMassStolen = 10;
		this.epsilon = 0.2f;
		this.massMode = MassMode.ANT_COUNT;
		
		switch(massMode) {
		case MASS:
			this.initialNodeMass = 100;
			break;
		case PASSAGES:
			this.initialNodeMass = 1;
			break;
		case ANT_COUNT:
			this.initialNodeMass = 0;
			break;
		}
	}
	
	public int getSaturation() {
		return saturation;
	}
	
	public float getRegulation() {
		return regulation;
	}
	
	public int getMinMassStolen() {
		return minMassStolen;
	}
	
	public int getMaxMassStolen() {
		return maxMassStolen;
	}
	
	public int getInitialNodeMass() {
		return initialNodeMass;
	}
	
	public boolean useLogarithmicMass() {
		return logarithmicMass;
	}
	
	public float getEpsilon() {
		return epsilon;
	}
}
