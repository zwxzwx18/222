from mininet.net import Mininet
from mininet.node import Controller, OVSSwitch, RemoteController, OVSKernelSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import TCLink
import time

def emptyNet():
    net=Mininet(controller=RemoteController, switch=OVSKernelSwitch)
    #net=Mininet(controller=RemoteController, link=TCLink)
    c1=net.addController('c1', controller=RemoteController, ip="10.0.2.2")
    h1=net.addHost('h1', ip='10.0.0.1')
    h2=net.addHost('h2', ip='10.0.0.2')
    h3=net.addHost('h3', ip='10.0.0.3')
    s1=net.addSwitch('s1', dpid="0000000000000001")
    s2=net.addSwitch('s2', dpid="0000000000000002")
    s3=net.addSwitch('s3', dpid="0000000000000003")
    s4=net.addSwitch('s4', dpid="0000000000000004")
    h4=net.addHost('h4', ip='10.0.0.4')
    s5=net.addSwitch('s5', dpid="0000000000000005")
    h5=net.addHost('h5', ip='10.0.0.5')
    s6=net.addSwitch('s6', dpid="0000000000000006")
    h6=net.addHost('h6', ip='10.0.0.6')
    h7=net.addHost('h7', ip='10.0.0.7')
    s7=net.addSwitch('s7', dpid="0000000000000007")
    h8=net.addHost('h8', ip='10.0.0.8')
    s8=net.addSwitch('s8', dpid="0000000000000008")
    h9=net.addHost('h9', ip='10.0.0.9')
    s9=net.addSwitch('s9', dpid="0000000000000009")
    s10=net.addSwitch('s10', dpid="0000000000000010")
    net.addLink(h1, s1)
    net.addLink(h2, s2)
    net.addLink(h3, s3)
    net.addLink(h4, s4)
    net.addLink(h5, s5)
    net.addLink(h6, s6)
    net.addLink(s1, s10)
    net.addLink(h7, s7)
    net.addLink(h8, s8)
    net.addLink(h9, s9)
    net.addLink(s4, s10)
    net.addLink(s5, s10)
    net.addLink(s6, s10)
    net.addLink(s2, s10)
    net.addLink(s3, s10)
    #net.build()
    #c1.start()
    #s1.start([c1])
    #s2.start([c1])
    #s3.start([c1])
    #s4.start([c1])
    #s5.start([c1])
    #s6.start([c1])
    net.start()
    print "network start"
    time.sleep(5)
    print h1.cmd('ping -c1 %s' % h4.IP())
    time.sleep(1)
    print h1.cmd('ping -c1 %s' % h5.IP())
    time.sleep(1)
    print h1.cmd('ping -c1 %s' % h5.IP())
    time.sleep(1)
    print h2.cmd('ping -c1 %s' % h5.IP())
    time.sleep(1)
    print h3.cmd('ping -c1 %s' % h6.IP())
    time.sleep(1)
    print h4.cmd('ping -c1 %s' % h7.IP())
    time.sleep(1)
    print h5.cmd('ping -c1 %s' % h8.IP())
    time.sleep(1)
    print h6.cmd('ping -c1 %s' % h9.IP())
    time.sleep(1)
    CLI(net)
    h1.cmd('python udps.py &')
    h2.cmd('python udps2.py &')
    h3.cmd('python udps3.py &')
    h7.cmd('python hostserver.py &')
    h8.cmd('python hostserver8.py &')
    h9.cmd('python hostserver9.py &')
    time.sleep(1)
    h4.cmd('python udpc.py &')
    h5.cmd('python udpc5.py &')
    h6.cmd('python udpc6.py &')
    time.sleep(110)
   # CLI(net)
    print "network stop"
    net.stop()
if __name__ == '__main__':
    setLogLevel('info')
    emptyNet()
