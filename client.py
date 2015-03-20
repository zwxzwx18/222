import socket
import sys
import random
import time

count = 0
NumOfPacket = random.randint(2, 5)
while count < NumOfPacket:
    destination = random.randint(1,3)
    if destination == 1:
        HOST = '10.0.0.1'
    elif destination == 2:
        HOST = '10.0.0.2'
    else:
        HOST = '10.0.0.3'
    HOST='10.0.0.1'
    PORT = 8000
    s = None
    for res in socket.getaddrinfo(HOST,PORT,socket.AF_UNSPEC,socket.SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        try:
            s = socket.socket(af,socktype,proto)
        except socket.error, msg:
            s = None
            continue
        try:
            s.connect(sa)
        except socket.error, msg:
            s = None
            continue
        break
    if s is None:   
        print 'could not open socket'
        sys.exit(1)
    s.sendall('Hello, world')
    print "ok"
   # time.sleep(1)
    s.close()
   # print 'Received', repr(data)
   # time.sleep(5.00)
    count = count + 1
