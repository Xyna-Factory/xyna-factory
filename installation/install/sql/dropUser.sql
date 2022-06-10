-----------------------------------------------------
-- Copyright 2022 GIP SmartMercial GmbH, Germany
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

WHENEVER SQLERROR EXIT FAILURE ROLLBACK;

BEGIN
  EXECUTE IMMEDIATE 'DROP USER  &UserName CASCADE';
  DBMS_OUTPUT.put_line( 'DB-User &UserName has been dropped');
END;
/

EXIT;