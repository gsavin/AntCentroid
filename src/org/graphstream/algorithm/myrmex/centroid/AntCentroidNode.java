package org.graphstream.algorithm.myrmex.centroid;

import org.graphstream.algorithm.myrmex.AntContext;
import org.graphstream.algorithm.myrmex.AntGraph;
import org.graphstream.algorithm.myrmex.AntNode;

public class AntCentroidNode extends AntNode {

	protected int previousMass;
	protected float stabilization;
	protected float previousStabilization;
	protected Mass mass;
	protected int passages;

	public AntCentroidNode(AntContext ctx, AntGraph g, String id) {
		super(ctx, g, id);
		reset();
	}

	public void reset() {
		super.reset();
		
		mass = new Mass(
				((AntCentroidParams) ctx.getAntParams()).getInitialNodeMass());
		previousMass = -1;
		passages = 0;
		previousStabilization = 0;
		stabilization = 1;
	}
	
	public Mass getMass() {
		return mass;
	}

	public void step(AntContext ctx) {
		super.step(ctx);
		
		AntCentroidParams params = ctx.getAntParams();
		
		previousStabilization = stabilization;

		if (previousMass < 0) {
			stabilization = 0;
		} else {
			stabilization = 1 - Math.abs(previousMass - mass.getMass())
					/ Math.max(1, mass.getMass());
			stabilization = Math.max(0, stabilization);
			stabilization = Math.min(1, stabilization);
			stabilization = 0.4f * stabilization + 0.6f * previousStabilization;
		}

		switch(params.massMode) {
		case MASS:
			break;
		case PASSAGES:
			mass.decrement(2);
			mass.increment(passages);
			break;
		case ANT_COUNT:
			mass.set(totalAntCount);
			break;
		}
		
		previousMass = mass.getMass();
		passages += totalAntCount;
	}

	public float getStabilization() {
		return stabilization;
	}
	
	public int getPassages() {
		return passages;
	}
}
