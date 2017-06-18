import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CreateInitialSolution {

	Graph collapsedGraph;
	int ipNodesSize  = 0;
	int otnNodesSize = 0;
	OverlayMapping ipOtn;
	
	public CreateInitialSolution(Graph ip, Graph otn, OverlayMapping ipOtn){
	
		ipNodesSize = ip.getAdjList().size();
		otnNodesSize = otn.getAdjList().size();
		this.ipOtn = ipOtn;
		
		//Create Collapsed Graph
		collapsedGraph = new Graph(ip,otn);
		//Create new IP Links that connects the IP node to the OTN node - Cost of 1 and unlimited capacity
		for(int i=0;i<ip.getAdjList().size();i++){
			collapsedGraph.addEndPoint(i,new EndPoint(ip.getAdjList().size()+ipOtn.getNodeMapping(i),1,Integer.MAX_VALUE,EndPoint.type.otn,0));
			collapsedGraph.addEndPoint(ip.getAdjList().size()+ipOtn.getNodeMapping(i),new EndPoint(i,1,Integer.MAX_VALUE,EndPoint.type.ip,0));		
		}
		System.out.println("Collapsed Graph: \n"+collapsedGraph);
	}

	
	/*
	 * This function returns an initial solution; 
	 * it takes as input the Virtual graph, the arraylist of location constraints, and the number of iterations k
	 */
	public Solutions getInitialSolution(Graph vn, ArrayList<Integer>[] locationConstraints, int k){
		//1- Initialize the list of solutions that will store the solution generated at the end of every run
		ArrayList<Solutions> listSol = new ArrayList<Solutions>();
		
		int iter = 0;
		do{
			//1- Get a new list order
			ArrayList<Integer> listOrder = getListOrder(vn);
			
			//2- Execute the function that performs the VN Nodes & Links embedding
			Solutions sol = execute(vn, locationConstraints, listOrder);
			if(sol != null)
				listSol.add(sol);
			iter ++;
		}while(iter <k);
		
		//3- Find the solution with the lowest cost
		//return execute(vn, locationConstraints, listOrder);
		//4- Return lowest cost solution
		//return sol;
		
		return null;
	}

	public Solutions execute(Graph vn,ArrayList<Integer>[] locationConstraints, ArrayList<Integer> order){
		
		Solutions sol = new Solutions(vn.getAdjList().size(),ipNodesSize);
			
		ArrayList<Integer> settledNodes = new ArrayList<Integer>();
		Random gn = new Random();
		
		// This counter will be used to set the ID of newly create metanodes
		int counter = otnNodesSize+ipNodesSize;
		
		System.out.println(order);
		//1- For each node in N	
		for(int i=0;i<order.size();i++){
			counter = otnNodesSize + ipNodesSize; // Restart Counter
			
			int startNode = order.get(i); //Get the index of the first node in the list
			if(settledNodes.contains(startNode))
				continue;
			else
				settledNodes.add(startNode);
			
			System.out.println("Start Node: "+startNode);
			
			//Select Location Constraint Randomly 
			int sourceLoc = gn.nextInt(locationConstraints[startNode].size());
			while(sol.vnIp.isOccupied(sourceLoc)) // Make Sure Node is not Occupied
				sourceLoc = gn.nextInt(locationConstraints[startNode].size());
			System.out.println("Source Node "+sourceLoc);
			
			//3- Create Metanodes for source's neighbors
			ArrayList<Integer> metaNodes = new ArrayList<Integer>();
			int maxLinkCap = 0;
			for(int j=0;j<vn.getAdjList().get(startNode).size();j++){	
				
				//Add to list of Meta Nodes
				EndPoint ep = new EndPoint(counter,1,1,EndPoint.type.meta,0);
				metaNodes.add(counter);
				
				if(vn.getAdjList().get(startNode).get(j).getBw() > maxLinkCap)
					maxLinkCap = vn.getAdjList().get(startNode).get(j).getBw();
			
				//Create an Adjacency vector for the meta-node of each neighboring node
				collapsedGraph.addEndPointList(counter, new ArrayList<EndPoint>());
			
				//a- Check if the node is settled or not
				if(!settledNodes.contains(vn.getAdjList().get(startNode).get(j).getNodeId())){
					//Add the adjacent nodes to the list of settled nodes
					settledNodes.add(vn.getAdjList().get(startNode).get(j).getNodeId()); 
					
				
					//Add every IP node in the location constraint to the adjacency list of the MetaNode
					for(int k=0;k<locationConstraints[vn.getAdjList().get(startNode).get(j).getNodeId()].size();k++){
						//collapsedGraph.get(counter).add(new EndPoint(locationConstraints[vn.getAdjList()[nodeIndex].get(j).getNodeId()].get(k),1,1,EndPoint.type.ip));
						collapsedGraph.addEndPoint(locationConstraints[vn.getAdjList().get(startNode).get(j).getNodeId()].get(k),ep);					
					}
				}else{//Connect Meta Node to the IP Node hosting this VN
					collapsedGraph.addEndPoint(sol.vnIp.nodeMapping[vn.getAdjList().get(startNode).get(j).getNodeId()],ep);									
				}
				counter++;
			}
			
			//4- Connect all Meta Nodes to a single Sink Node
			int sink = counter;
			System.out.println("Sink Node "+sink);
			collapsedGraph.addEndPointList(counter, new ArrayList<EndPoint>());
			
			EndPoint sinkMetaNode = new EndPoint(counter,1,1,EndPoint.type.meta,0);
			for(int j=0;j<metaNodes.size();j++)
				collapsedGraph.addEndPoint(metaNodes.get(j),sinkMetaNode);
			
			metaNodes.add(counter);
			counter++;
			
			//5- Find IP Nodes that are connected to more than one Meta Node
			for(int k=0;k<ipNodesSize;k++){
				ArrayList<EndPoint> adjMetaNodes = collapsedGraph.getAdjNodesByType(k, EndPoint.type.meta);
				if(adjMetaNodes.size() > 1){					
					//Remove Meta Nodes from the Adjacency List of this node
					collapsedGraph.getAllEndPoints(k).removeAll(adjMetaNodes);
					
					//Create an Adjacency vector for a new meta-node 
					collapsedGraph.addEndPointList(counter, new ArrayList<EndPoint>());
					metaNodes.add(counter);
					
					//Connect the new MetaNode to the existing MetaNodes
					for(int j=0;j<adjMetaNodes.size();j++)
						collapsedGraph.addEndPoint(counter,new EndPoint(adjMetaNodes.get(j).getNodeId(),1, 1,EndPoint.type.meta,0));
									
					//Add New Meta Node to the Adjacency List
					collapsedGraph.addEndPoint(k,new EndPoint(counter, 1, 1,EndPoint.type.meta,0));
					counter++;
				}			
			}
			
			System.out.println("Collapsed Graph with Meta Nodes \n"+collapsedGraph);
			
			//4- Call EK
			OverlayMapping embdSol = MaxFlow(collapsedGraph, sourceLoc, sink, maxLinkCap,(vn.getAdjList().get(startNode).size()+1));			
			aggregateSolution(embdSol, sol);
			
			//7- Delete All MetaNodes
			for(int j=0;j<ipNodesSize;j++){				
				ArrayList<EndPoint> adjMetaNodes = collapsedGraph.getAdjNodesByType(j, EndPoint.type.meta);
				collapsedGraph.getAllEndPoints(j).removeAll(adjMetaNodes);			
			}
			
			//8- Remove All Meta Nodes
			List<ArrayList<EndPoint>> subList = collapsedGraph.getAdjList().subList((ipNodesSize+otnNodesSize),collapsedGraph.getNodeCount());
			collapsedGraph.getAdjList().removeAll(subList);
			
			System.out.println("Collapsed Graph after removal of MetaNodes:\n"+collapsedGraph);
			
			if(settledNodes.size() == vn.getAdjList().size())
				break;
		}
		
		System.out.println("Solution :"+sol);
		return sol;
	}
	
	//This function iteratively aggregates the final Solution after every iteration
	public void aggregateSolution(OverlayMapping embdSol, Solutions sol){
		sol.vnIp.incrementNodeEmbed(embdSol);
		
		//5- Find Newly Created IP Links 
		 Iterator it = embdSol.linkMapping.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        Tuple t = (Tuple)pair.getKey();
	        ArrayList<Tuple> linkMapping = (ArrayList<Tuple>)pair.getValue();
	       //Examine First Link
	        int src = linkMapping.get(0).getSource();
	        int dst = linkMapping.get(0).getDestination();
	        //Case of New IP Link
	        if(dst>=ipNodesSize && dst < (otnNodesSize+ipNodesSize)){
	        	int ipSrc = src;
	        	int ipDst = linkMapping.get(linkMapping.size()-1).getDestination();
	        	sol.ipOtn.nodeMapping[src] = dst; //Add the OTN dst as the Node embedding of the IP src
	        	sol.ipOtn.nodeMapping[ipDst] = linkMapping.get(linkMapping.size()-1).getSource(); //Add the OTN src as the Node embedding of the IP dst
	        	ArrayList<Tuple> linkEmbedding = linkMapping;
	        	linkEmbedding.remove(ipSrc);
	        	linkEmbedding.remove(ipDst);
	        	
	        	//Create Tuple & Add Link Embedding Solution
	        	int tupleOrder = collapsedGraph.findTupleOrder(ipSrc, ipDst);
	        	Tuple newTup = new Tuple(ipSrc,ipDst,tupleOrder);
	        	sol.ipOtn.linkMapping.put(newTup, linkEmbedding);
	        	
	        	//Add Tuple to list of newly created Links
		        sol.newIpLinks.add(newTup);
		        
		        //Get Bandwidth Capacity of new IP Link
		        int newIPLinkCap = Math.min(collapsedGraph.getPortCapacity()[ipSrc], collapsedGraph.getPortCapacity()[ipDst]);
		        
	        	//Add ipSrc as neighbor of ipDst - Figure out the order
		        collapsedGraph.addEndPoint(ipSrc,new EndPoint(ipDst,1,newIPLinkCap,EndPoint.type.ip,tupleOrder));
	        	
		        //Add ipDst as neighbor of ipSrc - Figure out the order
		        collapsedGraph.addEndPoint(ipDst,new EndPoint(ipSrc,1,newIPLinkCap,EndPoint.type.ip,tupleOrder));
	        }
	        //Case of Existing IP Links
	        sol.vnIp.linkMapping.put(t, linkMapping);
	    }
	}
	
	//Returns a shuffled order of VN nodes
	public ArrayList<Integer> getListOrder(Graph vn){
		
		ArrayList<Integer> order = new ArrayList<Integer>();
		for(int i=0;i<vn.getAdjList().size();i++)
			order.add(i);
		Collections.shuffle(order);
		return order;
	}
	
	
	/*
	 * Run Max Flow to get the Link Embedding Solution
	 * This function received as input the collapsed graph, the source & sink meta nodes, the maxLinkCap,
	 * and the size of the overlay mapping solution "N"
	 */
	public OverlayMapping MaxFlow(Graph collapsedGraph,int source, int sink, int maxLinkCap, int N){
		OverlayMapping embdSol = new OverlayMapping(N);
		
		return embdSol;
	}
	
}
