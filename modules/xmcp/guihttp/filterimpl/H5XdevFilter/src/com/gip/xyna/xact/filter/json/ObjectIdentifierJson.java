/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xact.filter.json;

import java.util.Comparator;

import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonParserUtils;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;

public class ObjectIdentifierJson implements JsonSerializable {

  private static final String LABEL_TYPE = "type";
  private static final String LABEL_LABEL = "label";
  private static final String LABEL_OPERATION = "operation";
  private static final String LABEL_RUNTIME_CONTEXT = "rtc";
  private static final String READONLY = "readonly";
  private static final String LABEL_AMBIGUE = "ambigue";
  
  private Type type;
  private String label;
  private String dataTypeLabel;
  private FQNameJson fqName;
  private RuntimeContextJson runtimeContext;
  private RuntimeContextJson originRuntimeContext;
  private boolean ambigue;

  
  public enum Type {
    
    workflow, dataType(true), exceptionType, serviceGroup(true), instanceService, codedService;
    // Reihenfolge ist relevant für die compareTo-Methode. Verwendet von ObjectIdentifierJsonFQNSorter.
    
    private boolean multiView;
    
    
    private Type() {
      this.multiView = false;
    }
    
    private Type(boolean multiView) {
      this.multiView = multiView;
    }

    public static Type of(XMOMDatabaseType type) {
      switch( type ) {
      case DATAMODEL:
        break;
      case DATATYPE:
        return dataType;
      case EXCEPTION:
        return exceptionType;
      case FORMDEFINITION:
        break;
      case GENERIC:
        break;
      case OPERATION:
        return codedService;
      case SERVICE:
        break;
      case SERVICEGROUP:
        return serviceGroup;
      case WORKFLOW:
        return workflow;
      default:
        break;
      }
      return null;
    }

    public static Type of(XMOMType type) {
      switch( type ) {
      case DATATYPE:
        return dataType;
      case EXCEPTION:
        return exceptionType;
      case FORM:
        break;
      case ORDERINPUTSOURCE:
        break;
      case WORKFLOW:
        return workflow;
      default:
        break;
      
      }
      return null;
    }
    
    
    public boolean isMultiView() {
      return multiView;
    }
  }
  
  public ObjectIdentifierJson(){
  }
  
  public ObjectIdentifierJson(RuntimeContext rc, XMOMDatabaseSearchResultEntry resultEntry, boolean ambigue) {
    this.type = ObjectIdentifierJson.Type.of(resultEntry.getType());
    this.label = resultEntry.getLabel();
    if( this.type == Type.codedService ) {
      String name = resultEntry.getSimplename();
      int idx1 = name.indexOf('.');
      int idx2 = name.lastIndexOf('.');
      this.fqName = new FQNameJson( resultEntry.getSimplepath(), name.substring(0, idx1) );
      this.fqName.setOperation(name.substring(idx2+1));
    } else if(this.type == Type.serviceGroup) {
      String name = resultEntry.getSimplename();
      int idx1 = name.indexOf('.');
      this.fqName = new FQNameJson( resultEntry.getSimplepath(), name.substring(0, idx1) );
    } else {
      this.fqName = new FQNameJson( resultEntry.getSimplepath(), resultEntry.getSimplename() );
    }
    this.runtimeContext = new RuntimeContextJson(rc);
    if( ! resultEntry.getRuntimeContext().equals(rc) ) {
      this.originRuntimeContext = new RuntimeContextJson(resultEntry.getRuntimeContext());
    }
    this.ambigue = ambigue;
  }
  
  public ObjectIdentifierJson(RuntimeContext rc, XMOMDatabaseSearchResultEntry resultEntry) {
    this(rc, resultEntry, false);
  }

  public ObjectIdentifierJson(ObjectIdentifierJson oij) {
    this.type = oij.type;
    this.label = oij.label;
    this.fqName = new FQNameJson(oij.fqName);
    this.runtimeContext = oij.runtimeContext;
    this.originRuntimeContext = oij.originRuntimeContext;
  }

  @Override
  public void toJson(JsonBuilder jb) {
    jb.addEnumAttribute(LABEL_TYPE, type);
    jb.addStringAttribute(LABEL_LABEL, label);

    fqName.toJson(jb);
    if (type == Type.codedService || type == Type.instanceService) {
      jb.addStringAttribute(LABEL_OPERATION, fqName.getOperation());
    }

    if (originRuntimeContext != null && originRuntimeContext != runtimeContext) {
      jb.addObjectAttribute(LABEL_RUNTIME_CONTEXT, originRuntimeContext);
      jb.addBooleanAttribute(READONLY, Boolean.TRUE);
    } else {
      jb.addObjectAttribute(LABEL_RUNTIME_CONTEXT, runtimeContext);
    }
    
    //if(ambigue)
    jb.addBooleanAttribute(LABEL_AMBIGUE, ambigue);
  }

  public void setType(Type type) {
    this.type = type;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public void setFQName(FQNameJson fqName) {
    this.fqName = fqName;
  }
  
  public void setRuntimeContext(RuntimeContextJson runtimeContext) {
    this.runtimeContext = runtimeContext;
  }
  public void setOriginRuntimeContext(RuntimeContextJson originRuntimeContext) {
    this.originRuntimeContext = originRuntimeContext;
  }

  public Type getType() {
    return type;
  }
  public String getLabel() {
    return label;
  }
  public FQNameJson getFQName() {
    return fqName;
  }
  
  public RuntimeContextJson getRuntimeContext() {
    return runtimeContext;
  }
  
  public static Comparator<ObjectIdentifierJson> sortByLabel() {
    return new ObjectIdentifierJsonSorter();
  }
  
  public static Comparator<ObjectIdentifierJson> sortByFQN(String searchString) {
    return new ObjectIdentifierJsonFQNSorter(searchString);
  }
  
  public static class ObjectIdentifierJsonSorter implements Comparator<ObjectIdentifierJson> {

    @Override
    public int compare(ObjectIdentifierJson o1, ObjectIdentifierJson o2) {
      return ComparatorUtils.compareNullAware( o1.getLabel(), o2.getLabel(), false );
    }
    
  }
  
  public static class ObjectIdentifierJsonFQNSorter implements Comparator<ObjectIdentifierJson> {

    private String searchString;
    
    public ObjectIdentifierJsonFQNSorter(String searchString) {
      super();
      this.searchString = searchString;
    }
    
    @Override
    public int compare(ObjectIdentifierJson o1, ObjectIdentifierJson o2) {
      String fqnString1 = o1.getFQName().getTypePath() + "." + o1.getFQName().getTypeName();
      String fqnString2 = o2.getFQName().getTypePath() + "." + o2.getFQName().getTypeName();
      if(!searchString.equals(null) && !searchString.equals("")) {
        int labelMatch = compareFavoringExactMatch(o1.getLabel(), o2.getLabel(), searchString);
        if(labelMatch == 0) {
          int typenameMatch = compareFavoringExactMatch(o1.getFQName().getTypeName(), o2.getFQName().getTypeName(), searchString);
          if(typenameMatch == 0) {
            int fqnMatch = ComparatorUtils.compareIgnoreCaseNullAware(fqnString1, fqnString2, false);
            if(fqnMatch == 0) {
              int typeMatch = o1.type.compareTo(o2.type);  // use ordinals
              // bei gleichem FQN kann compare unter instanceServices/codedServices 0 ergeben, dann label vergleichen
              if(typeMatch == 0) return o1.getLabel().compareTo(o2.getLabel());
              return typeMatch;
            }
            return fqnMatch;
          }
          return typenameMatch;
        }
        return labelMatch;
      }
      //wird nur erreicht, wenn kein searchString angegeben ist
      return ComparatorUtils.compareIgnoreCaseNullAware(fqnString1, fqnString2, false);
    }
    
    /*
     * Vergleicht die beiden Strings "first" und "second" auf exakten Match mit dem "searchString".
     * Stimmt genau einer der beiden mit "searchString" überein, so wird 1 bzw. -1 zurückgegeben.
     * Stimmen beide oder keiner mit "searchString" überein, so wird 0 zurückgegeben. (Die beiden String sind gleichwertig hinsichtlich exaktem Match.)
     * Jeder Wert hat Vorrang vor null.
     */
    private int compareFavoringExactMatch(String first, String second, String searchString) {
      if(first == null) {
        if(second == null) return 0;
        return 1;
      }
      if(second == null) return -1;
      if(first.equalsIgnoreCase(searchString)) {
        if(second.equalsIgnoreCase(searchString)) return 0;
        return -1;  //first vor second bei asc-sortierung
      }
      else {
        if(second.equalsIgnoreCase(searchString)) return 1;  //second vor first bei asc-sortierung
        return 0;
      }
    }

  }

  public static XMOMDatabaseSelect fillSelects(XMOMDatabaseSelect xmomDatabaseSelect) {
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.FQNAME);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.LABEL);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.REVISION);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.DOCUMENTATION);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.PATH);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.ID);
    xmomDatabaseSelect.select(XMOMDatabaseEntryColumn.NAME);
    return xmomDatabaseSelect;
  }
  
  public static JsonVisitor<ObjectIdentifierJson> getJsonVisitor() {
    return new ObjectIdentifierJsonVisitor();
  }
  public static class ObjectIdentifierJsonVisitor extends EmptyJsonVisitor<ObjectIdentifierJson> {
    ObjectIdentifierJson oij = new ObjectIdentifierJson();
    
    @Override
    public ObjectIdentifierJson get() {
      return oij;
    }

    @Override
    public ObjectIdentifierJson getAndReset() {
      ObjectIdentifierJson ret = oij;
      oij = new ObjectIdentifierJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(LABEL_TYPE) ) {
        oij.type = JsonParserUtils.parseEnum(ObjectIdentifierJson.Type.class, label, value);
        return;
      }
      if( label.equals(LABEL_LABEL) ) {
        oij.label = value;
        return;
      }
      if( FQNameJson.useLabel(label) ) {
        oij.fqName = FQNameJson.parseAttribute(oij.fqName, label, value);
        return;
      }
      throw new UnexpectedJSONContentException(label);
    }
    
    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      if( label.equals(LABEL_RUNTIME_CONTEXT) ) {
        return RuntimeContextJson.getJsonVisitor();
      }
      
      throw new UnexpectedJSONContentException(label);
    }
    
    @Override
    public void object(String label, Object value) throws UnexpectedJSONContentException {
      if( label.equals(LABEL_RUNTIME_CONTEXT) ) {
        oij.runtimeContext = (RuntimeContextJson)value;
        return;
      }
      
      throw new UnexpectedJSONContentException(label);
    }
  }

  
  public String getDataTypeLabel() {
    return dataTypeLabel;
  }

  
  public void setDataTypeLabel(String dataTypeLabel) {
    this.dataTypeLabel = dataTypeLabel;
  }
  
}
