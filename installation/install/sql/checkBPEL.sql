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

WHENEVER SQLERROR EXIT FAILURE ROLLBACK;

--&1 = bpel-dir
--&2 = bpel-version
--&3 = domain-name

DECLARE
  n NUMBER;
  process VARCHAR2(100);
BEGIN
  process := SUBSTR('&1', INSTR('&1', '/', -1, 1) +1, LENGTH('&1')); --wandelt '/ora/bla/blubb/blubb/Planning' in 'Planning' um.
  select count(*)
  into n
  from orabpel.PROCESS p, orabpel.DOMAIN d
  where d.domain_ref = p.domain_ref --join process and domain
  and p.process_id = process
  and p.revision_tag = '&2'
  and d.domain_id = '&3';

  IF n > 0 THEN
    RAISE_APPLICATION_ERROR( -20001, 'BPEL ' || process || ' in Version &2 ist in Domain &3 bereits deployed.' );
  END IF;
END;
/

EXIT;
