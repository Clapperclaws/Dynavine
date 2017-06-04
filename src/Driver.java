import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Driver {

	public static void main(String [] args){
		
		//Initialize OTN Graph
		Graph otn;
		try {
			otn = ReadFromFile("./src/Dataset/OTN", EndPoint.type.otn);
			System.out.println("******* OTN Graph ******* \n"+otn); // Print graph for testing
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/*This function reads a graph from file*/
	public static Graph ReadFromFile(String filename, EndPoint.type graphType) throws IOException{
		Graph g;
		BufferedReader br = new BufferedReader(new FileReader(filename));
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        g = new Graph(Integer.parseInt(line)); //Initialize Graph
	        int counter = 0;
	        while (line != null) {
	            line = br.readLine();
	            if(line != null){
	            	String[] splitLine = line.split(","); //Split to get NodeID and Bw.
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
