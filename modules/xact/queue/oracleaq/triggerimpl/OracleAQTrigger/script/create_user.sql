 -- ************************************************************************
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
 -- ************************************************************************

create user us identified by us default tablespace users quota unlimited on users
temporary tablespace temp;


grant connect to us;
grant resource to us;
grant execute any procedure to us;
grant create table to us;
grant create view to us;

grant aq_user_role to us;
grant aq_administrator_role to us;
grant execute on dbms_aq to us;
grant execute on dbms_aqadm to us;