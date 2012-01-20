package org.graphstream.algorithm.myrmex.centroid;

import java.util.HashMap;

import org.graphstream.graph.implementations.AdjacencyListGraph;
import org.graphstream.organic.Organization;
import org.graphstream.organic.OrganizationListener;
import org.graphstream.organic.OrganizationManager;
import org.graphstream.organic.OrganizationManagerFactory;
import org.graphstream.organic.OrganizationsGraph;
import org.graphstream.organic.Validation;
import org.graphstream.organic.ui.OrganizationsView;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.stream.file.FileSourceDGS;

import static org.graphstream.algorithm.Parameter.parameter;

public class OrganizationWithCentroid implements OrganizationListener {
	OrganizationManager manager;
	HashMap<Object, AntCentroidAlgorithm> algorithms;
	AntCentroidParams params;

	public OrganizationWithCentroid(OrganizationManager manager) {
		this.manager = manager;
		algorithms = new HashMap<Object, AntCentroidAlgorithm>();
		params = new AntCentroidParams();
	}

	public void step() {
		for (AntCentroidAlgorithm algo : algorithms.values())
			algo.compute();
	}

	public void organizationCreated(Object metaIndex,
			Object metaOrganizationIndex, String rootNodeId) {
		Organization org = manager.getOrganization(metaOrganizationIndex);

		AntCentroidAlgorithm algo = new AntCentroidAlgorithm();

		algo.getContext().setAntParams(params);
		algo.init(parameter("graph", org));

		algorithms.put(metaOrganizationIndex, algo);
	}

	public void organizationRemoved(Object metaIndex,
			Object metaOrganizationIndex) {
		AntCentroidAlgorithm algo = algorithms.remove(metaOrganizationIndex);
		algo.terminate();
	}

	public void connectionCreated(Object metaIndex1,
			Object metaOrganizationIndex1, Object metaIndex2,
			Object metaOrganizationIndex2, String connection) {
	}

	public void connectionRemoved(Object metaIndex1,
			Object metaOrganizationIndex1, Object metaIndex2,
			Object metaOrganizationIndex2, String connection) {
	}

	public void organizationChanged(Object metaIndex,
			Object metaOrganizationIndex, ChangeType changeType,
			ElementType elementType, String elementId) {
	}

	public void organizationMerged(Object metaIndex,
			Object metaOrganizationIndex1, Object metaOrganizationIndex2,
			String rootNodeId) {
	}

	public void organizationRootNodeUpdated(Object metaIndex,
			Object metaOrganizationIndex, String rootNodeId) {
	}

	public void organizationSplited(Object metaIndex,
			Object metaOrganizationBase, Object metaOrganizationChild) {
	}

	public static void main(String... args) throws Exception {
		System.setProperty(OrganizationManagerFactory.PROPERTY,
				"plugins.replay.ReplayOrganizationManager");
		System.setProperty(Validation.PROPERTY, "none");

		String what = "replayable.dgs";

		FileSourceDGS dgs = new FileSourceDGS();
		FileSinkDGS out = new FileSinkDGS();
		AdjacencyListGraph g = new AdjacencyListGraph("g");
		OrganizationsGraph metaGraph = new OrganizationsGraph(g);
		OrganizationWithCentroid algos = new OrganizationWithCentroid(metaGraph
				.getManager());
		metaGraph.getManager().addOrganizationListener(algos);

		// g.addSink(new VerboseSink(System.err));
		dgs.addSink(g);
		g.addSink(out);

		out.begin(what + "_centroid.dgs");
		dgs.begin(what);

		int step = 0;

		while (dgs.nextStep()) {
			for (int i = 0; i < 15; i++)
				algos.step();
			System.out.printf("Step #%d\n", step++);
		}

		dgs.end();
		out.end();
	}
}
