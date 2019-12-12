package algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

import iotools.ColorFactory;
import iotools.CustomLogger;
import iotools.ResultsWriter;
import iotools.ToIndexedByCommunityOutput;

import network.Graph;
import network.LocalMaximumFinder;
import network.NeighborhoodOverlap;

import utils.TimeTracker;
import utils.Vector;
import utils.Wrapper;

public class MultilevelCommunityDetection{

	 public static String networkFile = "";
	 public static String outputDirectory = "";
	 public static String logDirectory = null;
         public static boolean weighted = true;
	
	//PROGRAM FLAGS
	public static boolean logCores = false;//Ids of cores at each level
	public static boolean logClasses = false;//classes of the nodes at each level (not original that are nodes written to the main output)
	public static boolean logVectors = false;//vectors of memberships of the nodes at each level (not memberships of original nodes)
	public static boolean logWeights = false;//weights and out-weights of nodes
	public static boolean logLocalMaxEdges = false;
	public static boolean logGraphs = false;
	
	public static boolean generateColors = false;

	public static CustomLogger logger = new CustomLogger("MultilevelCommunityDetection", Level.FINER);

	public static void main(String[] args) {
		TimeTracker t = new TimeTracker();
		if(!loadArgs(args)) {
			return;
		}
		
		MultilevelCommunityDetection generator = new MultilevelCommunityDetection();
		
		Wrapper<ArrayList<HashMap<Integer, ArrayList<String>>>, ArrayList<HashMap<String, Vector<Integer, Double>>>> result = 
				generator.generate();
		
		ArrayList<HashMap<Integer, ArrayList<String>>> levels = result.x;
		ArrayList<HashMap<String, Vector<Integer, Double>>> membershipsLists = result.y;
		
		ColorFactory f = new ColorFactory();
		ColorFactory.NodePainter painter = null;
		if(generateColors)painter = f.new NodePainter(true, null);
		for (int i = 0; i < levels.size()-1; i++) {
			HashMap<Integer, ArrayList<String>> groupsList = levels.get(i);
			if(generateColors)painter.setMemberships(membershipsLists.get(i));
			ResultsWriter.writeResults(groupsList, outputDirectory + "/communities-level" + (i+1) + ".dat", "communities-level-" + (i+1), true, " ", painter);
			ToIndexedByCommunityOutput.convert(outputDirectory + "/communities-level" + (i+1) + ".dat", outputDirectory + "/onePerLine-communities-level" + (i+1) + ".dat");
		}
		logger.log(Level.FINE, levels.size()-1 + " levels\n");
		
		t.stop();
		logger.log(Level.FINE, "finished in "+t.toString()+"\n");
	}

	HashMap<String, Vector<Integer, Double>> memberships = null;

	/**
	 * generate multiple classifications, each represent a level
	 * 
	 * @return two results, left levels and right result is memberships
	 */
	@SuppressWarnings("unchecked")
	public Wrapper<ArrayList<HashMap<Integer, ArrayList<String>>>, ArrayList<HashMap<String, Vector<Integer, Double>>>> generate() {
		Graph<String> graph = null;
		graph = Graph.loadFromFile(networkFile, false, weighted);

		ArrayList<HashMap<Integer, ArrayList<String>>> levels = new ArrayList<>();
		ArrayList<HashMap<String, Vector<Integer, Double>>> membershipsLists = new ArrayList<>();
		Wrapper<ArrayList<HashMap<Integer, ArrayList<String>>>, ArrayList<HashMap<String, Vector<Integer, Double>>>> result = 
				new Wrapper<>(null, null);
		result.x = levels;
		result.y = membershipsLists;

		//initial level
		Wrapper<HashMap<Integer, ArrayList<String>>, HashMap<String, Vector<Integer, Double>>> wrapper = generateLevel(graph, "1");
		HashMap<Integer, ArrayList<String>> groupsList = wrapper.x;//groups list
		Graph<String> newGraph = aggregateNodes(graph, groupsList, wrapper.y);
		memberships = wrapper.y;
		levels.add(groupsList);
		membershipsLists.add((HashMap<String, Vector<Integer, Double>>) memberships.clone());
		
		//calculate memberships using real nodes
		writeVectors(outputDirectory + "/memberships-level1.dat", memberships);
		if (logDirectory != null && logClasses)ResultsWriter.writeResults(groupsList, logDirectory + "/classes-1.dat", "class-level-1", true, " ",  null);
		
		int i = 2;
		while (newGraph.getAllNodes().size() > 1) {
			i++;
			try {
				if (logDirectory != null && logGraphs)newGraph.write(new FileWriter(logDirectory + "/graph-level" + (i) + ".dat"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.log(Level.FINE, "running on level " + i + "\n");
			wrapper = generateLevel(newGraph, (i - 1) + "");
			groupsList = wrapper.x;
			newGraph = aggregateNodes(newGraph, groupsList, wrapper.y);
			
			updateMemberships(memberships, wrapper.y, groupsList);
			
			if(newGraph.getAllNodes().size()>1)writeVectors(outputDirectory + "/memberships-level" + (i-1) + ".dat", memberships);
			membershipsLists.add((HashMap<String, Vector<Integer, Double>>)memberships.clone());
			
			if (logDirectory != null && logClasses)ResultsWriter.writeResults(groupsList, logDirectory + "/classes-" + i + ".dat", "class-level" + i, true, " ", null);
			groupsList = getRealGroupMembers(levels, groupsList);
			levels.add(groupsList);
		}
		
		return result;
	}

	/**
	 * removes the nodes from groups where their membership to it equal to zero. The function doesn't add any new node to any group
	 * @param memberships
	 */
	public void updateGroupsFromMemberships(HashMap<String, Vector<Integer, Double>> memberships,
			HashMap<Integer, ArrayList<String>> groupsList) {
		for(Integer key:groupsList.keySet()) {
			ArrayList<String> members = groupsList.get(key);
			for(int i=members.size()-1;i>=0;i--) {
				String member = members.get(i);
				if(memberships.get(member).get(key).equals(0.0)) {
					members.remove(member);
				}
			}
		}
	}
	
	public HashMap<Integer, ArrayList<String>> findCores(Graph<String> graph, String levelId) {
		logger.log(Level.FINER, "finding local maximal edges...\n");
		HashMap<String, Double> maximumEdges = LocalMaximumFinder.findLocalMaximumEdges(graph);
		if (logDirectory != null && logLocalMaxEdges)ResultsWriter.writeResults(maximumEdges, "localMax", logDirectory + "/localMaxEdges-" + levelId + ".dat", true);
		logger.log(Level.FINEST, "local max edges found:\n");
		for (String edge : maximumEdges.keySet())
			logger.log(Level.FINEST, edge + " (" + maximumEdges.get(edge) + ")\n");

		// create cores - merge maximal edges that have a node in common
		logger.log(Level.FINER, "creating cores...\n");
		int coreIdInc = 0;
		HashMap<Integer, ArrayList<String>> cores = new HashMap<>();
		for (String edge : maximumEdges.keySet()) {
			String node1 = edge.split(",")[0];
			String node2 = edge.split(",")[1];
			ArrayList<Integer> coresOfEdge = new ArrayList<>();// stores the cores that 'edge' has been added to
			for (Integer coreId : cores.keySet()) {
				if (cores.get(coreId).contains(node1) || cores.get(coreId).contains(node2)) {
					if (!cores.get(coreId).contains(node1))
						cores.get(coreId).add(node1);
					if (!cores.get(coreId).contains(node2))
						cores.get(coreId).add(node2);
					coresOfEdge.add(coreId);
					logger.log(Level.FINEST, "edge " + edge + " added to core " + coreId + "\n");
				}
			}
			if (coresOfEdge.size() == 0) {
				logger.log(Level.FINEST, "new core created from " + edge + "\n");
				ArrayList<String> members = new ArrayList<String>();
				members.add(node1);
				members.add(node2);
				cores.put(coreIdInc++, members);
			} else if (coresOfEdge.size() > 1) {
				for (int i = 1; i < coresOfEdge.size(); i++) {
					for (String node : cores.get(coresOfEdge.get(i))) {
						if (!cores.get(coresOfEdge.get(0)).contains(node))
							cores.get(coresOfEdge.get(0)).add(node);
					}
					cores.remove(coresOfEdge.get(i));
				}
				logger.log(Level.FINEST, "Cores ");
				for (Integer coreId : coresOfEdge)
					logger.log(Level.FINEST, coreId + " ");
				logger.log(Level.FINEST, "merged\n");
			}
		}
		return cores;
	}

	/**
	 * merge the entries of the given memberships based on the merged communities
	 * 
	 * @param memberships
	 *            vectors of actual nodes
	 * @param vectors
	 *            vectors of nodes that represent groups
	 * @param mergedGroups
	 * @return
	 */
	public void updateMemberships(HashMap<String, Vector<Integer, Double>> memberships,
			HashMap<String, Vector<Integer, Double>> vectors, HashMap<Integer, ArrayList<String>> mergedGroups) {
		for (String node : memberships.keySet()) {
			Vector<Integer, Double> vector = memberships.get(node);
			Vector<Integer, Double> newVector = new Vector<Integer, Double>(0.0, mergedGroups.keySet());
			for (Integer mergedGroupsKey : mergedGroups.keySet()) {
				ArrayList<String> merged = mergedGroups.get(mergedGroupsKey);
				double sum = 0.0;
				for (int i = 0; i < merged.size(); i++) {
					sum += vector.get(Integer.parseInt(merged.get(i)))
							* vectors.get(merged.get(i) + "").get(mergedGroupsKey);
				}
				newVector.put(mergedGroupsKey, sum);
			}
			memberships.put(node, newVector);
		}
	}

	/**
	 * replaces the nodes ids in groups lists with the real members, since after
	 * aggregation, the groups lists will contain group ids instead of nodes ids
	 * 
	 * @param allGroupsLists
	 * @param newGroupsList
	 * @return
	 */
	public HashMap<Integer, ArrayList<String>> getRealGroupMembers(
			ArrayList<HashMap<Integer, ArrayList<String>>> allGroupsLists,
			HashMap<Integer, ArrayList<String>> newGroupsList) {
		HashMap<Integer, ArrayList<String>> previousGroupsList = allGroupsLists.get(allGroupsLists.size() - 1);
		HashMap<Integer, ArrayList<String>> groupsList = new HashMap<>();
		for (Integer newGroupKey : newGroupsList.keySet()) {
			ArrayList<String> newGroupMembers = newGroupsList.get(newGroupKey);
			// replace new group members keys by corresponding real nodes
			// NOTE that each node in new groups lists is a group in previous groups lists
			ArrayList<String> realMembers = new ArrayList<String>();
			for (String node : newGroupMembers) {
				for (String previousNode : previousGroupsList.get(Integer.parseInt(node))) {
					if (realMembers.contains(previousNode) == false)
						realMembers.add(previousNode);
				}
			}
			groupsList.put(newGroupKey, realMembers);
		}
		return groupsList;
	}

	/**
	 * returns a new weighted graph where each node in it is a group in the old
	 * graph. The weights are calculated to represent the strength of the relation
	 * between the groups
	 * 
	 * @param graph
	 * @param groupsList
	 * @param nodeVectors
	 * @return
	 */
	public Graph<String> aggregateNodes(Graph<String> graph, HashMap<Integer, ArrayList<String>> groupsList,
			HashMap<String, Vector<Integer, Double>> nodeVectors) {
		logger.log(Level.FINER, "Aggregating nodes...\n");
		// create new graph with aggregated nodes
		HashMap<String, ArrayList<String>> associations = new HashMap<>();
		HashMap<String, Double> weights = new HashMap<>();

		for (Integer key : groupsList.keySet()) {
			// create node newNode
			String newNode = key + "";
			if (associations.containsKey(newNode))continue;// this group was already aggregated into a node
			ArrayList<String> newNodeSuccessors = new ArrayList<String>();
			// find out what groups does this group has relation with
			for (Integer gKey : groupsList.keySet()) {
				if (key == gKey)
					continue;// to avoid relation between the node and itself

				ArrayList<String> groupMembers = groupsList.get(gKey);
				ArrayList<String> newNodeGroupMembers = groupsList.get(key);

				boolean isSuccessor = false;
				double weight = 0.0;
				for (String node : newNodeGroupMembers) {
					ArrayList<String> nodeSuccessors = graph.getSuccessors(node);
					nodeSuccessors.retainAll(groupMembers);
					// now we got the edges going from 'node' to the group g
					if (nodeSuccessors.isEmpty())
						continue;
					isSuccessor = true;
					for (String nodeSuccessor : nodeSuccessors) {
						logger.log(Level.FINEST,
								key + " with " + gKey + " -> sum=" + weight + " += weight(" + node + "," + nodeSuccessor
										+ ") -> " + graph.getWeight(node, nodeSuccessor) + "*"
										+ nodeVectors.get(node).get(key) + "*"
										+ nodeVectors.get(nodeSuccessor).get(gKey) + "\n");
						// maybe instead of using stored weights (which are most likely deep weights),
						// use regular neighbourhood overlap
						weight += graph.getWeight(node, nodeSuccessor) * nodeVectors.get(node).get(key)
								* nodeVectors.get(nodeSuccessor).get(gKey);
					}
				}
				if (isSuccessor) {
					newNodeSuccessors.add(gKey + "");
					double newNodeGroupMemberships = 0.0;
					double groupMemberships = 0.0;
					for (String node : newNodeGroupMembers) {
						newNodeGroupMemberships += nodeVectors.get(node).get(key);
						logger.log(Level.FINEST, "group " + key + ": " + newNodeGroupMemberships + " -- node:" + node
								+ "=" + nodeVectors.get(node).get(key) + "\n");
					}
					for (String node : groupMembers) {
						groupMemberships += nodeVectors.get(node).get(gKey);
						logger.log(Level.FINEST, "group " + gKey + ": " + groupMemberships + " -- node:" + node + "="
								+ nodeVectors.get(node).get(gKey) + "\n");
					}
					weight = weight / Math.sqrt(groupMemberships * newNodeGroupMemberships);
					weights.put(newNode + "," + gKey, weight);
					logger.log(Level.FINEST, "weight(" + key + "," + gKey + ") = " + weight + "\n");
				}
			}
			if(newNodeSuccessors.size()>0)associations.put(newNode, newNodeSuccessors);
		}
		Graph<String> newGraph = new Graph<String>(associations);
		newGraph.setWeights(weights);
		return newGraph;
	}

	/**
	 * 
	 * @param graph
	 * @param levelId
	 * @return groups list and set of vectors of memberships
	 */
	public Wrapper<HashMap<Integer, ArrayList<String>>, HashMap<String, Vector<Integer, Double>>> generateLevel(
			Graph<String> graph, String levelId) {
		// step 1. find deep weights for all edges
		logger.log(Level.FINER, "calculating deep weights...\n");
		NeighborhoodOverlap.calculateDeepWeight = true;
		HashMap<String, Double> weights = NeighborhoodOverlap.calculate(graph, true);
		
		if (logDirectory != null && logWeights)
			ResultsWriter.writeResults(weights, "weight-initial", logDirectory + "/weights-level-" + levelId + ".dat", true);
		if (logDirectory != null && logWeights)
			ResultsWriter.writeResults(graph.getOutWeights(), "outWeights", logDirectory + "/outWeights_level-" + levelId + ".dat", false);

		// step 2. create cores consisted of terminals of edges with local maximal weights.
		// finding local maximal edges
		HashMap<Integer, ArrayList<String>> cores = findCores(graph, levelId);
		logger.log(Level.FINER, cores.size() + " cores in total\n");
		if (logDirectory != null && logCores)
			ResultsWriter.writeResults(cores, logDirectory + "/cores-" + levelId + ".dat", "coreId");

		HashMap<String, Vector<Integer, Double>> nodeVectors = calculateVectors(graph, cores, true);
		// save vectors
		if (logDirectory != null && logVectors)writeVectors(logDirectory + "/vectors-" + levelId + ".dat", nodeVectors);
		logger.log(Level.FINEST, "FINAL VECTORS\n");
		printVectors(nodeVectors);

		// step 8. each node fall within distance d from any core, we add it to that
		// core, if two cores fall within this distance, we merge them.
		HashMap<Integer, ArrayList<String>> groupsList = new HashMap<>();
		ArrayList<String> groupsToMerge = new ArrayList<>();
		for (Integer key : cores.keySet()) {
			ArrayList<String> members = cores.get(key);
			LinkedList<String> nodesToCheck = new LinkedList<>();
			nodesToCheck.addAll(members);
			logger.log(Level.FINEST, "expanding core #" + key + " intially " + members.size() + " nodes\n");
			while (nodesToCheck.isEmpty() == false) {
				// check a node means classifying its neighbours not classifying the node itself
				String node = nodesToCheck.poll();
				ArrayList<String> successors = graph.getSuccessors(node);
				successors.removeAll(members);
				for (String s : successors) {
					double distance = distance(nodeVectors.get(s), nodeVectors.get(node));
					logger.log(Level.FINEST, node + " <-> " + s + " = " + distance + "\n");
					double ratio = Math.sqrt(averageDistances(node, graph, nodeVectors)
							* averageDistances(s, graph, nodeVectors));//THRESHOLD ******
					if (distance <= ratio) {
						// add this node to the community
						if (members.contains(s) == false)
							members.add(s);
						if (!nodesToCheck.contains(s))
							nodesToCheck.add(s);
						logger.log(Level.FINEST, s + " added to core " + key + "\n");
						// if it is part of another core, merge them
						Integer oldGroupOfS = getGroupIdOf(s, groupsList);
						if (oldGroupOfS != null)
							if (!groupsToMerge.contains(key + "-" + oldGroupOfS)
									&& !groupsToMerge.contains(oldGroupOfS + "-" + key)) {
								groupsToMerge.add(key + "-" + oldGroupOfS);
								logger.log(Level.FINEST, "groups " + key + " and " + oldGroupOfS + " to be merged\n");
							}

					}
				}
			}
			groupsList.put(key, members);
		}

		// merge groups to be merged
		logger.log(Level.FINEST, "merging groups: ");
		for (String entry : groupsToMerge)
			logger.log(Level.FINEST, entry + " ");
		logger.log(Level.FINEST, "\n");
		for (String entry : groupsToMerge) {
			Integer key1 = Integer.parseInt(entry.split("-")[0]);
			Integer key2 = Integer.parseInt(entry.split("-")[1]);
			if (key1 == key2)
				continue;
			logger.log(Level.FINEST, "merging " + key1 + "<-" + key2 + "\n");
			// merge the groups
			groupsList.get(key1).removeAll(groupsList.get(key2));// to avoid having doubles
			groupsList.get(key1).addAll(groupsList.get(key2));
			groupsList.remove(key2);
			// update nodes` vectors
			for (String node : nodeVectors.keySet()) {
				Vector<Integer, Double> vector = nodeVectors.get(node);
				vector.put(key1, vector.get(key1) + vector.get(key2));
				vector.remove(key2);
			}
			// change key name in groupsToMerge to avoid Null pointer exception in next loop
			for (int i = 0; i < groupsToMerge.size(); i++) {
				String s = groupsToMerge.get(i);
				if (s.startsWith(key2 + "-")) {
					groupsToMerge.set(i, key1 + "-" + s.split("-")[1]);
				} else if (s.endsWith("-" + key2)) {
					groupsToMerge.set(i, s.split("-")[0] + "-" + key1);
				}
			}
		}

		nodeVectors = calculateVectors(graph, groupsList, false);
		if(logDirectory!=null && logVectors)writeVectors(logDirectory + "/vectors_end-level-" + levelId + ".dat", nodeVectors);
		logger.log(Level.FINER, "final communities #" + groupsList.size() + "\n");

		// add overlapping nodes
		logger.log(Level.FINER, "adding unclassified nodes\n");
		ArrayList<String> unclassifiedNodes = graph.getAllNodes();
		for (String node : unclassifiedNodes) {
			if (!ResultsWriter.groupsContains(groupsList, node)) {
				for (Integer groupKey : nodeVectors.get(node).keySet()) {
					if (nodeVectors.get(node).get(groupKey) != 0) {
						if (groupsList.get(groupKey).contains(node) == false)
							groupsList.get(groupKey).add(node);
						logger.log(Level.FINEST, "add " + node + " to " + groupKey + "\n");
					}
				}
			}
		}
		
		return new Wrapper<HashMap<Integer, ArrayList<String>>, HashMap<String, Vector<Integer, Double>>>(groupsList,
				nodeVectors);
	}
	
	/**
	 * initialise node vectors by associating a vector for each node with 1.0 in the
	 * index corresponding to its community
	 * 
	 * @param groups
	 * @return
	 */
	private HashMap<String, Vector<Integer, Double>> initializeVectors(HashMap<Integer, ArrayList<String>> groups) {
		HashMap<String, Vector<Integer, Double>> nodeVectors = new HashMap<>();// stores a vector for each node contains membership values
		for (Integer key : groups.keySet()) {
			ArrayList<String> core = groups.get(key);
			for (String node : core) {
				// create vector for node
				Vector<Integer, Double> vector = new Vector<Integer, Double>(0.0, groups.keySet());
				// initialise the vector
				for (Integer coreKey : groups.keySet())
					vector.put(coreKey, 0.0);
				vector.put(key, 1.0);
				nodeVectors.put(node, vector);
			}
		}
		return nodeVectors;
	}

	private HashMap<String, Vector<Integer, Double>> calculateVectors(Graph<String> graph,
			HashMap<Integer, ArrayList<String>> cores, boolean refreshVectors) {
		// step 3. create a vector of membership for each node to each core or
		// community.
		// step 4. start from one at the i-th entry for the nodes of the i-th core
		logger.log(Level.FINER, "calculating vectors and initial membership values\n");
		HashMap<String, Vector<Integer, Double>> nodeVectors = initializeVectors(cores);
		logger.log(Level.FINER, "calculating final nodes` vectors values\n");
		LinkedList<String> queue = new LinkedList<>();
		LinkedList<String> nextLevelQueue = new LinkedList<>();
		HashMap<String, Vector<Integer, Double>> nextLevelVectors = new HashMap<>();
		// add to the queue the first level of nodes to start with
		for (ArrayList<String> core : cores.values()) {
			for (String node : core) {
				for (String s : graph.getSuccessors(node)) {
					if (!queue.contains(s))
						queue.add(s);
				}
			}
		}
		if (refreshVectors)
			nodeVectors = refreshVectors(nodeVectors, graph);
		
		logger.log(Level.FINEST, "new iteration started...\n");
		// step 6. calculate vectors of neighbours of neighbours and continue like this
		// to calculate all.
		while (!queue.isEmpty() || !nextLevelQueue.isEmpty()) {
			// if queue is empty, go to next level
			if (queue.isEmpty()) {
				while (!nextLevelQueue.isEmpty()) {
					queue.add(nextLevelQueue.poll());
				}
				nextLevelQueue.clear();
				nodeVectors.putAll(nextLevelVectors);
				
				/*if (refreshVectors)//FIXME uncomment to refresh at each calculation iteration
					nodeVectors = refreshVectors(nodeVectors, graph);*/
				
				nextLevelVectors.clear();
				logger.log(Level.FINEST, "new iteration started...\n");
			}

			// calculate the vector of 'node'
			String node = queue.poll();
			if (nodeVectors.containsKey(node))
				continue;// avoid recalculating a vector from previous level
			ArrayList<String> neighbours = graph.getSuccessors(node);
			Vector<Integer, Double> vector = new Vector<>(0.0, cores.keySet());
			// calculate sum of weights
			double sumOfWeights = 0.0;
			for (String neighbour : neighbours) {
				Vector<Integer, Double> nvector = nodeVectors.get(neighbour);
				if (nvector != null)
					sumOfWeights += graph.getWeight(node, neighbour);
			}
			for (String neighbour : neighbours) {
				Vector<Integer, Double> nvector = nodeVectors.get(neighbour);
				if (nvector != null) {
					for (Integer key : nvector.getKeysPool()) {
						double val = 0;
						if (graph.getOutWeight(node) != 0)
							val = nvector.get(key) * graph.getWeight(node, neighbour) / sumOfWeights;
						logger.log(Level.FINEST, node + "-" + neighbour + " key:" + key + " + " + nvector.get(key) + "*"
								+ graph.getWeight(node, neighbour) + "/" + graph.getOutWeight(node) + "=" + val + "\n");
						if (vector.get(key) == null)
							vector.put(key, val);
						else
							vector.put(key, vector.get(key) + val);
					}
				}
			}
			nextLevelVectors.put(node, vector);
			for (String successor : graph.getSuccessors(node)) {
				if (!nextLevelQueue.contains(successor)) {
					nextLevelQueue.add(successor);
				}
			}
		}
		if (refreshVectors)//FIXME uncomment to refresh(recalculate) vectors at the end
			nodeVectors = refreshVectors(nodeVectors, graph);
		
		return nodeVectors;
	}

	private HashMap<String, Vector<Integer, Double>> refreshVectors(HashMap<String, Vector<Integer, Double>> vectors,
			Graph<String> graph) {
		HashMap<String, Vector<Integer, Double>> newVectors = new HashMap<>();
		logger.log(Level.FINER, "refreshing vectors...\n");
		// step 6. calculate vectors of neighbours of neighbours and continue like this
		// to calculate all.
		ArrayList<String> nodesKeys = new ArrayList<String>(vectors.keySet());
		//Collections.shuffle(nodesKeys);// to test if shuffling affects result
		for (String node : nodesKeys) {
			// calculate the vector of 'node'
			if (newVectors.containsKey(node))
				continue;// avoid recalculating a vector from previous level
			ArrayList<String> neighbours = graph.getSuccessors(node);
			Vector<Integer, Double> oldVector = vectors.get(node);
			
			@SuppressWarnings("unchecked")
			Vector<Integer, Double> vector = (Vector<Integer, Double>) oldVector.clone();
			
			double sumOfWeights = 0.0;
			// finding the sum of weights of neighbours who has vectors
			for (String neighbour : neighbours) {
				if (vectors.get(neighbour) != null)
					sumOfWeights += graph.getWeight(node, neighbour);
			}
			for (String neighbour : neighbours) {
				Vector<Integer, Double> nvector = vectors.get(neighbour);
				if (nvector != null) {
					for (Integer key : nvector.keySet()) {
						double val = nvector.get(key) * graph.getWeight(node, neighbour) / sumOfWeights;
						logger.log(Level.FINEST, node + "-" + neighbour + " key:" + key + " + " + nvector.get(key) + "*"
								+ graph.getWeight(node, neighbour) + "/" + sumOfWeights + "=" + val + "\n");
						if (vector.get(key) == null)
							vector.put(key, val);
						else
							vector.put(key, vector.get(key) + val);
					}
				}
			}
			newVectors.put(node, vector);
		}
		for (String node : newVectors.keySet()) {
			for (Integer key : newVectors.get(node).keySet())
				newVectors.get(node).put(key, newVectors.get(node).get(key) / 2);
		}
		return newVectors;
	}

	/**
	 * returns the group id of the given node. This function is needed because
	 * Groups list is indexed by group ids not node ids
	 * 
	 * @param node
	 * @param groupsList
	 * @return
	 */
	private static Integer getGroupIdOf(String node, HashMap<Integer, ArrayList<String>> groupsList) {
		for (Integer key : groupsList.keySet()) {
			if (groupsList.get(key).contains(node))
				return key;
		}
		return null;
	}

	public static void writeVectors(String fileName, HashMap<String, Vector<Integer, Double>> vectors) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			// write headers
			writer.write("node\t");
			for (String node : vectors.keySet()) {
				for (Integer key : vectors.get(node).keySet()) {
					writer.write(key + "\t");
				}
				writer.write("\n");
				break;
			}
			for (String node : vectors.keySet()) {
				writer.write(node + "\t");
				for (Integer key : vectors.get(node).keySet()) {
					writer.write(vectors.get(node).get(key) + "\t");
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printVectors(HashMap<String, Vector<Integer, Double>> vectors) {
		logger.log(Level.FINEST, "node :: {");
		for (String node : vectors.keySet()) {
			for (Integer key : vectors.get(node).keySet()) {
				logger.log(Level.FINEST, "core:" + key + ",");
			}
			logger.log(Level.FINEST, "}\n");
			break;
		}
		for (String node : vectors.keySet()) {
			logger.log(Level.FINEST, "V(" + node + ") = {");
			for (Integer key : vectors.get(node).keySet())
				logger.log(Level.FINEST, vectors.get(node).get(key) + ", ");
			logger.log(Level.FINEST, "}\n");
		}

	}

	/**
	 * returns the distance between the two given vectors
	 * 
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	public static double distance(Vector<Integer, Double> vector1, Vector<Integer, Double> vector2) {
		if (vector1.keySet().size() != vector2.keySet().size())
			throw new IllegalArgumentException("dimension mismatch");
		double sum = 0.0;
		for (Integer key : vector1.keySet()) {
			double val = Math.pow(vector1.get(key) - vector2.get(key), 2);
			sum += val;
		}
		return Math.sqrt(sum);
	}

	/**
	 * average of distances using weights
	 * 
	 * @param node
	 * @param graph
	 * @param vectors
	 * @return
	 */
	private static double averageDistances(String node, Graph<String> graph,
			HashMap<String, Vector<Integer, Double>> vectors) {
		double sum = 0;
		double sum0 = 0;
		for (String s : graph.getSuccessors(node)) {
			sum += distance(vectors.get(node), vectors.get(s)) * graph.getWeight(node, s);
			sum0 += graph.getWeight(node, s);
		}
		return sum / sum0;
	}
	
	public static boolean loadArgs(String[] args) {
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));

		if(args.length < 2 || args[0].equals("-h")) {
                //if(args[0].equals("-h")) {
			//print help
			System.out.println("Please provide the following arguments to run the program:");
			System.out.println("-f followed by the network file name (undirected, one edge per line)");
			System.out.println("-uw to deal with unweighted network (default is weighted)");
			return false;
		}
		
		if(args.length >= 2) {
			int index = -1;
			
			index = argsList.indexOf("-f");
			if(index==-1)return loadArgs(new String[]{"-h"});
			else networkFile = argsList.get(index+1);
			
			File networkF = new File(networkFile);
			
			if(!networkF.exists()) 
			{
				System.out.println(networkFile + " does not exist.");
				return false;
			}

			try{
	                        outputDirectory=(new File(networkF.getCanonicalPath())).getParentFile().getAbsolutePath();
                        
				index = argsList.indexOf("-uw");
				if(index != -1) weighted = false;
                        
				return true;
			}
			catch(IOException e)
			{
				return false;
			}
		}
		
		return loadArgs(new String[]{"-h"});
	}

}
