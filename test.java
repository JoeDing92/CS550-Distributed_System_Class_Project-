import java.io.*;
import java.util.Scanner;

public class test {
    //args[0] is how many concurrent node you want to create
    public static void main(String args[]){
        PeerNode[] peer_array=new PeerNode[10];
        System.out.println("star or linear?(1 for star,2 for linear)");
        Scanner sc0=new Scanner(System.in);
        int num0=sc0.nextInt();
        if(num0==1){
        for(int i=0;i<10;i++){
            peer_array[i]=new PeerNode();
            PeerNode_create p1=new PeerNode_create(peer_array[i],"Folder/peer"+(i+1),Topology.STAR);
            p1.start();
        }}
        else{
            for(int i=0;i<10;i++){
                peer_array[i]=new PeerNode();
                PeerNode_create p1=new PeerNode_create(peer_array[i],"Folder/peer"+(i+1),Topology.LINEAR);
                p1.start();
            }
        }
        //int num=Integer.parseInt(args[0]);
        System.out.println("how many concurrent node you want(from 1-5)");
        Scanner sc=new Scanner(System.in);
        int num=sc.nextInt();
        PrintStream old = System.out;
        try{
        FileOutputStream bos = new FileOutputStream("../output/output"+num0+"_"+num+".txt");
        System.setOut(new PrintStream(bos));
        }catch (Exception e){}
        long t=average_querry(num,peer_array);
        Thread thread=new Thread();
        try{
            thread.sleep((long)3000);
        }catch(Exception e){
            System.out.println(e);
        }
        while(thread.isAlive()){}
        System.out.println(num+" concurrent query 200 times average query use time:"+(double)(t/(double)(num))+" miliseconds");
        System.setOut(old);
        System.out.println("finish!");
    }
    private static long average_querry(int num,PeerNode[] peer_array){
        long totaltime=0;
        for(int i=0;i<200;i++){
            for(int j=0;j<num;j++){
                peer_array[j].Querry("4.txt");
            }
        }
        for(int i=0;i<num;i++){
            if(peer_array[i].totaltime!=0)totaltime+=(peer_array[i].totaltime/peer_array[i].num_receive);
            System.out.println(i+" "+peer_array[i].num_receive);
        }
        return totaltime;
        //System.out.println("200 times querry use time:"+(double)(totaltime/(double)200)+" seconds");
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
