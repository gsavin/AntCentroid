package org.graphstream.algorithm.myrmex.centroid;

import org.graphstream.algorithm.myrmex.Pheromone;

public class CentromyrmexPheromone extends Pheromone<Float> {
	public CentromyrmexPheromone(int marker, float load) {
		super(marker, load, null);
	}

	public CentromyrmexPheromone(int marker, float load, Float data) {
		super(marker, load, data);
	}

	protected void fusionData(Pheromone<?> pheromoneUC) {
		if (pheromoneUC instanceof CentromyrmexPheromone) {
			CentromyrmexPheromone pheromone = (CentromyrmexPheromone) pheromoneUC;

			Float data = pheromone.getData();

			if (data == null)
				return;

			if (this.data == null)
				this.data = data;
			else
				this.data += data;
		}
	}

	public Pheromone<Float> clone() {
		return new CentromyrmexPheromone(this.marker, this.load, this.data);
	}
}
