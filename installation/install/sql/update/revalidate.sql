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

SET SERVEROUTPUT ON SIZE 100000;

DECLARE
  num NUMBER;
BEGIN
  --Versuch, alle invaliden Objekte neu zu kompilieren
  DBMS_UTILITY.compile_schema( '&_USER' );

  --Übriggebliebene invalide Objekte ausgeben
  SELECT count(*) INTO num FROM User_Objects WHERE status='INVALID';
  IF num != 0 THEN
    DBMS_OUTPUT.put_line( num||' Objekte sind invalid!' );
    FOR rec IN (SELECT * FROM User_Objects WHERE status='INVALID' ) LOOP
      DBMS_OUTPUT.put_line( rec.object_type||' '||rec.object_name );
    END LOOP;
  END IF;
END;
/

--DECLARE
--  action VARCHAR2(4000);
--BEGIN
--  FOR rec IN ( SELECT *  FROM user_objects WHERE status = 'INVALID' ) LOOP
--    action :=  CASE rec.object_type
--      WHEN 'FUNCTION' THEN 'ALTER FUNCTION ' || rec.object_name || ' COMPILE'
--      WHEN 'TRIGGER' THEN 'ALTER TRIGGER ' || rec.object_name || ' COMPILE'
--      WHEN 'PROCEDURE' THEN 'ALTER PROCEDURE ' || rec.object_name || ' COMPILE'
--      WHEN 'PACKAGE BODY' THEN 'ALTER PACKAGE ' || rec.object_name || ' COMPILE'
--      WHEN 'VIEW' THEN 'ALTER VIEW ' || rec.object_name || ' COMPILE'
--      ELSE NULL
--    END;
--    IF action IS NOT NULL THEN
--     DBMS_OUTPUT.put_line( action );
--    BEGIN
--        EXECUTE IMMEDIATE action;
--      EXCEPTION
--        WHEN OTHERS THEN
--          DBMS_OUTPUT.put_line( 'Can not revalidate ' || rec.object_type || ' ' || rec.object_name || ' due to error ' || SQLERRM );
--      END;
--    ELSE
--      DBMS_OUTPUT.put_line( 'Can not revalidate ' || rec.object_type || ' ' || rec.object_name );
--    END IF;
--
--  END LOOP;
--END;
--/