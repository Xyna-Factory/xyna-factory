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

package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = Localization.COL_ID, tableName = Localization.TABLENAME)
public class Localization extends Storable<Localization> {

  public static final String TABLENAME = "localization";
  public static final String COL_ID = "id";
  public static final String COL_TYPE = "type";
  public static final String COL_IDENTIFIER = "identifier";
  public static final String COL_LANGUAGE = "language";
  public static final String COL_TEXT = "text";
  
  
  public static enum Type {
    RIGHT, RIGHT_SCOPE;
  };

  
  private static final long serialVersionUID = -2235096740449926160L;
  
  
  @Column(name = COL_ID, index = IndexType.PRIMARY)
  private long id;
  @Column(name = COL_TYPE, size = 50)
  private String type;
  @Column(name = COL_IDENTIFIER, size = 50)
  private String identifier;
  @Column(name = COL_LANGUAGE, size = 2)
  private String language;
  @Column(name = COL_TEXT, size = 200)
  private String text;
  
  
  public Localization() {}
  
  
  public Localization(String type, String identifier, String language) {
    this.type = type;
    this.identifier = identifier;
    this.language = language;
  }
  
  
  public Localization(long id, String type, String identifier, String language, String text) {
    this.id = id;
    this.type = type;
    this.identifier = identifier;
    this.language = language;
    this.text = text;
  }
  
  
  public long getId() {
    return this.id;
  }
  
  
  public String getType() {
    return this.type;
  }
  
  
  public String getIdentifier() {
    return this.identifier;
  }
  
  
  public String getLanguage() {
    return this.language;
  }
  
  
  public String getText() {
    return this.text;
  }
  
  
  public void setText(String text) {
    this.text = text;
  }
  
  
  
  public static ResultSetReader<Localization> reader = new ResultSetReader<Localization>() {
    
    public Localization read(ResultSet rs) throws SQLException {
      Localization l = new Localization();
      l.id = rs.getLong(COL_ID);
      l.type = rs.getString(COL_TYPE);
      l.identifier = rs.getString(COL_IDENTIFIER);
      l.language = rs.getString(COL_LANGUAGE);
      l.text = rs.getString(COL_TEXT);
      return l;
    }
    
  };
  

  @Override
  public ResultSetReader<? extends Localization> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends Localization> void setAllFieldsFromData(U data) {
    Localization cast = data;
    this.id = cast.id;
    this.type = cast.type;
    this.identifier = cast.identifier;
    this.language = cast.language;
    this.text = cast.text;
  }


  public void setId(long id) {
    this.id = id;
  }

}
