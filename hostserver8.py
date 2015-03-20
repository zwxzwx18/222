import socket
import sys
import time

HOST=''
PORT=8000

s=socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print "socket create"

try:
    s.bind((HOST,PORT))
except socket.error as msg:
    print 'Bind failed.'
    sys.exit()
print "Bind complete"

s.listen(10)
print "socket is listening"

filename = open("data8.txt", 'w')
count=0
t0=time.time()
filename.write("Initial time is " + str(t0) +'\n')
while 1:
    conn, addr = s.accept()
    data = conn.recv(1024)
    if not data: break
   ## print repr(data)
   # print 'connected with ' +addr[0] + ':' +str(addr[1])
    count=count+1
    t=time.time()
    filename.write("count: " +str(count) + ", time: " +str(t) + '\n')
filename.close()
s.close()
