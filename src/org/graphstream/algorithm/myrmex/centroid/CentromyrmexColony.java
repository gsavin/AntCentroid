package org.graphstream.algorithm.myrmex.centroid;

import org.graphstream.algorithm.myrmex.Ant;
import org.graphstream.algorithm.myrmex.AntContext;
import org.graphstream.algorithm.myrmex.AntFactory;
import org.graphstream.algorithm.myrmex.AntNode;
import org.graphstream.algorithm.myrmex.Colony;

public class CentromyrmexColony extends Colony {
	class CentromyrmexFactory implements AntFactory {
		public Ant newAnt(String id, AntNode start) {
			return new Centromyrmex(id, CentromyrmexColony.this, start, ctx);
		}
	}

	protected CentromyrmexPheromone pheromone;
	
	public CentromyrmexColony() {
		antFactory = new CentromyrmexFactory();
	}

	public CentromyrmexColony(AntContext context, String name, int index) {
		super(context, name, index);
		antFactory = new CentromyrmexFactory();
	}
}
