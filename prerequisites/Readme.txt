
Erstellen von Prerequisites:
############################

Bau der Prerequisites wird durch installation/delivery/delivery.xml erledigt.

Kompletter Ablauf des Baus:
===========================

cd installation/delivery

vi delivery.properties #release.number erhöhen

svn ci -m "neues Release" delivery.properties

ant -f delivery.xml release

cd -

XBE_REREQ_DIR="~/data/xyna/Xyna\ Black\ Edition/06_Releases/XynaBlackEditionPrerequisites/"

mkdir ${XBE_REREQ_DIR}/<ReleaseNumber>

cp release/* ${XBE_REREQ_DIR}/<ReleaseNumber>

