package org.graphstream.algorithm.myrmex.centroid;

import java.util.LinkedList;

import org.graphstream.algorithm.myrmex.Ant;
import org.graphstream.algorithm.myrmex.AntContext;
import org.graphstream.algorithm.myrmex.AntEdge;
import org.graphstream.algorithm.myrmex.AntNode;
import org.graphstream.algorithm.myrmex.Colony;
import org.graphstream.algorithm.myrmex.Pheromone;
import org.graphstream.algorithm.myrmex.centroid.AntCentroidParams.MassMode;

public class Centromyrmex extends Ant {

	// protected float stepWithoutMembrane;
	protected int stepWithoutJump;
	protected CentromyrmexPheromone pheromone;
	protected Mass mass;
	protected int maxDegree;
	protected int maxMass;
	protected float maxLoad;
	protected LinkedList<String> tabu;

	/**
	 * Perceived pheromone array. This should be allocated at each call to
	 * step(), but to avoid such an overhead, an over-sized array is created and
	 * only re-allocated when it is too small.
	 */
	protected float P[] = new float[30];

	public Centromyrmex(String id, Colony colony, AntNode start, AntContext ctx) {
		super(id, colony, start, ctx);
		pheromone = new CentromyrmexPheromone(colony.getIndex(), 0.03f);
		stepWithoutJump = 0;
		mass = new Mass(0);
		maxDegree = 0;
		maxLoad = 0;
		maxMass = 0;
		tabu = new LinkedList<String>();
	}

	private float gamma(AntEdge e, AntCentroidNode n, float g, float i) {
		return 1 - g + g * i;
	}

	public void step() {
		int nEdges = curNode.getDegree();
		float totalPerceived = 0;
		AntCentroidParams params = ctx.getAntParams();
		AntEdge next = null;

		maxMass *= 0.9f;
		maxLoad *= 0.99f;

		if (P.length <= nEdges)
			P = new float[nEdges];

		if (nEdges <= 0) {
			jumpRandomly();
		} else {
			float maxDegree = 0, minDegree = Float.MAX_VALUE;
			float maxMass = 0, minMass = Float.MIN_VALUE;

			for (int i = 0; i < nEdges; ++i) {
				AntEdge edge = curNode.getEdge(i);
				AntCentroidNode node = (AntCentroidNode) edge
						.getOpposite(curNode);

				minDegree = Math.min(minDegree, node.getDegree());
				maxDegree = Math.max(maxDegree, node.getDegree());

				minMass = Math.min(minMass, node.getMass().getMass());
				maxMass = Math.max(maxMass, node.getMass().getMass());
			}
			for (int i = 0; i < nEdges; ++i) {
				AntEdge edge = curNode.getEdge(i);
				AntCentroidNode node = (AntCentroidNode) edge
						.getOpposite(curNode);

				this.maxMass = Math.max(this.maxMass, node.getMass().getMass());

				if (tabu.contains(node.getId())) {
					P[i] = 0;
				} else if (node.getDegree() == 1) {
					// && node.getMass().getMass() < params.getMinMassStolen())
					// {
					P[i] = 0;
				} else {
					float d = minDegree == maxDegree ? 1 : (node.getDegree() - minDegree)
							/ (maxDegree - minDegree);
					float m = minMass == maxMass ? 1 : (node.getMass().getMass() - minMass)
							/ (maxMass - minMass);
					/*
					P[i] = (float) Math.pow(
							edge.getPheromones().getTotalLoad(), params.alpha)
							* gamma(edge, node, 0.2f, m)
							* gamma(edge, node, 0.5f, d);
					*/
					P[i] = (float) Math.pow(
							node.getEdgesTotalPheromoneLoad(), params.alpha)
							* gamma(edge, node, 0.5f, m)
							* gamma(edge, node, 0.5f, d);
				}

				totalPerceived += P[i];
			}

			for (int i = 0; i < nEdges; i++) {
				P[i] /= totalPerceived;

				if (mass.getMass() == 0)
					P[i] = 1 - P[i];
			}

			if (totalPerceived > 0)
				next = chooseEdge(P, nEdges);
			else
				next = curNode.getEdge(ctx.random().nextInt(nEdges));

			cross(next, true);
		}
	}

	public Pheromone<?> getPheromone() {
		return pheromone;
	}

	public void goTo(AntNode newNode) {
		super.goTo(newNode);

		
		
		if (tabu != null) {
			if (tabu.size() > 3)
				tabu.poll();

			tabu.addLast(newNode.getId());
		}

		AntCentroidParams params = ctx.getAntParams();

		maxDegree = Math.max(maxDegree, newNode.getDegree());

		float totalPheromones = 0;

		for (AntEdge edge : curNode.eachEdge())
			totalPheromones += edge.getPheromones().getTotalLoad();

		maxLoad = Math.max(maxLoad, totalPheromones);

		if (mass != null && params.massMode == MassMode.MASS) {
			Mass nodeMass = ((AntCentroidNode) curNode).getMass();

			if (mass.getMass() > 0) {
				int m = (int) (mass.getMass() * 0.9 * totalPheromones / maxLoad);
				m = Math.min(m + 1, mass.getMass());
				// System.out.printf("%d\n", mass.getMass());
				// mass.getMass() / 2 + (mass.getMass() % 2 > 0 ? 1 : 0);

				mass.transferTo(m, nodeMass);
			} else if (maxLoad == 0
					|| (1 - totalPheromones / maxLoad) < ctx.random()
							.nextFloat()) {
				int m = ctx.random().nextInt(
						params.getMaxMassStolen() - params.getMinMassStolen())
						+ params.getMinMassStolen();

				nodeMass.transferTo(m, mass);
			}
		}
	}

	protected AntEdge chooseEdge(float P[], int nEdges) {
		float r = ctx.random().nextFloat();
		float s = 0;

		for (int i = 0; i < nEdges; ++i) {
			s += P[i];

			if (s >= r)
				return (AntEdge) curNode.getEdge(i);
		}

		return (AntEdge) curNode.getEdge(nEdges - 1);
	}
}
