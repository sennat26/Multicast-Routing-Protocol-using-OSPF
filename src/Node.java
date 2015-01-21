

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Node {
	
	private int nodeNumber;
	private boolean sender = false;
	private boolean receiver= false;
	private String multicastSenderString;
	private int multicastSenderNodeNo;
	private int count = 0;
	private String inputFileName;
	private String outputFileName;
	private String receiverFile;
	private List<Integer> incomingNodeNumbers = new ArrayList<Integer>();
	private int timeStamp = 0;
	private Map<Integer, Integer> timeStampMap = new HashMap<Integer, Integer>();
	//storing as key can reach the value(nodes) list
	private Map<Integer, Set<Integer>> linkStateAdvMap = new HashMap<Integer, Set<Integer>>();
	private Map<Integer, Integer> linkStateTimer = new HashMap<Integer, Integer>();
	private Map<Integer, String> receiverShortestPath = null;
	private Map<Integer, Integer> receiverChildTimer = new HashMap<Integer, Integer>();
	//key as sender and value as receivers
	private Map<Integer, Set<Integer>> senderReceiverList = new HashMap<Integer, Set<Integer>>();
	
	
	public Node(String[] args) {
		// TODO Auto-generated constructor stub
		
		//node number
		nodeNumber = Integer.parseInt(args[0]);
		inputFileName = "input_" + nodeNumber + ".txt";
		outputFileName = "output_" + nodeNumber + ".txt";
		
		if (args.length == 3) {
			//either sender or receiver
			if(args[1].equalsIgnoreCase("sender")) {
				sender = true;
			} else {
				receiver = true;
			}
			
			//sender string or receiver node number.
			if (sender) {
				multicastSenderString = args[2];
			} else {
				multicastSenderNodeNo = Integer.parseInt(args[2]);
				receiverFile = nodeNumber + "_received_from_" + multicastSenderNodeNo + ".txt";
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		try {
			
			Node node = new Node(args);
			node.createInputOutputFiles();
			System.out.println(node.nodeNumber);
			System.out.println(node.sender);
			System.out.println(node.receiver);
			System.out.println(node.multicastSenderString);
			System.out.println(node.multicastSenderNodeNo);
			
			for (int i = 0; i < 150; i++) {
				
				if (i != 0 && i % 5 == 0) {
					node.sendHelloMessage();
				} 
				
				if (i != 0 && i % 10 == 0){
					node.sendLinkStateAdvertisementMessages();
				}
				
				
				//reading input file
//				sendHelloMessage();
//				sendLinkStateAdverstiment();
//				refereshParent();
//				readInputFile();
				
				
				List<String> messageList = node.readMessageFromInputFile();
				node.processMessages(messageList, i);
				
				node.validateLinkStateAdvertisement(i);
				
				
				if (i != 0 && i % 10 == 0) {
					if (node.receiver) {
						node.receiverShortestPath = node.computeShortestPath(node.nodeNumber);
						node.sendJoinMessages(node.nodeNumber, node.multicastSenderNodeNo);
					} else if (node.sender) {
						if (node.senderReceiverList.size() > 0) {
							node.sendMulticastDataMessage();
						}
					}
				}
				
				node.validateReceiverJoinMessage(i);
				
				Thread.sleep(1000);
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	private void createInputOutputFiles() throws Exception {
		
		File inputFile = new File(inputFileName);
		inputFile.createNewFile();
		File outputFile = new File(outputFileName);
		outputFile.createNewFile();
		
		if (receiver) {
			File receiver = new File(receiverFile);
			receiver.createNewFile();
		}
		
	}
	
	
	private void sendHelloMessage() throws Exception {
		
		//write hello message
		String hello = "hello " + nodeNumber;
		writeMessageToOutputFile(hello);
		
	}
	
	private void sendLinkStateAdvertisementMessages() throws Exception {
		if (incomingNodeNumbers.size() > 0) {
			//write link state advertisement messages
			String timeStampString = timeStamp + "";
			if (timeStampString.length() == 1) {
				timeStampString = "0" + timeStampString;
			}
			String linkStateMessages = "linkstate " + nodeNumber + " " + timeStampString + " ";
			for (Integer neighborNodes : incomingNodeNumbers) {
				linkStateMessages = linkStateMessages + neighborNodes + " ";
			}
			writeMessageToOutputFile(linkStateMessages.trim());
			timeStamp++;
		}
		
	}
	
	private void writeMessageToOutputFile(String message) throws Exception {
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFileName, true));
		bufferedWriter.write(message + "\n");
		bufferedWriter.close();
		
	}
	
	private List<String> readMessageFromInputFile() throws Exception {
		
		List<String> messageList = new ArrayList<String>();
		String message = "";
		BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFileName));
		int temp = 0;
		while((message = bufferedReader.readLine()) != null) {
			++temp;
			if (temp > count) {
				messageList.add(message);
			}
		}
		count = temp;
		bufferedReader.close();
		return messageList;
	}
	
	
	private void processMessages(List<String> messageList, int time) throws Exception {
		
		for (String message : messageList) {
			
			if (message.startsWith("hello")) {
				String[] helloMessage = message.split("hello ");
				if(!incomingNodeNumbers.contains(Integer.parseInt(helloMessage[1]))) {
					incomingNodeNumbers.add(Integer.parseInt(helloMessage[1]));
				}
				if (linkStateAdvMap.get(Integer.parseInt(helloMessage[1])) != null) {
					Set<Integer> helloNodeSet = linkStateAdvMap.get(Integer.parseInt(helloMessage[1]));
					helloNodeSet.add(nodeNumber);
				} else {
					Set<Integer> helloNodeSet = new HashSet<Integer>();
					helloNodeSet.add(nodeNumber);
					linkStateAdvMap.put(Integer.parseInt(helloMessage[1]), helloNodeSet);
				}
				
			} else if (message.startsWith("linkstate")) {
				processLinkStateAdvertisementMessages(message.trim(), time);
			} else if(message.startsWith("join")) {
				processJoinMessages(message, time);
			} else if (message.startsWith("data")) {
				processMulticastDataMessages(message);
			}
			
		}
		
	}
	
	
	private void processLinkStateAdvertisementMessages(String message, int time) throws Exception {
		
		String[] linkStateSplitMessage = message.split(" ");
		int linkStateSenderNodeNumber = Integer.parseInt(linkStateSplitMessage[1]);
		
		//checking whether the link state node number is current node number
		if (linkStateSenderNodeNumber != nodeNumber) {
			
			int timeStampForNodeNo = -1;
			if (timeStampMap.get(linkStateSenderNodeNumber) != null) {
				timeStampForNodeNo = timeStampMap.get(linkStateSenderNodeNumber);
			}
			int timeStampFromMessage = Integer.parseInt(linkStateSplitMessage[2]);
			if (timeStampFromMessage > timeStampForNodeNo) {
				//adding the link state advertisement node numbers to map.
				for (int i = 3; i < linkStateSplitMessage.length; i++) {
					
					if (linkStateAdvMap.get(Integer.parseInt(linkStateSplitMessage[i])) != null) {
						Set<Integer> linkStateNodeNumberList = 
							linkStateAdvMap.get(Integer.parseInt(linkStateSplitMessage[i]));
						linkStateNodeNumberList.add(linkStateSenderNodeNumber);
					} else {
						Set<Integer> linkStateNodeNumberList = new HashSet<Integer>();
						linkStateNodeNumberList.add(linkStateSenderNodeNumber);
						linkStateAdvMap.put(Integer.parseInt(linkStateSplitMessage[i]), linkStateNodeNumberList);
					}
				}
				
				timeStampMap.put(linkStateSenderNodeNumber, timeStampFromMessage);
				linkStateTimer.put(linkStateSenderNodeNumber, time);
				
				//write it to output file without changing the message
				writeMessageToOutputFile(message);
				
			}
			
		}
		
	}
	
	
	private Map<Integer, String> computeShortestPath(int shortestPathForNodeNumber) {
		//storing key as destination node and value same as source routing
		Map<Integer, String> shortestPathMap = new HashMap<Integer, String>();
		if (linkStateAdvMap.size() > 0) {
			Set<Integer> allNodes = new HashSet<Integer>();
			
			Set<Entry<Integer, Set<Integer>>> entrySet = linkStateAdvMap.entrySet();
			for (Entry<Integer, Set<Integer>> entry : entrySet) {
				allNodes.add(entry.getKey());
				for (Integer node : entry.getValue()) {
					allNodes.add(node);
				}
			}
			Map<Integer, Integer> distanceMap = new HashMap<Integer, Integer>();
			distanceMap.put(shortestPathForNodeNumber, 0);
					
			Map<Integer, Integer> previousNodeMap = new HashMap<Integer, Integer>();
			List<Integer> visitedNodes = new ArrayList<Integer>();
			
			while(allNodes.size() != visitedNodes.size()) {
				List<Integer> notVisitednodes = new ArrayList<Integer>();
				for(int node : allNodes) {
					if (!visitedNodes.contains(node)) {
						notVisitednodes.add(node);
					}
				}
				
				int smallestDistanceNode = getSmallestDistance(distanceMap, notVisitednodes);
				if (smallestDistanceNode != 100000) {
					int distance = distanceMap.get(smallestDistanceNode);
					visitedNodes.add(smallestDistanceNode);
					Set<Integer> neighboursOfNode = linkStateAdvMap.get(smallestDistanceNode);
					if (neighboursOfNode != null) {
						for (Integer neighNode : neighboursOfNode) {
							int currentDistance = distance + 1;
							//a default value
							int distanceFromMap = 100000;
							if (distanceMap.get(neighNode) != null) {
								distanceFromMap = distanceMap.get(neighNode);
							}
							if (currentDistance < distanceFromMap) {
								distanceMap.put(neighNode, currentDistance);
								previousNodeMap.put(neighNode, smallestDistanceNode);
							}
							
						}
					}
				} else {
					break;
				}
				
			}
			
			
			for(int node : allNodes) {
				if (node != shortestPathForNodeNumber) {
					
//					String shortestPath = previousNodeMap.get(node) + "," + node;
//					String path = previousNodeMap.get(node) + " --> " + node;
//					if (previousNodeMap.get(node) != null) {
//						
//						int newNode = previousNodeMap.get(node);
//						shortestPath = newNode + "," + shortestPath;
//						if (previousNodeMap.get(newNode) != null) {
//							path = nodeNumber + "---->" + path;
//							while (previousNodeMap.get(newNode) != nodeNumber) {
//								newNode = previousNodeMap.get(newNode);
//								path = newNode + "---->" + path;
//							}
//						}
//						
//						System.out.println("From node number : " + nodeNumber + 
//								" shortest path of " + node + " is " + path);
//						
//						//adding it to shortest path map
//						shortestPathMap.put(node, "");
					

						String path = "";
						int previousNode = -1;
						if (previousNodeMap.get(node) != null) {
							previousNode = previousNodeMap.get(node);
						}
						while(previousNode != -1) {
							if (path.length() == 0){
								if(previousNode != shortestPathForNodeNumber) {
									path = previousNode + "";
								} else {
									path = shortestPathForNodeNumber + "";
								}
							} else {
								if(previousNode != shortestPathForNodeNumber) {
									path = previousNode + "," + path;
								}
							}
							if (previousNodeMap.get(previousNode) != null) {
								previousNode = previousNodeMap.get(previousNode);
							} else {
								previousNode = -1;
							}
						}
						
						shortestPathMap.put(node, path);
					
					
					
				}
				
			}
			
			
		}
		
		return shortestPathMap;
		
			
			
			
	}
		
		
		
	private int getSmallestDistance(Map<Integer, Integer> distanceMap, List<Integer> notVisitedNodeList) {
		
		
		int smallestDistanceNode = 100000;
		//maximum distance same as null
		int smallestDistance = 100000;
		
		for (int node : notVisitedNodeList) {
			
			if (distanceMap.get(node) != null) {
				int distance = distanceMap.get(node);
				if (distance < smallestDistance) {
					smallestDistance = distance;
					smallestDistanceNode = node;
				}
			}
			
		}
		
		
		return smallestDistanceNode;
	}
	
	
	private void validateLinkStateAdvertisement(int time) {
		List<Integer> removeLinkStateTimeNodeList = new ArrayList<Integer>(); 
		Set<Entry<Integer, Integer>> linkValidateSet = linkStateTimer.entrySet();
		for (Entry<Integer, Integer> entry : linkValidateSet) {
			
			int nodeNumber = entry.getKey();
			int previousTime = entry.getValue();
			
			int checkTime = previousTime + 30;
			
			if (checkTime < time) {
				
				//remove link state advertisement
				List<Integer> deleteNodeNumberList = new ArrayList<Integer>();
				deleteNodeNumberList.add(nodeNumber);
				
				Set<Entry<Integer, Set<Integer>>> linkStateSet = linkStateAdvMap.entrySet();
				for (Entry<Integer, Set<Integer>> deleteEntrySet : linkStateSet) {
					
					if(deleteEntrySet.getKey() != nodeNumber) {
						
						if (deleteEntrySet.getValue().contains(nodeNumber)) {
							if (deleteEntrySet.getValue().size() == 1) {
								deleteNodeNumberList.add(deleteEntrySet.getKey());
							} else {
								Set<Integer> newValueList = new HashSet<Integer>();
								for(Integer notDeletedNode : deleteEntrySet.getValue()) {
									
									if (notDeletedNode != nodeNumber) {
										newValueList.add(notDeletedNode);
									}
									
								}
								linkStateAdvMap.put(deleteEntrySet.getKey(), newValueList);
							}
						}
						
					}
				}
				
				for(Integer deleteNode : deleteNodeNumberList) {
					linkStateAdvMap.remove(deleteNode);
//					System.out.println("node : "+ deleteNode + " has been deleted");
				}
				
				removeLinkStateTimeNodeList.add(nodeNumber);
				
				List<Integer> newIncomingNodeNumbers = new ArrayList<Integer>();
				for(int helloNodeNumber : incomingNodeNumbers) {
					if (helloNodeNumber != nodeNumber) {
						newIncomingNodeNumbers.add(helloNodeNumber);
					}
				}
				incomingNodeNumbers = new ArrayList<Integer>();
				incomingNodeNumbers.addAll(newIncomingNodeNumbers);
			}
			
			
		}
		
		for(int deleteLinkStateTimer : removeLinkStateTimeNodeList) {
			
			linkStateTimer.remove(deleteLinkStateTimer);
			
		}
	}
	
	
	private void sendJoinMessages(int receiverNodeOfJoin, int multicastNodeOfJoin) throws Exception {
		
		String joinMessage = "join " + receiverNodeOfJoin + " " + multicastNodeOfJoin;
		//computing shortest path from Source to Receiver
		Map<Integer, String> shortestPathMap = computeShortestPath(multicastNodeOfJoin);
		//Getting the shortest path for the receiver from the source
		String shortestPathForJoin = shortestPathMap.get(nodeNumber);
		
		
		if (shortestPathForJoin != null && shortestPathForJoin.length() > 0) {
			if (shortestPathForJoin.length() == 1) {
				if (!shortestPathForJoin.equals(multicastNodeOfJoin + "")) {
					//getting the intermediate parent that is the last element
					int intermediateParent = Integer.parseInt(shortestPathForJoin.trim());
					joinMessage = joinMessage + " " + intermediateParent;
					//getting the shortest path from current node to intermediate parent to send join message
					String intermediateShortestPath = receiverShortestPath.get(intermediateParent);
					if (intermediateShortestPath != null && intermediateShortestPath.length() > 0) {
						if (!intermediateShortestPath.equals(nodeNumber + "")) {
							String replacedString = intermediateShortestPath.trim().replaceAll(",", " ");
							joinMessage = joinMessage + " " + replacedString.trim();
							if (replacedString.trim().length() > 0) {
								writeMessageToOutputFile(joinMessage);
							}
						} else {
							writeMessageToOutputFile(joinMessage);
						}
						
					}
					//if the the source is my immediate parent add nothing
					//else the source is one hop away
//					joinMessage = joinMessage + " " + shortestPathForJoin;
				} else {
					joinMessage = joinMessage + " " + multicastNodeOfJoin;
					//if the shortest path gives the value as sender node number
					//getting the shortest path from current node to intermediate parent to send join message
					String intermediateShortestPath = receiverShortestPath.get(multicastNodeOfJoin);
					if (intermediateShortestPath != null && intermediateShortestPath.length() > 0) {
						if (!intermediateShortestPath.equals(nodeNumber + "")) {
							String replacedString = intermediateShortestPath.trim().replaceAll(",", " ");
							joinMessage = joinMessage + " " + replacedString.trim();
							if (replacedString.trim().length() > 0) {
								writeMessageToOutputFile(joinMessage);
							}
						} else {
							writeMessageToOutputFile(joinMessage);
						}
					}
				}
			} else {
				String[] splitPath = shortestPathForJoin.trim().split(",");
				//getting the intermediate parent that is the last element
				int intermediateParent = Integer.parseInt(splitPath[splitPath.length - 1]);
				joinMessage = joinMessage + " " + intermediateParent;
				//getting the shortest path from current node to intermediate parent to send join message
				String intermediateShortestPath = receiverShortestPath.get(intermediateParent);
				if (intermediateShortestPath != null && intermediateShortestPath.length() > 0) {
					if (!intermediateShortestPath.equals(nodeNumber + "")) {
						String replacedString = intermediateShortestPath.trim().replaceAll(",", " ");
						joinMessage = joinMessage + " " + replacedString.trim();
						if (replacedString.trim().length() > 0) {
							writeMessageToOutputFile(joinMessage);
						}
					} else {
						writeMessageToOutputFile(joinMessage);
					}
				}
				
			}
			
			
		}
		
	}
	
	
	private void processJoinMessages(String message, int time) throws Exception {
		
		String[] joinSplitMessage = message.split(" ");
		if (joinSplitMessage.length == 4) {
			String firstNodeOfPath = joinSplitMessage[3];
			int parentOfTheChildReceiver = Integer.parseInt(firstNodeOfPath);
			if (nodeNumber == parentOfTheChildReceiver) {
				int receiverOfJoinMessage = Integer.parseInt(joinSplitMessage[1]);
				int sourceOfJoinMessage = Integer.parseInt(joinSplitMessage[2]);
				
				//add child
				receiverChildTimer.put(receiverOfJoinMessage, time);
				if(senderReceiverList.get(sourceOfJoinMessage) != null) {
					Set<Integer> receiverList = senderReceiverList.get(sourceOfJoinMessage);
					receiverList.add(receiverOfJoinMessage);
				} else {
					Set<Integer> receiverList = new HashSet<Integer>();
					receiverList.add(receiverOfJoinMessage);
					senderReceiverList.put(sourceOfJoinMessage, receiverList);
				}
				if (parentOfTheChildReceiver != sourceOfJoinMessage) {
					// if the intermediate parent is not source
					receiverShortestPath = computeShortestPath(nodeNumber);
					sendJoinMessages(receiverOfJoinMessage, sourceOfJoinMessage);
				}
			}
			
		} else if (joinSplitMessage.length > 4) {
			//if length > 4 then intermediate node exist
			String firstNodeOfPath = joinSplitMessage[4];
			if (nodeNumber == Integer.parseInt(firstNodeOfPath)) {
				String joinMessageToForward = "";
				for(int i = 0; i < joinSplitMessage.length; i++) {
					if (i != 4) {
						joinMessageToForward = joinMessageToForward + joinSplitMessage[i] + " ";
					}
				}
				writeMessageToOutputFile(joinMessageToForward.trim());
			}
		}
		
	}
	
	private void sendMulticastDataMessage() throws Exception {
		String multicastMessage = "data " + nodeNumber + " " + nodeNumber + " " + multicastSenderString;
		writeMessageToOutputFile(multicastMessage);
	}
	
	
	private void processMulticastDataMessages(String message) throws Exception {
		String[] senderDataMessage = message.trim().split(" ");
		//computing shortest path to check whether the message is from the parent
		int multicastSenderNode = Integer.parseInt(senderDataMessage[2]);
		int parentNodeFromDataMessage = Integer.parseInt(senderDataMessage[1]);
		
		if (parentNodeFromDataMessage != nodeNumber) {
			
			Map<Integer, String> shortestPathMap = computeShortestPath(multicastSenderNode);
			//Getting the shortest path for the receiver from the source
			String shortestPathForJoin = shortestPathMap.get(nodeNumber);
			
			if (shortestPathForJoin != null && shortestPathForJoin.length() > 0) {
				String[] splitPath = shortestPathForJoin.trim().split(",");
				//getting the intermediate parent that is the last element
				int intermediateParent = Integer.parseInt(splitPath[splitPath.length - 1]);
				if (parentNodeFromDataMessage == intermediateParent) {
						
					if (receiver) {
						
						if (multicastSenderNodeNo == multicastSenderNode) {
							String multicastMessage = "";
							for(int i =3; i < senderDataMessage.length; i++) {
								multicastMessage = multicastMessage + " " + senderDataMessage[i];
							}
							writeMessageToReceiverOutputFile(multicastMessage.trim());
						}
						
					} else {
						
						if (senderReceiverList.size() > 0) {
							if (senderReceiverList.containsKey(multicastSenderNode)) {
								
								String newDataMessage = "";
								for(int i = 0; i<senderDataMessage.length; i++) {
									if (i != 1) {
										newDataMessage = newDataMessage + " " + senderDataMessage[i];
									} else {
										newDataMessage = newDataMessage + " " + nodeNumber;
									}
								}
								writeMessageToOutputFile(newDataMessage.trim());
								
							}
							
						}
						
					}
						
				}
				
			}
			
		}
		
		
		
		
	}
	
	private void writeMessageToReceiverOutputFile(String message) throws Exception {
		
		File receiverOutputFile = new File(nodeNumber + "_received_from_" + multicastSenderNodeNo + ".txt");
		if(receiverOutputFile.exists()) {
			
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(nodeNumber + "_received_from_" 
					+ multicastSenderNodeNo + ".txt", true));
			bufferedWriter.write(message + "\n");
			bufferedWriter.close();
			
		}
		
	}
	
	
	private void validateReceiverJoinMessage(int time) {
		List<Integer> removeReceiverTimerNodeList = new ArrayList<Integer>(); 
		Set<Entry<Integer, Integer>> receiverChildTimerSet = receiverChildTimer.entrySet();
		for (Entry<Integer, Integer> entry : receiverChildTimerSet) {
			
			int nodeNumber = entry.getKey();
			int previousTime = entry.getValue();
			
			int checkTime = previousTime + 20;
			
			if (checkTime < time) {
				
				//remove receiver child timer advertisement
				List<Integer> deleteNodeNumberList = new ArrayList<Integer>();
				
				Set<Entry<Integer, Set<Integer>>> senderReceiverSet = senderReceiverList.entrySet();
				for (Entry<Integer, Set<Integer>> deleteEntrySet : senderReceiverSet) {
					
					if(deleteEntrySet.getKey() != nodeNumber) {
						
						if (deleteEntrySet.getValue().contains(nodeNumber)) {
							if (deleteEntrySet.getValue().size() == 1) {
								deleteNodeNumberList.add(deleteEntrySet.getKey());
							} else {
								Set<Integer> newValueList = new HashSet<Integer>();
								for(Integer notDeletedNode : deleteEntrySet.getValue()) {
									
									if (notDeletedNode != nodeNumber) {
										newValueList.add(notDeletedNode);
									}
									
								}
								senderReceiverList.put(deleteEntrySet.getKey(), newValueList);
							}
						}
						
					}
				}
				
				for(Integer deleteNode : deleteNodeNumberList) {
					senderReceiverList.remove(deleteNode);
//					System.out.println("node : "+ deleteNode + " has been deleted for receiver");
				}
				
				removeReceiverTimerNodeList.add(nodeNumber);
				
			}
			
		}
		
		for(int deleteLinkStateTimer : removeReceiverTimerNodeList) {
			
			receiverChildTimer.remove(deleteLinkStateTimer);
			
		}
	}

}


 