package network;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import iotools.CustomLogger;

/**
 * computes neighbourhood overlap on the edges of the given graph, gives the output as a weighted graph or directly alter the graph
 * based on the results.
 *
 */
public class NeighborhoodOverlap {
	
	public static boolean calculateDeepWeight = false;
	
	public static CustomLogger logger = new CustomLogger("NeighborhoodOverlap", Level.FINER);
	
	/**test program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String graphPath = "";
		String weightsPath = "";
		String outWeightsPath = "";
		Graph<String> graph = Graph.loadFromFile(graphPath, false);
		HashMap<String, Double> weights = calculate(graph);
		//write results
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(weightsPath));
			writer.write("Source\tTarget\tweight\n");
			for(String edge:weights.keySet()) {
				writer.write(edge.split(",")[0]+"\t"+edge.split(",")[1]+"\t"+weights.get(edge).floatValue()+"\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outWeightsPath));
			ArrayList<String> nodes = graph.getAllNodes();
			writer.write("Id\toutWeight\n");
			for(String node:nodes) {
				writer.write(node+"\t"+graph.getOutWeight(node)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static HashMap<String, Double> calculate(Graph<String> graph){
		return calculate(graph, false);
	}
	/**
	 * 
	 * @param graph
	 * @param useWeights
	 * @return
	 */
	public static HashMap<String, Double> calculate(Graph<String> graph, boolean useWeights) {
		ArrayList<String> nodes = graph.getAllNodes();
		HashMap<String, Double> weights = new HashMap<>();
		
		for(String a:nodes) {
			for(String b:graph.getSuccessors(a)) {
				//calculate the overlap between a and b
				double overlap = 0;
				overlap =calculateOverlap(graph, a, b)*graph.getWeight(a, b);
				logger.log(Level.FINEST, "weight: "+a+","+b+" = "+overlap+"\n");//out
				weights.put(a+","+b, overlap);
			}
		}
		graph.setWeights(weights);
		
		if(calculateDeepWeight) {
			HashMap<String, Double> deepWeights = new HashMap<>();
			for(String a:nodes) {
				for(String b:graph.getSuccessors(a)) {
					//calculate deep weight equal to: oldWeight*(outWeight1*outWeight2)
					double deepWeight = graph.getWeight(a, b)*Math.sqrt(graph.getOutWeight(a)*graph.getOutWeight(b));
					logger.log(Level.FINEST, "deep weight: "+a+","+b+" = "+deepWeight+"\t<-> oldWeight("+graph.getWeight(a, b)+
							")*outWeight1("+graph.getOutWeight(a)+")*outWeight2("+graph.getOutWeight(b)+")\n");
					deepWeights.put(a+","+b, deepWeight);
				}
			}
			graph.setWeights(deepWeights);
			weights = deepWeights;
		}
		
		return weights;
	}

	
	private static double calculateOverlap(Graph<String> graph, String a, String b) {
		
		double sumUnion = 0;
		double sumInter = 0;
		double overlap = 0.0;
		
		ArrayList<String> sa = graph.getSuccessors(a);
		ArrayList<String> sb = graph.getSuccessors(b);
		@SuppressWarnings("unchecked")
		ArrayList<String> intersection = (ArrayList<String>)sa.clone();
		intersection.retainAll(sb);
		for(String s:intersection) {
			double wa = graph.getWeight(a, s);
			double wb = graph.getWeight(b, s);
			sumInter+=wa+wb;
		}
		for(String s:sa) {
			sumUnion+=graph.getWeight(a, s);
		}
		for(String s:sb) {
			sumUnion+=graph.getWeight(b, s);
		}
		
		overlap = (sumInter+1)/(sumUnion-(2*graph.getWeight(a, b))+1);
		
		/*BigDecimal bd = new BigDecimal(overlap);
		bd = bd.setScale(4, java.math.RoundingMode.HALF_UP);
		overlap = bd.doubleValue();*/
		
		logger.log(Level.FINEST, "weight: "+a+","+b+" = "+overlap+"\n");
		return overlap;
	}
}
