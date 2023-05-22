/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

package com.gip.xyna.update.outdatedclasses_7_0_2_7;


import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;



@Persistable(primaryKey = Right.COL_NAME, tableName = Right.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
public class Right extends Storable<Right> {

 private static final long serialVersionUID = -5368965457813480820L;

 public static final String TABLENAME = "rightsarchive";
 public static final String COL_NAME = "name";
 public static final String COL_DESCRIPTION = "description";

 @Column(name = COL_NAME, size = 50, index = IndexType.PRIMARY)
 private String name;
 @Column(name = COL_DESCRIPTION, size = 200)
 private String description;


 public Right() {
   // Leerer Konstruktor f�r Storable und einige PersistenceLayer ben�tigt
 }


 public Right(String name) {
   this();
   this.name = name;
 }


 public String getName() {
   return name;
 }


 public String getDescription() {
   return description;
 }


 public void setDescription(String description) {
   this.description = description;
 }


 @Override
 public Object getPrimaryKey() {
   return name;
 }


 public static ResultSetReader<Right> reader = new ResultSetReader<Right>() {

   public Right read(ResultSet rs) throws SQLException {
     Right r = new Right();
     r.name = rs.getString(COL_NAME);
     r.description = rs.getString(COL_DESCRIPTION);
     return r;
   }

 };


 @Override
 public ResultSetReader<? extends Right> getReader() {
   return reader;
 }


 @Override
 public <U extends Right> void setAllFieldsFromData(U data) {
   Right cast = data;
   name = cast.name;
   description = cast.description;
 }
 
}
