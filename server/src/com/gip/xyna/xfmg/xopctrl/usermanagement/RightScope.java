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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
  public final static String COL_DEFINITION = "definition";
  public final static String COL_PARTS = "parts";
  
  public final static ResultSetReader<RightScope> READER = new RightScopeReader();
  
  
  @Column(name = COL_NAME, size = 256)
  private String name;
  @Column(name = COL_DEFINITION, size = 1024)
  private String definition;
  @Column(name = COL_PARTS, type=ColumnType.BLOBBED_JAVAOBJECT)
  private List<ScopePart> parts;
  private transient List<ScopePartValidator> validators;
  
  private String documentation;
  
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
  
  
  public List<ScopePartValidator> getValidators() {
    if (validators == null) {
      generateValidators();
    }
    return validators;
  }
  
  public boolean validate(String[] scopeParts) {
    if (scopeParts.length != getValidators().size()) {
      return false;
    }
    for (int i = 0; i < scopeParts.length; i++) {
      if (!getValidators().get(i).isValid(scopeParts[i])) {
        return false;
      }
    }
    return true;
  }
  
  
  public boolean validate(String scopedRight) {
    String[] parts = RightScopeParser.splitScopedRightIntoParts(scopedRight);
    String[] partsWithoutScopeName = new String[parts.length - 1];
    System.arraycopy(parts, 1, partsWithoutScopeName, 0, partsWithoutScopeName.length);
    return validate(partsWithoutScopeName);
  }
  
  
  private void generateValidators() {
    List<ScopePartValidator> validators = new ArrayList<ScopePartValidator>();
    for (ScopePart part : parts) {
      validators.add(part.generateValidator());
    }
    this.validators = validators;
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

  
  public static enum ScopePartType {
    ENUMERATION, PRIMITIVE, WILDCARD, REGEXP;
    
    public static ScopePartType determineScopePartType(String scopePart) {
      if (scopePart.length() == 0) {
        return PRIMITIVE;
      } else if (scopePart.equals("*")) {
        return WILDCARD;
      } else if (scopePart.charAt(0) == '[') {
        return ENUMERATION;
      } else if (scopePart.charAt(0) == '/') {
        return REGEXP;
      } else {
        throw new UnsupportedOperationException("Not a valid scopePartDefinition: " + scopePart);
      }
    }
    
  }
  
  
  private static Pattern WILDCARD_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]*\\*?$");
  
  public static class ScopePart implements Serializable {
    
    private static final long serialVersionUID = -6489279257936963724L;
    private ScopePartType type;
    private String definition;
    
    public ScopePart(ScopePartType type, String definition) {
      this.type = type;
      this.definition = definition;
    }
    
    @Override
    public String toString() {
      return "{" + type.toString() + "}" + definition;
    }
    
    public ScopePartValidator generateValidator() {
      switch (type) {
        case PRIMITIVE :
          return new RegExpValidator(UserManagement.RIGHT_PATTERN_PATTERN);
        case ENUMERATION :
          return new WhiteListValidator(RightScopeParser.splitEnumValues(definition));
        case WILDCARD :
          return new RegExpValidator(WILDCARD_PATTERN);
        case REGEXP :
          return new RegExpValidator(definition.substring(1, definition.lastIndexOf('/')));
        default :
          throw new RuntimeException("Invalid type: " + type);
      }
    }
    
  }
  
  
  private static interface ScopePartValidator {
    
    public boolean isValid(String rightPart);
    
  }
  
  private static class WhiteListValidator implements ScopePartValidator {

    private final String[] values;
    
    private WhiteListValidator(String... values) {
      this.values = values;
      Arrays.sort(values);
    }
    
    public boolean isValid(String rightPart) {
      return Arrays.binarySearch(values, rightPart) >= 0;
    }
    
  }
  
  private static class RegExpValidator implements ScopePartValidator {
    
    private final Pattern pattern;
    
    private RegExpValidator(String regExp) {
      this(Pattern.compile(regExp));
    }
    
    private RegExpValidator(Pattern pattern) {
      this.pattern = pattern;
    }

    public boolean isValid(String rightPart) {
      return pattern.matcher(rightPart).matches();
    }
    
  }
  
  
}
