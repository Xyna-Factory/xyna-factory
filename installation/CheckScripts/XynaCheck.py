from os import popen
import os

class XynaException(Exception):
    pass

class XynaCheck:
    
    xynafactory_sh="undef"
    installationfolder="undef"
    blackeditionProps = {}
    
    def __init__(self):
        self.readBlackEditionProps()
        if len(self.blackeditionProps)<1:
            print "I can't work without Properties"
            raise XynaException
        
    def readBlackEditionProps(self):
        propFileName="black_edition_001.properties"
        blackEditionPropertiesFileName=self.findPropsFile(propFileName)
        if blackEditionPropertiesFileName:
            self.readprops(blackEditionPropertiesFileName,self.blackeditionProps)
            self.installationfolder=self.blackeditionProps.get("installation.folder")
            self.xynafactory_sh=self.installationfolder+"/server/xynafactory.sh"
        if len(self.blackeditionProps)<1:
            print "unable to read blackedition properties"
            
    def findPropsFile(self,fileName):
        home = os.path.expanduser("~")
        fullname = home + "/environment/" + fileName
        print fullname
        if os.path.exists(fullname):
            print "found it",fileName
            return fullname
            
        else:
            fullname = "/etc/opt/xyna/environment/"+ fileName
            if os.path.exists(fullname):
                print "found it",fileName
                return fullname
                
    def readprops(self,propfileName,props):
        try:
            with open(propfileName, "rt") as f:
                for line in f:
                    l = line.strip()
                    if l and not l.startswith("#"):
                        key_value = l.split("=")
                        key = key_value[0].strip()
                        value = "=".join(key_value[1:]).strip().strip('"') 
                        props[key] = value
        except IOError:
            print "could not read file",propfileName 
    
    def run_xynafactory_sh(self,params):
        cmd = self.xynafactory_sh + " "+ params 
        print cmd
        res = popen(cmd).read()
        print res
        
    def check_rmi_port_open(self):
        cmd = self.xynafactory_sh + " get -p -key xyna.rmi.port.registry"
        print cmd
        res = popen(cmd).read()
        import re
        parsedRes = re.search("xyna.rmi.port.registry.*:.* (\d{1,5})$",res)
        if parsedRes:
            rmiPort=parsedRes.group(1)
            cmd = "netstat -an | grep " + rmiPort + " | grep LISTEN" 
            print cmd
            res = popen(cmd).read()
            print res
        else:
            print "unable to retrieve xyna.rmi.port.registry"
            
        
    def db_connection(self):
        filename = self.installationfolder+"/server/storage/defaultHISTORY/pooldefinition.xml"
        from xml.dom import minidom
        try:
            xmldoc = minidom.parse(filename)
            pooldefinitions = xmldoc.getElementsByTagName("pooldefinition")
            for pool in pooldefinitions:
                user = pool.getElementsByTagName("user")[0].firstChild.nodeValue
                password = pool.getElementsByTagName("password")[0].firstChild.nodeValue
                url = pool.getElementsByTagName("connectstring")[0].firstChild.nodeValue
                import re
                parsedUrl = re.search("jdbc:mysql://(.+)/(.+)",url)
                if parsedUrl:
                    host = parsedUrl.group(1)
                    db   = parsedUrl.group(2)
                    self.check_mysql(user, password, host, db)
        except IOError:
            print "unable to read pooldefinition.xml"
                        
    def check_mysql(self,user,password,host,db):
        cmd = "mysql -u"+user+" -p"+password+" -h"+host+" "+db+" -q -e 'select 42 from dual'"
        print cmd
        res = popen(cmd).read().strip()
        if res == "42\n42":
            print "OK"
        else:
            print res
        
    def check_open_ports(self):
        pid = self.get_xyna_pid()
        if pid:
            cmd = "netstat -ltnup | grep " + pid + " | grep LISTEN"   
            print cmd
            res = popen(cmd).read()
            print res
        
    def get_xyna_pid(self):
        pidfilename = self.installationfolder+"/server/xynafactory.pid"
        try:
            with open(pidfilename,"r") as pidfile:
                return pidfile.read().strip()
        except: 
            print "unable to get xyna pid"

    def jstat(self):
        pid = self.get_xyna_pid()
        if pid:
            cmd = "jstat -gc " + pid + " 1000 10"
            print cmd
            res = popen(cmd).read()
            print res
        
    def check_xyna_log(self):
        logfileName=str(self.blackeditionProps.get("xyna.syslog.file"))
        cmd = "grep 'Caused by' " + logfileName + "| tail -1"   
        print cmd
        res = popen(cmd).read()
        print res
        
    def printHeader(self,check):
        print ""
        print "###################################################################"
        print "# XynaCheck ",check
        print "###################################################################"
        
         
    def level1(self):
        self.printHeader("Status");           self.run_xynafactory_sh("status -v")
        self.printHeader("Cluster");          self.run_xynafactory_sh("listclusterinstances")
        
    def level2(self):
        self.printHeader("Scheduler Info");   self.run_xynafactory_sh("listschedulerinfo")
#       mit den verschluesselten DB Passwoertern geht die Funktion nun nicht mehr und ist daher auskommentiert
#        self.printHeader("Database Conns");   self.db_connection()
        self.printHeader("DB Pools");         self.run_xynafactory_sh("listconnectionpools")
        self.printHeader("Threads");          self.run_xynafactory_sh("listthreadpoolinfo")
        self.printHeader("RMI Port Open");    self.check_rmi_port_open()
        
    def level3(self):
        self.printHeader("Running Apps");     self.run_xynafactory_sh("listapplications | grep RUNNING")
        self.printHeader("Xyna Log");         self.check_xyna_log()
        self.printHeader("WF Insts with Ex"); self.run_xynafactory_sh("listworkflowinstances | grep XynaException | tail -10")
        self.printHeader("JVM Heap");         self.run_xynafactory_sh("listsysteminfo | grep ' heap'")
        self.printHeader("Open Xyna Ports");  self.check_open_ports()
        self.printHeader("jstat");            self.jstat()

if __name__ == '__main__':        
    xyna = XynaCheck()
    xyna.level1()
    xyna.level2()
    xyna.level3()           
