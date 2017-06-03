import java.util.ArrayList;


// This class keeps track of the final solution
public class Solutions {

	OverlayMapping ipOtn; //This 
	OverlayMapping vnIp;	
	ArrayList<Tuple> newIpLinks;

	public Solutions(OverlayMapping ipOtn, OverlayMapping vnIp, ArrayList<Tuple> newIpLinks){
		this.ipOtn = ipOtn;
		this.vnIp  = vnIp;
		this.newIpLinks = newIpLinks;
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

}

