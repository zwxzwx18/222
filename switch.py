import socket
import sys

HOST='10.0.2.2'
PORT=6633
s=None
for res in socket.getaddrinfo(HOST, PORT, socket.AF_UNSPEC, socket.SOCK_STREAM):
    af, socktype, proto, canonname, sa = res
    try:
        s=socket.socket(af, socktype, proto)
    except socket.error, err_msg:
        print "fail at one"
        s=None
        continue
    try:
        s.connect(sa)
    except socket.error, msg:
        s.close()
        print "fail at two"
        s=None
        continue
    break
if s is None:
    print 'cannot open socket'
    sys.exit(1)

s.send('HI')
s.close()
   
