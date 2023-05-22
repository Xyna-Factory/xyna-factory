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
package xact.templates;



import java.lang.reflect.Method;
import java.util.*;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.templateprovider.InputData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;



/**
 * This is a class wraps an MOM object into an object that suits the current
 * template provider implementation.
 */
public final class InputDataAdapter implements InputData {

  private static final Logger logger = CentralFactoryLogging.getLogger(InputDataAdapter.class);

  private final TemplateInputData templateInputData;
  private final Map aliasData;


  

  static private Map toAliasMap(final TemplateInputData templateInputData) {
    String[] mapping=TemplateProviderImpl.getMappingString().split(";");//unschoen
    HashMap aliases=new HashMap();
    for(String oneMapping:mapping){
      if (oneMapping.indexOf('=')<0) continue;
      String[] parts=oneMapping.split("=");
      String alias=parts[0].trim();
      String path=parts[1].trim();
      Object obj=getObjectByPath(templateInputData,path);
      String[] aliasParts=alias.split("\\.");
      HashMap hm=aliases;

      for(int i=0;i<aliasParts.length-1;i++){
        if(hm.containsKey(aliasParts[i])){
          HashMap hmNeu=getHashMap(hm.get(aliasParts[i]));
          hm.put(aliasParts[i], hmNeu);
          hm=hmNeu;
        } else {
          HashMap hmNeu=new HashMap();
          hm.put(aliasParts[i], hmNeu);
          hm=hmNeu;
        }            
      }      
      if(obj==null){ //to prevent Nullpointer Exception
        obj=new HashMap();
      }
      hm.put(aliasParts[aliasParts.length-1],obj);                    
    }

    return aliases;   
  }


  
  
  private static HashMap getHashMap(Object object) {
    if (object instanceof XynaObject){
      Method m;
      try {
        m = object.getClass().getMethod("getVariableNames");
        Set<String> variableNames=(Set<String>) m.invoke(object);
        HashMap hm=new HashMap();
        for(String key:variableNames){
          hm.put(key, ((XynaObject)object).get(key));
        }
        return hm;
      }
      catch (Exception e) {
        logger.error("error conve ",e);
      }

    }    
    
    return (HashMap) object;   
  }




  public InputDataAdapter(final TemplateInputData templateInputData) {
    if (templateInputData == null) {
      throw new IllegalArgumentException("Template input data may not be null.");
    }
    this.templateInputData = templateInputData;
    this.aliasData = toAliasMap(templateInputData);
  }
   
    public String getValue(final String key) {
      Object obj = getObjectByPath(aliasData, key);
      if (obj != null) {
        return obj.toString();
      }
      obj = getObjectByPath(templateInputData, key);
      if (obj == null) {
        return null;
      }
      return obj.toString();
    }
    
    private static int extractIndex(String key){
      String number=key.substring(key.indexOf("(")+1, key.indexOf(")"));    
      return Integer.parseInt(number);    
    }

    static private Object getObjectByPath(Object object, final String path) {
      Object obj;
      try {
        String[] keyParts=path.split("\\.");
        obj = object;
        for (String keyPart:keyParts){
          if (obj instanceof XynaObject){
            obj = ((XynaObject)obj).get(keyPart);        
          } else if (obj instanceof Map){
            obj=((Map)obj).get(keyPart);           
          }else if (obj instanceof List){
            obj=((List)obj).get(extractIndex(keyPart));           
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to get key: <" + path + ">.", e);
        obj= null;
      } 
      return obj;
    }


  @Override
  public String toString() {
    StringBuffer sb= new StringBuffer();
    for(String key:templateInputData.getVariableNames()){
      try {
        sb.append(key+":"+templateInputData.get(key)+",");
      }
      catch (InvalidObjectPathException e) {
       //should not happen
      }
    }
    return sb.toString();
  }

}
