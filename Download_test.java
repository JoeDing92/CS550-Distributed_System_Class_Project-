
import java.util.Scanner;
import java.io.*;
public class Download_test {
     public static void main(String args[]){
     	PeerNode[] peer_array=new PeerNode[10];
     	for(int i=0;i<10;i++){
     		peer_array[i]=new PeerNode();
     		PeerNode_create p1=new PeerNode_create(peer_array[i],"Folder/peer"+(i+1),Topology.STAR);
     		p1.start();
     	}
         PrintStream old = System.out;
     	peer_array[0].Querry("2.txt");
         Thread thread=new Thread();
    	    try{
                thread.sleep((long)3000);
            }catch(Exception e){
                System.out.println(e);
            }
    	    while(thread.isAlive()){}

     	peer_array[0].Download("2.txt");
         System.setOut(old);
         System.out.println("finish!");
        
     }
 	public static class PeerNode_create extends Thread{
 		private String folder;
 		private Topology topo;
 		private PeerNode p;
 		public PeerNode_create(PeerNode peer,String s,Topology t){this.folder=s;this.topo=t;this.p=peer;}
 		public void run(){
 			try{	
 				p.initialize(folder, topo);
 			}
 			catch(Exception e){
 				System.out.println("PeerNodeCreateERROR:"+e);
 			}
 		}
 	}
}
