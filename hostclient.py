import socket
import sys
import time

HOST='10.0.0.2'
PORT=8000
s=None
for res in socket.getaddrinfo(HOST, PORT, socket.AF_UNSPEC, socket.SOCK_STREAM):
    af, socktype, proto, canonname, sa = res
    try:
        s=socket.socket(af, socktype, proto)
    except socket.error, msg:
        print 'except 1'
        s=None
        continue
    try:
        s.connect(sa)
    except socket.error, meg:
        print 'except 2'
        s.close()
        s=None
        continue
    break
if s is None:
    print 'could not open socket'
    sys.exit(1)

string='Hello world'
#s.send(string.encode('utf-8'))
#byar=bytearray('\xff')
#s.send(byar)
s.send(string)
print 'send data'
data=s.recv(1024)
s.close()
print data
#time.sleep(1)
#s.send(bytearray('\x01'))
#print 'send data2'
#data=s.recv(1024)
#s.close()
#print data[0]
