

/*Each node in the graph has an array of end points. Each element in the array represents an incident link;
 *  i.e., the index and type of the node at the other end of the link and the bandwidth (demand or capacity) of that link. */
public class EndPoint {

	public enum type{ virtual, otn, meta, ip, none};

	private int nodeId; // Index of the node at the other end of the link
	private int bw; //Bandwidth (demand/capacity) of the link
	private type t; //Type of the node at the other end of the link
	private int cost; // Cost/weight on the link

	//Default Constructor
	public EndPoint(){
		nodeId = -1;
		bw = 0;
		cost = 0;
		t = type.none;	
	}
	
	//Initializing Constructor
	public EndPoint(int nodeId, int bw, type t, int cost){
		this.nodeId = nodeId;
		this.bw     = bw;
		this.t   = t;
		this.cost = cost;
	}
	
	//Copy Constructor
	public EndPoint(EndPoint p){
		this.nodeId = p.nodeId;
		this.bw     = p.bw;
		this.t      = p.t;
		this.cost   = p.cost;
	}
	
	//Return the index of the node at the other end of the link
	public int getNodeId() {
		return nodeId;
	}
	
	//Set the index of the node at the other end of the link
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	
	//Get the bandwidth (demand/capacity) of the incident link
	public int getBw() {
		return bw;
	}

	//Set the bandwidth (demand/capacity) of the incident link
	public void setBw(int bw) {
		this.bw = bw;
	}
	
	//Get the type of the node at the other end of the link
	public type getT() {
		return t;
	}
	
	//Set the type of the node at the other end of the link
	public void setT(type t) {
		this.t = t;
	}
	
	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	//Print the attributes of the incident link
	public String toString(){
		return "Node "+nodeId+" of type "+t+", BW = "+bw+"\n";
	}
}
