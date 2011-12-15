package org.graphstream.algorithm.myrmex.centroid;

import java.util.Locale;

import javax.swing.JOptionPane;

import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.Centroid;
import org.graphstream.algorithm.Parameter;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.IncompleteGridGenerator;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.myrmex.AntAlgorithm;
import org.graphstream.algorithm.myrmex.AntEdge;
import org.graphstream.algorithm.myrmex.AntGraph;
import org.graphstream.algorithm.myrmex.AntNode;
import org.graphstream.algorithm.myrmex.centroid.AntCentroidParams.MassMode;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.NodeFactory;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSourceDGS;

import static org.graphstream.algorithm.Parameter.parameter;

public class AntCentroidAlgorithm extends AntAlgorithm {

	protected float stabilization;

	public AntCentroidAlgorithm() {
		super();

		this.context.getInternalGraph().setNodeFactory(
				new NodeFactory<AntCentroidNode>() {
					public AntCentroidNode newInstance(String id, Graph graph) {
						return new AntCentroidNode(context, (AntGraph) graph, id);
					}
				});

		stabilization = 0;
	}

	public AntCentroidContext getDefaultContext() {
		return new AntCentroidContext();
	}

	public void compute() {
		super.compute();

		float s = 0;

		for (AntNode node : context.eachNode()) {
			AntCentroidNode acn = (AntCentroidNode) node;
			s += acn.getStabilization();
		}

		stabilization = 0.4f * (s / (float) context.getNodeCount()) + 0.6f
				* stabilization;

		displayResults();
	}

	public float getStabilization() {
		return stabilization;
	}

	public void markCentroid(String attribute) {
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		AntCentroidParams params = context.getAntParams();

		for (AntNode node : context.eachNode()) {
			AntCentroidNode acn = (AntCentroidNode) node;
			float p = acn.getMass().getMass();

			if (params.useLogarithmicMass())
				p = p == 0 ? 0 : (float) Math.log(p);
			;

			min = Math.min(min, p);
			max = Math.max(max, p);
		}

		if (max == min)
			max += 0.1f;

		for (AntNode node : context.eachNode()) {
			AntCentroidNode acn = (AntCentroidNode) node;
			Node n = registeredGraph.getNode(node.getId());
			float m = acn.getMass().getMass();

			if (params.useLogarithmicMass())
				m = m == 0 ? 0 : (float) Math.log(m);

			n.setAttribute(attribute, m >= max - params.getEpsilon());
		}
	}

	public void displayResults() {
		context.lock();

		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		AntCentroidParams params = context.getAntParams();

		for (AntNode node : context.eachNode()) {
			AntCentroidNode acn = (AntCentroidNode) node;
			float p = acn.getMass().getMass();

			if (params.useLogarithmicMass())
				p = p == 0 ? 0 : (float) Math.log(p);
			;

			min = Math.min(min, p);
			max = Math.max(max, p);

			node.setAttribute("centroid.pheromone", p);
		}

		if (max == min)
			max += 0.1f;

		for (AntNode node : context.eachNode()) {
			AntCentroidNode acn = (AntCentroidNode) node;
			Node n = registeredGraph.getNode(node.getId());
			float m = acn.getMass().getMass();

			if (params.useLogarithmicMass())
				m = m == 0 ? 0 : (float) Math.log(m);

			boolean baseCentroid = n.hasAttribute("truecentroid");
			boolean centroid = (m >= max - params.getEpsilon());

			String uiClass = "";

			if (baseCentroid && centroid) {
				uiClass = "centroid";
			} else if (baseCentroid) {
				uiClass = "baseCentroid";
			} else if (centroid) {
				uiClass = "antCentroid";
			}

			n.setAttribute("ui.class", uiClass);

			n.setAttribute("label",
					String.format("%.3f", acn.getEdgesTotalPheromoneLoad()));
			// n.setAttribute("label", String.format("[ %.1f ]", m));
			m = (m - min) / (max - min);

			n.setAttribute("ui.size",
					String.format(Locale.ROOT, "%fgu", m * 0.7 + 0.2));
			n.setAttribute("ui.color", m);
		}

		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;

		for (AntEdge edge : context.eachEdge()) {
			float p = edge.getPheromones().getTotalLoad();

			min = Math.min(min, p);
			max = Math.max(max, p);
		}

		for (AntEdge edge : context.eachEdge()) {
			Edge e = registeredGraph.getEdge(edge.getId());
			float p = edge.getPheromones().getTotalLoad();

			p = (p - min) / (max - min);

			e.setAttribute("label", String.format("( %.3f )", p));
			e.setAttribute("ui.size",
					String.format(Locale.ROOT, "%fgu", p * 0.3));
		}

		context.unlock();
	}

	public void init(Parameter... params) {
		super.init(params);
		context.addColony("r1");
		// context.addColony("r2");
	}

	public static enum WhichGenerator {
		PREFERENTIAL_ATTACHMENT(true, 100), DOROGOVTSEV_MENDES(true, 100), GRID(
				false, 10), INCOMPLETE_GRID(false, 10);
		boolean layout;
		int size;

		WhichGenerator(boolean layout, int size) {
			this.layout = layout;
			this.size = size;
		}

		public String toString() {
			return name().replace("_", " ").toLowerCase();
		}
	}

	public static void genGraph(Graph g, WhichGenerator wg) {
		boolean askGenerator = wg == null;
		Generator gen = null;

		int size = wg.size;

		if (askGenerator) {
			wg = WhichGenerator.PREFERENTIAL_ATTACHMENT;
			wg = (WhichGenerator) JOptionPane.showInputDialog(null,
					"Choose your generator", "Generator",
					JOptionPane.INFORMATION_MESSAGE, null,
					WhichGenerator.values(),
					WhichGenerator.PREFERENTIAL_ATTACHMENT);

			if (wg == null)
				System.exit(0);

			Integer[] sizes = { 5, 10, 15, 20, 50, 100, 200, 500 };
			Integer sObject = (Integer) JOptionPane.showInputDialog(null,
					"Choose the size of graph generate with " + wg.toString(),
					"Size", JOptionPane.INFORMATION_MESSAGE, null, sizes, 20);

			if (sObject == null)
				System.exit(0);

			size = sObject;
		}

		switch (wg) {
		case PREFERENTIAL_ATTACHMENT:
			gen = new BarabasiAlbertGenerator();
			break;
		case DOROGOVTSEV_MENDES:
			gen = new DorogovtsevMendesGenerator();
			break;
		case GRID:
			gen = new GridGenerator();
			break;
		case INCOMPLETE_GRID:
			gen = new IncompleteGridGenerator();
			break;
		}

		gen.addSink(g);

		gen.begin();
		for (int s = 0; s < size; s++)
			gen.nextEvents();
		gen.end();

		gen.removeSink(g);
	}

	public static void readGraph(Graph g, String filename) {
		FileSourceDGS gen = new FileSourceDGS();
		gen.addSink(g);

		try {
			gen.readAll(filename);
		} catch (Exception e) {
			e.printStackTrace();
		}

		gen.removeSink(g);
	}

	public static void main(String... args) throws Exception {
		/*
		if(args==null || args.length < 2) {
			System.err.printf("Usage: java %s GEN MODE\n", AntCentroidAlgorithm.class.getName());
			System.err.printf("With GEN in :\n");
			for(WhichGenerator g : WhichGenerator.values())
				System.err.printf("  - %s\n", g.name());
			System.err.printf("and MODE in :\n");
			for(MassMode m : MassMode.values())
				System.err.printf("  - %s\n", m.name());
			System.exit(1);
		}
		*/
		
		System.setProperty("gs.ui.renderer",
				"org.graphstream.ui.j2dviewer.J2DGraphRenderer");

		DefaultGraph g = new DefaultGraph("Web Generator Example");
		AntCentroidAlgorithm algo = new AntCentroidAlgorithm();

		APSP apsp = new APSP();
		Centroid algoOrg = new Centroid();
		WhichGenerator wg = WhichGenerator.DOROGOVTSEV_MENDES;//WhichGenerator.valueOf(args[0]);// 
		MassMode mode = MassMode.PASSAGES;//MassMode.valueOf(args[1]);

		g.addAttribute(
				"ui.stylesheet",
				"graph { padding: 50px; fill-color: white; } "
						+ "node {text-padding:3px;text-background-mode:rounded-box;text-background-color:rgba(220,220,220,150);size-mode: dyn-size;fill-mode:dyn-plain;fill-color:black,red; text-style:bold;text-size:12px;text-alignment:center;stroke-color:black;stroke-mode:plain;stroke-width:2px; } "
						+ "node .baseCentroid {shape:box; fill-color: blue;} "
						+ "node .antCentroid { fill-color:yellow; } "
						+ "node .centroid { fill-color: green; shape: triangle; } "
						+ "edge {text-visibility-mode:under-zoom;text-visibility:0.4;size-mode: dyn-size; fill-mode: dyn-plain; fill-color: rgba(255,0,0,100), rgba(0,255,0,100); }");
		g.addAttribute("ui.quality");
		g.addAttribute("ui.antialias");
		g.display(wg.layout);

		genGraph(g, wg);
		// readGraph(g, "/home/raziel/thesis/papers/conf/ECCS11/pref50.dgs");

		algo.init(parameter("graph", g),
				parameter("ant.params.evaporation", 0.7f),
				parameter("ant.centroid.params.mass_mode", mode));

		System.out.printf(" * start\n");

		apsp.init(g);
		algoOrg.init(g);
		apsp.compute();
		System.out.printf("* APSP computed%n");
		algoOrg.compute();
		System.out.printf("* Centroid computed%n");

		for (Node node : g.getEachNode()) {
			boolean isCentroid = node.hasAttribute("centroid")
					&& (Boolean) node.getAttribute("centroid");

			if (isCentroid) {
				node.setAttribute("truecentroid");
				node.setAttribute("ui.class", "truecentroid");
				System.out.printf("\"%s\" is in centroid.%n", node.getId());
			}
		}

		boolean bool = true;

		while (bool) {// algo.getStabilization() < 0.99)
			algo.compute();
			Thread.sleep(500);
		}

		System.out.printf(" * ants centroid computed\n");

		algo.markCentroid("ant.centroid");

		int b, a;
		b = a = 0;

		for (Node node : g.getEachNode()) {
			boolean baseCentroid = false;
			boolean antsCentroid = false;

			if (node.hasAttribute("centroid")
					&& (Boolean) node.getAttribute("centroid")) {
				baseCentroid = true;
				b++;
			}

			if (node.hasAttribute("ant.centroid")
					&& (Boolean) node.getAttribute("ant.centroid")) {
				antsCentroid = true;
				a++;
			}

			String uiClass = "";

			if (baseCentroid && antsCentroid) {
				uiClass = "centroid";
			} else if (baseCentroid) {
				uiClass = "baseCentroid";
			} else if (antsCentroid) {
				uiClass = "antCentroid";
			}

			node.setAttribute("ui.class", uiClass);
		}

		System.out.printf(
				"base centroid : %d elements%nants centroid : %d elements%n",
				b, a);
	}
}
