package org.graphstream.algorithm.myrmex.centroid;

import java.util.ArrayList;

import org.graphstream.algorithm.generator.BaseGenerator;
import org.graphstream.graph.implementations.DefaultGraph;

public class WebGenerator extends BaseGenerator {

	protected int nodeByLayer = 5;
	protected ArrayList<String> currentLayer;
	protected int currentLayerIndex;

	public void begin() {
		currentLayer = new ArrayList<String>(nodeByLayer);
		currentLayerIndex = 0;
		addNode("root");

		for (int i = 0; i < nodeByLayer; i++)
			currentLayer.add("root");
	}

	public boolean nextEvents() {
		String edgeId;
		for (int i = 0; i < nodeByLayer; i++) {
			String nodeId = String.format("layer%x_node%x", currentLayerIndex,
					i);
			edgeId = currentLayer.get(i) + "__to__" + nodeId;

			addNode(nodeId);
			addEdge(edgeId, currentLayer.get(i), nodeId);
			sendEdgeAttributeAdded("web", edgeId, "layout.weight", 0.5f);

			currentLayer.set(i, nodeId);

			if (i > 0) {
				edgeId = nodeId + "__to__" + currentLayer.get(i - 1);
				addEdge(edgeId, nodeId, currentLayer.get(i - 1));
				sendEdgeAttributeAdded("web", edgeId, "layout.weight", (float) currentLayerIndex + 1);
			}
		}

		edgeId = currentLayer.get(0) + "__to__"
				+ currentLayer.get(currentLayer.size() - 1);
		addEdge(edgeId, currentLayer.get(0),
				currentLayer.get(currentLayer.size() - 1));
		//sendEdgeAttributeAdded("web", edgeId, "layout.weight", 1f);

		currentLayerIndex++;

		return true;
	}

	public void end() {
		for(int i=0;i<currentLayer.size();i++)
			sendNodeAttributeAdded("web",currentLayer.get(i),"meta.membrane",true);
	}

	public static void main(String... args) throws Exception {
		WebGenerator gen = new WebGenerator();
		DefaultGraph g = new DefaultGraph("Web Generator Example");
		int size = 10;
		gen.addSink(g);

		g.addAttribute(
				"ui.stylesheet",
				"graph { padding: 50px; fill-color: black; } node { fill-mode: dyn-plain; fill-color: blue,yellow,red; } edge { fill-color: white; }");
		g.addAttribute("ui.quality");
		g.addAttribute("ui.antialias");
		g.display(true);

		gen.begin();
		while (size-->0) {
			gen.nextEvents();
			Thread.sleep(2000);
		}
		// gen.end();
	}

}
