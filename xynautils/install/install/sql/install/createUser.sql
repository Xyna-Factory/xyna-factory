-----------------------------------------------------
-- Copyright 2023 Xyna GmbH, Germany
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--  http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-----------------------------------------------------

--Unterdruecken der old/new-Ausgaben:
SET VERIFY OFF;
SET SERVEROUTPUT ON;

DEFINE UserName=&1
DEFINE UserPWD=&2
DEFINE tablespace=&3

WHENEVER SQLERROR EXIT FAILURE ROLLBACK;

DECLARE
  n NUMBER;
BEGIN
  SELECT count(*) INTO n FROM ALL_USERS WHERE Username = UPPER('&UserName');
  
  IF n = 1 THEN
    RAISE_APPLICATION_ERROR( -20001, 'DB-User &UserName already exists.' ); 
  END IF;
END;
/

--Benutzer anlegen
CREATE USER &UserName
  IDENTIFIED BY &UserPWD
  DEFAULT TABLESPACE &tablespace
  QUOTA UNLIMITED ON &tablespace
  TEMPORARY TABLESPACE temp;

-- Rollen zuweisen
GRANT connect TO &UserName;
GRANT resource TO &UserName;
GRANT CREATE VIEW TO &UserName;

--ADJUSTMENT: add grants needed by component

EXIT;