package iotools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * Takes input:
 * <ul>
 * <li>graphPath</li>
 * <li>resultPath</li>
 * <li>N, an integer</li>
 * <li>isDirected, "directed" or "undirected"</li>
 * </ul>
 * 
 * <ul>
 * <li>v1.9.13122018 in this version, findDisconnectedCommunities were fixed to gain performance. Now we don't iterate over grouped 
 * nodes twice. Also addToGroup were updated to use <code>attributes</code> object to query node's group id rather than searching for
 * it in <code>groups</code> object which is faster.</li>
 * <li> v1.10.18122018: don't check predecessors after removing a node</li>
 * <li> v1.11.22012019: added fixedClassification parameter to {@link ResultsWriter#addNodesToClosestGroup(HashMap, boolean, boolean, boolean)}</li>
 * <li> v1.12.23012019: added useWeightsInAddition parameter to addNodesToMaximizeMod</li>
 * <li> v1.13.26012019: normalize sum of weights from node to a community by out weight of the node</li>
 * <li> v1.14.30012019: add mergeDuplicates flag to writeResults function</li>
 * </ul>
 */
public abstract class ResultsWriter {

	public static final CustomLogger logger = new CustomLogger("CommunitiesGenerator", Level.FINEST);
	
	/**
	 * check if the given set of groups contains the given node
	 * @param groups
	 * @param node
	 * @return
	 */
	public static boolean groupsContains(HashMap<Integer, ArrayList<String>> groups, String node) {
		for(ArrayList<String> group:groups.values()) {
			if(group.contains(node)) {
				return true;
			}
		}
		return false;
	}
	
	public static void writeResults(HashMap<Integer, ArrayList<String>> groupsList, String file,String attributeName) {
		writeResults(groupsList, file, "class", false, " ", null);
	}
	
	public static void writeResults(HashMap<Integer, ArrayList<String>> groupsList, String file,
			String attributeName, boolean mergeDuplicates, String delimiter, ColorFactory.NodePainter painter) {
		if(mergeDuplicates) {
			//merging duplicates and assigning colors
			HashMap<Integer, String> groupsColors=null;
			if(painter!=null) {
				groupsColors = painter.getGroupsColors(new ArrayList<Integer>(groupsList.keySet()));
			}
			HashMap<String, String> nodesAttrs = new HashMap<>();
			HashMap<String, String> nodesColors = new HashMap<>();
			for(Integer key:groupsList.keySet()) {
				for(String node:groupsList.get(key)) {
					if(!nodesAttrs.containsKey(node))nodesAttrs.put(node,key+"");
					else nodesAttrs.put(node,nodesAttrs.get(node)+delimiter+key);
				}
				for(String node:groupsList.get(key)) {
					if(groupsColors!=null) {
						nodesColors.put(node, painter.getColorOf(node, groupsColors));
					}
				}
			}
			//writing results
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				writer.write("Id\t"+attributeName);
				if(painter!=null)writer.write("\t"+attributeName+"colors\n");
				else writer.write("\n");
				for(String node:nodesAttrs.keySet()) {
					writer.write(node+"\t"+nodesAttrs.get(node));
					if(painter!=null)writer.write("\t"+((nodesColors.get(node)!=null)?nodesColors.get(node):"")+"\n");
					else writer.write("\n");
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				for(Integer groupId:groupsList.keySet()) {
					for(String node: groupsList.get(groupId)) {
						writer.write(node+"\t"+groupId+"\n");
					}
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * writes results to a file
	 * @param attributes contains the classification for each node
	 * @param attributeName
	 * @param file
	 * @param edgeAttrs if true, the string keys are considered to be tuples delimited with a ','
	 */
	public static void writeResults(HashMap<String, Double> attributes, String attributeName, String file, boolean edgeAttrs) {
		try {
			//write results with attributes
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			if(!edgeAttrs)writer.write("Id\t"+attributeName+"\n");else writer.write("Source\tTarget\t"+attributeName+"\n");
			writeResults(attributes, writer, edgeAttrs);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeResults(HashMap<String, Double> attributes, BufferedWriter writer, boolean edgeAttrs) {
		try {
			//write results with attributes
			for(String node:attributes.keySet()) {
				String edge = node;
				if(edgeAttrs) {
					node = node.replace(",", "\t");
				}
				writer.write(node+"\t"+attributes.get(edge)+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
