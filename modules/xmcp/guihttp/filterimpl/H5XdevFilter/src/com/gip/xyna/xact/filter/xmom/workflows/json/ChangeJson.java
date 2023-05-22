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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;

public class ChangeJson extends XMOMGuiJson {


  private int revision;
  private String label;
  private String text;
  private String expression;
  private Boolean isList = null;
  private Boolean freeCapacities = null;
  private Boolean detachable = null;
  private String castToFqn = null;
  private Boolean overrideCompensation = null;
  private JsonSerializable parameter; 
  private JsonSerializable conditionalChoice;
  private Integer limitResults;
  private Boolean queryHistory;
  private Boolean ascending;
  private String storableRole;
  private String fqn;
  private String primitiveType;
  private String implementationType;
  private String implementation;
  private String reference;
  private Boolean isAbortable;
  private Boolean isAbstract;
  private String baseType;
  private String exceptionMsgLanguage;
  private String exceptionMsgText;
  private String name;
  private Boolean isUsed;
  
  
  private ChangeJson() {
  }
  
  public ChangeJson(int revision) {
    this.revision = revision;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setIsList(boolean isList) {
    this.isList = isList;
  }

  public void setParameter(JsonSerializable parameter) {
    this.parameter = parameter;
  }

  public static JsonVisitor<ChangeJson> getJsonVisitor() {
   return new ChangeJsonVisitor();
  }

  private static class ChangeJsonVisitor extends EmptyJsonVisitor<ChangeJson> {
    ChangeJson cj = new ChangeJson();

    @Override
    public ChangeJson get() {
      return cj;
    }
    @Override
    public ChangeJson getAndReset() {
      ChangeJson ret = cj;
      cj = new ChangeJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals(Tags.REVISION) ) {
        cj.revision = Integer.valueOf(value);
        return;
      }
      if( label.equals(Tags.LABEL) ) {
        cj.label = value;
        return;
      }
      if( label.equals(Tags.TEXT) ) {
        cj.text = value;
        return;
      }
      if( label.equals(Tags.EXPRESSION) ) {
        cj.expression = value;
        return;
      }
      if( label.equals(Tags.IS_LIST) ) {
        cj.isList = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.FREE_CAPACITIES) ) {
        cj.freeCapacities = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.DETACHED) ) {
        cj.detachable = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.CAST_TO_FQN) ) {
        cj.castToFqn = value;
        return;
      }
      if( label.equals(Tags.OVERRIDE_COMPENSATION) ) {
        cj.overrideCompensation = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.LIMIT_RESULTS) ) {
        cj.limitResults = Integer.valueOf(value);
        return;
      }
      if( label.equals(Tags.QUERY_HISTORY) ) {
        cj.queryHistory = Boolean.valueOf(value);
        return;
      }
      if( label.equals(Tags.QUERY_ASCENDING) ) {
        cj.ascending = Boolean.valueOf(value);
        return;
      }
      if( label.equals(Tags.DATA_TYPE_STORABLE_ROLE) ) {
        cj.storableRole = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_FQN) ) {
        cj.fqn = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_PRIMITIVE_TYPE) ) {
        cj.primitiveType = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_IMPLEMENTATION_TYPE) ) {
        cj.implementationType = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_IMPLEMENTATION) ) {
        cj.implementation = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_REFERENCE) ) {
        cj.reference = value;
        return;
      }
      if( label.equals(Tags.DATA_TYPE_IS_ABORTABLE) ) {
        cj.isAbortable = Boolean.valueOf(value);
        return;
      }
      if( label.equals(Tags.DATA_TYPE_IS_ABSTRACT) ) {
        cj.isAbstract = Boolean.valueOf(value);
        return;
      }
      if( label.equals(Tags.DATA_TYPE_IS_ABSTRACT) ) {
        cj.isAbstract = Boolean.valueOf(value);
        return;
      }
      if( label.equals(Tags.DATA_TYPE_BASE_TYPE) ) {
        cj.baseType = value;
        return;
      }
      if( label.equals(Tags.EXCEPTION_TYPE_MESSAGE_LANGUAGE) ) {
        cj.exceptionMsgLanguage = value;
        return;
      }
      if( label.equals(Tags.EXCEPTION_TYPE_MESSAGE_TEXT) ) {
        cj.exceptionMsgText = value;
        return;
      }
      if( label.equals(Tags.STEP_FUNCTION_ORDER_INPUT_SOURCE_NAME) ) {
        cj.name = value;
        return;
      }
      if( label.equals(Tags.SERVICE_GROUP_LIB_IS_USED) ) {
        cj.isUsed = Boolean.valueOf(value);
        return;
      }
    }

    @Override
    public JsonVisitor<?> objectStarts(String content) {
      return null; //FIXME wie Objekt erkennen?
    }

    @Override
    public void object(String label, Object value) {
      if( label.equals("parameter") ) {
        cj.parameter = (JsonSerializable)value; //FIXME
        return;
      }
      if( label.equals("conditionalChoice") ) {
        cj.conditionalChoice = (JsonSerializable)value;//FIXME
        return;
      }
    }
  }

  public String getLabel() {
    return label;
  }

  public String getText() {
    return text;
  }

  public String getExpression() {
    return expression;
  }

  public Boolean isList() {
    return isList;
  }

  public int getRevision() {
    return revision;
  }

  public Boolean getFreeCapacities() {
    return freeCapacities;
  }

  public Boolean getDetachable() {
    return detachable;
  }

  public String getCastToFqn() {
    return castToFqn;
  }

  public Boolean isOverrideCompensation() {
    return overrideCompensation;
  }

  
  public Integer getLimitResults() {
    return limitResults;
  }

  
  public Boolean getQueryHistory() {
    return queryHistory;
  }

  
  public Boolean getAscending() {
    return ascending;
  }


  public String getStorableRole() {
    return storableRole;
  }


  public String getFqn() {
    return fqn;
  }


  public String getPrimitiveType() {
    return primitiveType;
  }


  public String getImplementationType() {
    return implementationType;
  }


  public String getImplementation() {
    return implementation;
  }


  public String getReference() {
    return reference;
  }


  public Boolean isAbortable() {
    return isAbortable;
  }

  public Boolean isAbstract() {
    return isAbstract;
  }

  public String getBaseType() {
    return baseType;
  }
  
  public String getExceptionMsgLanguage() {
    return exceptionMsgLanguage;
  }
  
  public String getExceptionMsgText() {
    return exceptionMsgText;
  }

  public String getName() {
    return name;
  }

  public boolean isUsed() {
    return isUsed;
  }

}
