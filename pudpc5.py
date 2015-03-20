from socket import *
import sys
import random
import time
import threading
import math
Host='10.0.0.1'
Port=8000
Bufsize=1024

def poisson(L):
    p=1.0
    k=0
    e=math.exp(-L)
    while p>=e:
        u=random.random()
        p*=u
        k+=1
    return k-1

def udpdata(name):
    count=0.0
    sNumpack=0
    while count<=100:
        t1=1.0*poisson(58)/100
        time.sleep(t1)
        ranhost=random.randint(1,3)
        if ranhost==1:
            Host='10.0.0.1'
        elif ranhost==2:
            Host='10.0.0.2'
        else:
            Host='10.0.0.3'
        Addr=(Host,Port)
        t=time.time()
        udpCliSock=socket(AF_INET,SOCK_DGRAM)
        udpCliSock.sendto("hello world",Addr)
        udpCliSock.close()
        file=open("host5.txt", 'w')
        sNumpack=sNumpack+1
        file.write("count:"+str(count+1)+"Num of Packet:"+str(sNumpack)+"to:"+str(ranhost)+"time:"+str(t)+'\n')
        count=count+t1
        file.close()

def tcp(name):
    import socket
    Host='10.0.0.8'
    Port=8000
    count=0
    while count<20:
        s=None
        time.sleep(5)
        print "start tcp"
        for res in socket.getaddrinfo(Host,Port,socket.AF_UNSPEC,socket.SOCK_STREAM):
            af, socktype, proto, canonname, sa=res
            try:
                s=socket.socket(af,socktype,proto)
            except socket.error, msg:
                s=None
                continue
            try:
                s.connect(sa)
            except socket.error, msg:
                s=None
                continue
            break
        if s is None:
            print 'cannot open'
            sys.exit(1)
        s.sendall('Hello world')
        s.close()
        count=count+1

threads=[]
t1=threading.Thread(target=udpdata, args=(u'a'))
t2=threading.Thread(target=tcp, args=(u'b'))
threads.append(t1)
threads.append(t2)

for t in threads:
    t.setDaemon(True)
    t.start()
t.join()
