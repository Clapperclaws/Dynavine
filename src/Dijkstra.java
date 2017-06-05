import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;


/**
 * 
 * @author saraa
 *  This class runs Dijkstra to find the shortest path between a pair of nodes
 */
public class Dijkstra {

	private Graph g;
	private boolean[] finished;
	private int[] predecessors;
	private int[] distance;
    // private Map<Integer, Integer> distance;

    public Dijkstra(Graph graph) {
       g = new Graph(graph); // Create a copy of the graph
    }
    
    protected class PQEntry implements Comparable<PQEntry> {
        private int nodeId;
        private int distanceFromSource;
        
        public PQEntry() {
            nodeId = distanceFromSource = -1;
        }
        
        public PQEntry(int nodeId, int distanceFromSource) {
            this.nodeId = nodeId;
            this.distanceFromSource = distanceFromSource;
        }
        
        public void setNodeId(int nodeId) {
            this.nodeId = nodeId;
        }
        
        public int getNodeId() {
            return this.nodeId;
        }
        
        public void setDistanceFromSource(int distanceFromSource) {
            this.distanceFromSource = distanceFromSource;
        }
        
        public int getDistanceFromSource() {
            return distanceFromSource;
        }
        
        @Override
        public int compareTo(PQEntry o) {
            return distanceFromSource - o.distanceFromSource;
        }
    }
    
    public void execute(int source) {
        int numNodes = g.nodeCount();
        finished = new boolean[numNodes];
        predecessors = new int[numNodes];
        distance = new int[numNodes];
        for (int i = 0; i < g.nodeCount(); ++i) {
            distance[i] = Integer.MAX_VALUE;
            predecessors[i] = -1;
            finished[i] = false;
        }
        distance[source] = 0;
        PriorityQueue<PQEntry> pQueue = new PriorityQueue<PQEntry>();
        pQueue.add(new PQEntry(source, 0));
        while(!pQueue.isEmpty()) {
            PQEntry entry = pQueue.poll();
            int u = entry.getNodeId();
            finished[u] = true;
            for (int i = 0; i < g.getAdjList()[u].size(); ++i) {
                EndPoint e = g.getAdjList()[u].get(i);
                int v = e.getNodeId();
                int linkCost = e.getCost();
                if (finished[v]) continue;
                if (distance[v] > distance[u] + linkCost) {
                    distance[v] = distance[u] + linkCost;
                    predecessors[v] = u;
                    pQueue.add(new PQEntry(v, distance[v]));
                }
            }
        }
    }
 
    /* This method returns the path from the source to the selected target and
    * NULL if no path exists
    */
    public ArrayList<Tuple> getPath(int target) {
        LinkedList<Integer> nodePath = new LinkedList<Integer>();
        int step = target;
        // check if a path exists
        if (predecessors[step] == -1) {
            return null;
        }
        nodePath.add(step);
        while (predecessors[step] != -1) {
            step = predecessors[step];
            nodePath.add(step);
        }
        // Put it into the correct order
        Collections.reverse(nodePath);
        ArrayList<Tuple> path = new ArrayList<Tuple>();
        for(int i = 1; i < nodePath.size(); i++){
        	path.add(new Tuple(0, nodePath.get(i-1), nodePath.get(i)));
        }
        return path;
    }
	
}
