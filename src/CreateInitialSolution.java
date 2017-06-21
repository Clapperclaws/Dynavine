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
    int ipNodesSize = 0;
    int otnNodesSize = 0;
    OverlayMapping ipOtn;

    public CreateInitialSolution(Graph ip, Graph otn, OverlayMapping ipOtn) {

        ipNodesSize = ip.getAdjList().size();
        otnNodesSize = otn.getAdjList().size();
        this.ipOtn = ipOtn;

        // Create Collapsed Graph
        collapsedGraph = new Graph(ip, otn);
        // Create new IP Links that connects the IP node to the OTN node - Cost
        // of 1 and unlimited capacity
        for (int i = 0; i < ip.getAdjList().size(); i++) {
            collapsedGraph.addEndPoint(i,
                    new EndPoint(
                            ip.getAdjList().size() + ipOtn.getNodeMapping(i), 1,
                            Integer.MAX_VALUE, EndPoint.type.otn, 0));
            collapsedGraph.addEndPoint(
                    ip.getAdjList().size() + ipOtn.getNodeMapping(i),
                    new EndPoint(i, 1, Integer.MAX_VALUE, EndPoint.type.ip, 0));
        }
        System.out.println("Collapsed Graph: \n" + collapsedGraph);
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
        ArrayList<Solutions> listSol = new ArrayList<Solutions>();

        int iter = 0;
        do {
            // 1- Get a new list order
            ArrayList<Integer> listOrder = getListOrder(vn);

            // 2- Execute the function that performs the VN Nodes & Links
            // embedding
            Solutions sol = execute(vn, locationConstraints, listOrder);
            if (sol != null)
                listSol.add(sol);
            iter++;
        } while (iter < k);

        // 3- Find the solution with the lowest cost
        // return execute(vn, locationConstraints, listOrder);
        // 4- Return lowest cost solution
        // return sol;

        return null;
    }

    public Solutions execute(Graph vn, ArrayList<Integer>[] locationConstraints,
            ArrayList<Integer> order) {
        Solutions sol = new Solutions(vn.getAdjList().size(), ipNodesSize);
        ArrayList<Integer> settledNodes = new ArrayList<Integer>();
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

            if (settledNodes.contains(startNode))
                continue;
            settledNodes.add(startNode);
            System.out.println("Start Node: " + startNode);

            // Randomly select a node from location constraint set that is not
            // occupied.
            int sourceLoc = gn.nextInt(locationConstraints[startNode].size());
            while (sol.vnIp.isOccupied(sourceLoc))
                sourceLoc = gn.nextInt(locationConstraints[startNode].size());
            System.out.println("Source Node " + sourceLoc);

            // 3- Create Metanodes for source's neighbors
            ArrayList<Integer> metaNodes = new ArrayList<Integer>();
            int[] vNodeToMetaNodeMap = new int[vn.getNodeCount()];
            Arrays.fill(vNodeToMetaNodeMap, -1);
            int maxLinkCap = 0;
            ArrayList<EndPoint> adjList = vn.getAdjList().get(startNode);
            for (int j = 0; j < adjList.size(); j++) {
                EndPoint vendPoint = adjList.get(j);

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
                if (!settledNodes.contains(vendPoint.getNodeId())) {
                    // Add the adjacent nodes to the list of settled nodes
                    settledNodes.add(vendPoint.getNodeId());

                    // Add every IP node in the location constraint to the
                    // adjacency list of the MetaNode
                    for (int k = 0; k < locationConstraints[vendPoint
                            .getNodeId()].size(); ++k) {
                        EndPoint ep = new EndPoint(counter, 1, 1,
                                EndPoint.type.meta, 0);
                        collapsedGraph.addEndPoint(
                                locationConstraints[vendPoint.getNodeId()]
                                        .get(k),
                                ep);
                    }
                } else {
                    // Connect Meta Node to the IP Node hosting this VN.
                    EndPoint ep = new EndPoint(counter, 1, 1,
                            EndPoint.type.meta, 0);
                    collapsedGraph
                            .addEndPoint(
                                    sol.vnIp.nodeMapping[vendPoint.getNodeId()],
                                    ep);
                }
                counter++;
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

            // 5- Find IP Nodes that are connected to more than one Meta Node
            for (int k = 0; k < ipNodesSize; k++) {
                ArrayList<EndPoint> adjMetaNodes = collapsedGraph
                        .getAdjNodesByType(k, EndPoint.type.meta);
                if (adjMetaNodes.size() > 1) {
                    // Remove Meta Nodes from the Adjacency List of this node
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
            System.out.println(
                    "Collapsed Graph with Meta Nodes \n" + collapsedGraph);

            // 4- Call EK
            ArrayList<ArrayList<Tuple>> embeddingPaths = MaxFlow(collapsedGraph,
                    sourceLoc, sink, maxLinkCap,
                    (vn.getAdjList().get(startNode).size() + 1));

            // Could not find sufficient paths; Embedding failed.
            if (embeddingPaths == null)
                return null;

            // EK returned a set of paths only. Figure out the link and node
            // emebdding form the set of paths. Now we need to populate embdSol.
            OverlayMapping embdSol = new OverlayMapping(
                    vn.getAdjList().get(startNode).size() + 1);

            aggregateSolution(embdSol, sol);

            // 7- Delete All MetaNodes
            for (int j = 0; j < ipNodesSize; j++) {
                ArrayList<EndPoint> adjMetaNodes = collapsedGraph
                        .getAdjNodesByType(j, EndPoint.type.meta);
                collapsedGraph.getAllEndPoints(j).removeAll(adjMetaNodes);
            }

            // 8- Remove All Meta Nodes
            List<ArrayList<EndPoint>> subList = collapsedGraph.getAdjList()
                    .subList((ipNodesSize + otnNodesSize),
                            collapsedGraph.getNodeCount());
            collapsedGraph.getAdjList().removeAll(subList);
            System.out.println("Collapsed Graph after removal of MetaNodes:\n"
                    + collapsedGraph);
            if (settledNodes.size() == vn.getAdjList().size())
                break;
        }
        System.out.println("Solution :" + sol);
        return sol;
    }

    // This function iteratively aggregates the final Solution after every
    // iteration.
    public void aggregateSolution(OverlayMapping embdSol, Solutions sol) {
        sol.vnIp.incrementNodeEmbed(embdSol);

        // 5- Find Newly Created IP Links
        Iterator it = embdSol.linkMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Tuple t = (Tuple) pair.getKey();
            ArrayList<Tuple> linkMapping = (ArrayList<Tuple>) pair.getValue();
            // Examine First Link
            int src = linkMapping.get(0).getSource();
            int dst = linkMapping.get(0).getDestination();
            // Case of New IP Link
            if (dst >= ipNodesSize && dst < (otnNodesSize + ipNodesSize)) {
                int ipSrc = src;
                int ipDst = linkMapping.get(linkMapping.size() - 1)
                        .getDestination();

                // Add the OTN dst as the Node embedding of the IP src.
                sol.ipOtn.nodeMapping[src] = dst;

                // Add the OTN src as the Node embedding of the IP dst.
                sol.ipOtn.nodeMapping[ipDst] = linkMapping
                        .get(linkMapping.size() - 1).getSource();

                ArrayList<Tuple> linkEmbedding = linkMapping;
                linkEmbedding.remove(ipSrc);
                linkEmbedding.remove(ipDst);

                // Create Tuple & Add Link Embedding Solution
                int tupleOrder = collapsedGraph.findTupleOrder(ipSrc, ipDst);
                Tuple newTup = new Tuple(ipSrc, ipDst, tupleOrder);
                sol.ipOtn.linkMapping.put(newTup, linkEmbedding);

                // Add Tuple to list of newly created Links
                sol.newIpLinks.add(newTup);

                // Get Bandwidth Capacity of new IP Link
                int newIPLinkCap = Math.min(
                        collapsedGraph.getPortCapacity()[ipSrc],
                        collapsedGraph.getPortCapacity()[ipDst]);

                // Add ipSrc as neighbor of ipDst - Figure out the order
                collapsedGraph.addEndPoint(ipSrc, new EndPoint(ipDst, 1,
                        newIPLinkCap, EndPoint.type.ip, tupleOrder));

                // Add ipDst as neighbor of ipSrc - Figure out the order
                collapsedGraph.addEndPoint(ipDst, new EndPoint(ipSrc, 1,
                        newIPLinkCap, EndPoint.type.ip, tupleOrder));
            }
            // Case of Existing IP Links
            sol.vnIp.linkMapping.put(t, linkMapping);
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
        int[][] capacity = new int[kNodeCount][kNodeCount];
        int[][] flow = new int[kNodeCount][kNodeCount];
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
                if (endPoint.getT() == EndPoint.type.meta) {
                    capacity[i][endPoint.getNodeId()] = 1;
                    capacity[endPoint.getNodeId()][i] = 0;
                } else {
                    capacity[i][endPoint.getNodeId()] = endPoint.getBw()
                            / maxLinkCap;
                }
            }
        }

        // Set capacity[u][metanode] = 1,
        for (int i = 0; i < collapsedGraph.getAdjList().get(source)
                .size(); ++i) {
            int v = collapsedGraph.getAdjList().get(source).get(i).getNodeId();
            capacity[source][v] = N - 1;
            capacity[v][source] = N - 1;
        }

        int maxFlow = 0;
        while (true) {
            // Find an augmenting path, i.e., a path that can carry some
            // positive flow.
            Dijkstra dijkstraDriver = new Dijkstra(collapsedGraph, capacity, 0);
            dijkstraDriver.execute(source);
            ArrayList<Tuple> path = dijkstraDriver.getPath(sink);

            // After finding an augmenting path, compute the minimum capacity
            // available along this path. That would be how much flow we are
            // able to push. For our specific problem, this number should be 1.
            int minCap = Integer.MAX_VALUE;
            for (int i = 0; i < path.size(); ++i) {
                Tuple link = path.get(i);
                minCap = Math.min(minCap,
                        capacity[link.getSource()][link.getDestination()]);
            }
            if (minCap == Integer.MAX_VALUE)
                break;

            maxFlow += minCap;
            augPaths.add(path);
            // We now push minCap units of flow through the augmenting path.
            // Update the residual capacities and flow matrix accordingly.
            for (int i = 0; i < path.size(); ++i) {
                Tuple link = path.get(i);
                capacity[link.getSource()][link.getDestination()] -= minCap;
                capacity[link.getDestination()][link.getSource()] += minCap;
                flow[link.getSource()][link.getDestination()] += minCap;
                flow[link.getDestination()][link.getSource()] -= minCap;

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
        if (maxFlow < N - 1)
            return null;

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
