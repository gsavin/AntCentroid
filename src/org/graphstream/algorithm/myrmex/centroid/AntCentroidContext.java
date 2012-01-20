package org.graphstream.algorithm.myrmex.centroid;

import org.graphstream.algorithm.myrmex.AntContext;

public class AntCentroidContext extends AntContext {

	public AntCentroidContext() {
		super();
		internalGraph.setStrict(false);
	}
	
	public AntCentroidParams getDefaultAntParams() {
		return new AntCentroidParams();
	}

}
