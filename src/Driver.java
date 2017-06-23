import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Driver {

    private static ArrayList<Tuple> ipLinks; // List of IP Links
    private static ArrayList<Tuple> otnLinks; // List of OTN Links
    private static ArrayList<Tuple> vnLinks; // List of VN Links
    private static String otnTopologyFile;
    private static String ipTopologyFile;
    private static String vnTopologyFile;
    private static String vnLocationFile;
    private static String ipNodeMapFile;
    private static String ipLinkMapFile;
    private static String ipPortInfoFile;

    private static HashMap<String, String> ParseArgs(String[] args) {
        HashMap<String, String> ret = new HashMap<String, String>();
        for (int i = 0; i < args.length; ++i) {
            StringTokenizer tokenizer = new StringTokenizer(args[i], "=");
            ret.put(tokenizer.nextToken(), tokenizer.nextToken());
        }
        return ret;
    }

    public static void main(String[] args) throws IOException {
        HashMap<String, String> parsedArgs = ParseArgs(args);
        // 1-Initialize OTN Graph
        otnLinks = new ArrayList<Tuple>();
        Graph otn = ReadTopology(parsedArgs.get("--otn_topology_file"),
                EndPoint.type.otn, otnLinks);
        // Print graph for testing.
        System.out.println("******* OTN Graph ******* \n" + otn);

        // 2-Initialize IP Graph & populate ipLinks.
        ipLinks = new ArrayList<Tuple>();
        Graph ip = ReadTopology(parsedArgs.get("--ip_topology_file"),
                EndPoint.type.ip, ipLinks);
        // Print graph for testing
        System.out.println("******* IP Graph ******* \n" + ip);

        // Read IP Ports and Port Capacities
        ReadPortAttributes(parsedArgs.get("--ip_port_info_file"), ip);
        for (int i = 0; i < ip.getPorts().length; i++)
            System.out.println("Node " + i + " has " + ip.getPorts()[i]
                    + " ports each of capacity " + ip.getPortCapacity()[i]);

        // 3- Map IP to OTN
        OverlayMapping ipOtn = ReadOverlayMapping(ip.getAdjList().size(),
                parsedArgs.get("--ip_node_mapping_file"),
                parsedArgs.get("--ip_link_mapping_file"));
        System.out.println(
                "******* IP to OTN Overlay Mapping: ******* \n" + ipOtn);

        // 4- Initialize VN Graph
        vnLinks = new ArrayList<Tuple>();
        Graph vn = ReadTopology(parsedArgs.get("--vn_topology_file"),
                EndPoint.type.virtual, vnLinks);
        // Print graph for testing.
        System.out.println("******* VN Graph ******* \n" + vn);

        // 5- Initialize Location Constraints (Set all IP nodes as candidates
        // for each VN node).
        ArrayList<Integer> locationConstraints[] = ReadLocationConstraints(
                parsedArgs.get("--vn_location_file"), vn.getAdjList().size());
        for (int i = 0; i < locationConstraints.length; i++) {
            System.out.println("Location Constraints of node " + i);
            for (int j = 0; j < locationConstraints[i].size(); j++)
                System.out.print(locationConstraints[i].get(j) + ",");
            System.out.println();
        }

        // 6- Get Initial Solution
        CreateInitialSolution cis = new CreateInitialSolution(ip, otn, ipOtn);
        Solutions solution = cis.getInitialSolution(vn, locationConstraints, 1);
        WriteSolutionToFile(solution, vn, parsedArgs.get("--vn_topology_file"));
    }

    private static void WriteSolutionToFile(Solutions solution, Graph vn,
            String filePrefix) throws IOException {
        if (solution == null) System.out.println("Fake");
        OverlayMapping vnIp = solution.getVnIp();
        FileWriter fw = new FileWriter(filePrefix + ".nmap");
        BufferedWriter bw = new BufferedWriter(fw);
        for (int i = 0; i < vn.getNodeCount(); ++i) {
            bw.write(Integer.toString(i) + ","
                    + Integer.toString(vnIp.getNodeMapping(i)) + "\n");
        }
    }

    public static String ReadFromFile(String filename) throws IOException {
        String content = "";
        BufferedReader br = new BufferedReader(new FileReader(filename));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                content += line + "\n";
                line = br.readLine();
            }
        } finally {
            if (br != null)
                br.close();
        }
        return content;
    }

    /* This function reads the port info for the IP */
    public static void ReadPortAttributes(String filename, Graph ip)
            throws IOException {
        String content = ReadFromFile(filename);
        Scanner scanner = new Scanner(content);

        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(",");
            ip.getPorts()[Integer.parseInt(line[0])] = Integer
                    .parseInt(line[1]);
            ip.getPortCapacity()[Integer.parseInt(line[0])] = Integer
                    .parseInt(line[2]);
        }
    }

    /* This function reads the location constraints */
    public static ArrayList<Integer>[] ReadLocationConstraints(String fileName,
            int n) throws IOException {
        ArrayList<Integer>[] loc = new ArrayList[n];

        String content = ReadFromFile(fileName);
        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(",");
            loc[Integer.parseInt(line[0])] = new ArrayList<Integer>();
            for (int i = 1; i < line.length; i++)
                loc[Integer.parseInt(line[0])].add(Integer.parseInt(line[i]));
        }
        return loc;
    }

    /* This function reads the IP-OTN OverlayMapping Solution */
    public static OverlayMapping ReadOverlayMapping(int n,
            String nodeMappingFile, String linkMappingFile) throws IOException {

        // Create OverlayMapping Solution
        OverlayMapping ipOtn = new OverlayMapping(n);

        // Read Node Mapping Solution
        Scanner scanner = new Scanner(ReadFromFile(nodeMappingFile));
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(",");
            ipOtn.nodeMapping[Integer.parseInt(line[0])] = Integer
                    .parseInt(line[1]);
        }

        // Read Link Mapping Solution
        scanner = new Scanner(ReadFromFile(linkMappingFile));
        while (scanner.hasNextLine()) {
            String[] splitLine = scanner.nextLine().split(",");
            Tuple ipLink = new Tuple(0, Integer.parseInt(splitLine[0]),
                    Integer.parseInt(splitLine[1]));

            for (int i = 0; i < ipLinks.size(); i++) {
                if (ipLinks.get(i).equals(ipLink))
                    ipOtn.setLinkMapping(ipLinks.get(i),
                            new Tuple(0, Integer.parseInt(splitLine[2]),
                                    Integer.parseInt(splitLine[3])));
            }
        }
        return ipOtn;
    }

    /* This function reads a graph from file */
    public static Graph ReadTopology(String filename, EndPoint.type graphType,
            ArrayList<Tuple> links) throws IOException {

        Scanner scanner = new Scanner(ReadFromFile(filename));
        Graph g = new Graph(Integer.parseInt(scanner.nextLine())); // Initialize
                                                                   // Graph with
                                                                   // the number
                                                                   // of Nodes

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line != null) {
                // Split the line to get 0: index of the first node; 1: index of
                // the second node; 2: bandwidth
                String[] splitLine = line.split(",");
                // Create Tuple for this link
                links.add(new Tuple(0, Integer.parseInt(splitLine[0]),
                        Integer.parseInt(splitLine[1])));
                // Create two end points
                EndPoint ep1 = new EndPoint(Integer.parseInt(splitLine[1]),
                        Integer.parseInt(splitLine[2]),
                        Integer.parseInt(splitLine[3]), graphType, 0);
                g.addEndPoint(Integer.parseInt(splitLine[0]), ep1);
                EndPoint ep2 = new EndPoint(Integer.parseInt(splitLine[0]),
                        Integer.parseInt(splitLine[2]),
                        Integer.parseInt(splitLine[3]), graphType, 0);
                g.addEndPoint(Integer.parseInt(splitLine[1]), ep2);
            }
        }
        return g;
    }

}
