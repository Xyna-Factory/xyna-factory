--#---------------------------------------------------
--# Copyright 2010 GIP AG
--# (http://www.gip.com)
--#
--# Heinrich-von-Brentano-Str. 2
--# 55130 Mainz
--#----------------------------------------------------
--# $Revision: 80231 $
--# $Date: 2010-12-16 11:32:37 +0100 (Do, 16. Dez 2010) $
--#----------------------------------------------------

CREATE TABLE velocity_template (
    id INT NOT NULL,
    type_name VARCHAR(16) NOT NULL,
    part_name VARCHAR(32) NOT NULL,
    constraints_set CLOB,
    constraints_score NUMBER(10,0) NOT NULL,
    content CLOB,
    PRIMARY KEY(id)
)
