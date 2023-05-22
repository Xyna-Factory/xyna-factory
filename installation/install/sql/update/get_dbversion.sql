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

SET SERVEROUTPUT on;
SET FEEDBACK OFF;

DECLARE
  table_or_view_not_found EXCEPTION;
  PRAGMA EXCEPTION_INIT(table_or_view_not_found, -942);
  dbversion VarChar2(50);
  
  FUNCTION identifyDBVersion RETURN VarChar2 IS
  BEGIN
    NULL;
    --code to get version if not found in table
  END;  

  BEGIN
    execute immediate 'SELECT value from XynaProperty WHERE key = ''xyna.db.version''' INTO dbversion;
    DBMS_OUTPUT.put_line(dbversion);
  EXCEPTION
    WHEN table_or_view_not_found THEN
      DBMS_OUTPUT.put_line('notFound');
    
    WHEN no_data_found THEN
      DBMS_OUTPUT.put_line('notFound');
  END;
/