package network;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * Encapsulates a graph, constituted of nodes and edges. It can be loaded and accessed but cannot be manipulated.
 */
public class Graph<T> implements Cloneable{

	protected HashMap<T, ArrayList<T>> graph = new HashMap<>();
	
	protected HashMap<String, Double> weights = new HashMap<>();
	protected HashMap<T, Double> outWeights = new HashMap<>();
	
	protected boolean directed;
	public String sourceFile = null;
	
	/**
	 * when -1 then it is not calculated, otherwise this value is ready to be returned
	 */
	protected int numberOfEdges = -1;
	
	public Graph(){
		
	}
	
	public Graph(HashMap<T, ArrayList<T>> graph){
		this.graph=graph;
	}
	
	public Graph(Graph<T> graph){
		this(graph.graph);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Graph<T> newGraph = new Graph<>(new HashMap<>());
		for(T node:this.graph.keySet()) {
			newGraph.graph.put(node, (ArrayList<T>)this.graph.get(node).clone());
		}
		newGraph.sourceFile = sourceFile+"";
		newGraph.weights = weights;
		newGraph.outWeights = (HashMap<T, Double>)outWeights.clone();
		return newGraph;
	}
	
	/**
	 * returns all nodes that have successors
	 * @return
	 */
	public Set<T> getNodes(){
		return graph.keySet();
	}
	
	/**
	 * @return all nodes in the graph
	 */
	public ArrayList<T> getAllNodes() {
		ArrayList<T> result = new ArrayList<>();
		for(T node:graph.keySet()) {
			if(!result.contains(node))result.add(node);
			ArrayList<T> successors = graph.get(node);
			for(T s:successors) {
				if(!result.contains(s))result.add(s);
			}
		}
		return result;
	}
	
	public ArrayList<T> getSuccessors(T id){
		return getSuccessors(id, true);
	}
	
	/**
	 * Returns all the nodes where there is an edge pointing to them originated from the node with id.
	 * @param id
	 * @param copy if true, returns a clone of successors list for safe use, in case of false, hidden nodes are not returned
	 * @return ArrayList<T> containing ids of successors
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<T> getSuccessors(T id, boolean copy){
		
		ArrayList<T> result = graph.get(id);
		if(result==null)result=new ArrayList<>();
		if(!copy)return result;
		
		result = (ArrayList<T>) result.clone();
		
		return result;
	}
	
	/**
	 * @param a
	 * @param b
	 * @return
	 */
	public double getWeight(String a, String b) {
		if(weights.containsKey(a+","+b)) {
			return weights.get(a+","+b);
		}else if((graph.get(a).contains(b) || graph.get(b).contains(a))) {
			return 1.0;
		}
		return 0.0;
	}
	
	
	
	public double getOutWeight(T node, boolean forceRecalculate) {
		if(!forceRecalculate)return outWeights.get(node);
		ArrayList<T> successors = getSuccessors(node);
		double sumWeights = 0;
		for(T s:successors) {
			sumWeights += getWeight(node.toString(), s.toString());
		}
		return sumWeights;
	}
	
	public double getOutWeight(T node) {
		return getOutWeight(node, false);
	}
	
	public HashMap<T,Double> getOutWeights(){
		return outWeights;
	}
	
	public void setWeights(HashMap<String, Double> weights) {
		this.weights = weights;
		
		//recalculate out weights
		outWeights.clear();
		
		ArrayList<T> nodes = getAllNodes();
		for(T node:nodes) {
			double outWeight = getOutWeight(node, true);
			outWeights.put(node, outWeight);
		}
	}
	
	/**
	 * Loads the graph from a file, the file should follow the format: source\ttarget.
	 * @param fileName path to graph file
	 * @param invert if true, the edges of the graph will be inverted.
	 */
	public static Graph<String> loadFromFile(String fileName, boolean directed){
		return loadFromFile(fileName, directed, false);
	}
	
	/**
	 * 
	 * @param fileName
	 * @param directed
	 * @param loadWeights load the weights from the column with header named 'weight' or 
	 * from the third column by default (v1.10.16052019)
	 * @return
	 */
	public static Graph<String> loadFromFile(String fileName, boolean directed, boolean loadWeights){
		Graph<String> inst = new Graph<>();
		inst.sourceFile = fileName;
		inst.directed = directed;
		HashMap<String, Double> weights = new HashMap<>();
		int weightIndex=2;
		System.out.println("loading graph from file "+fileName);
		try {
			Scanner scanner = new Scanner(new FileReader(fileName));
			while(scanner.hasNext()){
				String rawLine = scanner.nextLine();
				if(rawLine.startsWith("#") || !rawLine.contains("\t") || rawLine.startsWith("Id") || rawLine.startsWith("Source")) {
					rawLine=rawLine.toLowerCase();
					if(rawLine.contains("weight")){
						String[] line = rawLine.split("\t");
						ArrayList<String> headerList = new ArrayList<String>(Arrays.asList(line));
						weightIndex = headerList.indexOf("weight");
					}
					continue;
				}
				String[] line = rawLine.split("\t");
				try{
					String srcId = line[0];
					String trgtId = line[1];
					
					if(srcId.equals(trgtId))continue;
					
					/* Add edge here */
					if(inst.graph.containsKey(srcId)){
						if(!inst.graph.get(srcId).contains(trgtId)) {
							inst.graph.get(srcId).add(trgtId);
						}
					}else{
						ArrayList<String> targets = new ArrayList<>();
						targets.add(trgtId);
						inst.graph.put(srcId, targets);
					}
					if(loadWeights)weights.put(srcId+","+trgtId, Double.parseDouble(line[weightIndex]));
					if(!directed) {
						//add the same edge in the other direction i.e. from trgtId -> srcId
						if(inst.graph.containsKey(trgtId)){
							if(!inst.graph.get(trgtId).contains(srcId)) {
								inst.graph.get(trgtId).add(srcId);
							}
						}else{
							ArrayList<String> targets = new ArrayList<>();
							targets.add(srcId);
							inst.graph.put(trgtId, targets);
						}
						if(loadWeights)weights.put(trgtId+","+srcId, Double.parseDouble(line[weightIndex]));
					}
				}catch (NumberFormatException e){
					System.out.println("error parsing one of the elements of '"+rawLine+"'");
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		inst.setWeights(weights);
		return inst;
	}
	
	/**
	 * Writes the graph using the given Writer object
	 * @param writer
	 * @param writeHidden if true hidden nodes will be shown in the written file
	 */
	public void write(Writer writer){
		try {
			writer.write("Source\tTarget\tEdgeWeight\n");
			for(T s:graph.keySet()){
				for(T t:graph.get(s)){
					writer.write(s.toString()+"\t"+t.toString()+"\t"+getWeight(s.toString(),t.toString())+"\n");
				}
			}

			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

