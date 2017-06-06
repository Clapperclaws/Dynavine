import java.util.ArrayList;

/*Generic Graph that will be used to represent virtual, IP, and OTN graphs*/
public class Graph {
	// Every node in the graph is associated with a list of endpoints
	private ArrayList<EndPoint> adjList[];
	
	private int[] ports;
	private int[] portCapacity;
	
	//Default constructor
	public Graph(int N){	
		adjList = new ArrayList[N];
		for(int i=0;i<N;i++){
			adjList[i] = new ArrayList<EndPoint>();
		}
		ports = new int[N];
		portCapacity = new int[N];
	}
	
	//Copy Constructor
	public Graph(Graph g){
		adjList = new ArrayList[g.adjList.length];
		for(int i=0;i<g.adjList.length;i++){
			adjList[i] = new ArrayList<EndPoint>();
			for(int j=0;j<g.adjList[i].size();j++){
				EndPoint p = new EndPoint(g.adjList[i].get(j));
				adjList[i].add(p);
			}
		}
	}

	//Add a single end point to the list of end points for a given node.
	public void addEndPoint(int nodeId, EndPoint endPnt){	
		adjList[nodeId].add(endPnt);
	}
	
	//Add a list of end points for a given node
	public void addEndPointList(int nodeId, ArrayList<EndPoint> endPnts){
		adjList[nodeId].addAll(endPnts);
	}
	
	//Get all end points of a given node
	public ArrayList<EndPoint> getAllEndPoints(int nodeId){
		return adjList[nodeId];
	}
	
	//Get the bandwidth of an incident link
	public int getBW(int source, int destination){
		
		ArrayList<EndPoint> endPoints = adjList[source];
		for(int i=0;i<endPoints.size();i++){
			if(endPoints.get(i).getNodeId() == destination)
				return endPoints.get(i).getBw();
		}
		return -1;
	}
	
	//Get the Weight of an incident link
	public int getCost(int source, int destination){
		
		ArrayList<EndPoint> endPoints = adjList[source];
		for(int i=0;i<endPoints.size();i++){
			if(endPoints.get(i).getNodeId() == destination)
				return endPoints.get(i).getCost();
		}
		return -1;
	}
	
	//Get the complete adjacency list
	public ArrayList<EndPoint>[] getAdjList(){
		return adjList;
	}
	
	// Returns the number of nodes in the graph.
	public int nodeCount() {
	    return adjList.length;
	}
	
	public int[] getPorts() {
		return ports;
	}

	public void setPorts(int[] ports) {
		this.ports = ports;
	}

	public int[] getPortCapacity() {
		return portCapacity;
	}

	public void setPortCapacity(int[] portCapacity) {
		this.portCapacity = portCapacity;
	}

	//Print the complete Adjacency List
	public String toString(){
		String content = "Adjacency List:\n";
		for(int i=0;i<adjList.length;i++)
			content += "- Node "+i+" is attached to: \n"+adjList[i].toString()+"\n";
		return content;
	}
	
}
