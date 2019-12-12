package network;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * As it is clear from the name, this class contains methods to return local maximum edges or nodes in a given weighted graph.
 *
 *<ul>
 *<li>v1.1.27012019: fixed a bug where node with out weight zero is detected as local maximum if no other 
 *neighbours has higher out-weight</li>
 *</ul>
 */
public class LocalMaximumFinder {
	
	/**
	 * returns edges with maximum weights locally
	 * @param graph weighted graph
	 * @return HashMap where key is a string with format "node1,node2", and value is the edge weight
	 */
	public static HashMap<String, Double> findLocalMaximumEdges(Graph<String> graph){
		HashMap<String, Double> maximumEdges = new HashMap<String, Double>();
		ArrayList<String> nodes = graph.getAllNodes();
		for(String a:nodes) {
			for(String b:graph.getSuccessors(a)) {
				//edge a-b
				boolean localMaxima=true;
				double weight = graph.getWeight(a, b);
				//find all edges surrounding [a-b]
				for(String n_b:graph.getSuccessors(b)) {
					if(compare(weight,graph.getWeight(b, n_b),10)<0 /*weight<graph.getWeight(b, n_b)*/) {
						localMaxima=false;
						break;
					}
				}
				for(String n_a:graph.getSuccessors(a)) {
					if(compare(weight,graph.getWeight(a, n_a),10)<0 /*weight<graph.getWeight(a, n_a)*/) {
						localMaxima=false;
						break;
					}
				}
				if(localMaxima && !maximumEdges.keySet().contains(b+","+a))maximumEdges.put(a+","+b, weight);
			}
		}
		return maximumEdges;
	}
	
	/**
	 * returns nodes with maximum out-weights locally
	 * @param graph weighted graph
	 * @return
	 */
	public static HashMap<String, Double> findLocalMaximumNodes(Graph<String> graph){
		HashMap<String, Double> maximumNodes = new HashMap<String, Double>();
		ArrayList<String> nodes = graph.getAllNodes();
		for(String a:nodes) {
			boolean localMaxima=true;
			double weight = graph.getOutWeight(a)/**graph.getSuccessors(a).size()*/;
			if(weight==0) {
				localMaxima=false;
			}else {
				for(String b:graph.getSuccessors(a)) {
					if(weight<graph.getOutWeight(b))/**graph.getSuccessors(b).size())*/ {
						localMaxima=false;
						break;
					}
				}
			}
			if(localMaxima)maximumNodes.put(a, weight);
		}
		return maximumNodes;
	}

	/**
	 * compares two doubles after trimming floating points > 'floatingPoints'
	 * @param a
	 * @param b
	 * @param floatingPoints
	 * @return >0 if a>b, 0 if a=b, <0 otherwise
	 */
	public static int compare(double a, double b, int floatingPoints) {
		double factor=Math.pow(10, floatingPoints);
		double o1 = Math.floor(a*factor)/factor;
		double o2 = Math.floor(b*factor)/factor;
		
		if(o1>o2)return 1;
		else if(o1<o2)return -1;
		else return 0;
	}

}
