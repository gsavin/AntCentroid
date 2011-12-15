package org.graphstream.algorithm.myrmex.centroid;

import java.util.HashSet;

import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class Similarity {

	static class Entry {
		int graph;
		Element e;
		Object label;

		public Entry(int graph, Element e, Object label) {
			this.graph = graph;
			this.e = e;
			this.label = label;
		}
	}

	static final Object NULL = new Object();

	public Similarity(Graph g, String attr1, String attr2) {
		HashSet<Entry> entries1 = new HashSet<Entry>();
		HashSet<Entry> entries2 = new HashSet<Entry>();

		for (Node n : g.getEachNode()) {
			Object label1 = n.getAttribute(attr1);
			Object label2 = n.getAttribute(attr2);

			if (label1 == null)
				label1 = NULL;

			if (label2 == null)
				label2 = NULL;

			entries1.add(new Entry(1, n, label1));
			entries2.add(new Entry(2, n, label2));
		}

		HashSet<Entry> entries1N2 = new HashSet<Entry>();

		for (Entry e1 : entries1) {
			for(Entry e2: entries2) {
				if(e1.e == e2.e && e1.label.equals(e2.label)) {
					entries1N2.add(e1);
					entries1N2.add(e2);
				}
			}
		}
	}
}
