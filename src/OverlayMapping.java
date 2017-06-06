import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 * This class represents an Overlay Mapping Solution
 *
 */
public class OverlayMapping {

	int[] nodeMapping; //This array represents the node mapping; where the index of the array is the virtual node index and the value is the physical node index
	HashMap<Tuple, ArrayList<Tuple>> linkMapping; // This array represents the link mapping.

	//Initialization Constructor
	public OverlayMapping(int N){
		nodeMapping = new int[N];
		linkMapping = new HashMap<Tuple, ArrayList<Tuple>>();	
	}
	
	// Get the node mapping
	public int getNodeMapping(int virtualNode){
		return nodeMapping[virtualNode];
	}
	
	//Set the node mapping
	public void setNodeMappingSolution(int virtualNode, int physicalNode){
		nodeMapping[virtualNode] = physicalNode;
	}
	
	
	//Get link mapping.
	public ArrayList<Tuple> getLinkMapping(Tuple t){
		return linkMapping.get(t);
	}
	
	public void setLinkMapping(Tuple t, Tuple p){
		if(!linkMapping.containsKey(t))
			linkMapping.put(t,  new ArrayList<Tuple>());
		linkMapping.get(t).add(p);			
	}
	
	//Set link Mapping
	public void setLinkMappingPath(Tuple t, ArrayList<Tuple> path){
		if(!linkMapping.containsKey(t))
			linkMapping.put(t,  new ArrayList<Tuple>());
		linkMapping.get(t).addAll(path);			
	}
	
	//Print the content of the overlay mapping
	public String toString(){
		String content ="";
		for(int i=0;i < nodeMapping.length;i++)
			content +=" Node at index "+i+" is mapped to Physical Node Index "+nodeMapping[i]+"\n";
		
		Iterator it = linkMapping.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        content +=" Link "+pair.getKey()+" is routed via Path "+pair.getValue()+"\n";
	    }
	    return content;
	}
	
}
