db-user anlegen mit folgendem skript:


CREATE USER black IDENTIFIED BY black;

GRANT connect TO black;
GRANT resource TO black;
GRANT EXECUTE ON dbms_aqadm TO black;
GRANT EXECUTE ON dbms_aq TO black;
