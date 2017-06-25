import java.awt.event.AdjustmentListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CreateInitialSolution {

    Graph collapsedGraph;
    Graph rootCollapsedGraph;
    int ipNodesSize = 0;
    int otnNodesSize = 0;
    OverlayMapping ipOtn;

    public CreateInitialSolution(Graph ip, Graph otn, OverlayMapping ipOtn) {
        ipNodesSize = ip.getAdjList().size();
        otnNodesSize = otn.getAdjList().size();
        this.ipOtn = ipOtn;

        // Create Collapsed Graph
        rootCollapsedGraph = new Graph(ip, otn);
        // Create new IP Links that connects the IP node to the OTN node. For
        // each free port in the IP node create a link with the OTN node. Set
        // the capacity of the link to the capacity of the port and cost to 1.
        for (int i = 0; i < ip.getAdjList().size(); i++) {
            for (int order = 0; order < ip.getPorts()[i]; ++order) {
            	rootCollapsedGraph.addEndPoint(i, new EndPoint(
                        ip.getAdjList().size() + ipOtn.getNodeMapping(i), 1,
                        ip.getPortCapacity()[i], EndPoint.type.otn, order));
            	rootCollapsedGraph.addEndPoint(
                        ip.getAdjList().size() + ipOtn.getNodeMapping(i),
                        new EndPoint(i, 1, ip.getPortCapacity()[i],
                                EndPoint.type.ip, order));
            }
        }
        // System.out.println("Collapsed Graph: \n" + collapsedGraph);
    }

    /*
     * This function returns an initial solution; it takes as input the Virtual
     * graph, the arraylist of location constraints, and the number of
     * iterations k
     */
    public Solutions getInitialSolution(Graph vn,
            ArrayList<Integer>[] locationConstraints, int k) {
        // 1- Initialize the list of solutions that will store the solution
        // generated at the end of every run
        int iter = 0;
        Solutions bestSolution = null;
        long bestCost = Integer.MAX_VALUE;
        do {
        	 
        	// 1- Get a new list order
            ArrayList<Integer> listOrder = getListOrder(vn);

            //Create a copy of the collapsed graph - Reset the graph
            collapsedGraph = new Graph(rootCollapsedGraph);
            
            // 2- Execute the function that performs the VN Nodes & Links
            // embedding
            Solutions sol = execute(vn, locationConstraints, listOrder);
            if (sol.isSuccessful()) {
                long cost = GetSolutionCost(sol, vn);
                System.out.println("Iter = " + Integer.toString(iter)
                        + ": Current cost = " + Long.toString(cost) + "\n");
                if (cost < bestCost) {
                    bestCost = cost;
                    bestSolution = sol;
                }
                System.out.println("Iter = " + Integer.toString(iter)
                        + ": Current best cost = " + Long.toString(bestCost)
                        + "\n");
            } else {
                System.out.println("Iter = " + Integer.toString(iter)
                        + ": no success!!\n");
            }
            resetCollapsedGraph(sol);
            ++iter;
        } while (iter < k);

        // 3- Find the solution with the lowest cost
        // return execute(vn, locationConstraints, listOrder);
        // 4- Return lowest cost solution
        // return sol;
        System.out.println(
                "Best solution cost = " + Long.toString(bestCost) + "\n");
        System.out.println(bestSolution);
        return bestSolution;
    }

    private long GetSolutionCost(Solutions solution, Graph vn) {
        long cost = 0;
        // First add the cost of vlinks.
        OverlayMapping vnIp = solution.getVnIp();
        for (Tuple vlink : vnIp.linkMapping.keySet()) {
            ArrayList<Tuple> ipPath = vnIp.getLinkMapping(vlink);
            long bw = vn.getBW(vlink.getSource(), vlink.getDestination(),
                    vlink.getOrder());
            for (Tuple link : ipPath) {
                cost += (bw * collapsedGraph.getCost(link.getSource(),
                        link.getDestination(), link.getOrder()));
            }
        }

        // Add the cost of new IP links.
        OverlayMapping ipOtn = solution.getIpOtn();
        for (Tuple ipLink : ipOtn.linkMapping.keySet()) {
            ArrayList<Tuple> otnPath = ipOtn.getLinkMapping(ipLink);
            long bw = Math.min(
                    collapsedGraph.getPortCapacity()[ipLink.getSource()],
                    collapsedGraph.getPortCapacity()[ipLink.getDestination()]);
            for (Tuple link : otnPath) {
                cost += (bw * collapsedGraph.getCost(link.getSource(),
                        link.getDestination(), link.getOrder()));
            }
        }
        return cost;
    }

    public Solutions execute(Graph vn, ArrayList<Integer>[] locationConstraints,
            ArrayList<Integer> order) {
        Solutions sol = new Solutions(vn.getAdjList().size(), ipNodesSize);
        Random gn = new Random();

        // This counter will be used to set the ID of newly create metanodes
        int counter = otnNodesSize + ipNodesSize;
        System.out.println(order);
        // 1- For each node in N
        for (int i = 0; i < order.size(); i++) {
            // Restart Counter.
            counter = otnNodesSize + ipNodesSize;

            // Get the index of the first node in the list.
            int startNode = order.get(i);

            if (sol.vnIp.isNodeSettled(startNode,
                    vn.getAdjList().get(startNode).size()))
                continue;

            // settledNodes.add(startNode);
            System.out.println("Start Node: " + startNode);

            // Randomly select a node from location constraint set that is not
            // occupied.
            int sourceLocIdx = gn
                    .nextInt(locationConstraints[startNode].size());
            int sourceLoc = locationConstraints[startNode].get(sourceLocIdx);
            while (sol.vnIp.isOccupied(sourceLoc)) {
                sourceLocIdx = gn
                        .nextInt(locationConstraints[startNode].size());
                sourceLoc = locationConstraints[startNode].get(sourceLocIdx);
            }
            System.out.println("Source Node " + sourceLoc);

            // 3- Create Metanodes for source's neighbors
            ArrayList<Integer> metaNodes = new ArrayList<Integer>();
            int[] vNodeToMetaNodeMap = new int[vn.getNodeCount()];
            Arrays.fill(vNodeToMetaNodeMap, -1);
            int maxLinkCap = 0;
            ArrayList<EndPoint> adjList = vn.getAdjList().get(startNode);
            for (int j = 0; j < adjList.size(); j++) {
                EndPoint vendPoint = adjList.get(j);
                if (sol.ipOtn.isNodeSettled(vendPoint.getNodeId(),
                        vn.getAdjList().get(vendPoint.getNodeId()).size()))
                    continue;
                // Add to list of Meta Nodes
                metaNodes.add(counter);
                vNodeToMetaNodeMap[vendPoint.getNodeId()] = counter;
                if (vendPoint.getBw() > maxLinkCap)
                    maxLinkCap = vendPoint.getBw();

                // Create an Adjacency vector for the meta-node of each
                // neighboring node
                collapsedGraph.addEndPointList(counter,
                        new ArrayList<EndPoint>());

                // a- Check if the node is settled or not
                if (sol.vnIp.getNodeMapping(vendPoint.getNodeId()) == -1) {
                    // Add every IP node in the location constraint to the
                    // adjacency list of the MetaNode
                    for (int k = 0; k < locationConstraints[vendPoint
                            .getNodeId()].size(); ++k) {
                        // If the IP node is already used for embedding another
                        // node then skip.
                        if (sol.vnIp.isOccupied(
                                locationConstraints[vendPoint.getNodeId()]
                                        .get(k))) {
                            continue;
                        } else if (locationConstraints[vendPoint.getNodeId()]
                                .get(k) == sourceLoc) {
                            continue;
                        } else {
                            EndPoint ep = new EndPoint(counter, 1, 1,
                                    EndPoint.type.meta, 0);
                            collapsedGraph.addEndPoint(
                                    locationConstraints[vendPoint.getNodeId()]
                                            .get(k),
                                    ep);
                        }
                    }
                } else {
                    // Connect Meta Node to the IP Node hosting this VN.
                    EndPoint ep = new EndPoint(counter, 1, 1,
                            EndPoint.type.meta, 0);
                    collapsedGraph.addEndPoint(
                            sol.vnIp.nodeMapping[vendPoint.getNodeId()], ep);
                }
                counter++;
            }
            if (maxLinkCap <= 0) {
                cleanAllMetaNodeLink();
                return sol;
            }
            // 4- Connect all Meta Nodes to a single Sink Node
            int sink = counter;
            System.out.println("Sink Node " + sink);
            collapsedGraph.addEndPointList(counter, new ArrayList<EndPoint>());
            for (int j = 0; j < metaNodes.size(); j++) {
                EndPoint sinkMetaNode = new EndPoint(counter, 1, 1,
                        EndPoint.type.meta, 0);
                collapsedGraph.addEndPoint(metaNodes.get(j), sinkMetaNode);
            }
            metaNodes.add(counter);
            counter++;

            // 5- Find IP Nodes that are connected to more than one Meta
            // Node
            for (int k = 0; k < ipNodesSize; k++) {
                ArrayList<EndPoint> adjMetaNodes = collapsedGraph
                        .getAdjNodesByType(k, EndPoint.type.meta);
                if (adjMetaNodes.size() > 1) {
                    // Remove Meta Nodes from the Adjacency List of this
                    // node
                    collapsedGraph.getAllEndPoints(k).removeAll(adjMetaNodes);

                    // Create an Adjacency vector for a new meta-node
                    collapsedGraph.addEndPointList(counter,
                            new ArrayList<EndPoint>());
                    metaNodes.add(counter);

                    // Connect the new MetaNode to the existing MetaNodes
                    for (int j = 0; j < adjMetaNodes.size(); j++)
                        collapsedGraph.addEndPoint(counter,
                                new EndPoint(adjMetaNodes.get(j).getNodeId(), 1,
                                        1, EndPoint.type.meta, 0));

                    // Add New Meta Node to the Adjacency List
                    collapsedGraph.addEndPoint(k,
                            new EndPoint(counter, 1, 1, EndPoint.type.meta, 0));
                    counter++;
                }
            }
            // System.out.println(
            // "Collapsed Graph with Meta Nodes \n" + collapsedGraph);

            // 4- Call EK
            ArrayList<ArrayList<Tuple>> embeddingPaths = MaxFlow(collapsedGraph,
                    sourceLoc, sink, maxLinkCap,
                    (vn.getAdjList().get(startNode).size() + 1));

            // Could not find sufficient paths; Embedding failed.
            if (embeddingPaths == null) {
                System.out.println("Link embedding failed!");
                cleanAllMetaNodeLink();
                return sol;
            }

            if (embeddingPaths.size() < adjList.size()) {
                System.out.println(
                        "Insufficient number of paths to embed all adjacent virtual links.");
                cleanAllMetaNodeLink();
                return sol;
            }

            // EK returned a set of paths only. Figure out the link and node
            // emebdding form the set of paths. Now we need to populate
            // embdSol.
            OverlayMapping embdSol = new OverlayMapping(vn.getNodeCount());
            embdSol.setNodeMappingSolution(startNode, sourceLoc);
            for (int j = 0; j < adjList.size(); ++j) {
                EndPoint vendPoint = adjList.get(j);
                int metanode = vNodeToMetaNodeMap[vendPoint.getNodeId()];
                if (metanode == -1)
                    continue;

                int k = 0;
                for (; k < embeddingPaths.size(); ++k) {
                    ArrayList<Tuple> path = embeddingPaths.get(k);
                    int l = 0;
                    for (; l < path.size(); ++l) {
                        Tuple link = path.get(l);
                        if (link.getSource() == metanode
                                || link.getDestination() == metanode) {
                            break;
                        }
                    }
                    if (l < path.size()) {
                        break;
                    }
                }
                if (k < embeddingPaths.size()) {
                    // Clean up the path first, i.e., remove meta nodes.
                    Iterator<Tuple> it = embeddingPaths.get(k).iterator();
                    while (it.hasNext()) {
                        Tuple link = it.next();
                        if (metaNodes.contains(link.getSource())
                                || metaNodes.contains(link.getDestination())) {
                            if (locationConstraints[vendPoint.getNodeId()]
                                    .contains(link.getSource())) {
                                embdSol.setNodeMappingSolution(
                                        vendPoint.getNodeId(),
                                        link.getSource());
                            } else if (locationConstraints[vendPoint
                                    .getNodeId()]
                                            .contains(link.getDestination())) {
                                embdSol.setNodeMappingSolution(
                                        vendPoint.getNodeId(),
                                        link.getDestination());
                            }
                            it.remove();
                        }
                    }
                    embdSol.setLinkMappingPath(
                            new Tuple(0, startNode, vendPoint.getNodeId()),
                            embeddingPaths.get(k));
                }
            }
            aggregateSolution(vn, embdSol, sol);
//<<<<<<< HEAD
            
         // Add embedded nodes to the list of settled nodes
         //   for(int j=0;j<adjList.size();j++){
          //  	if(sol.getVnIp().getNodeMapping(adjList.get(j).getNodeId()) != -1){
           // 		settledNodes.add(adjList.get(j).getNodeId());
           // 	}
           // 	else
           // 		return sol;
        //    }
//=======
//>>>>>>> branch 'master' of https://github.com/Clapperclaws/FAST-MULE

            cleanAllMetaNodeLink();
        }
        System.out.println("Solution :" + sol);
        for (int i = 0; i < vn.getNodeCount(); ++i) {
            if (sol.vnIp.getNodeMapping(i) == -1) {
                cleanAllMetaNodeLink();
                return sol;
            }
        }
        sol.setSuccessful(true);
        return sol;
    }

    void cleanAllMetaNodeLink() {
        // 7- Delete All MetaNodes
       // System.out.println("Cleaning up meta nodes/links.");
        for (int j = 0; j < ipNodesSize; j++) {
            ArrayList<EndPoint> adjMetaNodes = collapsedGraph
                    .getAdjNodesByType(j, EndPoint.type.meta);
       //     System.out.println("Removing metanodes adjacent to " + j + ": "
       //             + adjMetaNodes);
            collapsedGraph.getAllEndPoints(j).removeAll(adjMetaNodes);
        }

        // 8- Remove All Meta Nodes
        List<ArrayList<EndPoint>> subList = collapsedGraph.getAdjList().subList(
                (ipNodesSize + otnNodesSize), collapsedGraph.getNodeCount());
        collapsedGraph.getAdjList().removeAll(subList);
    }

    // This function iteratively aggregates the final Solution after every
    // iteration.
    public void aggregateSolution(Graph vn, OverlayMapping embdSol,
            Solutions sol) {
        // 1- Aggregate Node Mapping Solution to Final Solution
        sol.vnIp.incrementNodeEmbed(embdSol);

        // 2- Aggregate Link Mapping Solution to Final Solution
        Iterator it = embdSol.linkMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            // Get the Virtual Link and its embedding path.
            Tuple vLink = (Tuple) pair.getKey();
            ArrayList<Tuple> linkMapping = (ArrayList<Tuple>) pair.getValue();

            // Initialize a Path entry for the VLink
            sol.vnIp.linkMapping.put((Tuple) pair.getKey(),
                    new ArrayList<Tuple>());

            int vLinkDestIndex = vn.getNodeIndex(vLink.getSource(),
                    vLink.getDestination(), vLink.getOrder());
            // Get BW Demand
            int bw = vn.getAdjList().get(vLink.getSource()).get(vLinkDestIndex)
                    .getBw();

            // Initialize Potential Variables that will be used to populate New
            // IP Link Path
            ArrayList<Tuple> newIpLinkPath = new ArrayList<Tuple>();
            int srcIP = -1;
            int dstIP = -1;
            for (int i = 0; i < linkMapping.size(); i++) {
                // Examine Link
                int src = linkMapping.get(i).getSource();
                int dst = linkMapping.get(i).getDestination();

                // Case of IP -> IP
                if ((src >= 0 && src < ipNodesSize)
                        && (dst >= 0 && dst < ipNodesSize)) {
                    // Add to VLink Path in VN->IP overlay solution
                    sol.vnIp.linkMapping.get((Tuple) pair.getKey())
                            .add(linkMapping.get(i));
                }

                // Case of IP -> OTN
                if ((src >= 0 && src < ipNodesSize) && dst >= ipNodesSize
                        && dst < (otnNodesSize + ipNodesSize)) {

                    // Add the OTN dst as the Node embedding of the IP src.
                    sol.ipOtn.nodeMapping[src] = dst;
                    srcIP = src;

                    // Initialize a new path in the IP->OTN Solution
                    newIpLinkPath = new ArrayList<Tuple>();
                }

                // Case of New OTN -> OTN
                if ((src >= ipNodesSize && src < (otnNodesSize + ipNodesSize))
                        && dst >= ipNodesSize
                        && dst < (otnNodesSize + ipNodesSize)) {

                    // Add link to IP Path
                    newIpLinkPath.add(linkMapping.get(i));
                }

                // Case of OTN -> IP
                if ((dst >= 0 && dst < ipNodesSize) && src >= ipNodesSize
                        && src < (otnNodesSize + ipNodesSize)) {

                    // Add the OTN src as the Node embedding of the IP dst.
                    sol.ipOtn.nodeMapping[dst] = src;
                    dstIP = dst;

                    // Check if this link has already been created
                    int index = sol.isIPLinkCreated(srcIP, dstIP);
                    if (index != -1) {
                        // Check if the link has enough capacity
                        if (collapsedGraph.getBW(srcIP, dstIP, sol
                                .getNewIpLinks().get(index).getOrder()) >= bw)
                            // Add the IP Link in the VN->IP Overlay Solution
                            sol.vnIp.linkMapping.get((Tuple) pair.getKey())
                                    .add(sol.getNewIpLinks().get(index));
                    } else {

                        // Find the order of the new IP Link
                        int tupleOrder = collapsedGraph.findTupleOrder(srcIP,
                                dstIP);

                        // Create new Tuple for the IP Link
                        Tuple ipTup = new Tuple(tupleOrder, srcIP, dstIP);

                        // Add the path in the IP->OTN Overlay Solution
                        sol.ipOtn.linkMapping.put(ipTup, newIpLinkPath);

                        // Add the IP Link in the VN->IP Overlay Solution
                        sol.vnIp.linkMapping.get((Tuple) pair.getKey())
                                .add(ipTup);

                        // Get Bandwidth Capacity of new IP Link
                        int newIPLinkCap = Math.min(
                                collapsedGraph.getPortCapacity()[srcIP],
                                collapsedGraph.getPortCapacity()[dstIP]);

                        // Add ipSrc as neighbor of ipDst
                        collapsedGraph.addEndPoint(srcIP, new EndPoint(dstIP, 1,
                                newIPLinkCap, EndPoint.type.ip, tupleOrder));

                        // Add ipDst as neighbor of ipSrc
                        collapsedGraph.addEndPoint(dstIP, new EndPoint(srcIP, 1,
                                newIPLinkCap, EndPoint.type.ip, tupleOrder));

                        // Add IP Link to the list of new IP Links
                        sol.newIpLinks.add(ipTup);

                        // Update IP Ports
                        collapsedGraph.setPort(srcIP,
                                collapsedGraph.getPorts()[srcIP] - 1);
                        collapsedGraph.setPort(dstIP,
                                collapsedGraph.getPorts()[dstIP] - 1);

                        // Update OTN Links Capacity
                        updateResidualCapacity(ipTup, newIpLinkPath,
                                newIPLinkCap);
                    }
                }
            }
            updateResidualCapacity(vLink, sol.vnIp.linkMapping.get(vLink), bw);
        }
    }

    void resetCollapsedGraph(Solutions sol) {
        ArrayList<Tuple> newIps = sol.getNewIpLinks();
        for (int i = 0; i < newIps.size(); ++i) {
            Tuple link = newIps.get(i);
            System.out.println("Removing IP link (" + link.getSource() + ", "
                    + link.getDestination() + "," + link.getOrder() + ")");
            int bw = collapsedGraph.getBW(link.getSource(),
                    link.getDestination(), link.getOrder());

            // Remove the new IP links from collapsedGraph.
            boolean ret = collapsedGraph.removeLink(link.getSource(),
                    link.getDestination(), link.getOrder());
            ret = collapsedGraph.removeLink(link.getDestination(),
                    link.getSource(), link.getOrder());
            // Adjust port count of the IP nodes.
            collapsedGraph.setPort(link.getSource(),
                    collapsedGraph.getPorts()[link.getSource()] + 1);
            collapsedGraph.setPort(link.getDestination(),
                    collapsedGraph.getPorts()[link.getDestination()] + 1);

            // Restore bandwidth on the mapped OTN path.
            ArrayList<Tuple> otnPath = sol.ipOtn.getLinkMapping(link);
            updateResidualCapacity(link, otnPath, -bw);
        }
    }

    public void updateResidualCapacity(Tuple t, ArrayList<Tuple> path, int bw) {
        // Update Network Capacity
        for (int k = 0; k < path.size(); k++) {
            // Get the first edge of the link
            int src = path.get(k).getSource();
            // Get the second edge of the link
            int dst = path.get(k).getDestination();
            // Find the index of the second edge
            int dstIndex = collapsedGraph.getNodeIndex(src, dst,
                    path.get(k).getOrder());
            // Find the index of the first edge
            int srcIndex = collapsedGraph.getNodeIndex(dst, src,
                    path.get(k).getOrder());

            // Update BW Capacity for the first edge
            collapsedGraph.getAdjList().get(src).get(dstIndex).setBw(
                    collapsedGraph.getAdjList().get(src).get(dstIndex).getBw()
                            - bw);

            // Update BW Capacity for the second edge
            collapsedGraph.getAdjList().get(dst).get(srcIndex).setBw(
                    collapsedGraph.getAdjList().get(dst).get(srcIndex).getBw()
                            - bw);
        }
    }

    // Returns a shuffled order of VN nodes
    public ArrayList<Integer> getListOrder(Graph vn) {
        ArrayList<Integer> order = new ArrayList<Integer>();
        for (int i = 0; i < vn.getAdjList().size(); i++)
            order.add(i);
        Collections.shuffle(order);
        return order;
    }

    /*
     * Run Max Flow to get the Link Embedding Solution This function received as
     * input the collapsed graph, the source & sink meta nodes, the maxLinkCap,
     * and the size of the overlay mapping solution "N"
     */
    public ArrayList<ArrayList<Tuple>> MaxFlow(Graph collapsedGraph, int source,
            int sink, int maxLinkCap, int N) {
        final int kNodeCount = collapsedGraph.getNodeCount();
        int[][][] capacity = new int[kNodeCount][kNodeCount][];
        int[][][] flow = new int[kNodeCount][kNodeCount][];
        for (int i = 0; i < collapsedGraph.getNodeCount(); ++i) {
            for (int j = 0; j < collapsedGraph.getNodeCount(); ++j) {
                if (i >= 0 && i < ipNodesSize) {
                    capacity[i][j] = new int[collapsedGraph.getPorts()[i]];
                    flow[i][j] = new int[collapsedGraph.getPorts()[i]];
                } else if ((i >= ipNodesSize && i < ipNodesSize + otnNodesSize)
                        && (j >= 0 && j < ipNodesSize)) {
                    capacity[i][j] = new int[collapsedGraph.getPorts()[j]];
                    flow[i][j] = new int[collapsedGraph.getPorts()[j]];
                } else {
                    capacity[i][j] = new int[1];
                    flow[i][j] = new int[1];
                }
            }
        }

        int numNodes = collapsedGraph.getNodeCount();

        // List of augmenting paths that we've found while running max-flow.
        ArrayList<ArrayList<Tuple>> augPaths = new ArrayList<ArrayList<Tuple>>();

        // A reverse mapping between a link in the collapsed graph and the
        // augmenting path that link belongs to. We store an int[] against the
        // link. int[0] is the index of augmenting path from the list of
        // augmenting paths. int[1] is the index of the link inside that
        // augmenting path.
        HashMap<Tuple, ArrayList<int[]>> linkToPathMap = new HashMap<Tuple, ArrayList<int[]>>();

        // Initialize capacity matrix. capacity[metanode][u] = 0;
        // capacity[source][u] = num_vlinks_to_embed, (that is N - 1),
        // capacity[u][v] = bw_capacity / maxLinkCap.
        for (int i = 0; i < numNodes; ++i) {
            ArrayList<EndPoint> adjList = collapsedGraph.getAdjList().get(i);
            for (int j = 0; j < adjList.size(); ++j) {
                EndPoint endPoint = adjList.get(j);
                if (endPoint.getNodeId() == i) {
                    capacity[i][endPoint.getNodeId()][endPoint.getOrder()] = 0;
                    continue;
                }
                if (endPoint.getT() == EndPoint.type.meta) {
                    capacity[i][endPoint.getNodeId()][endPoint.getOrder()] = 1;
                } else {
                    capacity[i][endPoint.getNodeId()][endPoint
                            .getOrder()] = endPoint.getBw() / maxLinkCap;
                }
            }
        }

        // Set capacity[source][metanode] = N - 1.
        for (int i = 0; i < collapsedGraph.getAdjList().get(source)
                .size(); ++i) {
            EndPoint endPoint = collapsedGraph.getAdjList().get(source).get(i);
            int v = endPoint.getNodeId();
            int order = endPoint.getOrder();
            capacity[source][v][order] = Math.min(capacity[source][v][order],
                    N - 1);
            capacity[v][source][order] = Math.min(capacity[v][source][order],
                    N - 1);
        }

        int maxFlow = 0;
        while (true) {
            // Find an augmenting path, i.e., a path that can carry some
            // positive flow.
            Dijkstra dijkstraDriver = new Dijkstra(collapsedGraph, capacity);
            ArrayList<Tuple> path = dijkstraDriver.getPath(source, sink, 1);
            if (path == null)
                break;
            // After finding an augmenting path, compute the minimum capacity
            // available along this path. That would be how much flow we are
            // able to push. For our specific problem, this number should be 1.
            int minCap = Integer.MAX_VALUE;
            for (int i = 0; i < path.size(); ++i) {
                Tuple link = path.get(i);
                minCap = Math.min(minCap,
                        capacity[link.getSource()][link.getDestination()][link
                                .getOrder()]);
            }
            if (minCap == Integer.MAX_VALUE)
                break;
            maxFlow += minCap;
            augPaths.add(path);

            // We now push minCap units of flow through the augmenting path.
            // Update the residual capacities and flow matrix accordingly.
            System.out.println(path);
            for (int i = 0; i < path.size(); ++i) {
                Tuple link = path.get(i);
                capacity[link.getSource()][link.getDestination()][link
                        .getOrder()] -= minCap;
                capacity[link.getDestination()][link.getSource()][link
                        .getOrder()] += minCap;
                flow[link.getSource()][link.getDestination()][link
                        .getOrder()] += minCap;
                flow[link.getDestination()][link.getSource()][link
                        .getOrder()] -= minCap;

                // Populate the reverse mapping between a link and the
                // augmenting path the link belongs to.
                int[] pair = new int[2];
                pair[0] = augPaths.size() - 1;
                pair[1] = i;
                if (linkToPathMap.get(link) == null) {
                    linkToPathMap.put(link, new ArrayList<int[]>());
                }
                linkToPathMap.get(link).add(pair);
            }
        }

        // If maximum flow is less than the number of links to be embedded this
        // means that embedding has failed.
        if (maxFlow < N - 1) {
            System.out.println("Cannot find sufficient paths between " + source
                    + " and " + sink + "; Maxflow = " + maxFlow
                    + ", required flow = " + (N - 1));
            return null;
        }

        // Construct flow paths from augmenting paths. The algorithm is as
        // follows: If augmenting path a and b contains links (u, v) and (v, u),
        // respectively, then it means flow along (u, v) is cancelled by b by
        // pushing flow along (v, u). In this case, we splice up the path a with
        // the sub-path following (v, u) in b. Similar operation is performed on
        // b as well.
        ArrayList<ArrayList<Tuple>> flowPaths = new ArrayList<ArrayList<Tuple>>();
        for (int i = 0; i < augPaths.size(); ++i) {
            ArrayList<Tuple> tentativeFlowPath = new ArrayList<Tuple>(
                    augPaths.get(i));
            int k = 0;
            while (k < tentativeFlowPath.size()) {
                Tuple link = augPaths.get(i).get(k);
                Tuple reverseLink = new Tuple(link.getOrder(),
                        link.getDestination(), link.getSource());
                ArrayList<int[]> pairs = linkToPathMap.get(reverseLink);
                if (pairs != null) {
                    // A link on the tentative flow path is cancelled by a
                    // reverse link on another path. Now we splice up the paths
                    // and create a new one.
                    if (pairs.size() > 0) {
                        int[] pair = pairs.get(0).clone();
                        pairs.remove(0);
                        int otherPathIndex = pair[0];
                        int reverseLinkIndex = pair[1];
                        tentativeFlowPath = (ArrayList<Tuple>) tentativeFlowPath
                                .subList(0, k - 1);
                        tentativeFlowPath.addAll(augPaths.get(otherPathIndex)
                                .subList(reverseLinkIndex + 1,
                                        augPaths.get(otherPathIndex).size()));
                    }

                } else
                    ++k;
            }
            flowPaths.add(tentativeFlowPath);
        }
        return flowPaths;
    }
}
