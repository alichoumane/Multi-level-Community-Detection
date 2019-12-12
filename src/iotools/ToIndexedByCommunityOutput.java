package iotools;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ToIndexedByCommunityOutput {

	public static String nodeIndexedInput = "/media/ali/Data/Datasets/LFR_benchmark/undirected_weighted_ov/n=1000/n=1000bmut=0.1muw=0.1On=10%/Om=2/1/MultiLevel/communities-level1.dat";
	public static String groupIndexedOutput = "/media/ali/Data/Datasets/LFR_benchmark/undirected_weighted_ov/n=1000/n=1000bmut=0.1muw=0.1On=10%/Om=2/1/MultiLevel/onePerLine-communities-level1.dat";
	
	public static String inputDelimiter = "\t";//the delimiter separating the nodeId from its groups
	public static String inputDelimiterG = " ";//the delimiter separating the groups ids from each other
	public static String outputDelimiter = " ";//the delimiter separating the nodes of one groups on one line

	public static void main(String[] args) {
		System.out.println("start converting "+nodeIndexedInput+" to "+groupIndexedOutput);
		convert(nodeIndexedInput,groupIndexedOutput);
	}
	
	public static void convert(String nodeIndexedInput, String groupIndexedOutput) {
		HashMap<String, ArrayList<String>> groups = loadGroups(nodeIndexedInput);
		writeGroups(groupIndexedOutput, groups);
	}
	
	public static HashMap<String, ArrayList<String>> loadGroups(String nodeIndexedInput){
		HashMap<String, ArrayList<String>> groups = new HashMap<>();
		//System.out.println("loading classes indexed by nodes from file "+nodeIndexedInput);
		try {
			Scanner scanner = new Scanner(new FileReader(nodeIndexedInput));
			while(scanner.hasNext()){
				String rawLine = scanner.nextLine();
				if(rawLine.startsWith("#") || rawLine.startsWith("Id")) {
					continue;
				}
				if(inputDelimiter.equals(inputDelimiterG)) {
					String[] groupsNames = rawLine.split(inputDelimiter);
					String node = groupsNames[0];//first entry in the file
					node = node.replace(":", "");//replace anything else that is expected to be not a part of node name
					for(int i=1;i<groupsNames.length;i++) {
						String group = groupsNames[i];
						if(groups.containsKey(group)==false)groups.put(group, new ArrayList<>());
						groups.get(group).add(node);
					}
				}else {
					String[] line = rawLine.split(inputDelimiter);
					String node = line[0];
					String[] groupsNames = line[1].split(inputDelimiterG);
					for(int i=0;i<groupsNames.length;i++) {
						String group = groupsNames[i];
						if(groups.containsKey(group)==false)groups.put(group, new ArrayList<>());
						groups.get(group).add(node);
					}
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return groups;
	}
	
	/**
	 * write groups indexed by group id, i.e. nodeId1 nodeId2 nodeId3...
	 * @param nodeIndexedOutput
	 * @param groups
	 */
	public static void writeGroups(String nodeIndexedOutput, HashMap<String, ArrayList<String>> groups) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(nodeIndexedOutput));
			for(String group:groups.keySet()) {
				for(String node:groups.get(group)) {
					writer.write(node+outputDelimiter);
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
