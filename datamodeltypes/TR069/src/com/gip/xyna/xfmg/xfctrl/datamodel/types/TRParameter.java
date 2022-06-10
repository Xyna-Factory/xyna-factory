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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;


/**
 *
 */
public class TRParameter {

  
  private static Logger logger = CentralFactoryLogging.getLogger(TRParameter.class);
  
  private String name;
  private TRObject object;
  private String description;
  private Type type;
  private String fileName;
  private String fileNameOfLastChange;
  
  public static enum Type {
    
    String(PrimitiveType.STRING, false),
    UnsignedInt(PrimitiveType.INTEGER, false), //TODO LONG?
    Int(PrimitiveType.INTEGER, false), 
    Boolean(PrimitiveType.BOOLEAN_OBJ, false),
    List(PrimitiveType.STRING, true),
    Base64(PrimitiveType.STRING, false),
    DateTime(PrimitiveType.STRING, false),
    DataType(PrimitiveType.STRING, false), //FIXME
    Unknown(PrimitiveType.STRING, false);
    
    private boolean isList;
    private PrimitiveType simpleType;
    private static Map<String,Type> map; 
    
    private Type(PrimitiveType simpleType, boolean isList) {
      this.simpleType = simpleType;
      this.isList= isList;
    }
    
    public PrimitiveType getSimpleType() {
      return simpleType;
    }
    
    public boolean isList() {
      return isList;
    }
    
    static {
      map = new HashMap<String,Type>();
      for( Type t : values() ) {
        String lower = t.name().substring(0,1).toLowerCase()+t.name().substring(1);
        map.put(lower, t);
      }
     
    }

    public static Type getType(String type) {
      Type t = map.get(type);
      if( t == null ) {
        logger.warn( "Unknown type "+ type );
        return Unknown;
      }
      return t;
    }
    
  }
  
  public TRParameter(TRObject object, String name) {
    this.name = name.trim();
    this.object = object;
    this.fileName = object.getFileName();
  }
  
  public TRParameter(TRObject object, String name, String fileName) {
    this.name = name.trim();
    this.object = object;
    this.fileName = fileName;
  }

  public TRParameter(TRObject object, TRParameter parameter) {
    this.name = parameter.name;
    this.object = object;
    this.description = parameter.description;
    this.type = parameter.type;
    this.fileName = parameter.fileName;
  }

 
  public String getName() {
    return name;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setType(String type) {
    this.type = Type.getType(type);
  }
  
  public Type getType() {
    return type;
  }

  public PrimitiveType getSimpleType() {
    return type.getSimpleType();
  }
  public boolean isList() {
    return type.isList();
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
}
