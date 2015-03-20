from socket import *
import time

Host='10.0.0.3'
Port=8000
Bufsize=1024

Addr=(Host,Port)

udpSerSock=socket(AF_INET, SOCK_DGRAM)
udpSerSock.bind(Addr)
#file=open("host3.txt", 'w')
count=0
while True:
    data, addr=udpSerSock.recvfrom(Bufsize)
    #udpSerSock.sendo('[%s] %s'%(ctime(),data),addr)
    print 'receive from', addr
    if data=="hello world":
        count=count+1
        file=open("host3.txt", 'w')
        file.write("count: " + str(count) + "data :" + repr(data) + '\n')
        file.close()
udpSerSock.close()
