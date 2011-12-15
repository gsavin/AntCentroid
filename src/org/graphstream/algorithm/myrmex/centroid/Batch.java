package org.graphstream.algorithm.myrmex.centroid;

import static org.graphstream.algorithm.Parameter.parameter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.Centroid;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.IncompleteGridGenerator;
import org.graphstream.algorithm.generator.PreferentialAttachmentGenerator;
import org.graphstream.algorithm.myrmex.centroid.AntCentroidParams.MassMode;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSinkDGS;
import org.graphstream.stream.file.FileSourceDGS;

public class Batch {

	public static enum WhichGenerator {
		PREFERENTIAL_ATTACHMENT, DOROGOVTSEV_MENDES, GRID, INCOMPLETE_GRID;
		public String toString() {
			return name().replace("_", " ").toLowerCase();
		}
	}

	public static void genGraph(Graph g, WhichGenerator wg, Integer size) {
		boolean askGenerator = wg == null;
		Generator gen = null;

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
			size = (Integer) JOptionPane.showInputDialog(null,
					"Choose the size of graph generate with " + wg.toString(),
					"Size", JOptionPane.INFORMATION_MESSAGE, null, sizes, 20);

			if (size == null)
				System.exit(0);
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
		while (size-- > 0)
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

	public static Chunk run(WhichGenerator wg, int size) {
		Chunk stat = new Chunk();

		DefaultGraph g = new DefaultGraph("Web Generator Example");
		AntCentroidAlgorithm algo = new AntCentroidAlgorithm();

		APSP apsp = new APSP();
		Centroid algoOrg = new Centroid();

		long m1, m2;

		genGraph(g, wg, size);

		stat.gen = wg;
		stat.genSize = size;
		stat.graphNodeCount = g.getNodeCount();
		stat.graphEdgeCount = g.getEdgeCount();

		m1 = time();
		apsp.init(g);
		algoOrg.init(g);
		apsp.compute();
		algoOrg.compute();
		m2 = time();

		stat.baseAlgoRunTime = m2 - m1;
		stat.graphDiameter = 0;

		LinkedList<Node> baseCentroid = new LinkedList<Node>();

		FileSinkDGS dgs = new FileSinkDGS();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			dgs.writeAll(g, gzip);
			stat.graphDGS = out.toByteArray();// .toString();
		} catch (IOException e) {
			stat.graphDGS = new byte[0];
		}

		for (Node node : g.getEachNode()) {
			APSP.APSPInfo info = node
					.getAttribute(APSP.APSPInfo.ATTRIBUTE_NAME);

			stat.graphDiameter = Math.max(info.getMaximumLength(),
					stat.graphDiameter);

			if (node.hasAttribute("centroid")
					&& (Boolean) node.getAttribute("centroid"))
				baseCentroid.add(node);
		}

		stat.baseCentroidSize = baseCentroid.size();

		LinkedList<Node> antsCentroid = new LinkedList<Node>();

		for (MassMode massMode : MassMode.values()) {
			Result r = new Result();

			for (Node n : g.getEachNode())
				n.removeAttribute("ant.centroid");
			
			algo = new AntCentroidAlgorithm();
			algo.init(parameter("graph", g),
					parameter("ant.params.evaporation", 0.7f),
					parameter("ant.centroid.params.mass_mode", massMode));

			m1 = time();
			while (algo.getStabilization() < 0.99)
				algo.compute();
			m2 = time();

			algo.markCentroid("ant.centroid");

			r.antsAlgoRunTime = m2 - m1;

			antsCentroid.clear();

			for (Node node : g.getEachNode()) {
				if (node.hasAttribute("ant.centroid")
						&& (Boolean) node.getAttribute("ant.centroid"))
					antsCentroid.add(node);
			}

			r.antsCentroidSize = antsCentroid.size();
			r.mean = 0;
			r.distances = new double[antsCentroid.size()];

			for (int i = 0; i < antsCentroid.size(); i++) {
				if (baseCentroid.contains(antsCentroid.get(i)))
					r.distances[i] = 0;
				else {
					double min = Double.MAX_VALUE;
					APSP.APSPInfo info = antsCentroid.get(i).getAttribute(
							APSP.APSPInfo.ATTRIBUTE_NAME);

					for (int j = 0; j < baseCentroid.size(); j++) {
						min = Math.min(min,
								info.getLengthTo(baseCentroid.get(j).getId()));
					}

					r.distances[i] = min;
				}

				r.mean += r.distances[i];
			}

			r.mean /= r.distances.length;

			stat.results.put(massMode, r);
		}
		return stat;
	}

	public static enum Mode {
		BATCHER, STATS, TEST
	}

	public static void main(String... args) throws Exception {
		Mode mode = Mode.valueOf(args[0]);// BATCHER;

		switch (mode) {
		case BATCHER:
			WhichGenerator gen = WhichGenerator.valueOf(args[1]);
			int size = Integer.parseInt(args[2]);
			int ite = Integer.parseInt(args[3]);
			String data = args[4];

			batcher(gen, size, ite, data);
			break;
		case STATS:
			for (int i = 1; i < args.length; i++)
				countStats(args[i]);
			/*
			 * countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-preferential_attachment-50.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-preferential_attachment-100.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-preferential_attachment-500.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-preferential_attachment-1000.raw"
			 * );
			 * 
			 * countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-dorogovtsev_mendes-50.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-dorogovtsev_mendes-100.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-dorogovtsev_mendes-500.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-dorogovtsev_mendes-1000.raw"
			 * );
			 * 
			 * countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-grid-5.raw");
			 * countStats
			 * ("/home/raziel/thesis/papers/conf/ECCS11/data/data-grid-10.raw");
			 * countStats
			 * ("/home/raziel/thesis/papers/conf/ECCS11/data/data-grid-15.raw");
			 * 
			 * countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-incomplete_grid-5.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-incomplete_grid-10.raw"
			 * ); countStats(
			 * "/home/raziel/thesis/papers/conf/ECCS11/data/data-incomplete_grid-15.raw"
			 * );
			 */
			break;
		case TEST:
			test();
			break;
		}
	}

	public static LinkedList<Chunk> loadFile(File f)
			throws FileNotFoundException {
		FileChannel channel = null;
		RandomAccessFile dataFile = null;

		dataFile = new RandomAccessFile(f, "r");
		channel = dataFile.getChannel();

		return loadFile(channel);
	}

	public static LinkedList<Chunk> loadFile(FileChannel channel) {
		LinkedList<Chunk> stats = new LinkedList<Chunk>();

		try {
			ByteBuffer size = ByteBuffer.allocate(4);

			while (channel.position() < channel.size()) {
				size.clear();
				channel.read(size);

				ByteBuffer buffer = ByteBuffer.allocate(size.getInt(0));
				buffer.clear();
				buffer.putInt(size.getInt(0));
				channel.read(buffer);

				buffer.clear();

				Chunk t = importStat(buffer);
				stats.add(t);
			}
		} catch (Exception e) {
			System.out.printf("nothing to load.\n");
		}

		return stats;
	}

	public static void countStats(String data) {
		FileChannel channel = null;
		RandomAccessFile dataFile = null;

		LinkedList<Chunk> stats = new LinkedList<Chunk>();

		try {
			dataFile = new RandomAccessFile(data, "r");
			channel = dataFile.getChannel();

			ByteBuffer size = ByteBuffer.allocate(4);

			while (channel.position() < channel.size()) {
				size.clear();
				channel.read(size);

				ByteBuffer buffer = ByteBuffer.allocate(size.getInt(0));
				buffer.clear();
				buffer.putInt(size.getInt(0));
				channel.read(buffer);

				buffer.clear();

				Chunk t = importStat(buffer);
				stats.add(t);
			}
		} catch (Exception e) {
			System.out.printf("nothing to load.\n");
		}

		printStats(stats);
	}

	public static class Value {

		double mean;
		double deviation;

		public Value() {
			mean = 0;
			deviation = 0;
		}

		public void addValue(double d, double p) {
			mean += p * d;
			deviation += p * d * d;
		}

		public double mean() {
			return mean;
		}

		public double deviation() {
			return Math.sqrt(deviation - mean * mean);
		}
	}

	public static void printStats(LinkedList<Chunk> stats) {
		float nodes, edges;

		double p = 1.0 / stats.size();

		//

		EnumMap<MassMode, Value> centroidSizes = new EnumMap<MassMode, Value>(
				MassMode.class);
		EnumMap<MassMode, Value> gammas = new EnumMap<MassMode, Value>(
				MassMode.class);
		EnumMap<MassMode, Value> times = new EnumMap<MassMode, Value>(
				MassMode.class);
		EnumMap<MassMode, AtomicInteger> counts = new EnumMap<MassMode,AtomicInteger>(MassMode.class);

		for (MassMode mode : MassMode.values()) {
			centroidSizes.put(mode, new Value());
			gammas.put(mode, new Value());
			times.put(mode, new Value());
			counts.put(mode, new AtomicInteger(0));
		}

		nodes = edges = 0;

		HashSet<WhichGenerator> gens = new HashSet<WhichGenerator>();

		for (int i = 0; i < stats.size(); i++) {
			Chunk t = stats.get(i);
			gens.add(t.gen);
			nodes += t.graphNodeCount;
			edges += t.graphEdgeCount;

			for (MassMode mode : t.results.keySet()) {
				Result r = t.results.get(mode);
				centroidSizes.get(mode).addValue(
						r.antsCentroidSize - t.baseCentroidSize, p);

				times.get(mode).addValue(
						100 * (r.antsAlgoRunTime - t.baseAlgoRunTime)
								/ Math.max(1, t.baseAlgoRunTime), p);

				gammas.get(mode).addValue(r.mean / t.graphDiameter, p);
				counts.get(mode).incrementAndGet();
			}
		}

		nodes /= stats.size();
		edges /= stats.size();

		for (WhichGenerator g : gens)
			System.out.printf("%s ", g);
		System.out.printf(
				"(%d objects)\n--------------------------------------------\n",
				stats.size());
		System.out.printf(Locale.ROOT, "nodes................... : %.1f%n",
				nodes);
		System.out.printf(Locale.ROOT, "edges................... : %.1f%n",
				edges);
		for (MassMode mode : MassMode.values()) {
			Value centroidSize = centroidSizes.get(mode);
			Value gamma = gammas.get(mode);
			Value time = times.get(mode);

			System.out.printf("%s (%d)\n", mode.name(), counts.get(mode).get());

			System.out.printf(Locale.ROOT,
					"| delta time (average).. : %s%.2f%%%n",
					time.mean() > 0 ? "+" : "", time.mean());
			System.out.printf(Locale.ROOT,
					"| delta time (deviation) : %.2f%%%n", time.deviation());
			System.out.printf(Locale.ROOT,
					"| delta size (average).. : %s%.2f%n",
					centroidSize.mean() > 0 ? "+" : "", centroidSize.mean());
			System.out.printf(Locale.ROOT, "| delta size (deviation) : %.2f%n",
					centroidSize.deviation());
			System.out.printf(Locale.ROOT,
					"| gamma (average)....... : %3.1f%%%n", gamma.mean() * 100);
			System.out.printf(Locale.ROOT,
					"| gamma (deviation)..... : %3.1f%%%n",
					gamma.deviation() * 100);

		}

		System.out.printf("--------------------------------------------\n");
	}

	public static void batcher(WhichGenerator gen, int genSize, int ite,
			String data) {
		FileChannel channel = null;
		RandomAccessFile dataFile = null;

		LinkedList<Chunk> stats = new LinkedList<Chunk>();

		try {
			dataFile = new RandomAccessFile(data, "rws");
			channel = dataFile.getChannel();

			ByteBuffer size = ByteBuffer.allocate(4);

			while (channel.position() < channel.size()) {
				size.clear();
				channel.read(size);

				ByteBuffer buffer = ByteBuffer.allocate(size.getInt(0));
				buffer.clear();
				buffer.putInt(size.getInt(0));
				channel.read(buffer);

				buffer.clear();

				Chunk t = importStat(buffer);
				stats.add(t);
			}

			System.out.printf("%d objects read%n", stats.size());
		} catch (Exception e) {
			System.out.printf("nothing to load.\n");
		}

		for (int i = 0; i < ite; i++) {
			Chunk t = run(gen, genSize);
			stats.add(t);

			if (channel != null) {
				try {
					channel.write(exportStat(t));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		try {
			channel.close();
			dataFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.printf("%s %d done !%n", gen, genSize);
	}

	private static long time() {
		return System.currentTimeMillis();
	}

	public static class Result {
		long antsAlgoRunTime;
		int antsCentroidSize;
		double[] distances;
		double mean;

		public boolean equals(Object o) {
			if (o instanceof Result) {
				Result r = (Result) o;

				return r.antsAlgoRunTime == antsAlgoRunTime
						&& r.antsCentroidSize == antsCentroidSize
						&& r.mean == mean
						&& Arrays.equals(r.distances, distances);
			}

			return false;
		}
	}

	public static class Chunk {

		WhichGenerator gen;
		int genSize;
		long baseAlgoRunTime;
		double graphDiameter;
		int graphNodeCount;
		int graphEdgeCount;
		int baseCentroidSize;
		byte[] graphDGS;

		// long antsAlgoRunTime;
		// int antsCentroidSize;
		// double[] distances;
		// double mean;

		EnumMap<AntCentroidParams.MassMode, Result> results;

		public Chunk() {
			results = new EnumMap<AntCentroidParams.MassMode, Result>(
					AntCentroidParams.MassMode.class);
		}

		public boolean equals(Object o) {
			if (o instanceof Chunk) {
				Chunk s = (Chunk) o;
				return s.gen == gen && s.genSize == genSize
						&& s.baseAlgoRunTime == baseAlgoRunTime
						&& s.graphDiameter == graphDiameter
						&& s.graphNodeCount == graphNodeCount
						&& s.graphEdgeCount == graphEdgeCount
						&& s.baseCentroidSize == baseCentroidSize
						&& s.graphDGS.equals(graphDGS)
						&& s.results.equals(results);
			}

			return false;
		}

		public void debug(Chunk s) {
			if (s.gen != gen)
				System.err.printf("** gen ** %s != %s\n", gen, s.gen);
			if (s.genSize != genSize)
				System.err.printf("** genSize ** %d != %d\n", genSize,
						s.genSize);
			if (s.baseAlgoRunTime != baseAlgoRunTime)
				System.err.printf("** baseAlgoRunTime ** %d != %d\n",
						baseAlgoRunTime, s.baseAlgoRunTime);
			if (s.graphDiameter != graphDiameter)
				System.err.printf("** graphDiameter ** %f != %f\n",
						graphDiameter, s.graphDiameter);
			if (s.graphNodeCount != graphNodeCount)
				System.err.printf("** graphNodeCount ** %d != %d\n",
						graphNodeCount, s.graphNodeCount);
			if (s.graphEdgeCount != graphEdgeCount)
				System.err.printf("** graphEdgeCount ** %d != %d\n",
						graphEdgeCount, s.graphEdgeCount);
			if (s.baseCentroidSize != baseCentroidSize)
				System.err.printf("** baseCentroidSize ** %d != %d\n",
						baseCentroidSize, s.baseCentroidSize);
			if (!s.results.equals(results))
				System.err.printf("** results ** not equals\n");
			if (!s.graphDGS.equals(graphDGS)) {
				System.err.printf("** graphDGS **\n");
				byte[] dgs1 = graphDGS;
				byte[] dgs2 = s.graphDGS;

				if (dgs1.length != dgs2.length)
					System.err.printf("different lenght\n");
				else {
					int k, l;
					k = 0;
					l = 0;

					System.err.printf("%d; %d\n", dgs1.length, dgs2.length);

					while (k < dgs1.length) {
						do
							System.err.printf("%02X", dgs1[k++]);
						while (k < dgs1.length && (k % 25) != 0);

						System.err.printf("\n");

						do {
							System.err.printf("%02X", dgs2[l]);

							if (dgs2[l] != dgs1[l]) {
								System.err.printf(" < here\n");
								l = dgs1.length + 1;
								return;
							}

							l++;
						} while (l < dgs1.length && (l % 25) != 0);

						System.err.printf("\n");
					}

				}
			}
		}

		public String toString() {
			StringBuilder buffer = new StringBuilder();

			buffer.append("---------------------------------\n");
			buffer.append(gen.name()).append(" ").append(genSize).append("\n");
			buffer.append("---------------------------------\n");
			buffer.append("nodes.............: ").append(graphNodeCount)
					.append("\n");
			buffer.append("edges.............: ").append(graphEdgeCount)
					.append("\n");
			buffer.append("diameter..........: ").append(graphDiameter)
					.append("\n");
			buffer.append("base time.........: ").append(baseAlgoRunTime)
					.append("\n");
			buffer.append("base size.........: ").append(baseCentroidSize)
					.append("\n");

			for (MassMode mode : results.keySet()) {
				Result r = results.get(mode);
				float dt = 100 * (r.antsAlgoRunTime - baseAlgoRunTime)
						/ baseAlgoRunTime;
				int ds = r.antsCentroidSize - baseCentroidSize;

				buffer.append("mass mode.........: ").append(mode.name());
				buffer.append("| ants time.......: ").append(r.antsAlgoRunTime)
						.append("\n");
				buffer.append("delta time........: ").append(
						String.format("%s%.1f%%\n", dt > 0 ? "+" : "", dt));
				buffer.append("| ants size.......: ")
						.append(r.antsCentroidSize).append("\n");
				buffer.append("delta size........: ").append(
						String.format("%s%d%n", ds > 0 ? "+" : "", ds));
				buffer.append("| mean............: ").append(r.mean)
						.append("\n");
				buffer.append("| distances.......: {");
				for (int i = 0; i < r.distances.length; i++)
					buffer.append(r.distances[i]).append(
							i < r.distances.length - 1 ? "," : "");
				buffer.append("}\n");
			}

			/*
			 * buffer.append("dgs      .........: \n"); byte[] dgs = graphDGS;
			 * for (int i = 0; i < dgs.length; i++)
			 * buffer.append(String.format("%02X", dgs[i])).append( i % 70 == 0
			 * ? "\n" : ""); buffer.append("\n");
			 */

			return buffer.toString();
		}
	}

	public static ByteBuffer exportStat(Chunk t) {
		int bytes = 7 * Integer.SIZE + 2 * Long.SIZE + 2 * Double.SIZE;

		for (Result r : t.results.values())
			bytes += 2 * Integer.SIZE + Long.SIZE + (r.antsCentroidSize + 1)
					* Double.SIZE;

		bytes /= 8;

		byte[] dgs = t.graphDGS;
		bytes += dgs.length;

		ByteBuffer buffer = ByteBuffer.allocate(bytes);
		buffer.clear();

		buffer.putInt(bytes);

		buffer.putInt(t.gen.ordinal());
		buffer.putInt(t.genSize);
		buffer.putLong(t.baseAlgoRunTime);
		buffer.putDouble(t.graphDiameter);
		buffer.putInt(t.graphNodeCount);
		buffer.putInt(t.graphEdgeCount);
		buffer.putInt(t.baseCentroidSize);
		buffer.putInt(dgs.length);
		buffer.put(dgs);
		buffer.putInt(t.results.size());

		for (MassMode mode : t.results.keySet()) {
			Result r = t.results.get(mode);

			buffer.putInt(mode.ordinal());
			buffer.putLong(r.antsAlgoRunTime);
			buffer.putInt(r.antsCentroidSize);

			for (int i = 0; i < r.antsCentroidSize; i++)
				buffer.putDouble(r.distances[i]);
			buffer.putDouble(r.mean);
		}

		buffer.clear();
		return buffer;
	}

	public static Chunk importStat(ByteBuffer buffer) {
		Chunk t = new Chunk();

		buffer.getInt();

		t.gen = WhichGenerator.values()[buffer.getInt()];
		t.genSize = buffer.getInt();
		t.baseAlgoRunTime = buffer.getLong();
		t.graphDiameter = buffer.getDouble();
		t.graphNodeCount = buffer.getInt();
		t.graphEdgeCount = buffer.getInt();
		t.baseCentroidSize = buffer.getInt();

		int dgsSize = buffer.getInt();
		byte[] dgsBytes = new byte[dgsSize];

		buffer.get(dgsBytes);
		t.graphDGS = dgsBytes;

		int results = buffer.getInt();

		for (int k = 0; k < results; k++) {
			Result r = new Result();
			MassMode mode = MassMode.values()[buffer.getInt()];

			r.antsAlgoRunTime = buffer.getLong();
			r.antsCentroidSize = buffer.getInt();
			r.distances = new double[r.antsCentroidSize];
			for (int i = 0; i < r.antsCentroidSize; i++)
				r.distances[i] = buffer.getDouble();
			r.mean = buffer.getDouble();

			t.results.put(mode, r);
		}
		return t;
	}

	public static void plot(String out) throws IOException {
		File in = new File("/home/raziel/thesis/papers/conf/ECCS11/data.raw");
		RandomAccessFile raf = new RandomAccessFile(out, "rw");
		LinkedList<Chunk> stats = loadFile(in);

		for (int i = 0; i < stats.size(); i++) {
			Chunk t = stats.get(i);
			StringBuilder b = new StringBuilder();

			b.append(t.baseCentroidSize);
			for (int m = 0; m < MassMode.values().length; m++) {
				b.append("\t");
				if (t.results.containsKey(MassMode.values()[m]))
					b.append(t.results.get(MassMode.values()[m]).antsCentroidSize);
				else
					b.append(-1);
			}

			raf.writeBytes(b.toString());
		}

		raf.close();
	}

	public static void test() throws IOException {
		LinkedList<Chunk> stats = new LinkedList<Chunk>();
		Random r = new Random();
		File f = File.createTempFile("data_", ".raw");
		f.deleteOnExit();

		for (int i = 0; i < 10; i++) {
			FileChannel channel = null;
			RandomAccessFile dataFile = null;

			try {
				dataFile = new RandomAccessFile(f, "rws");
				channel = dataFile.getChannel();

				ByteBuffer size = ByteBuffer.allocate(4);

				while (channel.position() < channel.size()) {
					size.clear();
					channel.read(size);

					ByteBuffer buffer = ByteBuffer.allocate(size.getInt(0));
					buffer.clear();
					buffer.putInt(size.getInt(0));
					channel.read(buffer);

					buffer.clear();
					importStat(buffer);
				}
			} catch (Exception e) {
			}

			String abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-*/ {}()/\\.;,?!_-\"";

			for (int j = 0; j < 1000; j++) {
				Chunk st = new Chunk();

				st.baseAlgoRunTime = r.nextLong();
				st.baseCentroidSize = r.nextInt(10);
				st.gen = WhichGenerator.values()[r.nextInt(WhichGenerator
						.values().length)];
				st.genSize = r.nextInt();
				st.graphDiameter = r.nextDouble();
				st.graphNodeCount = r.nextInt();
				st.graphNodeCount = r.nextInt();

				int l = r.nextInt(256) + 64;
				StringBuilder sb = new StringBuilder();
				for (int m = 0; m < l; m++)
					sb.append(abc.charAt(r.nextInt(abc.length())));
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(out);
				gzip.write(sb.toString().getBytes());
				st.graphDGS = out.toByteArray();

				for (MassMode mode : MassMode.values()) {
					Result re = new Result();
					re.antsAlgoRunTime = r.nextLong();
					re.antsCentroidSize = r.nextInt(10);
					re.distances = new double[re.antsCentroidSize];
					for (int k = 0; k < re.antsCentroidSize; k++)
						re.distances[k] = r.nextDouble();
					re.mean = r.nextDouble();
					st.results.put(mode, re);
				}

				channel.write(exportStat(st));
				stats.add(st);
			}

			channel.close();
			dataFile.close();
		}

		LinkedList<Chunk> stats2 = loadFile(f);

		if (stats.size() != stats2.size()) {
			System.err.printf("error: different sizes%n");
			return;
		}

		for (int i = 0; i < stats.size(); i++) {
			if (!stats.get(i).equals(stats2.get(i))) {
				System.err.printf("found different entries at %d.%n", i);
				stats.get(i).debug(stats2.get(i));
				/*
				 * byte[] dgs1 = stats.get(i).graphDGS.getBytes(); byte[] dgs2 =
				 * stats2.get(i).graphDGS.getBytes();
				 * 
				 * if (dgs1.length != dgs2.length)
				 * System.err.printf("different lenght\n"); else { int k, l; k =
				 * 0; l = 0;
				 * 
				 * System.err.printf("%d; %d\n", dgs1.length, dgs2.length);
				 * 
				 * while (k < dgs1.length) { do System.err.printf("%02X",
				 * dgs1[k++]); while (k < dgs1.length && (k % 25) != 0);
				 * 
				 * System.err.printf("\n");
				 * 
				 * do { System.err.printf("%02X", dgs2[l]);
				 * 
				 * if (dgs2[l] != dgs1[l]) { System.err.printf(" < here\n"); l =
				 * dgs1.length + 1; return; }
				 * 
				 * l++; } while (l < dgs1.length && (l % 25) != 0);
				 * 
				 * System.err.printf("\n"); }
				 * 
				 * }
				 */
				return;
			}
		}

		System.out.printf("stats are equals.%n");
	}
}
