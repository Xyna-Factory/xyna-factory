# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 Xyna GmbH, Germany
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
#

create database xyna;
use xyna;


CREATE TABLE orderarchive (
  id BIGINT UNSIGNED NOT NULL,
  parentid BIGINT NULL,
  executiontype VARCHAR(50) NULL,
  priority INTEGER NULL,
  status VARCHAR(50) NULL,
  statuscompensate VARCHAR(50) NULL,
  suspensionstatus VARCHAR(50) NULL,
  suspensioncause VARCHAR(100) NULL,
  startTime BIGINT NULL,
  lastUpdate BIGINT NULL,
  stopTime BIGINT NULL,
  ordertype VARCHAR(200) NULL,
  monitoringlevel INTEGER NULL,
  custom0 VARCHAR(200) NULL,
  custom1 VARCHAR(200) NULL,
  custom2 VARCHAR(200) NULL,
  custom3 VARCHAR(200) NULL,
  exceptions BLOB NULL,
  auditdataasxml LONGBLOB NULL,
  auditdataasjavaobject LONGBLOB NULL,
  sessionid VARCHAR(50) NULL,
  PRIMARY KEY(lastupdate, id)
)
TYPE=InnoDB;

CREATE INDEX idindex
  ON orderarchive (id);

CREATE INDEX parentidindex
  ON orderarchive (parentid);

grant ALL on xyna.* to 'xynauser'@'localhost' identified by 'xynapassword';
