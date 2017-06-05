import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Driver {

	private static ArrayList<Tuple> ipLinks;  // List of IP Links
	private static ArrayList<Tuple> otnLinks; // List of OTN Links
	private static ArrayList<Tuple> vnLinks;  // List of VN Links
	private static HashMap<Integer, ArrayList<Integer>> locationConstraints;
	
	
	public static void main(String [] args) throws IOException{
		
		//1-Initialize OTN Graph
		otnLinks = new ArrayList<Tuple>();
		Graph otn = ReadFromFile("./src/Dataset/OTN", EndPoint.type.otn,otnLinks);
		System.out.println("******* OTN Graph ******* \n"+otn); // Print graph for testing
	
		//2-Initialize IP Graph & populate ipLinks
		ipLinks = new ArrayList<Tuple>();
		Graph ip  = ReadFromFile("./src/Dataset/IP", EndPoint.type.ip,ipLinks);
		System.out.println("******* IP Graph ******* \n"+ip); // Print graph for testing
		
		//3- Map IP to OTN
		OverlayMapping ipOtn = getInitialOtnIP(ip, otn);
		
		//Print IP to OTN Overlay mapping
		System.out.println("******* IP to OTN Overlay Mapping: ******* \n"+ipOtn);
		
		//4- Initialize VN Graph
		vnLinks = new ArrayList<Tuple>();
		Graph vn  = ReadFromFile("./src/Dataset/VN", EndPoint.type.virtual,vnLinks);
		System.out.println("******* VN Graph ******* \n"+vn); // Print graph for testing	
		
		//5- Initialize Location Constraints (Set all IP nodes as candidates for each VN node).
		locationConstraints = new HashMap<Integer, ArrayList<Integer>>();
		for(int i=0;i<vn.getAdjList().length;i++){
			ArrayList<Integer> lc = new ArrayList<Integer>();
			for (int j=0;j<ip.getAdjList().length;j++)
				lc.add(j);
			locationConstraints.put(i, lc);
		}
	}
	
	public static OverlayMapping getInitialOtnIP(Graph ip, Graph otn){
		OverlayMapping ipOtn = new OverlayMapping(ip.getAdjList().length);
		
		//Assign each IP node index i to OTN node index i
		for(int i=0;i<ipOtn.nodeMapping.length;i++){
			ipOtn.nodeMapping[i] = i;
		}
		//Routing of IP Links over OTN links using Shortest Path
		Dijkstra djAlgo = new Dijkstra(otn);
		for(int i=0;i<ipLinks.size();i++){
			int source = ipOtn.nodeMapping[ipLinks.get(i).getSource()];
			int destination = ipOtn.nodeMapping[ipLinks.get(i).getDestination()];
			djAlgo.execute(source);
			ArrayList<Tuple> nodePath = djAlgo.getPath(destination);
			ipOtn.linkMapping.put(ipLinks.get(i), nodePath);
		}
		return ipOtn;
	}
	
	/*This function reads a graph from file*/
	public static Graph ReadFromFile(String filename, EndPoint.type graphType, ArrayList<Tuple> links) throws IOException{
		Graph g;
		BufferedReader br = new BufferedReader(new FileReader(filename));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        g = new Graph(Integer.parseInt(line)); //Initialize Graph with the number of Nodes
	        int counter = 0;
	        while (line != null) {
	            line = br.readLine();
	            if(line != null){
	            	//Split the line to get 0: index of the first node; 1: index of the second node; 2: bandwidth
	            	String[] splitLine = line.split(",");
	            	//Create Tuple for this link
	            	links.add(new Tuple(0,Integer.parseInt(splitLine[0]),Integer.parseInt(splitLine[1])));
	            	//Create two end points
	            	EndPoint ep1 = new EndPoint(Integer.parseInt(splitLine[1]),Integer.parseInt(splitLine[2]),graphType,0);
	            	g.addEndPoint(Integer.parseInt(splitLine[0]), ep1);
	               	EndPoint ep2 = new EndPoint(Integer.parseInt(splitLine[0]),Integer.parseInt(splitLine[2]),graphType,0);
	            	g.addEndPoint(Integer.parseInt(splitLine[1]), ep2);
	 
	            }
	        }       
	    } finally {
	    	if (br != null)
	    		br.close();
	    }
	    return g;
	}
	
	
}
