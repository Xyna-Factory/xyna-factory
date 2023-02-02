#Installieren von lsof:
yum install lsof

#Umbenennen:
mv nof.txt create.sh

#Ausfuehrbar machen:
chmod + x create.sh

#Als der Xyna User muss folgender Cron hinzugefügt werden:
xyna@hostname:~> crontab -e
#dort ergaenzen:
*/5 * * * * /opt/xyna/xyna_001/nof/create.sh

