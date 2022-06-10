from os import popen
import XynaCheck
import subprocess,shlex

class TomcatCheck:
    tomcatfolder="undef"
    xynaWSProps = {}
    
    def __init__(self):
        xyna = XynaCheck.XynaCheck()
        self.tomcatfolder = xyna.blackeditionProps.get("tomcat.folder")
        self.readXynaBlackEditionWSprops()
            
    def status(self):
        cmd = "/etc/init.d/tomcat_001 status"
        print cmd
        res = popen(cmd).read()
        print res
        
    def check_open_ports(self):
        pidfilename = self.tomcatfolder+"/work/catalina.pid"
        pid = "undef"
        try:
            with open(pidfilename,"r") as pidfile:
                pid=pidfile.read().strip()
        except IOError:
            print "unable to read PID File"
        if pid != "undef":
            cmd = "netstat -ltnup | grep " + pid + " | grep LISTEN | grep -v '127.0.0'"   
            print cmd
            res = popen(cmd).read()
            print res
        
        
    def readXynaBlackEditionWSprops(self):
        try:
            propFileName = self.tomcatfolder+"/webapps/XynaBlackEditionWebServices/WEB-INF/classes/XynaBlackEditionWebServices.properties"
            with open(propFileName, "rt") as f:
                for line in f:
                    l = line.strip()
                    if l and not l.startswith("#"):
                        key_value = l.split("=")
                        key = key_value[0].strip()
                        value = "=".join(key_value[1:]).strip().strip('"') 
                        self.xynaWSProps[key] = value
        except IOError:
            print "could not read file",propFileName 

    def telnetXynaRMIPort(self):
        if len(self.xynaWSProps)>0:
            host = str(self.xynaWSProps.get("host"))
            port = str(self.xynaWSProps.get("port"))
            from threading import Timer
            cmd = "telnet " + host + " " + port
            print cmd
            proc = subprocess.Popen(shlex.split(cmd), stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            kill_proc = lambda p: p.kill()
            timer = Timer(10, kill_proc, [proc])
            try:
                timer.start()
                stdout,stderr = proc.communicate()
            finally:
                timer.cancel()
                
            print stdout
            print stderr
        else:
            print "No XynaBlackEditionWebServices.properties found"   
            
    def printHeader(self,check):
        print ""
        print "###################################################################"
        print "# TomcatCheck ",check
        print "###################################################################"

if __name__ == '__main__':
    tomcatCheck = TomcatCheck()        
    tomcatCheck.printHeader("Status");          tomcatCheck.status()
    tomcatCheck.printHeader("Open Ports");      tomcatCheck.check_open_ports()
    tomcatCheck.printHeader("Telnet RMI Port"); tomcatCheck.telnetXynaRMIPort()
