--# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
--# Copyright 2023 Xyna GmbH, Germany
--#
--# Licensed under the Apache License, Version 2.0 (the "License");
--# you may not use this file except in compliance with the License.
--# You may obtain a copy of the License at
--#
--#  http://www.apache.org/licenses/LICENSE-2.0
--#
--# Unless required by applicable law or agreed to in writing, software
--# distributed under the License is distributed on an "AS IS" BASIS,
--# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--# See the License for the specific language governing permissions and
--# limitations under the License.
--# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
--#

CREATE USER XYNA PROFILE DEFAULT IDENTIFIED BY XYNA DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP QUOTA 1024000 K ON USERS ACCOUNT UNLOCK;
GRANT ALTER SESSION TO XYNA;
GRANT CREATE JOB TO XYNA;
GRANT CREATE SEQUENCE TO XYNA;
GRANT CREATE SESSION TO XYNA;
GRANT CREATE SYNONYM TO XYNA;
GRANT CREATE TABLE TO XYNA;
GRANT CREATE TRIGGER TO XYNA;
GRANT CREATE VIEW TO XYNA;
GRANT CONNECT TO XYNA;

CREATE TABLE orderarchive(
  id NUMBER(14) NOT NULL,
  parentid NUMBER(14) NULL,
  executiontype VARCHAR(50) NULL,
  priority NUMBER(2) NULL,
  status VARCHAR(50) NULL,
  statuscompensate VARCHAR(50) NULL,
  suspensionstatus VARCHAR(50) NULL,
  suspensioncause VARCHAR(100) NULL,
  starttime NUMBER(14) NULL,
  lastupdate NUMBER(14) NULL,
  stoptime NUMBER(14) NULL,
  ordertype VARCHAR(200) NULL,
  monitoringlevel NUMBER(2) NULL,
  custom0 VARCHAR(200) NULL,
  custom1 VARCHAR(200) NULL,
  custom2 VARCHAR(200) NULL,
  custom3 VARCHAR(200) NULL,
  exceptions BLOB NULL,
  auditdataasxml CLOB NULL,
  auditdataasjavaobject BLOB NULL,
  sessionid VARCHAR(50) NULL);

ALTER TABLE orderarchive
ADD CONSTRAINT orderarchive_pk PRIMARY KEY(lastupdate, id) enable;

CREATE INDEX idindex
ON orderarchive(id);

CREATE INDEX parentidindex
ON orderarchive(parentid);


CREATE TABLE idgeneration (
  id NUMBER(14) NOT NULL,
  laststoredid NUMBER(14) NOT NULL,
  resultingfromshutdown VARCHAR(1) NOT NULL
);

ALTER TABLE idgeneration
ADD CONSTRAINT idgeneration_pk PRIMARY KEY(id) enable;

CREATE TABLE orderbackup (
  id NUMBER(14) NOT NULL,
  xynaorder BLOB NULL,
  details BLOB NULL,
  backupcause VARCHAR2(100) NULL
);

ALTER TABLE orderbackup
ADD CONSTRAINT orderbackup_pk PRIMARY KEY(id) enable;

CREATE TABLE securestorage (
  id VARCHAR(500) NOT NULL,
  encryptedData BLOB NULL,
  dataType VARCHAR(50) NULL
);

ALTER TABLE securestorage
ADD CONSTRAINT securestorage_pk PRIMARY KEY(id) enable;

CREATE TABLE nsndslam5600 (
  id VARCHAR(200) NOT NULL,
  hostname VARCHAR(200) NULL,
  port NUMBER(8) NULL,
  username VARCHAR(200) NULL
);

ALTER TABLE nsndslam5600
ADD CONSTRAINT nsndslam5600_pk PRIMARY KEY(id) enable;


CREATE TABLE nsndslam5600portgroup (
  id VARCHAR(200) NOT NULL,
  dslamid VARCHAR(200) NOT NULL,
  dslamPorts VARCHAR(200) NULL,
  bridgeportindex NUMBER(6) NULL,
  gbondinterfaceindex NUMBER(6) NULL
);

ALTER TABLE nsndslam5600portgroup
ADD CONSTRAINT nsndslam5600portgroup_pk PRIMARY KEY(id) enable;

CREATE TABLE codegroup (
  codeGroupName VARCHAR(200) NOT NULL,
  indexOfCurrentlyUsedPattern NUMBER(3) NULL
);

ALTER TABLE codegroup
ADD CONSTRAINT codegroup_pk PRIMARY KEY(codeGroupName) enable;

CREATE TABLE codepattern (
  id VARCHAR(200) NOT NULL,
  codeGroupName VARCHAR(200) NOT NULL,
  prefix VARCHAR(200) NULL,
  suffix VARCHAR(200) NULL,
  padding NUMBER(2) NULL,
  startIndex NUMBER(10) NULL,
  endIndex NUMBER(10) NULL,
  currentIndex NUMBER(10) NULL,
  patternIndex NUMBER(3) NULL
);

ALTER TABLE codepattern
ADD CONSTRAINT codepattern_pk PRIMARY KEY(id) enable;

CREATE TABLE sessionmanagament (
  id VARCHAR(100) NOT NULL,
  token VARCHAR(200) NOT NULL,
  startDate NUMBER(20) NOT NULL,
  lastInteraction NUMBER(20) NOT NULL,
  absoluteSessionTimeout NUMBER(20) NOT NULL,
  role VARCHAR(100) NOT NULL,
  state VARCHAR(20) NOT NULL,
  currentPriviliges BLOB NULL
);

ALTER TABLE sessionmanagament
ADD CONSTRAINT sessionmanagament_pk PRIMARY KEY(id) enable;

CREATE TABLE fqctrltaskinformation (
  taskid VARCHAR(14) NOT NULL,
  tasklabel VARCHAR(200) NULL,
  eventcount NUMBER(14) NULL,
  finishedevents NUMBER(14) NULL,
  failedevents NUMBER(14) NULL,
  taskstatus VARCHAR(200) NULL,
  maxevents NUMBER(14) NULL,
  eventcreationinfo VARCHAR(200) NULL,
  statistics BLOB NULL,
  starttime NUMBER(14) NULL,
  stoptime NUMBER(14) NULL
);

ALTER TABLE fqctrltaskinformation
ADD CONSTRAINT fqctrltaskinformation_pk PRIMARY KEY(taskid) enable;
