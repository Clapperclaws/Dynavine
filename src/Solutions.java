import java.util.ArrayList;

// This class keeps track of the final solution
public class Solutions {

    OverlayMapping ipOtn; // This
    OverlayMapping vnIp;
    ArrayList<Tuple> newIpLinks;
    boolean successful;

    public Solutions() {
        successful = false;
    }
    
    public Solutions(int numVN, int numIP) {
        ipOtn = new OverlayMapping(numIP);
        vnIp = new OverlayMapping(numVN);
        newIpLinks = new ArrayList<Tuple>();
        successful = false;
    }

    public Solutions(OverlayMapping ipOtn, OverlayMapping vnIp,
            ArrayList<Tuple> newIpLinks) {
        this.ipOtn = ipOtn;
        this.vnIp = vnIp;
        this.newIpLinks = newIpLinks;
        successful = false;
    }

    public OverlayMapping getIpOtn() {
        return ipOtn;
    }

    public void setIpOtn(OverlayMapping ipOtn) {
        this.ipOtn = ipOtn;
    }

    public OverlayMapping getVnIp() {
        return vnIp;
    }

    public void setVnIp(OverlayMapping vnIp) {
        this.vnIp = vnIp;
    }

    public ArrayList<Tuple> getNewIpLinks() {
        return newIpLinks;
    }

    public void setNewIpLinks(ArrayList<Tuple> newIpLinks) {
        this.newIpLinks = newIpLinks;
    }

    public int isIPLinkCreated(int source, int destination) {
        for (int i = 0; i < newIpLinks.size(); i++) {
            if (newIpLinks.get(i).getSource() == source
                    && newIpLinks.get(i).getDestination() == destination)
                return i;

            if (newIpLinks.get(i).getDestination() == source
                    && newIpLinks.get(i).getSource() == destination)
                return i;
        }
        return -1;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean val) {
        successful = val;
    }

    public String toString() {
        String content = "**** VN -> IP **** \n";
        content += vnIp + "\n";
        content += "**** IP -> OTN **** \n";
        content += ipOtn + "\n";
        content += "**** New IP Links **** \n";
        content += newIpLinks.toString() + "\n";
        content += "Successful = " + successful + "\n";
        return content;
    }

}
