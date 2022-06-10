from os import popen
from XynaCheck import XynaCheck, XynaException

class OsCheck:

    
    def df_check(self,limit,option):
        cmd = "df "+option
        res=popen(cmd).read()
        lines = res.split("\n")
        for line in lines:
            import re
            usage = re.search(" (\d+)% (.*)",line)
            if usage:
                if int(usage.group(1)) > limit:
                    print "filesystem ",usage.group(2)," uses ", usage.group(1),'%'
                  
                  
    def ram_check(self):
        res=popen("free").read()
        print res
    
    def printHeader(self,check):
        print ""
        print "###################################################################"
        print "# Check ",check
        print "###################################################################"
    
if __name__ == '__main__': 
    osCheck = OsCheck()        
    osCheck.printHeader("Filesystem"); osCheck.df_check(80,"-h")
    osCheck.printHeader("INodes");     osCheck.df_check(80,"-i")
    osCheck.printHeader("RAM");        osCheck.ram_check()