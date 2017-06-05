import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


/**
 * 
 * @author saraa
 *  This class runs Dijkstra to find the shortest path between a pair of nodes
 */
public class Dijkstra {

	private Graph g;
    private Set<Integer> settledNodes;
    private Set<Integer> unSettledNodes;
    private Map<Integer, Integer> predecessors;
    private Map<Integer, Integer> distance;

    public Dijkstra(Graph graph) {
       g = new Graph(graph); // Create a copy of the graph
    }
    
    public void execute(int source) {
        settledNodes = new HashSet<Integer>();
        unSettledNodes = new HashSet<Integer>();
        distance = new HashMap<Integer, Integer>();
        predecessors = new HashMap<Integer, Integer>();
        distance.put(source, 0);
        unSettledNodes.add(source);
        while (unSettledNodes.size() > 0) {
            int node = getMinimum(unSettledNodes);
            settledNodes.add(node);
            unSettledNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    
    private void findMinimalDistances(int node) {
        List<Integer> adjacentNodes = getNeighbors(node);
        for (int target : adjacentNodes) {
            if (getShortestDistance(target) > getShortestDistance(node) + g.getCost(node, target)) {
                distance.put(target, getShortestDistance(node)+ g.getCost(node, target));
                predecessors.put(target, node);
                unSettledNodes.add(target);
            }
        }
    }
    
    private List<Integer> getNeighbors(int node) {
        List<Integer> neighbors = new ArrayList<Integer>();
        for (int i=0;i<g.getAllEndPoints(node).size();i++) {
            if (!isSettled(g.getAllEndPoints(node).get(i).getNodeId())) {
                neighbors.add(g.getAllEndPoints(node).get(i).getNodeId());
            }
        }
        return neighbors;
    }

    private int getMinimum(Set<Integer> vertexes) {
        int minimum = Integer.MAX_VALUE;
        for (int vertex : vertexes) {
            if (minimum == Integer.MAX_VALUE) {
                minimum = vertex;
            } else {
                if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
                    minimum = vertex;
                }
            }
        }
        return minimum;
    }

    private boolean isSettled(int vertex) {
        return settledNodes.contains(vertex);
    }

    private int getShortestDistance(int destination) {
        Integer d = distance.get(destination);
        if (d == null) {
            return Integer.MAX_VALUE;
        } else {
            return d;
        }
    }

    /*
     * This method returns the path from the source to the selected target and
     * NULL if no path exists
     */
    public ArrayList<Tuple> getPath(int target) {
        LinkedList<Integer> nodePath = new LinkedList<Integer>();
        int step = target;
        // check if a path exists
        if (predecessors.get(step) == null) {
            return null;
        }
        nodePath.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            nodePath.add(step);
        }
        // Put it into the correct order
        Collections.reverse(nodePath);
        ArrayList<Tuple> path = new ArrayList<Tuple>();
        for(int i=1;i<nodePath.size();i++){
        	path.add(new Tuple(0,nodePath.get(i-1),nodePath.get(i)));
        }
        return path;
    }
	
}
