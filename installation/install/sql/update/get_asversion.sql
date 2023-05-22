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

SET VERIFY OFF;
SET SERVEROUTPUT ON;
SET FEEDBACK OFF;
DEFINE asip=&1;

DECLARE
  table_or_view_not_found EXCEPTION;
  PRAGMA EXCEPTION_INIT(table_or_view_not_found, -942);
  asversion VarChar2(50);
  
  FUNCTION identifyASVersion RETURN VarChar2 IS
  BEGIN
    NULL;
    --code to get version if not found in table
  END;
    
  BEGIN
    execute immediate 'SELECT value from XynaProperty WHERE key = ''xyna.as.version.&asip''' INTO asversion;
    DBMS_OUTPUT.put_line(asversion);
  EXCEPTION
    WHEN table_or_view_not_found THEN
      DBMS_OUTPUT.put_line('notFound');
    WHEN no_data_found THEN
      DBMS_OUTPUT.put_line('notFound');
  END;
/