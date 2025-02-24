/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xfmg.xfctrl.datamodel.json.impl;



import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DataModelInformation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

import xact.templates.Document;
import xact.templates.JSON;
import xfmg.xfctrl.datamodel.json.JSONDatamodelServicesServiceOperation;
import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueWriter;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONTokenType;
import xfmg.xfctrl.datamodel.json.parameter.JSONParsingOptions;
import xfmg.xfctrl.datamodel.json.parameter.JSONWritingOptions;
import xfmg.xfctrl.datamodel.json.parameter.ListToMapTransformation;
import xfmg.xfctrl.datamodel.json.parameter.ListWrapper;
import xfmg.xfctrl.datamodel.json.parameter.MemberSubstitution;
import xfmg.xfctrl.datamodel.json.parameter.XynaObjectDecider;



public class JSONDatamodelServicesServiceOperationImpl implements ExtendedDeploymentTask, JSONDatamodelServicesServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(JSONDatamodelServicesServiceOperationImpl.class);

  public void onDeployment() throws XynaException {
  }


  public void onUndeployment() throws XynaException {
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  @Override
  public Document decodeValue(Document doc) {
    String s = doc.getText();
    JSONToken token = new JSONToken(JSONTokenType.text, 0, s.length()-1);
    JSONParser jp = new JSONParser(s);
    String content = jp.getValueAsString(token);
    return new Document(doc.getDocumentType(), content);
  }

  @Override
  public Document encodeValue(Document doc) {
    JSONValue value = new JSONValue();
    value.unversionedSetStringOrNumberValue(doc.getText());
    value.unversionedSetType(JSONVALTYPES.STRING);
    String jsonString = JSONValueWriter.toJSON("", value);
    return new Document(doc.getDocumentType(), jsonString.substring(1, jsonString.length() - 1));
  }

  
  @Override
  public GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject jSONBaseModel) {
    return parseObjectFromJSON(document, jSONBaseModel, new JsonOptions(), null);
  }
  
  @Override
  public GeneralXynaObject parseObjectFromJSONWithOptions(Document document, GeneralXynaObject jSONBaseModel, JSONParsingOptions jSONParsingOptions) {
    JsonOptions options = convertParsingOptions(jSONParsingOptions);
    return parseObjectFromJSON(document, jSONBaseModel, options, jSONParsingOptions.getObjectDecider());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<GeneralXynaObject> parseListFromJSON(Document document, GeneralXynaObject xo) {
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, new JsonOptions(), null);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<GeneralXynaObject> parseListFromJSONWithOptions(Document document, GeneralXynaObject xo, JSONParsingOptions options) {
    JsonOptions jsonOptions = convertParsingOptions(options);
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, jsonOptions, options.getObjectDecider());
  }

  @Override
  public Document writeJSON(GeneralXynaObject jSONBaseModel) {
    return writeJSON(jSONBaseModel, new JsonOptions(), OASScope.none, null);
  }
  
  @Override
  public Document writeJSONList(List<GeneralXynaObject> list) {
    return writeJSONList(list, new JsonOptions(), OASScope.none, null);
  }

  @Override
  public Document writeJSONListWithOptions(List<GeneralXynaObject> jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    JsonOptions options = convertWritingOptions(jSONWritingOptions);
    return writeJSONList(jSONBaseModel, options, OASScope.valueOfOrNone(jSONWritingOptions.getOASMessageType()), jSONWritingOptions.getObjectDecider());
  }

  @Override
  public Document writeJSONWithOptions(GeneralXynaObject jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    JsonOptions options = convertWritingOptions(jSONWritingOptions);
    return writeJSON(jSONBaseModel, options, OASScope.valueOfOrNone(jSONWritingOptions.getOASMessageType()), jSONWritingOptions.getObjectDecider());
  }


  private GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject xo, JsonOptions options, XynaObjectDecider decider) {
    String json = document.getText();
    if (json == null || json.isBlank()) {
      return null;
    }
    JSONTokenizer jt = new JSONTokenizer();
    List<JSONToken> tokens = jt.tokenize(json);
    JSONParser jp = new JSONParser(json);
    if(tokens.get(0).type.equals(JSONTokenType.curleyBraceOpen)) {
      JSONObject job = new JSONObject();
      jp.fillObject(tokens, 0, job);
      GeneralXynaObject continueObject = determineContinueObject(xo, job, decider);
      fillXynaObjectRecursivly(continueObject, job, "", options, decider);
    } else if (tokens.get(0).type.equals(JSONTokenType.squareBraceOpen) && options.listwrapper.contains(xo.getClass().getCanonicalName())) {
      List<JSONValue> job = new ArrayList<>();
      jp.fillArray(tokens, 0, job);
      fillXynaObjectListWrapper(xo, job, "", options, decider);
    } else {
      throw new RuntimeException("Could not parse Object from Json. Neither an object nor a list wrapper");
    }

    return xo;
  }
  
  private Set<String> convertListWrappers(List<? extends ListWrapper> wrappers) {
    if(wrappers == null) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<String>();
    wrappers.forEach(x -> result.add(x.getFqn()));
    return result;
  }


  @SuppressWarnings("unchecked")
  public void fillXynaObjectListWrapper(GeneralXynaObject xo, List<? extends JSONValue> job, String currentPath, JsonOptions options, XynaObjectDecider decider) {
    if(xo == null) {
      return;
    }

    Set<String> listwrapper = options.listwrapper;
    String varNameInXyna = findListWrapperMember(xo);
    String newPath = currentPath.isEmpty() ? varNameInXyna : currentPath + "." + varNameInXyna; 
    newPath += "[]";
    Pair<Class<?>, Type> fieldTypeInfo = determineTypeOfField(xo, varNameInXyna);
    Class<?> typeOfField = fieldTypeInfo.getFirst();
    Type genericType = fieldTypeInfo.getSecond();
    if ((typeOfField == null) || (genericType == null)) {
      logger.debug("parameter " + varNameInXyna + " not found in " + xo);
      return;
    }
    List<Object> objects = new ArrayList<>();
    ParameterizedType pt = (ParameterizedType) genericType;
    Type genType = pt.getActualTypeArguments()[0];
    
    for(JSONValue value : job) {
      if(JSONVALTYPES.NULL.equals(value.getType())) {
        objects.add(null);
      } else if(JSONVALTYPES.ARRAY.equals(value.getType())) {
        Object obj = null;
        try {
          obj = (GeneralXynaObject) ((Class<?>)genType).getConstructor().newInstance();
        } catch (Exception e) {
          logger.warn("could not instantiate object of type " + genType);
          continue;
        }
        if(listwrapper.contains(obj.getClass().getCanonicalName())) {
          fillXynaObjectListWrapper((GeneralXynaObject) obj, value.getArrayValue(), newPath, options, decider);
          objects.add(obj);
        } else {
          List<GeneralXynaObject> objc = createList((Class<GeneralXynaObject>)genType, value.getArrayValue(), newPath, options, decider);
          objects.addAll(objc);
        }
      } else if(JSONVALTYPES.OBJECT.equals(value.getType()))   {
        pt = (ParameterizedType) genericType;
        genType = pt.getActualTypeArguments()[0];
        Class<?> genTypeClass = (Class<?>) genType;
        GeneralXynaObject innerObj = createXynaObject((Class<GeneralXynaObject>) genTypeClass, value.getObjectValue(), decider);
        GeneralXynaObject continueObject = determineContinueObject(innerObj, value.getObjectValue(), decider);
        fillXynaObjectRecursivly(continueObject, value.getObjectValue(), newPath, options, decider);
        objects.add(innerObj);
      } else {
        objects.add(getPrimitiveValue(value));
      }
    }
    
    try {
      xo.set(varNameInXyna, objects);
    } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
      throw new RuntimeException("Could not set " + varNameInXyna + " at " + currentPath, e);
    }
    
  }

  private Object getPrimitiveValue(JSONValue value) {
    switch(value.getType()) {
      case JSONVALTYPES.STRING: 
      case JSONVALTYPES.NUMBER: return value.getStringOrNumberValue();
      case JSONVALTYPES.BOOLEAN: return value.getBooleanValue();
      default: return null;
    }
  }

  private String findListWrapperMember(GeneralXynaObject xo) { 
    Set<String> varNamesOfXynaObject = getVarNames(xo);
    if(varNamesOfXynaObject.size() != 1) {
      throw new RuntimeException("List wrapper " + xo.getClass().getCanonicalName() + " defines " + varNamesOfXynaObject.size() + " members, instead of exactly one.");
    }
    return varNamesOfXynaObject.stream().findAny().orElse(null);
  }
  
  
  private String determineVarNameInXyna(String varName, String currentPath, Map<String, String> substitutions, boolean useLabels, Map<String,String> varNamesOfXynaObject) {
    if (substitutions.containsKey(varName) &&
        substitutions.get(varName).substring(0, substitutions.get(varName).lastIndexOf('.')).equals(currentPath)) {
      return substitutions.get(varName).substring(substitutions.get(varName).lastIndexOf('.') + 1);
    } else if( useLabels ) {
      for (Entry<String,String> ev : varNamesOfXynaObject.entrySet() ) {
        if (ev.getValue().equals(varName)) {
          return ev.getKey();
        }
      }
    } else {
      if (varNamesOfXynaObject.containsKey(varName)) {
        return varName;
      } else {
        return varNamesOfXynaObject.keySet().stream().filter(x -> x.equalsIgnoreCase(varName)).findFirst().orElse(null);
      }
    }
    return null;
  }


  private Pair<Class<?>, Type> determineTypeOfField(GeneralXynaObject xo, String varNameInXyna) {
    try {
      Method m = xo.getClass().getMethod("getField", String.class);
      Field f = (Field) m.invoke(null, varNameInXyna);
      if (f != null && !Modifier.isStatic(f.getModifiers())) {
        Type genericType = f.getGenericType();
        Class<?> typeOfField = f.getType();
        return new Pair<Class<?>, Type>(typeOfField, genericType);
      }
    } catch (Exception ex) {
    }
    throw new RuntimeException("Could not determine type of field " + varNameInXyna + " for " + xo.getClass());
  }
  
  @SuppressWarnings("unchecked")
  public void fillXynaObjectRecursivly(GeneralXynaObject xo, JSONObject job, String currentPath, JsonOptions options, XynaObjectDecider decider) {
    if (xo == null) {
      return;
    }
    
    if(options.inlineGenerics && xo.getClass() == JSONObject.class) {
      ((JSONObject)xo).unversionedSetMembers((List<JSONKeyValue>) job.getMembers());
      return;
    }
    
    boolean useLabels = options.useLabels;
    Map<String, String> substitutions = options.substitutions;
    Map<String,String> varNamesOfXynaObject = getVarNames(xo, useLabels);
    for (JSONKeyValue e : job.getMembers()) {
      String varName = e.getKey();
      JSONValue value = e.getValue();
      String varNameInXyna = determineVarNameInXyna(varName, currentPath, substitutions, useLabels, varNamesOfXynaObject);
      
      if (varNameInXyna == null) {
        if (decider != null) {
          decider.onUnknownMember(xo, varName, value);
        } else {
          logger.debug("parameter " + varNameInXyna + " not found in " + xo);
        }
        continue;
      }

      Pair<Class<?>, Type> fieldTypeInfo = determineTypeOfField(xo, varNameInXyna);
      Class<?> typeOfField = fieldTypeInfo.getFirst();
      Type genericType = fieldTypeInfo.getSecond();
      if ((typeOfField == null) || (genericType == null)) {
        logger.debug("parameter " + varNameInXyna + " not found in " + xo);
        continue;
      }

      String newPath = currentPath.isEmpty() ? varNameInXyna : (currentPath + "." + varNameInXyna);

      try {
        setFieldInObject(xo, options, decider, varName, value, varNameInXyna, typeOfField, genericType, newPath);
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND ex) {
        logger.debug("parameter " + varName + " not found in " + xo);
        continue;
      }
    }
  }

  private void setFieldInObject(GeneralXynaObject xo, JsonOptions options, XynaObjectDecider decider, String varName, JSONValue value, String varNameInXyna,
                                  Class<?> typeOfField, Type genericType, String newPath)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {

    switch (value.getType()) {
      case JSONVALTYPES.BOOLEAN :
        processBooleanValue(xo, varName, value, varNameInXyna, typeOfField);
        break;
      case JSONVALTYPES.NULL :
        processNullValue(xo, varName, varNameInXyna, typeOfField);
        break;
      case JSONVALTYPES.STRING :
        processStringValue(xo, varName, value, varNameInXyna, typeOfField);
        //fall through
      case JSONVALTYPES.NUMBER :
        extractNumberValue(xo, varName, value, varNameInXyna, typeOfField);
        break;
      case JSONVALTYPES.ARRAY :
        processArrayValue(xo, options, decider, varName, value, varNameInXyna, typeOfField, genericType, newPath);
        break;
      case JSONVALTYPES.OBJECT :
        processObjectValue(xo, options, decider, varName, value, varNameInXyna, typeOfField, genericType, newPath);
        break;
      default :
        throw new RuntimeException("unexpected type : " + value.getType());
    }
  }

  @SuppressWarnings("unchecked")
  private void processObjectValue(GeneralXynaObject xo, JsonOptions options, XynaObjectDecider decider, String varName, JSONValue value,
                                  String varNameInXyna, Class<?> typeOfField, Type genericType, String newPath)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    Object o;
    try {
      o = xo.get(varNameInXyna);
    } catch (InvalidObjectPathException e1) {
      logger.debug("parameter " + varName + " not found in " + xo);
      return;
    }
    if (options.transformations.containsKey(newPath) && List.class.isAssignableFrom(typeOfField)) {
      ParameterizedType pt = (ParameterizedType) genericType;
      Type genType = pt.getActualTypeArguments()[0];
      if (genType instanceof Class) {
        Class<?> genTypeClass = (Class<?>) genType;
        if (XynaObject.class.isAssignableFrom(genTypeClass)) {
          List<JSONValue> list = new ArrayList<JSONValue>();
          list.addAll(value.getObjectValue().getMembers().stream().map(x -> x.getValue()).collect(Collectors.toList()));
          List<?> l = createList((Class<? extends XynaObject>) genTypeClass, list, newPath, options, decider);
          xo.set(varNameInXyna, l);
        } else {
          throw new RuntimeException("unexpected xynaobject type " + genTypeClass + " of parameter " + varName + " in " + xo);
        }
      } else {
        throw new RuntimeException("unexpected generic type " + genericType + " of parameter " + varName + " in " + xo);
      }
    } else {
      if (o == null) {
        if (Modifier.isAbstract(typeOfField.getModifiers())) {
          throw new RuntimeException("Can not instantiate abstract member type " + typeOfField + " for member " + varNameInXyna + ".");
        }
        o = createXynaObject((Class<GeneralXynaObject>) typeOfField, value.getObjectValue(), decider);
        GeneralXynaObject continueObject = determineContinueObject((GeneralXynaObject)o, value.getObjectValue(), decider);
        xo.set(varNameInXyna, o);
        o = continueObject;
      }
      if (o instanceof XynaObject) {
        fillXynaObjectRecursivly((XynaObject) o, value.getObjectValue(), newPath, options, decider);
      } else {
        logger.debug("skipping member " + varName + " in " + xo + ", because it is not of complex type");
      }
    }
  }


  @SuppressWarnings("unchecked")
  private void processArrayValue(GeneralXynaObject xo, JsonOptions options, XynaObjectDecider decider, String varName, JSONValue value,
                                 String varNameInXyna, Class<?> typeOfField, Type genericType, String newPath)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if (typeOfField == List.class) {
      if (genericType instanceof ParameterizedType) {
        ParameterizedType pt = (ParameterizedType) genericType;
        Type genType = pt.getActualTypeArguments()[0];
        if (genType instanceof Class) {
          Class<?> genTypeClass = (Class<?>) genType;
          if (XynaObject.class.isAssignableFrom(genTypeClass)) {
            List<?> l = createList((Class<? extends XynaObject>) genTypeClass, value.getArrayValue(), newPath, options, decider);
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == String.class) {
            List<String> l = new ArrayList<String>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType()) || JSONVALTYPES.NUMBER.equals(jv.getType())) {
                l.add(jv.getStringOrNumberValue());
              } else if (JSONVALTYPES.BOOLEAN.equals(jv.getType())) {
                l.add(String.valueOf(jv.getBooleanValue()));
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of string-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == Boolean.class) {
            List<Boolean> l = new ArrayList<Boolean>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType())) {
                l.add(Boolean.valueOf(jv.getStringOrNumberValue()));
              } else if (JSONVALTYPES.BOOLEAN.equals(jv.getType())) {
                l.add(jv.getBooleanValue());
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of boolean-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == Integer.class) {
            List<Integer> l = new ArrayList<Integer>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType()) || JSONVALTYPES.NUMBER.equals(jv.getType())) {
                try {
                  l.add(Integer.valueOf(jv.getStringOrNumberValue()));
                } catch (NumberFormatException ex) {
                  logger.warn("Skipped array element " + jv.getStringOrNumberValue() + ", because it is not a " + genTypeClass
                      + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                }
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == Long.class) {
            List<Long> l = new ArrayList<Long>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType()) || JSONVALTYPES.NUMBER.equals(jv.getType())) {
                try {
                  l.add(Long.valueOf(jv.getStringOrNumberValue()));
                } catch (NumberFormatException ex) {
                  logger.warn("Skipped array element " + jv.getStringOrNumberValue() + ", because it is not a " + genTypeClass
                      + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                }
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == Double.class) {
            List<Double> l = new ArrayList<Double>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType()) || JSONVALTYPES.NUMBER.equals(jv.getType())) {
                try {
                  l.add(Double.valueOf(jv.getStringOrNumberValue()));
                } catch (NumberFormatException ex) {
                  logger.warn("Skipped array element " + jv.getStringOrNumberValue() + ", because it is not a " + genTypeClass
                      + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                }
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else if (genTypeClass == Float.class) {
            List<Float> l = new ArrayList<Float>();
            for (JSONValue jv : value.getArrayValue()) {
              if (JSONVALTYPES.STRING.equals(jv.getType()) || JSONVALTYPES.NUMBER.equals(jv.getType())) {
                try {
                  l.add(Float.valueOf(jv.getStringOrNumberValue()));
                } catch (NumberFormatException ex) {
                  logger.warn("Skipped array element " + jv.getStringOrNumberValue() + ", because it is not a " + genTypeClass
                      + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                }
              } else if (JSONVALTYPES.NULL.equals(jv.getType())) {
                l.add(null);
              } else {
                logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                    + " contains a list of " + genType);
              }
            }
            xo.set(varNameInXyna, l);
          } else {
            logger.debug("parameter " + varName + " is of unsupported type " + genTypeClass + "-List in " + xo + ".");
          }
        } else {
          throw new RuntimeException("unexpected generic parameter type " + genType + " of parameter " + varName + " in " + xo);
        }
      } else {
        throw new RuntimeException("unexpected generic type " + genericType + " of parameter " + varName + " in " + xo);
      }
    } else if (options.listwrapper.contains(typeOfField.getCanonicalName())) {
      GeneralXynaObject obj = null;
      try {
        obj = (GeneralXynaObject) typeOfField.getConstructor().newInstance();
      } catch (Exception e) {
        logger.debug("could not create instance of " + typeOfField);
        return;
      }
      fillXynaObjectListWrapper(obj, value.getArrayValue(), newPath, options, decider);
      xo.set(varNameInXyna, obj);
    } else {
      logger.debug("parameter " + varName + " is of type " + typeOfField + " in " + xo + ", but of type array in JSON");
    }
  }


  private void extractNumberValue(GeneralXynaObject xo, String varName, JSONValue value, String varNameInXyna, Class<?> typeOfField)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if (typeOfField == String.class) {
      xo.set(varNameInXyna, value.getStringOrNumberValue());
    } else if (Number.class.isAssignableFrom(typeOfField) || typeOfField.isPrimitive()) {
      double d;
      try {
        d = Double.parseDouble(value.getStringOrNumberValue());
      } catch (NumberFormatException e1) {
        logger.debug("parameter " + varName + " can not be converted to field of type " + typeOfField + " in " + xo);
        return;
      }
      if (typeOfField == int.class || typeOfField == Integer.class) {
        xo.set(varNameInXyna, Integer.valueOf((int) d));
      } else if (typeOfField == float.class || typeOfField == Float.class) {
        xo.set(varNameInXyna, Float.valueOf((float) d));
      } else if (typeOfField == double.class || typeOfField == Double.class) {
        xo.set(varNameInXyna, d);
      } else if (typeOfField == long.class || typeOfField == Long.class) {
        xo.set(varNameInXyna, Long.valueOf((long) d));
      } else {
        logger.debug("unsupported type " + typeOfField);
      }
    } else {
      logger.debug("parameter " + varName + " is of type " + typeOfField + " in " + xo + ", but of type number in JSON");
    }
  }


  private void processStringValue(GeneralXynaObject xo, String varName, JSONValue value, String varNameInXyna, Class<?> typeOfField)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if (typeOfField == Boolean.class || typeOfField == boolean.class) {
      if (value.getStringOrNumberValue().equalsIgnoreCase("true")) {
        xo.set(varNameInXyna, true);
      } else if (value.getStringOrNumberValue().equalsIgnoreCase("false")) {
        xo.set(varNameInXyna, false);
      } else {
        logger.debug("parameter " + varName + " can not be converted to field of type " + typeOfField + " in " + xo);
      }
    }
  }


  private void processNullValue(GeneralXynaObject xo, String varName, String varNameInXyna, Class<?> typeOfField)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if (typeOfField.isPrimitive()) {
      logger.debug("parameter " + varName + " is of primitive type (" + typeOfField + "), but null in JSON");
    } else {
      xo.set(varNameInXyna, null);
    }
  }


  private void processBooleanValue(GeneralXynaObject xo, String varName, JSONValue value, String varNameInXyna, Class<?> typeOfField)
      throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if (typeOfField == boolean.class || typeOfField == Boolean.class) {
      xo.set(varNameInXyna, value.getBooleanValue());
    } else if (typeOfField == String.class) {
      xo.set(varNameInXyna, String.valueOf(value.getBooleanValue()));
    } else {
      logger.debug("parameter " + varName + " has type " + typeOfField + " in " + xo + ", but is of type boolean in JSON.");
    }
  }

  
  @SuppressWarnings("unchecked")
  private <A extends GeneralXynaObject> A createXynaObject(Class<A> genTypeClass, JSONObject obj, XynaObjectDecider decider) {
    try {
      return decider == null ? genTypeClass.getConstructor().newInstance() : (A) decider.decideObjectOnRead(genTypeClass.getCanonicalName(), obj);
    } catch (InstantiationException e1) {
      throw new RuntimeException("Could not instantiate " + genTypeClass.getName(), e1);
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
  }
  
  private GeneralXynaObject determineContinueObject(GeneralXynaObject createdObject, JSONObject obj, XynaObjectDecider decider) {
    return decider == null ? createdObject : decider.continueReadWithObject(createdObject, obj);
  }

  @SuppressWarnings("unchecked")
  public <A extends GeneralXynaObject> List<A> createList(Class<A> genTypeClass, List<? extends JSONValue> array, String currentPath, JsonOptions options, XynaObjectDecider decider) {
    if (decider == null && Modifier.isAbstract(genTypeClass.getModifiers())) {
      throw new RuntimeException("Can not instantiate list elements of abstract type " + genTypeClass + ".");
    }
    
    if(options.inlineGenerics && genTypeClass == JSONValue.class) {
      return (List<A>) array;
    }
    
    List<A> l = new ArrayList<A>();
    
    String newPath = currentPath.isEmpty() ? currentPath : currentPath+"[]";
    
    for (JSONValue jv : array) {
      A listElement;
      GeneralXynaObject continueObject;
      if (JSONVALTYPES.OBJECT.equals(jv.getType())) {
        listElement = createXynaObject(genTypeClass, jv.getObjectValue(), decider);
        continueObject = determineContinueObject(listElement, jv.getObjectValue(), decider);
        fillXynaObjectRecursivly(continueObject, jv.getObjectValue(), newPath, options, decider);
        l.add(listElement);
      } else if (JSONVALTYPES.ARRAY.equals(jv.getType()) && options.listwrapper.contains(genTypeClass.getCanonicalName())) {
        listElement = createXynaObject(genTypeClass, jv.getObjectValue(), decider);
        continueObject = determineContinueObject(listElement, jv.getObjectValue(), decider);
        fillXynaObjectListWrapper(continueObject, jv.getArrayValue(), newPath, options, decider);
        l.add(listElement);
      } else {
        logger.debug("array element " + jv + " is not of object type");
      }
    }
    return l;
  }
  
  
  private HashMap<String, String> getVarNames(GeneralXynaObject xo, boolean useLabels) {
    Set<String> varNames = getVarNames(xo);
    HashMap<String, String> ret = new HashMap<String, String>();
    for (String v : varNames) {
      ret.put(v,  useLabels ? XOUtils.getLabelFor(xo, v) : v);
    }

    return ret;
  }
  

  @SuppressWarnings("unchecked")
  private Set<String> getVarNames(GeneralXynaObject xo) {
    try {
      Method methodGetVarNames = xo.getClass().getMethod("getVariableNames");
      return (Set<String>) methodGetVarNames.invoke(xo);
    } catch (Exception e) {
      // not expected
      throw new RuntimeException(e);
    }
  }


  public Document writeJSON(GeneralXynaObject jSONBaseModel, JsonOptions options, OASScope scope, XynaObjectDecider decider) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    JSONValue job = createValFromXynaObjectRecursively(jSONBaseModel, "", options, scope, decider);
    if (job != null) {
      d.setText(JSONValueWriter.toJSON("", job));
    } else {
      d.setText("");
    }
    return d;
  }


  private static final XynaPropertyBoolean includeNulls = new XynaPropertyBoolean("xfmg.xfctrl.datamodel.json.createjson.includenulls",
                                                                                  false)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Determines whether JSON strings created from XMOM Data Types include null values for all members that are not set. \nExample: {\"name\":\"XYZ\", address:null}\nvs\n{\"name\":\"XYZ\"}");
  
  private JsonOptions convertParsingOptions(JSONParsingOptions options) {
    Map<String, String> mapTransformations = convertListToMapTransformations(options.getListToMapTransformation());
    Map<String, String> mapSubstitutions = convertMemberSubstitutions(options.getMemberSubstitution());
    Set<String> listWrappers = convertListWrappers(options.getListWrapper());
    boolean inline = options.getProcessAllInputGenerically() != null ? !options.getProcessAllInputGenerically() : true;
    JsonOptions result = new JsonOptions(mapTransformations, mapSubstitutions, listWrappers, options.getUseLabels(), inline);
    return result;
  }
  

  private JsonOptions convertWritingOptions(JSONWritingOptions options) {
    Map<String, String> mapTransformations = convertListToMapTransformations(options.getListToMapTransformation());
    Map<String, String> mapSubstitutions = convertMemberSubstitutions(options.getMemberSubstitution());
    Set<String> listWrappers = convertListWrappers(options.getListWrapper());
    boolean inline = options.getProcessAllInputGenerically() != null ? !options.getProcessAllInputGenerically() : true;
    JsonOptions result = new JsonOptions(mapTransformations, mapSubstitutions, listWrappers, options.getUseLabels(), inline);
    return result;
  }

  
  private Map<String, String> convertListToMapTransformations(List<? extends ListToMapTransformation> transformations) {
    Map<String, String> mapTransformations = new HashMap<String, String>();
    if (transformations != null) {
      for (ListToMapTransformation listToMapTransformation : transformations) {
        mapTransformations.put(listToMapTransformation.getPathToList(), listToMapTransformation.getKeyName());
      }
    }
    return mapTransformations;
  }


  private Map<String, String> convertMemberSubstitutions(List<? extends MemberSubstitution> substitutions) {
    Map<String, String> mapSubstitutions = new HashMap<String, String>();
    if (substitutions != null) {
      for (MemberSubstitution memberSubstitution : substitutions) {
        mapSubstitutions.put(memberSubstitution.getPathToMemberInDataType(), memberSubstitution.getJsonName());
      }
    }
    return mapSubstitutions;
  }
  
  public enum OASScope {
    request, response, none;
    
    public static OASScope valueOfOrNone(String val) {
      if (val == null) {
        return OASScope.none;
      }
      return valueOf(val.toLowerCase());
    }
  }
  
  public JSONValue createValFromXynaObjectListRecurisvely(List<? extends GeneralXynaObject> xo, String currentPath, JsonOptions options, OASScope scope, XynaObjectDecider decider) {
    if(xo == null) {
      return createNullValue();
    }
    JSONValue result = new JSONValue();
    result.unversionedSetType(JSONVALTYPES.ARRAY);
    List<JSONValue> values = new ArrayList<JSONValue>(xo.size());
    xo.forEach(xObj -> values.add(createValFromXynaObjectRecursively(xObj, currentPath, options, scope, decider)));
    result.unversionedSetArrayValue(values);
    return result;
  }
  
  @SuppressWarnings("unchecked")
  public JSONValue createValFromXynaObjectRecursively(GeneralXynaObject xo, String currentPath, JsonOptions options, OASScope scope, XynaObjectDecider decider) {
    if(xo == null) {
      return null;
    }
    
    if(options.inlineGenerics && xo.getClass() == JSONValue.class) {
      return (JSONValue)xo;
    }
    
    JSONValue result = new JSONValue();
    if(options.listwrapper.contains(xo.getClass().getCanonicalName())) {
      String member = findListWrapperMember(xo);
      String newPath = currentPath.isEmpty() ? member : currentPath + "." + member; 
      newPath += "[]";
      Pair<Class<?>, Type> fieldTypeInfo = determineTypeOfField(xo, member);
      Type genericType = fieldTypeInfo.getSecond();
      ParameterizedType pt = (ParameterizedType) genericType;
      Type genType = pt.getActualTypeArguments()[0];
      Class<?> genTypeClass = (Class<?>) genType;
      if (XynaObject.class.isAssignableFrom(genTypeClass)) {
        List<GeneralXynaObject> list = (List<GeneralXynaObject>) get(xo, member);
        result = createValFromXynaObjectListRecurisvely(list, newPath, options, scope, decider);
      } else {
        List<Object> list = (List<Object>) get(xo, member);
        if(list == null) {
          result = createNullValue();
        } else {
          result.unversionedSetType(JSONVALTYPES.ARRAY);
          List<JSONValue> values = new ArrayList<>();
          list.forEach(x -> values.add(createPrimitiveJsonValue(x)));
          result.setArrayValue(values);
        }
      }
    } else {
      result.unversionedSetType(JSONVALTYPES.OBJECT);
      JSONObject obj = createFromXynaObjectRecursivly(xo, currentPath, options, scope, decider);
      result.unversionedSetObjectValue(obj);
    }
    return result;
  }
  
  private JSONValue createPrimitiveJsonValue(Object val) {
    JSONValue value = new JSONValue();
    if (val instanceof String) {
      value.unversionedSetType(JSONVALTYPES.STRING);
      value.unversionedSetStringOrNumberValue((String) val);
    } else if (val instanceof Boolean) {
      value.unversionedSetType(JSONVALTYPES.BOOLEAN);
      value.unversionedSetBooleanValue((Boolean) val);
    } else if (val instanceof Number) {
      value.unversionedSetType(JSONVALTYPES.NUMBER);
      value.unversionedSetStringOrNumberValue(val.toString());
    } else {
      logger.debug("Unsupported parameter type: " + val);
      return null;
    }
      
    return value;
  }
  
  private Object get(GeneralXynaObject xo, String member) {
    try {
      return xo.get(member);
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
  }
  

  private boolean skipMember(GeneralXynaObject xo, String varNameInXyna, OASScope scope) {
    switch (scope) {
      case none :
        return false;
      case request :
        return isOASMarked(xo, varNameInXyna, true);
      case response :
        return isOASMarked(xo, varNameInXyna, false);
    }
    return false;
  }
  
  private JSONValue createNullValue() {
    if (includeNulls.get()) {
      JSONValue result = new JSONValue();
      result.unversionedSetType(JSONVALTYPES.NULL);
      return result;
    }
    return null; 
  }
  
  private JSONValue createJSONValue(GeneralXynaObject xo, String newPath, JsonOptions options, HashMap<String,String> varNamesOfXynaObject, String varNameInXyna, OASScope scope, XynaObjectDecider decider) {
    JSONValue value = new JSONValue();
    try {
      Object val = xo.get(varNameInXyna);
      if (val == null) {
        value = createNullValue();
      } else if (val instanceof XynaObject) {
        value.unversionedSetType(JSONVALTYPES.OBJECT);
        JSONObject obj = createFromXynaObjectRecursivly((XynaObject) val, newPath, options, scope, decider);
        value.unversionedSetObjectValue(obj);
      } else if (val instanceof List) {
        value = createListValue(options, scope, newPath, val, decider);
      } else {
        value = createPrimitiveJsonValue(val);
      } 
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
    return value;
  }
  
  public JSONObject createFromXynaObjectRecursivly(GeneralXynaObject xo, String currentPath, JsonOptions options, OASScope scope, XynaObjectDecider decider) {
    if (xo == null) {
      return null;
    }
    
    if(options.inlineGenerics && xo.getClass() == JSONObject.class) {
      return (JSONObject)xo;
    }
    
    xo = decider == null ? xo : decider.decideObjectOnWrite(xo);
    
    JSONObject job = new JSONObject();
    List<JSONKeyValue> members = new ArrayList<JSONKeyValue>();
    HashMap<String,String> varNamesOfXynaObject = getVarNames(xo, options.useLabels);
    for (String varNameInXyna : varNamesOfXynaObject.keySet()) {
      if(skipMember(xo, varNameInXyna, scope)) {
        continue;
      }
      String varName = varNamesOfXynaObject.get(varNameInXyna);
      String newPath = currentPath.isEmpty() ? varNameInXyna : currentPath + "." + varNameInXyna;
      varName = options.substitutions.getOrDefault(newPath, varName);
      JSONValue value = createJSONValue(xo, newPath, options, varNamesOfXynaObject, varNameInXyna, scope, decider);
      if (value != null) {
        members.add(new JSONKeyValue(varName, value));
      }
    }
    job.unversionedSetMembers(members);
    return job;
  }


  private JSONValue createListValue(JsonOptions options, OASScope scope, String newPath, Object val, XynaObjectDecider decider) throws InvalidObjectPathException {
    JSONValue value = new JSONValue();
    value.unversionedSetType(JSONVALTYPES.ARRAY);
    if (options.transformations.containsKey(newPath)) {
      String keyName = options.transformations.get(newPath);
      value.unversionedSetType(JSONVALTYPES.OBJECT);
      JSONObject map = new JSONObject();
      List<JSONKeyValue> list = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<? extends XynaObject> l = (List<? extends XynaObject>) val;
      for (XynaObject xoe: l) {
        JSONValue childValue = new JSONValue();
        JSONObject childJob = createFromXynaObjectRecursivly(xoe, newPath+"[]", options, scope, decider);
        childValue.unversionedSetObjectValue(childJob);
        childValue.unversionedSetType(JSONVALTYPES.OBJECT);
        list.add(new JSONKeyValue(xoe.get(keyName).toString(), childValue));
      }
      map.unversionedSetMembers(list);
      value.unversionedSetObjectValue(map);
    } else {
      List<JSONValue> arr = new ArrayList<JSONValue>();
      List<?> l = (List<?>) val;
      for (Object o : l) {
        JSONValue jval = new JSONValue();
        if (o == null) {
          jval.unversionedSetType(JSONVALTYPES.NULL);
        } else if (o instanceof XynaObject) {
          JSONObject childJob = createFromXynaObjectRecursivly((XynaObject) o, newPath+"[]", options, scope, decider);
          jval.unversionedSetObjectValue(childJob);
          jval.unversionedSetType(JSONVALTYPES.OBJECT);
        } else {
          jval = createPrimitiveJsonValue(o);
        }
        arr.add(jval);
      }
      value.unversionedSetArrayValue(arr);
      value.unversionedSetType(JSONVALTYPES.ARRAY);
    }
    return value;
  }


  private static final GenerationBaseCache cacheForOASDataModelTypes = new GenerationBaseCache();


  /**
   * returns true if the datatype is part of an OAS datamodel and its membervar is marked as readonly/writeonly in its
   * metadata (in the xml definition).
   * readOnly = false means writeOnly
   */
  private boolean isOASMarked(GeneralXynaObject xo, String varNameInXyna, boolean readOnly) {
    Class<?> clazz = xo.getClass();
    ClassLoader cl = clazz.getClassLoader();
    logger.trace("checking OAS marked for " + clazz.getName() + ", varName=" + varNameInXyna + "...");
    if (!isPartOfOASDatamodel(cl)) {
      return false;
    }
    logger.trace(clazz.getName() + " is in oas datamodel.");

    String fqXmlName = clazz.getAnnotation(XynaObjectAnnotation.class).fqXmlName();
    Long revision = ((MDMClassLoader) cl).getRevision();
    DOM dom;
    try {
      dom = DOM.getOrCreateInstance(fqXmlName, cacheForOASDataModelTypes, revision);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException();
    }
    try {
      dom.parseGeneration(true, false, false);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }
    for (AVariable v : dom.getAllMemberVarsIncludingInherited()) {
      if (v.getVarName() == null) {
        continue;
      }
      if (v.getVarName().equals(varNameInXyna)) {
        DataModelInformation dmi = v.getDataModelInformation();
        if (dmi == null) {
          continue;
        }
        String scope = dmi.get("OASScope");
        if (scope == null) {
          return false;
        }
        if (readOnly && scope.equals("readOnly")) {
          return true;
        }
        if (!readOnly && scope.equals("writeOnly")) {
          return true;
        }
        return false;
      }
    }
    return false;
  }


  /*
   * list (cache) of all known OAS datamodel revisions. 
   */
  private static final Set<Long> knownOASRevisions = new HashSet<>();
  private static long maxRevisionCheckedForCache = -1;


  private boolean isPartOfOASDatamodel(ClassLoader cl) {
    if (cl instanceof MDMClassLoader) {
      long rev = ((MDMClassLoader) cl).getRevision();
      synchronized (knownOASRevisions) {
        if (knownOASRevisions.contains(rev)) {
          return true;
        }
        if (rev < maxRevisionCheckedForCache) {
          return false;
        }
        //encountered new revision, cache reinitialization
        maxRevisionCheckedForCache = -1;
        Set<Long> previousOASRevisions = new HashSet<>(knownOASRevisions);
        knownOASRevisions.clear();
        try {
          List<DataModel> oasModels =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement().listDataModels("OAS");
          logger.debug("found " + oasModels.size() + " OAS models");
          RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
          for (DataModel dm : oasModels) {
            String appName = dm.getType().getLabel();
            String version = dm.getVersion();
            long dmrev = rm.getRevision(new Application(appName, version));
            knownOASRevisions.add(dmrev);
            maxRevisionCheckedForCache = Math.max(maxRevisionCheckedForCache, dmrev);
          }
          logger.debug("oas revisions: " + knownOASRevisions.toString() + ", checking " + rev);
          if (!knownOASRevisions.equals(previousOASRevisions)) { //else keep the cached type info, it is still valid.
            cacheForOASDataModelTypes.clear();
          }
          maxRevisionCheckedForCache = Math.max(maxRevisionCheckedForCache, rm.getAllRevisions().stream().max(Long::compare).get());
          logger.debug("maxrevchecked = " + maxRevisionCheckedForCache);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException(e);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }
        return knownOASRevisions.contains(rev);
      }
    }
    return false;
  }

  private Document writeJSONList(List<? extends GeneralXynaObject> list, JsonOptions options, OASScope scope, XynaObjectDecider decider) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    if (list == null || list.isEmpty()) {
      d.setText("[]");
    } else {
      JSONValue value = createValFromXynaObjectListRecurisvely(list, "", options, scope, decider);
      d.setText(JSONValueWriter.toJSON("", value));
    }
    return d;
  }


  public List<? extends GeneralXynaObject> parseListFromJSONWithOptions(Document document, GeneralXynaObject xo, JsonOptions options,
                                                                        XynaObjectDecider decider) {

    String json = document.getText();
    if (json == null || json.isBlank()) {
      return new ArrayList<GeneralXynaObject>();
    }
    JSONTokenizer jt = new JSONTokenizer();
    List<JSONToken> tokens = jt.tokenize(json);
    JSONParser jp = new JSONParser(json);
    List<JSONValue> arr = new ArrayList<JSONValue>();
    jp.fillArray(tokens, 0, arr);

    return createList(xo.getClass(), arr, "", options, decider);
  }

  
  public static class JsonOptions {
    private final Map<String, String> transformations;
    private final Map<String, String> substitutions;
    private final Set<String> listwrapper;
    private final boolean useLabels;
    private final boolean inlineGenerics;

    public JsonOptions() {
      transformations = Collections.emptyMap();
      substitutions = Collections.emptyMap();
      listwrapper = Collections.emptySet();
      useLabels = false;
      inlineGenerics = true;
    }
    
    public JsonOptions(Map<String, String> transformations, Map<String, String> substitutions, Set<String> listwrapper, boolean useLabels, boolean inlineGenerics) {
      this.transformations = transformations;
      this.substitutions = substitutions;
      this.listwrapper = listwrapper;
      this.useLabels = useLabels;
      this.inlineGenerics = inlineGenerics;
    }
    
  }
}
