-----------------------------------------------------
-- Copyright 2022 Xyna GmbH, Germany
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

DEFINE asversion=&1;
DEFINE asip=&2;

-- #############################################################################
-- XynaProperty xyna.as.version
-- #############################################################################
DECLARE
  cnt NUMBER;
BEGIN
  SELECT count(*) INTO cnt FROM XynaProperty WHERE key = 'xyna.as.version.&asip';
  IF cnt = 0 THEN
    INSERT INTO XynaProperty (key, value, changedate, accessclass) VALUES ('xyna.as.version.&asip', '&asversion', sysdate, 0);
  ELSE
    UPDATE XynaProperty SET value = '&asversion', changedate=sysdate, accessclass=0 WHERE key = 'xyna.as.version.&asip';
  END IF;
  COMMIT;
END;
/

EXIT;