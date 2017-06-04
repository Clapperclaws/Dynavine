import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Driver {

	public static void main(String [] args) throws IOException{
		
		//Initialize OTN Graph
		Graph otn = ReadFromFile("./src/Dataset/OTN", EndPoint.type.otn);
		System.out.println("******* OTN Graph ******* \n"+otn); // Print graph for testing
	
		//Initialize IP Graph
		Graph ip  = ReadFromFile("./src/Dataset/IP", EndPoint.type.ip);
		System.out.println("******* IP Graph ******* \n"+ip); // Print graph for testing
	
		
	}
	
	/*This function reads a graph from file*/
	public static Graph ReadFromFile(String filename, EndPoint.type graphType) throws IOException{
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
