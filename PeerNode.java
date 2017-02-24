
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Ding
 *This class is PeerNode class, it functions as Peer client and Peer Server
 *Peerclient could lookup data, download data, registry itself
 *Peerserver could provide download data and automatic update Peer's Folder's
 *state(add a file or delete a file).
 */
public class PeerNode {
    private  int server_Port;
    private  final int BUFFER_SIZE=1024*16;//16k bytes
    private final int TTL=10;//total number of node
    //this is the list for the center indexing server to maintain the locations of folder
    //one peer could have multiple files locate in different folder
    private ConcurrentHashMap<String,String> FileList=new ConcurrentHashMap<String,String>();
    private ConcurrentHashMap<String,ArrayList<Integer>> DownloadList=new ConcurrentHashMap<>();
    //to know past 10 seconds which message has came, if it is, reject it
    private ConcurrentHashMap<Integer,Integer> messagelist=new ConcurrentHashMap<>();
    private  String Myfolder=null;
    private  int file_num=0;
    public long totaltime=0;
    public boolean wait=true;
    public int num_receive=0;
    public int messagequeue=0;
    
    private  int[] neighbors; //a list of neighbor peers' port numbers
    
    
    /**
     * Initializing a peer node: get port number, file list, neighbor list
     * @param peerFolder: e.g. "src/Folder/peer1"
     * @param topo: Topology.STAR, Topology.LINEAR
     * @throws IOException
     */
    public void initialize(String peerFolder, Topology topo)throws IOException{
        Myfolder=peerFolder+"/files";
        String configFile=null;
        switch (topo) {
            case LINEAR:
                configFile="linear.config.txt";
            case STAR:
                configFile="star.config.txt";
            default :
                break;
        }
        //read the config file
        //and get the assigned port number and the list of neighbor peers' port numbers
        BufferedReader br=new BufferedReader(new FileReader(new File(peerFolder+"/"+configFile)));
        server_Port=Integer.parseInt(br.readLine());//the first line is port no. of itself
        String[] items=br.readLine().split(" ");//the second line is a list of neighbors' port numbers
        neighbors=new int[items.length];
        for(int i=0;i<items.length;i++){
            neighbors[i]=Integer.parseInt(items[i]);
        }
        br.close();
        // copy all the files to filelist
        File folder=new File(Myfolder);
        File[] array = folder.listFiles();
        file_num=array.length;
        ArrayList<String> files=new ArrayList<>(array.length);
        if(folder.isDirectory()){
            for(File f:array){
                String file=f.toString();//path of every file
                files.add(f.getName());
                FileList.put(f.getName(), file);
            }
        }
        else System.out.println("ERROR:this is not a path of a folder");
        UpdateChecking uc=new UpdateChecking();
        uc.start();
        new Messagelist_update().start();
        ServerSocket server = new ServerSocket(server_Port);
        try{
            while(true){
                //always listening at port number
                new PeerServer(server.accept()).start();
            }
        }catch (Exception e){
            System.out.println("ClientServer Accept ERROR:"+e);
        }
        finally{
            server.close();
        }//server keep accept message
        
    }
    private class UpdateChecking extends Thread{
        public void run(){
            try{
                sleep((long)1000);
                while(true){
                    //check the file in folder has been changed or not
                    File folder=new File(Myfolder);
                    File[] array = folder.listFiles();
                    if(array.length!=file_num){
                        for(File f:array){
                            if(!FileList.containsKey(f.getName())){
                                FileList.put(f.getName(), f.toString());
                                System.out.println("File "+f.getName()+"is added in PeerServer "+server_Port);
                            }
                        }
                    }
                    file_num=array.length;
                }
            }catch(Exception e){
                System.out.println("UpdateChecking ERROR:"+e);
            }
        }
    }
    private class Messagelist_update extends Thread{
        public void run(){
            try{
                while(true){
                    sleep((long)300000);
                    messagelist=new ConcurrentHashMap<>();
                    messagequeue=0;
                }
            }catch(Exception e){
                System.out.println("Messagelist_updateERROR"+e);
            }
        }
    }
    private class PeerServer extends Thread{
        private Socket socket;
        public PeerServer(Socket socket){
            this.socket=socket;
        }
        public void run(){
            try{
                ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
                MSG message=(MSG)in.readObject();//MSG from client
                String request_type=message.getType();
                String fileLocation=null;
                String fileName=(String) message.getFilename();//get data
                MSG ack=new MSG();
                ack.setType("ACK");
                ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(ack);
                //System.out.println("write ack to for message"+message.getID()+" server port "+message.getPath().getLast());
                if(request_type.equals("OBTAIN")){
                    //download file
                    fileLocation=FileList.get(fileName);
                    if(fileLocation==null)System.out.println("file not found");
                    else
                    {
                        System.out.println("download request received in Peer Server");
                        File sendfile=new File(fileLocation);
                        //buffer sending file throw socket
                        byte[] content=new byte[(int)sendfile.length()];
                        BufferedInputStream download_data=new BufferedInputStream(new FileInputStream(sendfile));
                        download_data.read(content, 0, content.length);
                        OutputStream output=socket.getOutputStream();
                        output.write(content, 0, content.length);
                        output.flush();
                        System.out.println("successfully sent download file:"+fileName);
                        //closing all the buffer and socket
                        download_data.close();
                        output.close();
                        out.close();
                        in.close();
                        socket.close();
                        //Thread.currentThread().interrupt();
                    }
                    
                }
                else if(request_type.equals("QUERRY")&&message.getTTL()>0){
                    
                    out.close();
                    in.close();
                    socket.close();
                    String FileName=message.getFilename();
                    
                    if(FileList.containsKey(FileName)&&!messagelist.containsKey(message.getID())&&message.getTTL()>0){
                        
                        messagelist.put(message.getID(), 0);
                        //System.out.println("Port "+server_Port+" receive querry from port "+message.getSource()+" for file "+FileName);
                        int port=message.delete();//delete and return last port
                        Socket ssocket=new Socket("127.0.0.1",port);
                        MSG hitquerry=new MSG();
                        hitquerry.setType("HITQUERRY");
                        hitquerry.setSource(server_Port);
                        hitquerry.setTTL(TTL);
                        hitquerry.setID(message.getID());
                        hitquerry.setFilename(FileName);
                        hitquerry.setPath(message.getPath());
                        hitquerry.setstarttime(message.getstarttime());
                        //hitquerry.setID(message.getID());//hit message should have the same id with querry
                        ObjectOutputStream output=new ObjectOutputStream(ssocket.getOutputStream());
                        output.writeObject(hitquerry);
                        ObjectInputStream input=new ObjectInputStream(ssocket.getInputStream());
                        MSG ackb=(MSG)input.readObject();
                        if(ackb.getType().equals("ACK")){
                            //means it works fine
                            //System.out.println("ACK");
                        }
                        else {
                            System.out.println("connection error");
                        }
                        input.close();
                        output.close();
                        ssocket.close();
                    }
                    else if(message.getTTL()>0&&!messagelist.containsKey(message.getID())){//forwarding msg to all the neighbor
                        //System.out.println("peerserver here querr3y");
                        int last_port=message.getPath().getLast();
                        messagelist.put(message.getID(), 0);//to store the message has came
                        for(Integer port:neighbors){//do not pass to the node already saw
                            if(port!=last_port){
                                //System.out.println("forward message "+message.getID()+" to "+ port);
                                
                                Socket ssocket=new Socket("127.0.0.1",port);
                                message.setTTL(message.getTTL()-1);
                                message.addPath(server_Port);
                                ObjectOutputStream output=new ObjectOutputStream(ssocket.getOutputStream());
                                output.writeObject(message);
                                ObjectInputStream input=new ObjectInputStream(ssocket.getInputStream());
                                
                                MSG ackb=(MSG)input.readObject();
                                if(ackb.getType().equals("ACK")){
                                    //means it works fine
                                    //System.out.println("ACK");
                                }
                                else {
                                    System.out.print("connection error");
                                }
                                
                                input.close();
                                output.close();
                                ssocket.close();
                                
                            }
                        }
                    }
                }
                else if(request_type.equals("HITQUERRY")&&message.getTTL()>0){
                    
                    out.close();
                    in.close();
                    socket.close();
                    
                    if(message.getPath().isEmpty()){
                        //I am the request node
                        long endTime=System.currentTimeMillis();
                        totaltime+=endTime-message.getstarttime();
                        num_receive++;
                        /*long temp=endTime-message.getstarttime();
                         System.out.println(endTime+" "+message.getstarttime());
                         System.out.println(" this querry use time:"+(temp)+" miliseconds");*/
                        int port=message.getSource();
                        String filename=message.getFilename();
                        if(!DownloadList.containsKey(filename))DownloadList.put(filename, new ArrayList<>());
                        DownloadList.get(filename).add(port);
                        System.out.println(server_Port+" receive querry result from port"+ port);
                        
                    }
                    else{
                        int port=message.delete();
                        Socket ssocket=new Socket("127.0.0.1",port);
                        message.setTTL(message.getTTL()-1);
                        //hitquerry.setID(message.getID());//hit message should have the same id with querry
                        ObjectOutputStream output=new ObjectOutputStream(ssocket.getOutputStream());
                        output.writeObject(message);
                        ObjectInputStream input=new ObjectInputStream(ssocket.getInputStream());
                        MSG ackb=(MSG)input.readObject();
                        if(ackb.getType().equals("ACK")){
                            //means it works fine
                            //System.out.println("ACK");
                        }
                        else {
                            System.out.println("connection error");
                        }
                        input.close();
                        output.close();
                        ssocket.close();
                    }
                }
                else{
                    out.close();
                    in.close();
                    socket.close();
                    
                }
            }catch (Exception e){
                System.out.println("Peer Server "+server_Port+" ERROR:"+e);
            }
            
        }
    }
    /**
     * Querry a file: input a filename
     * @param filename: e.g. "3_5.txt"
     * @throws IOException
     */
    public void Querry(String filename){
        QuerryFile querry=new QuerryFile(filename);
        querry.start();
        while(true){
            if(!querry.isAlive())break;
        }
        return;
    }
    private class QuerryFile extends Thread{
        private String fileName;
        public QuerryFile(String fileName){
            this.fileName=fileName;
        }
        public void run(){
            try{
                if(wait){
                    sleep((long)3000);
                    wait=false;
                }
                //send the querry message to all the neighbors
                //int original=DownloadList.get(fileName).size();
                for(Integer port:neighbors){
                    //System.out.println("send querry to neighbor"+port);
                    Socket socket=new Socket("127.0.0.1",port);
                    MSG querry=new MSG();
                    querry.setSource(server_Port);
                    querry.setFilename(fileName);
                    querry.setTTL(TTL);
                    querry.setPath(new LinkedList<Integer>());
                    querry.addPath(server_Port);
                    querry.setID(server_Port*100000+messagequeue);//unique id for every message
                    querry.setType("QUERRY");
                    querry.setstarttime(System.currentTimeMillis());
                    ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
                    output.writeObject(querry);
                    ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
                    MSG ack=(MSG)in.readObject();
                    if(ack.getType().equals("ACK")){
                        //means receive correct;
                    }
                    else{
                        System.out.println("connection error!");
                    }
                    in.close();
                    output.close();
                    socket.close();
                }
                messagequeue++;
            }catch(Exception e){
                System.out.println("QuerryFileERROR:"+e);
            }
        }
    }
    /**
     * download a file: input filename
     * @param fileName: e.g. "3_5.txt"
     * @throws IOException
     */
    public void Download(String fileName){
        Obtain download=new Obtain(fileName);
        download.start();
        while(true){
            if(!download.isAlive())break;
        }
        return;
    }
    private class Obtain extends Thread{
        private String fileName;
        public Obtain(String fileName){
            this.fileName=fileName;
        }
        public void run(){
            try{
                if(DownloadList.containsKey(fileName)){
                    //from the querry we stored some value in download list, now we can choose which one to download from
                    System.out.print("choose which node you want to download from: ");
                    for(Integer i:DownloadList.get(fileName))System.out.print(i+" ");
                    Scanner sc=new Scanner(System.in);
                    int Download_port=sc.nextInt();
                    sc.close();
                    FileOutputStream bos = new FileOutputStream("output_download.txt");
                    System.setOut(new PrintStream(bos));
                    //create a connection to the desired server
                    Socket socket=new Socket("127.0.0.1",Download_port);
                    MSG request_download=new MSG();
                    request_download.setType("OBTAIN");
                    request_download.setFilename(fileName);
                    //using buffer to down load the file
                    ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(request_download);
                    InputStream download_file=socket.getInputStream();
                    System.out.println("download file recieve!");
                    byte[] bufferarray=new byte[BUFFER_SIZE];
                    BufferedOutputStream file_download=new BufferedOutputStream(new FileOutputStream(Myfolder +"/"+ fileName));
                    while(true){
                        int bytes_read=download_file.read(bufferarray,0,bufferarray.length);
                        System.out.println("downloading "+bytes_read+"bytes");
                        if(bytes_read>0)file_download.write(bufferarray,0,bytes_read);
                        else break;
                    }
                    System.out.println("download "+fileName+" finished!");
                    file_download.close();
                    download_file.close();
                    socket.close();
                    out.close();
                }
                else{
                    System.out.println("you should querry this file first");
                }
            }
            catch(Exception e){
                System.out.println("ObtainERROR:"+e);
            }
        }
    }
}







