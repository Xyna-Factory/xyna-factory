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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 */
public class TRObject {

  private final static String P_OPTIONAL_INDEX = "(\\.\\{i\\})?";
  private final static String P_OPTIONAL_PARENT = "(.*?)";
  private final static String P_NAME = "([\\w-]+)";
  private final static String P_FULLNAME = P_OPTIONAL_PARENT+P_OPTIONAL_INDEX+"\\.?"+P_NAME+P_OPTIONAL_INDEX+"\\.";
   
  private final static Pattern PATTERN_FULLNAME = Pattern.compile(P_FULLNAME);
    
  private String fqName;
  private TRObjectContainer container;
  private List<TRParameter> parameters;
  private Map<String, TRParameter> parameterMap;
  private List<TRObjectReference> children;
  private String name;
  private String fqParent;
  private boolean isListInParent;
  private String description;
  private String fileName;
  private String fileNameOfLastChange;
  
  public TRObject(TRObjectContainer container, String fqName) {
    this.fqName = fqName.trim();
    this.container = container;
    this.parameters = new ArrayList<TRParameter>();
    this.parameterMap = new HashMap<String,TRParameter>();
    parseFqName();
    this.children = new ArrayList<TRObjectReference>();
    this.fileName = container.getFileName();
  }

  public TRObject(TRObjectContainer container, TRObject object) {
    this(container, object, object.getFqName());
    
  }
  
  public TRObject(TRObjectContainer container, TRObject object, String fqName) {
    this(container, fqName);
    for( TRParameter param : object.getParameters() ) {
      addParameter( new TRParameter(this, param) ); 
    }
    this.children = new ArrayList<TRObjectReference>();
    for (TRObjectReference child : object.getChildren()) {
      if (container instanceof TRModel) {
        children.add(child);
      } else if (container instanceof TRComponent) {
        children.add(new TRObjectReference(child, container.getName()));
      }
    }
    
    this.fileName = object.getFileName(); //FileName muss erhalten bleiben
  }

  private void parseFqName() {
    Matcher m = PATTERN_FULLNAME.matcher(fqName);
    if( m.matches() ) {
      this.name = m.group(3);
      if( m.group(1).length() != 0 ) {
        this.fqParent = m.group(1)+( m.group(2)==null?"":m.group(2))+".";
      } else {
        this.fqParent = null;
      }
      this.isListInParent = m.group(4) != null;
    }
  }
 
  
  @Override
  public String toString() {
    return "TRObject("+fqName+", "+fqParent+", "+parameters.size()+" parameters, "+children.size()+" children)";
  }
  
  
  
  public void addParameter(TRParameter parameter) {
    parameters.add(parameter);
    parameterMap.put( parameter.getName(), parameter);
  }


  public String getParentObjectFqName() {
    return fqParent;
  }
  
  public String getName() {
    return name;
  }


  public void addChild(TRObject object) {
    children.add( new TRObjectReference(object) );
  }
  public void addChild(TRObject object, TRComponent component) {
    children.add( new TRObjectReference(object,component) );
  }
  
  public List<TRObjectReference> getChildren() {
    return children;
  }
  
  public String getFqName() {
    return fqName;
  }
  
  public List<TRParameter> getParameters() {
    return parameters;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  
  public boolean isListInParent() {
    return isListInParent;
  }
  
  public void markAsListInParent() {
    isListInParent = true;
  }

  public TRParameter getParameter(String name) {
    return parameterMap.get(name);
  }
  
  public String getFileName() {
    return fileName;
  }

  public void setFileNameOfLastChange(String fileName) {
    this.fileNameOfLastChange = fileName;
  }
  
  public String getFileNameOfLastChange() {
    if( fileNameOfLastChange == null ) {
      return fileName;
    }
    return fileNameOfLastChange;
  }
  
  
  public TRObjectContainer getContainer() {
    return container;
  }

}
