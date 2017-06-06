
public class CreateInitialSolution {

	Graph collapsedGraph;
	
	public CreateInitialSolution(Graph ip, Graph otn, OverlayMapping ipOtn){
		//Create Collapsed Graph
		collapsedGraph = new Graph(ip.getAdjList().length+otn.getAdjList().length);
		//Begin by adding the IP Links
		for(int i=0;i<ip.getAdjList().length;i++){
			for(int j=0;j<ip.getAdjList()[i].size();j++){
				collapsedGraph.addEndPoint(i, new EndPoint(ip.getAdjList()[i].get(j)));
			}
			//Create new IP Links that connects the IP node to the OTN node - Cost of 1 and unlimited capacity
			collapsedGraph.addEndPoint(i, new EndPoint(ipOtn.getNodeMapping(i),1,Integer.MAX_VALUE,EndPoint.type.otn));
			collapsedGraph.addEndPoint(ip.getAdjList().length+ipOtn.getNodeMapping(i), new EndPoint(i,1,Integer.MAX_VALUE,EndPoint.type.ip));
			
		}
		//Continue by adding the OTN Links
		for(int i=0;i<otn.getAdjList().length;i++){
			for(int j=0;j<otn.getAdjList()[i].size();j++){
				EndPoint ep = new EndPoint(otn.getAdjList()[i].get(j));
				ep.setCost(Integer.MAX_VALUE); // Set the Cost of OTN Links to a very high value
				collapsedGraph.addEndPoint(ip.getAdjList().length+i, new EndPoint(ep));
			}
		}
		System.out.println(collapsedGraph);
	}
	
	public Solutions execute(Graph vn){
		Solutions sol = new Solutions(null, null, null);
		
		//1- For each node in N
		
		//2- Create Metanodes for N and its neighbors...
		
		
		return sol;
	}
	
	
}
