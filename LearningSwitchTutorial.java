/**
 * Copyright 2011, Stanford University. This file is licensed under GPL v2 plus
 * a special exception, as described in included LICENSE_EXCEPTION.txt.
 */
package net.beaconcontroller.tutorial;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import net.beaconcontroller.core.IBeaconProvider;
import net.beaconcontroller.core.IOFMessageListener;
import net.beaconcontroller.core.IOFSwitch;
import net.beaconcontroller.core.IOFSwitchListener;
import net.beaconcontroller.packet.Ethernet;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tutorial class used to teach how to build a simple layer 2 learning switch.
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - 10/14/12
 */
@SuppressWarnings("unused")
public class LearningSwitchTutorial implements IOFMessageListener, IOFSwitchListener {
    protected static Logger log = LoggerFactory.getLogger(LearningSwitchTutorial.class);
    protected IBeaconProvider beaconProvider;
    protected Map<IOFSwitch, Map<Long,Short>> macTables =
        new HashMap<IOFSwitch, Map<Long,Short>>();
    //following is what I implement
    protected Map<Integer, Map<Long,Integer>> packet=
        new HashMap<Integer, Map<Long,Integer>>();
    //protected OFPacketIn buf[][]=new OFPacketIn[3][100];
    protected Map<Integer, Map<Long, Vector<OFPacketIn>>> buf=
            new HashMap<Integer, Map<Long, Vector<OFPacketIn>>>();
    protected Map<Integer, Map<Long, Vector<Integer>>> time=
            new HashMap<Integer, Map<Long, Vector<Integer>>>();
    protected int sequence[]={0, 0, 0};
    //protected int len[]={0, 0, 0};
    protected Vector<Long> select=new Vector<Long>();
    protected int turn=0;
    protected int transmit=0;
    protected double s1=1/1.555, s2=1/1.21, s3=1;
    protected double sum[]={0,0,0};
    protected int agg=0;
    protected int mark=0;//indicate that if it is the first node of this time slot which means controller need to make decision
    protected int ack[]={-1, -1, -1};
    protected Map<Long, Short> hmatchs=
        new HashMap<Long, Short>(); //用来记录host对应的相应switch
    protected Long allocate[]=new Long[3];
    protected int throughput = 0;
    protected int threshold = 10;
    protected int overall[]={0,0,0};
    
   
    public Command receive(IOFSwitch sw, OFMessage msg) throws IOException {
        initMACTable(sw);
        OFPacketIn pi = (OFPacketIn) msg;
        if((int)sw.getId()==1||(int)sw.getId()==2||(int)sw.getId()==3||(int)sw.getId()==7||(int)sw.getId()==8||(int)sw.getId()==9||(int)sw.getId()==10) {
            forwardAsLearningSwitch(sw, pi);
            return Command.CONTINUE;
        }
        if((int)pi.getTotalLength()==53) {//indicate that it is udp
            OFMatch match = new OFMatch();
            match.loadFromPacket(pi.getPacketData(), pi.getInPort());
            Long destinationMACHash = Ethernet.toLong(match.getDataLayerDestination());
            //System.out.println("hi");
            if(buf.containsKey((int)sw.getId())) {
                //System.out.println(buf.get((int)sw.getId()).size());
                if(buf.get((int)sw.getId()).containsKey(destinationMACHash)) {
                    Map<Long, Vector<OFPacketIn>> tmp=buf.get((int)sw.getId());
                    Vector<OFPacketIn> x=tmp.get(destinationMACHash);
                    x.add(pi);
                    tmp.put(destinationMACHash, x);
                    buf.put((int)sw.getId(), tmp);
                    Map<Long, Vector<Integer>> tmp2=time.get((int)sw.getId());
                    Vector<Integer> x2=tmp2.get(destinationMACHash);
                    x2.add(sequence[(int)sw.getId()-4]);//
                    sequence[(int)sw.getId()-4]++;
                    tmp2.put(destinationMACHash, x2);
                    time.put((int)sw.getId(), tmp2);
                }
                else {
                    Map<Long, Vector<OFPacketIn>> tmp=buf.get((int)sw.getId());
                    Vector<OFPacketIn> x=new Vector<OFPacketIn>();
                    x.add(pi);
                    tmp.put(destinationMACHash, x);
                    buf.put((int)sw.getId(), tmp);
                    Map<Long, Vector<Integer>> tmp2=time.get((int)sw.getId());
                    Vector<Integer> x2=new Vector<Integer>();
                    x2.add(sequence[(int)sw.getId()-4]);//
                    sequence[(int)sw.getId()-4]++;
                    tmp2.put(destinationMACHash, x2);
                    time.put((int)sw.getId(), tmp2);
                }
            }
            else {
                Map<Long, Vector<OFPacketIn>> tmp=new HashMap<Long, Vector<OFPacketIn>>();
                Vector<OFPacketIn> x=new Vector<OFPacketIn>();
                x.add(pi);
                tmp.put(destinationMACHash, x);
                buf.put((int)sw.getId(), tmp);
                Map<Long, Vector<Integer>> tmp2=new HashMap<Long, Vector<Integer>>();
                Vector<Integer> x2=new Vector<Integer>();
                x2.add(sequence[(int)sw.getId()-4]);//
                sequence[(int)sw.getId()-4]++;
                tmp2.put(destinationMACHash, x2);
                time.put((int)sw.getId(), tmp2);
            }
            if(packet.containsKey((int)sw.getId())) {
                if(packet.get((int)sw.getId()).containsKey(destinationMACHash)) {
                    int count=packet.get((int)sw.getId()).get(destinationMACHash)+1;
                    Map<Long, Integer> tmp=packet.get((int)sw.getId());
                    tmp.put(destinationMACHash, count);
                    packet.put((int)sw.getId(), tmp);//update了switch中有几个这个host的包
                }
                else {
                    int count=1;
                    Map<Long, Integer> tmp=packet.get((int)sw.getId());
                    tmp.put(destinationMACHash, count);
                    packet.put((int)sw.getId(), tmp);//update了switch中有几个这个host的包
                }
            }
            else {
                Map<Long, Integer> tmp=new HashMap<Long, Integer>();
                tmp.put(destinationMACHash, 1);
                packet.put((int)sw.getId(), tmp);
            }
            System.out.println(sw.getId() + ":receive the packet and hold");
            return Command.CONTINUE;
        }
        else {
            if((int)pi.getTotalLength()==74) {//indicate that time slot begin
                //注意ack
                ack[(int)sw.getId()-4]=(-ack[(int)sw.getId()-4]);
                if(ack[(int)sw.getId()-4]==-1) {
                    //indicate that it is an ack packet
                    forwardAsLearningSwitch(sw, pi);
                    return Command.CONTINUE;
                }
                else {
                    System.out.println(sw.getId()+":tcp start");
                    forwardAsLearningSwitch(sw, pi);//先把tcp传了
                    //naive algorithm
                    /*if(turn==0) {
                        int max=0;
                        Long fuck=(long) 0;
                        //allocate the path for this host
                        Iterator<Entry<Long, Integer>> iter=
                                packet.get((int)sw.getId()).entrySet().iterator();
                        if(select.size()>=2) {
                            while(iter.hasNext()) {
                            Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                            if(entry.getKey()!=select.lastElement() && entry.getKey()!=select.firstElement()) {
                                fuck=entry.getKey();
                                max=entry.getValue();
                            }}
                            select.addElement(fuck);
                        }
                        else {
                        if(select.size()==1) {
                            while(iter.hasNext()) {
                                Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                                if(entry.getValue() >= max && entry.getKey()!=select.firstElement()) {
                                    fuck=entry.getKey();
                                    max=entry.getValue();
                                }
                            }
                            select.addElement(fuck);
                        }
                        else {
                            while(iter.hasNext()) {
                                Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                                if(entry.getValue() >= max) {
                                    fuck=entry.getKey();
                                    max=entry.getValue();
                                }
                            }
                            select.addElement(fuck);
                        }
                        }
                        Map<Long, Integer> tmp=packet.get((int)sw.getId());
                        int i=0;
                        while(i<threshold) {
                            Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                            Vector<OFPacketIn> x=now.get(select.lastElement());
                            if(x.size()<=0) break;
                            forwardAsLearningSwitch(sw, x.firstElement());
                            x.removeElementAt(0);
                            now.put(select.lastElement(), x);
                            buf.put((int)sw.getId(), now);
                            i++;
                        }
                        System.out.println(sw.getId()+"sent: "+ i);
                        tmp.put(select.lastElement(), Math.max(0,max-threshold));
                        packet.put((int)sw.getId(), tmp);
                    }
                    if(select.size()>=3) select.clear();*/
                    /*if(mark==0) {//round robin
                        //allocate the channel
                        int max=0;
                        Long fuck=(long) 0;
                        //allocate the path for this host
                        if(turn==0) {
                            Iterator<Entry<Long, Integer>> iter=
                                    packet.get(4).entrySet().iterator();
                            while(iter.hasNext()) {
                                Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                                if(entry.getValue() >= max) {
                                    fuck=entry.getKey();
                                    max=entry.getValue();
                                }
                            }
                            allocate[0]=fuck;
                            max=0;
                            Iterator<Entry<Long, Integer>> iter2=
                                    packet.get(5).entrySet().iterator();
                            while(iter2.hasNext()) {
                                Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter2.next();
                                if(entry.getValue() >= max && entry.getKey()!=allocate[0]) {
                                    fuck=entry.getKey();
                                    max=entry.getValue();
                                }
                            }
                            allocate[1]=fuck;
                            max=0;
                            Iterator<Entry<Long, Integer>> iter3=
                                    packet.get(6).entrySet().iterator();
                            while(iter3.hasNext()) {
                                Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter3.next();
                                if(entry.getKey()!=allocate[0] && entry.getKey()!=allocate[1]) {
                                    fuck=entry.getKey();
                                    max=entry.getValue();
                                }
                            }
                            allocate[2]=fuck;
                        }
                        else {
                            if(turn==1) {
                                Iterator<Entry<Long, Integer>> iter=
                                        packet.get(5).entrySet().iterator();
                                while(iter.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                                    if(entry.getValue() >= max) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                    }
                                }
                                allocate[1]=fuck;
                                max=0;
                                Iterator<Entry<Long, Integer>> iter2=
                                        packet.get(6).entrySet().iterator();
                                while(iter2.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter2.next();
                                    if(entry.getValue() >= max && entry.getKey()!=allocate[1]) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                    }
                                }
                                allocate[2]=fuck;
                                max=0;
                                Iterator<Entry<Long, Integer>> iter3=
                                        packet.get(4).entrySet().iterator();
                                while(iter3.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter3.next();
                                    if(entry.getKey()!=allocate[1] && entry.getKey()!=allocate[2]) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                        break;
                                    }
                                }
                                allocate[0]=fuck;
                            }
                            else {
                                Iterator<Entry<Long, Integer>> iter=
                                        packet.get(6).entrySet().iterator();
                                while(iter.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                                    if(entry.getValue() >= max) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                    }
                                }
                                allocate[2]=fuck;
                                max=0;
                                Iterator<Entry<Long, Integer>> iter2=
                                        packet.get(4).entrySet().iterator();
                                while(iter2.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter2.next();
                                    if(entry.getValue() >= max && entry.getKey()!=allocate[2]) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                    }
                                }
                                allocate[0]=fuck;
                                max=0;
                                Iterator<Entry<Long, Integer>> iter3=
                                        packet.get(5).entrySet().iterator();
                                while(iter3.hasNext()) {
                                    Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter3.next();
                                    if(entry.getKey()!=allocate[1] && entry.getKey()!=allocate[0]) {
                                        fuck=entry.getKey();
                                        max=entry.getValue();
                                    }
                                }
                                allocate[1]=fuck;
                            }
                        }
                        mark=1;
                        turn=(turn+1)%3;
                    }
                    //forward packet based on the decision
                    int i=0;
                    while(i<threshold) {
                        Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                        Map<Long, Integer> now2=packet.get((int)sw.getId());
                        Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                        int x2=now2.get(allocate[(int)sw.getId()-4]);
                        if(x.size()<=0) break;
                        forwardAsLearningSwitch(sw, x.firstElement());
                        x.removeElementAt(0);
                        x2--;
                        now.put(allocate[(int)sw.getId()-4], x);
                        now2.put(allocate[(int)sw.getId()-4], x2);
                        buf.put((int)sw.getId(), now);
                        packet.put((int)sw.getId(), now2);
                        i++;
                    }
                    overall[(int)sw.getId()-4]+=i;
                    System.out.println(sw.getId()+"sent: "+ overall[(int)sw.getId()-4]);
                    agg=(agg+1)%3;
                    if(agg==0) mark=0;*/
                    //shortest deadline first
                    /*if(mark==0){
                    int min=100000;
                    mark=1;
                    Long fuck=(long) 0;
                    int thistime=4;
                    Iterator<Entry<Long, Vector<Integer>>> iter;
                    for(int j=4; j<=6; j++) {
                    iter=time.get(j).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Vector<Integer>> entry=(Map.Entry<Long, Vector<Integer>>) iter.next();
                        if(entry.getValue().size()>0) {
                        if(entry.getValue().firstElement()<min) {
                            fuck=entry.getKey();
                            min=entry.getValue().firstElement();
                            thistime=j;
                        }}
                    }}
                    allocate[thistime-4]=fuck;
                    //System.out.println("ok1");
                    fuck=(long) 0;
                    min=100000;
                    int thistime2=4;
                    for(int j=4; j<=6; j++) {
                    if(j==thistime) continue;
                    iter=time.get(j).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Vector<Integer>> entry=(Map.Entry<Long, Vector<Integer>>) iter.next();
                        if(entry.getValue().size()>0) {
                        if(entry.getValue().firstElement()<min&&entry.getKey()!=allocate[thistime-4]) {
                            fuck=entry.getKey();
                            min=entry.getValue().firstElement();
                            thistime2=j;
                        }}
                    }}
                    allocate[thistime2-4]=fuck;
                    //System.out.println("ok2");
                    fuck=(long) 0;
                    int thistime3=4;
                    for(int j=4; j<=6; j++) {
                    if(j==thistime||j==thistime2) continue;
                    iter=time.get(j).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Vector<Integer>> entry=(Map.Entry<Long, Vector<Integer>>) iter.next();
                        if(entry.getKey()!=allocate[thistime-4]&&entry.getKey()!=allocate[thistime2-4]) {
                            fuck=entry.getKey();
                            thistime3=j;
                            break;
                        }
                    }}
                    allocate[thistime3-4]=fuck;
                    //System.out.println("ok3");
                    }
                    int i=0;
                    int send=-1;
                    while(i<threshold) {
                        Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                        Map<Long, Vector<Integer>> now2=time.get((int)sw.getId());
                        Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                        Vector<Integer> x2=now2.get(allocate[(int)sw.getId()-4]);
                        if(x.size()<=0) break;
                        forwardAsLearningSwitch(sw, x.firstElement());
                        x.removeElementAt(0);
                        x2.removeElementAt(0);
                        if(x2.size()>0) send=x2.firstElement();
                        else send=-1;
                        now.put(allocate[(int)sw.getId()-4], x);
                        now2.put(allocate[(int)sw.getId()-4], x2);
                        buf.put((int)sw.getId(), now);
                        time.put((int)sw.getId(), now2);
                        i++;
                    }
                    System.out.println(sw.getId()+"sent: "+ i + "the oldest packet is: " + send);
                    agg=(agg+1)%3;
                    if(agg==0) mark=0;*/
                  //algorithm for best throughput
                   /*if(mark==0){
                    mark=1;
                    int max=0;
                    Iterator<Entry<Long, Integer>> iter=packet.get(4).entrySet().iterator();
                    Iterator<Entry<Long, Integer>> iter2=packet.get(5).entrySet().iterator();
                    Iterator<Entry<Long, Integer>> iter3=packet.get(6).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                        int t1=entry.getValue()>threshold?threshold:entry.getValue();
                        while(iter2.hasNext()) {
                            Map.Entry<Long, Integer> entry2=(Map.Entry<Long, Integer>) iter2.next();
                            int t2=entry2.getValue()>threshold?threshold:entry2.getValue();
                            if(entry2.getKey()==entry.getKey()) continue;
                            while(iter3.hasNext()) {
                                Map.Entry<Long, Integer> entry3=(Map.Entry<Long, Integer>) iter3.next();
                                if(entry3.getKey()==entry.getKey()||entry3.getKey()==entry2.getKey()) continue;
                                int t3=entry3.getValue()>threshold?threshold:entry3.getValue();
                                if(t1+t2+t3>=max) {
                                    max=t1+t2+t3;
                                    allocate[0]=entry.getKey();
                                    allocate[1]=entry2.getKey();
                                    allocate[2]=entry3.getKey();
                                }
                            }
                        }
                    }
                    }
                    int i=0;
                    while(i<threshold) {
                        Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                        Map<Long, Integer> now2=packet.get((int)sw.getId());
                        Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                        int x2=now2.get(allocate[(int)sw.getId()-4]);
                        if(x.size()<=0) break;
                        forwardAsLearningSwitch(sw, x.firstElement());
                        x.removeElementAt(0);
                        x2--;
                        now.put(allocate[(int)sw.getId()-4], x);
                        now2.put(allocate[(int)sw.getId()-4], x2);
                        buf.put((int)sw.getId(), now);
                        packet.put((int)sw.getId(), now2);
                        i++;
                    }
                    overall[(int)sw.getId()-4]+=i;
                    System.out.println(sw.getId()+"sent: "+ overall[(int)sw.getId()-4]);
                    agg=(agg+1)%3;
                    if(agg==0) mark=0;*/
                    //find the longest total queue length and let it determine first
                    if(mark==0){
                    int length[]={0,0,0};
                    int order[]={4,5,6};
                    int tmp=0;
                    int max=0;
                    Long fuck=(long) 0;
                    Iterator<Entry<Long, Integer>> iter;
                    for(int j=4; j<=6; j++) {
                        tmp=0;
                        iter=packet.get(j).entrySet().iterator();
                        while(iter.hasNext()) {
                            Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                            tmp=tmp+entry.getValue();
                        }
                        if(tmp>length[0]) {
                            order[2]=order[1];
                            order[1]=order[0];
                            order[0]=j;
                            length[2]=length[1];
                            length[1]=length[0];
                            length[0]=tmp;
                        }
                        else {
                            if(tmp>length[1]) {
                                order[2]=order[1];
                                length[2]=length[1];
                                order[1]=j;
                                length[1]=tmp;
                            }
                            else {
                                order[2]=j;
                                length[2]=tmp;
                            }
                        }
                    }
                    iter=packet.get(order[0]).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                        if(entry.getValue()>=max) {
                            max=entry.getValue();
                            fuck=entry.getKey();
                        }
                    }
                    max=0;
                    allocate[order[0]-4]=fuck;
                    iter=packet.get(order[1]).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                        if(entry.getValue()>=max && entry.getKey()!=allocate[order[0]-4]) {
                            max=entry.getValue();
                            fuck=entry.getKey();
                        }
                    }
                    allocate[order[1]-4]=fuck;
                    iter=packet.get(order[2]).entrySet().iterator();
                    while(iter.hasNext()) {
                        Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                        if(entry.getKey()!=allocate[order[1]-4]&&entry.getKey()!=allocate[order[0]-4]) {
                            fuck=entry.getKey();
                        }
                    }
                    allocate[order[2]-4]=fuck;
                    mark=1;System.out.println("ok");
                    }
                    int i=0;
                    int send=-1;
                    while(i<threshold) {
                        /*Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                        Map<Long, Integer> now2=packet.get((int)sw.getId());
                        Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                        int x2=now2.get(allocate[(int)sw.getId()-4]);
                        if(x.size()<=0) break;
                        forwardAsLearningSwitch(sw, x.firstElement());
                        x.removeElementAt(0);
                        x2--;
                        now.put(allocate[(int)sw.getId()-4], x);
                        now2.put(allocate[(int)sw.getId()-4], x2);
                        buf.put((int)sw.getId(), now);
                        packet.put((int)sw.getId(), now2);
                        i++;*/
                        Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                        Map<Long, Vector<Integer>> now2=time.get((int)sw.getId());
                        Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                        Vector<Integer> x2=now2.get(allocate[(int)sw.getId()-4]);
                        Map<Long, Integer> now3=packet.get((int)sw.getId());
                        if(x.size()<=0) break;
                        forwardAsLearningSwitch(sw, x.firstElement());
                        int x3=now3.get(allocate[(int)sw.getId()-4]);
                        x3--;
                        now3.put(allocate[(int)sw.getId()-4], x3);
                        buf.put((int)sw.getId(), now);
                        packet.put((int)sw.getId(), now3);
                        x.removeElementAt(0);
                        x2.removeElementAt(0);
                        if(x2.size()>0) send=x2.firstElement();
                        else send=-1;
                        now.put(allocate[(int)sw.getId()-4], x);
                        now2.put(allocate[(int)sw.getId()-4], x2);
                        buf.put((int)sw.getId(), now);
                        time.put((int)sw.getId(), now2);
                        i++;
                    }
                    overall[(int)sw.getId()-4]+=i;
                    System.out.println(sw.getId()+"sent: "+ i + "the oldest packet is: " + send);
                    System.out.println(sw.getId()+"sent: "+ overall[(int)sw.getId()-4]);
                    agg=(agg+1)%3;
                    if(agg==0) mark=0;
                    //stride scheduling
                    /*if(mark==0){
                        int order[]={4,5,6};
                        int max=0;
                        if(sum[2]<sum[1]&&sum[2]<sum[0]) {
                            order[0]=6;
                            sum[2]=sum[2]+s3;
                            if(sum[1]<sum[0]) {order[1]=5; order[2]=4;
                            sum[1]=sum[1]+s2/2;}
                            else {order[1]=4; order[2]=5;
                            sum[0]=sum[0]+s1/2;}
                        }
                        else {
                            if(sum[1]<sum[2]&&sum[1]<sum[0]) {
                                order[0]=5;
                                sum[1]=sum[1]+s2;
                                if(sum[2]<sum[0]) {order[1]=6; order[2]=4;
                                sum[2]=sum[2]+s3/2;}
                                else {order[1]=4; order[2]=6;
                                sum[0]=sum[0]+s1/2;}
                            }
                            else {
                                order[0]=4;
                                sum[0]=sum[0]+s1;
                                if(sum[2]<sum[1]) {order[1]=6; order[2]=5;
                                sum[2]=sum[2]+s3/2;}
                                else {order[1]=5; order[2]=6;
                                sum[1]=sum[1]+s2/2;}
                            }
                        }
                        Long fuck=(long) 0;
                        Iterator<Entry<Long, Integer>> iter;
                        iter=packet.get(order[0]).entrySet().iterator();
                        while(iter.hasNext()) {
                            Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                            if(entry.getValue()>=max) {
                                max=entry.getValue();
                                fuck=entry.getKey();
                            }
                        }
                        max=0;
                        allocate[order[0]-4]=fuck;
                        iter=packet.get(order[1]).entrySet().iterator();
                        while(iter.hasNext()) {
                            Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                            if(entry.getValue()>=max && entry.getKey()!=allocate[order[0]-4]) {
                                max=entry.getValue();
                                fuck=entry.getKey();
                            }
                        }
                        allocate[order[1]-4]=fuck;
                        iter=packet.get(order[2]).entrySet().iterator();
                        while(iter.hasNext()) {
                            Map.Entry<Long, Integer> entry=(Map.Entry<Long, Integer>) iter.next();
                            if(entry.getKey()!=allocate[order[1]-4]&&entry.getKey()!=allocate[order[0]-4]) {
                                fuck=entry.getKey();
                            }
                        }
                        allocate[order[2]-4]=fuck;
                        mark=1;System.out.println("ok");
                        }
                        int i=0;
                        while(i<threshold) {
                            Map<Long, Vector<OFPacketIn>> now=buf.get((int)sw.getId());
                            Map<Long, Integer> now2=packet.get((int)sw.getId());
                            Vector<OFPacketIn> x=now.get(allocate[(int)sw.getId()-4]);
                            int x2=now2.get(allocate[(int)sw.getId()-4]);
                            if(x.size()<=0) break;
                            forwardAsLearningSwitch(sw, x.firstElement());
                            x.removeElementAt(0);
                            x2--;
                            now.put(allocate[(int)sw.getId()-4], x);
                            now2.put(allocate[(int)sw.getId()-4], x2);
                            buf.put((int)sw.getId(), now);
                            packet.put((int)sw.getId(), now2);
                            i++;
                        }
                        overall[(int)sw.getId()-4]+=i;
                        System.out.println(sw.getId()+"sent: "+ overall[(int)sw.getId()-4]);
                        agg=(agg+1)%3;
                        if(agg==0) mark=0;*/
                    return Command.CONTINUE;
                }
            }
            else {
                //ping packets or ARP packets
                forwardAsLearningSwitch(sw, pi);
                return Command.CONTINUE;
            }
        }
        //forwardAsLearningSwitch(sw, pi);
        //return Command.CONTINUE;
    }

    /**
     * EXAMPLE CODE: Floods the packet out all switch ports except the port it
     * came in on.
     *
     * @param sw the OpenFlow switch object
     * @param pi the OpenFlow Packet In object
     * @throws IOException
     */
    public void forwardAsHub(IOFSwitch sw, OFPacketIn pi) throws IOException {
        // Create the OFPacketOut OpenFlow object
        OFPacketOut po = new OFPacketOut();

        // Create an output action to flood the packet, put it in the OFPacketOut
        OFAction action = new OFActionOutput(OFPort.OFPP_FLOOD.getValue());
        po.setActions(Collections.singletonList(action));

        // Set the port the packet originally arrived on
        po.setInPort(pi.getInPort());

        // Reference the packet buffered at the switch by id
        po.setBufferId(pi.getBufferId());
        if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
            /**
             * The packet was NOT buffered at the switch, therefore we must
             * copy the packet's data from the OFPacketIn to our new
             * OFPacketOut message.
             */
            po.setPacketData(pi.getPacketData());
        }
        // Send the OFPacketOut to the switch
        sw.getOutputStream().write(po);
    }

    /**
     * TODO: Learn the source MAC:port pair for each arriving packet. Next send
     * the packet out the port previously learned for the destination MAC, if it
     * exists. Otherwise flood the packet similarly to forwardAsHub.
     *
     * @param sw the OpenFlow switch object
     * @param pi the OpenFlow Packet In object
     * @throws IOException
     */
    public void forwardAsLearningSwitch(IOFSwitch sw, OFPacketIn pi) throws IOException {
        Map<Long,Short> macTable = macTables.get(sw);
        //String str = new String(pi.getPacketData(), "UTF-8");
        //System.out.println(sw.getId()+" "+pi.getTotalLength());
        int i=0;
        //while (i<pi.getPacketData().length)
        //{System.out.print(pi.getPacketData()[i]+" ");
        //i++;}
        //System.out.print(pi.getPacketData());
        //OFPacketOut po = new OFPacketOut();
         OFMatch match = new OFMatch();
         match.loadFromPacket(pi.getPacketData(), pi.getInPort());
         Long sourceMACHash = Ethernet.toLong(match.getDataLayerSource());
         Long destinationMACHash = Ethernet.toLong(match.getDataLayerDestination());
         Short outPort = macTable.get(destinationMACHash);
         Short source = macTable.get(sourceMACHash);
         if (outPort != null) {
             OFAction action = new OFActionOutput(outPort);
             OFFlowMod fm = new OFFlowMod();
             fm.setBufferId(pi.getBufferId());
             //fm.setBufferId(0xffffffff);
             fm.setCommand(OFFlowMod.OFPFC_ADD);
             fm.setIdleTimeout( (short) 200);
             fm.setMatch(match);
             fm.setActions(Collections.singletonList(action));
             sw.getOutputStream().write(fm);
             //po.setInPort(pi.getInPort());
             //po.setBufferId(pi.getBufferId());
             //po.setActions(Collections.singletonList(action));
             //sw.getOutputStream().write(po);
             if(source == null) {
                 macTable.put(sourceMACHash, pi.getInPort());
             }
             if(pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
                 OFPacketOut po = new OFPacketOut();
                 action = new OFActionOutput(outPort);
                 po.setActions(Collections.singletonList(action));
                 po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
                 po.setInPort(pi.getInPort());
                 po.setPacketData(pi.getPacketData());
                 sw.getOutputStream().write(po);
             }
             //sw.getOutputStream().write(po);
         }
         else {
             if(source == null) {
                 macTable.put(sourceMACHash, pi.getInPort());
             }
             forwardAsHub(sw, pi);
         }
    }

    // ---------- NO NEED TO EDIT ANYTHING BELOW THIS LINE ----------

    /**
     * Ensure there is a MAC to port table per switch
     * @param sw
     */
    private void initMACTable(IOFSwitch sw) {
        Map<Long,Short> macTable = macTables.get(sw);
        if (macTable == null) {
            macTable = new HashMap<Long,Short>();
            macTables.put(sw, macTable);
        }
    }

    @Override
    public void addedSwitch(IOFSwitch sw) {
    }

    @Override
    public void removedSwitch(IOFSwitch sw) {
        macTables.remove(sw);
    }

    /**
     * @param beaconProvider the beaconProvider to set
     */
    public void setBeaconProvider(IBeaconProvider beaconProvider) {
        this.beaconProvider = beaconProvider;
    }

    public void startUp() {
        log.trace("Starting");
        beaconProvider.addOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.addOFSwitchListener(this);
    }

    public void shutDown() {
        log.trace("Stopping");
        beaconProvider.removeOFMessageListener(OFType.PACKET_IN, this);
        beaconProvider.removeOFSwitchListener(this);
    }

    public String getName() {
        return "tutorial";
    }
}
