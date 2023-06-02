/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import java.util.List;

import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope.ScopePart;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = RightScope.COL_NAME, tableName = RightScope.TABLENAME)
public class RightScope extends Storable<RightScope> {
 
 private static final long serialVersionUID = 427315615818749633L;
 
 public final static String TABLENAME = "rightscope";
 public final static String COL_NAME = "name";
 public final static String COL_DOCUMENTATION = "documentation";
 public final static String COL_DEFINITION = "definition";
 public final static String COL_PARTS = "parts";
 
 public final static ResultSetReader<RightScope> READER = new RightScopeReader();
 
 
 @Column(name = COL_NAME, size = 256)
 private String name;
 @Column(name = COL_DEFINITION, size = 1024)
 private String definition;
 @Column(name = COL_DOCUMENTATION, size = 2048)
 private String documentation;
 @Column(name = COL_PARTS, type=ColumnType.BLOBBED_JAVAOBJECT)
 private List<ScopePart> parts;
 
 
 public RightScope() {
 }
 
 
 public RightScope(String name) {
   this.name = name;
 }
 
 public RightScope(String definition, String name, List<ScopePart> parts) {
   this.definition = definition;
   this.name = name;
   this.parts = parts;
 }
 
 
 public String getName() {
   return name;
 }


 
 public void setName(String name) {
   this.name = name;
 }
 
 
 public String getDefinition() {
   return definition;
 }


 
 public void setDefinition(String definition) {
   this.definition = definition;
 }
 
 
 public String getDocumentation() {
   return documentation;
 }


 
 public void setDocumentation(String documentation) {
   this.documentation = documentation;
 }

 
 public List<ScopePart> getParts() {
   return parts;
 }


 
 public void setParts(List<ScopePart> parts) {
   this.parts = parts;
 }
 
 
 
 @Override
 public ResultSetReader<? extends RightScope> getReader() {
   return READER;
 }

 @Override
 public Object getPrimaryKey() {
   return name;
 }

 @Override
 public <U extends RightScope> void setAllFieldsFromData(U data) {
   RightScope cast = data;
   this.name = cast.name;
   this.definition = cast.definition;
   this.documentation = cast.documentation;
   this.parts = cast.parts;
 }
 
 
 
 @Override
 public String toString() {
   return definition + " - " + documentation;
 }
 
 
 private static void fillByResultSet(RightScope scope, ResultSet rs) throws SQLException {
   scope.setDefinition(rs.getString(COL_DEFINITION));
   scope.setDocumentation(rs.getString(COL_DOCUMENTATION));
   scope.setName(rs.getString(COL_NAME));
   scope.setParts((List<ScopePart>) scope.readBlobbedJavaObjectFromResultSet(rs, COL_PARTS));
 }
 
 
 private static class RightScopeReader implements ResultSetReader<RightScope> {

   public RightScope read(ResultSet rs) throws SQLException {
     RightScope scope = new RightScope();
     fillByResultSet(scope, rs);
     return scope;
   }
   
 }
 
}
