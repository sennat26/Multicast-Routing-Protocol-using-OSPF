

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Controller {
	
	
	Map<String, Integer> nodeReadCount = new HashMap<String, Integer>();
	Map<Integer, List<Integer>> nodeNeighbours = new HashMap<Integer, List<Integer>>();
	
	
	public Controller() {
		// TODO Auto-generated constructor stub
		
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			Controller controller = new Controller();
			
			//read topology file.
			controller.readTopologyFile();
			
			List<Integer> nodeKeyList = new ArrayList<Integer>(controller.nodeNeighbours.keySet());
			
			while(true) {
				
				for (int i = 0; i < 150; i++) {
					
					for(int nodeKey : nodeKeyList) {
//					for (int j = 0; j < controller.nodeNeighbours.size(); j++) {
						
						//reading output file
						int count = 0;
						if (controller.nodeReadCount.get(nodeKey + "") != null) {
							count = controller.nodeReadCount.get(nodeKey + "");
						}
						
						//read output of all nodes.
						List<String> messageList = controller.readMessageFromOutputFile(count, 
								"output_" + nodeKey, nodeKey);
						
						//writing to the input file of other nodes
						List<Integer> outGoingNodeList = controller.nodeNeighbours.get(nodeKey);
						if (outGoingNodeList != null) {
							for (Integer nodeNumber : outGoingNodeList) {
								controller.writeMessageToInputFile(messageList, nodeNumber);
							}
						}
						
						
					}
					
				}
				
			}
			
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	
	private void readTopologyFile() throws Exception {
		
		String topology = "";
		BufferedReader bufferedReader = new BufferedReader(new FileReader("topology.txt"));
		while ((topology = bufferedReader.readLine()) != null) {
			String[] node = topology.split(" ");
			System.out.println(node[0] + " " + node[1]);
			
			int firstNode = Integer.parseInt(node[0]);
			int secondNode = Integer.parseInt(node[1]);
			
			if (nodeNeighbours.get(firstNode) != null) {
				List<Integer> nodeList = nodeNeighbours.get(firstNode);
				nodeList.add(secondNode);
			} else {
				List<Integer> nodeList = new ArrayList<Integer>();
				nodeList.add(secondNode);
				nodeNeighbours.put(firstNode, nodeList);
			}
		}
		
		Set<Entry<Integer, List<Integer>>> entrySet = nodeNeighbours.entrySet();
		for (Entry<Integer, List<Integer>> entry : entrySet) {
			System.out.println( entry.getKey() + " " + ((List)entry.getValue()).toString());
		}
		
		bufferedReader.close();
	}
	
	
	private List<String> readMessageFromOutputFile(int count, String inputFileName, int nodeNumber) throws Exception {
		
		List<String> messageList = new ArrayList<String>();
		String message = "";
		File existsOrNot = new File(inputFileName + ".txt");
		if (existsOrNot.exists()) {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFileName + ".txt"));
			int temp = 0;
			while((message = bufferedReader.readLine()) != null) {
				++temp;
				if (temp > count) {
					messageList.add(message);
				}
			}
			count = temp;
			nodeReadCount.put(nodeNumber + "", count);
			bufferedReader.close();
		}
		
		return messageList;
	}
	
	
	private void writeMessageToInputFile(List<String> messageList, int nodeNumber) throws Exception {
		
		File existsOrNot = new File("input_" + nodeNumber + ".txt");
		if (existsOrNot.exists()) {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("input_" + nodeNumber + ".txt", true));
			for (String message : messageList) {
				bufferedWriter.write(message + "\n");
			}
			bufferedWriter.close();
		}
		
	}

}
