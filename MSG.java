
import java.io.Serializable;
/**
 * @author Ding
 * This class is the message object send between different Peer or Peer to Server
 */
import java.util.*;
public class MSG implements Serializable{
	public MSG(){};
	//private String IP_address;//indentify unique node
	private int port;//port num
	private LinkedList<Integer> path_list;
	private Integer MSGId;//the peer who transmit this message
	private Integer TTL;
	private Integer Source_Port;
	private String type;//message type{Querry,HitQuerry}
	private String Filename;//some data being transfer
	private long starttime;
	
	//public ArrayList<String> address_list;//a list of path
	//public HashMap<Integer,Integer> peer_id_list;//a list of peer id and it's server port num
	
	
    //set the variable------------------------------------------
	//public void setIP(String address){this.IP_address=address;}
	public void setstarttime(long l){this.starttime=l;}
	public int delete(){return this.path_list.removeLast();}
	public void setPort(int port){this.port=port;}
	public void addPath(int port){path_list.add(port);}
	public void setPath(LinkedList<Integer> list){this.path_list=list;}
	public void setSource(int port){this.Source_Port=port;}
	public void setType(String type){this.type=type;}
	public void setFilename(String s){this.Filename=s;}
	//public void setAddress_List(ArrayList<String> list){this.address_list=list;}
	//public void setPeerid_List(HashMap<Integer,Integer> list){this.peer_id_list=list;}
	public void setID(int i){this.MSGId=i;}
	public void setTTL(int i){this.TTL=i;}
	
	//get the variable-------------------------------------------
	//public String getIp(){return this.IP_address;}
	public long getstarttime(){return this.starttime;}
	public int getPort(){return this.port;}
	public LinkedList<Integer> getPath(){return this.path_list;}
	public int getSource(){return this.Source_Port;}
	public String getType(){return this.type;}
	public String getFilename(){return this.Filename;}
	//public ArrayList<String> getAddress_List(){return this.address_list;}
	//public HashMap<Integer,Integer> getPeerid_List(){return this.peer_id_list;}
	public Integer getID(){return this.MSGId;}
	public Integer getTTL(){return this.TTL;}
}

