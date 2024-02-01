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
import java.lang.reflect.InvocationTargetException;
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
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObjectWriter;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONVALTYPES;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueWriter;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONTokenType;
import xfmg.xfctrl.datamodel.json.parameter.JSONParsingOptions;
import xfmg.xfctrl.datamodel.json.parameter.JSONWritingOptions;
import xfmg.xfctrl.datamodel.json.parameter.ListToMapTransformation;
import xfmg.xfctrl.datamodel.json.parameter.MemberSubstitution;
import xfmg.xfctrl.datamodel.json.parameter.XynaObjectDecider;



public class JSONDatamodelServicesServiceOperationImpl implements ExtendedDeploymentTask, JSONDatamodelServicesServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(JSONDatamodelServicesServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
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
  public GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject jSONBaseModel) {
    return parseObjectFromJSON(document, jSONBaseModel, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(),false, null);
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

  
  public GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject jSONBaseModel, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels, XynaObjectDecider decider) {
    String json = document.getText();
    if (json == null || json.isBlank()) {
      return null;
    }
    JSONTokenizer jt = new JSONTokenizer();
    List<JSONToken> tokens = jt.tokenize(json);
    JSONParser jp = new JSONParser(json);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);

    fillXynaObject(jSONBaseModel, job, transformations, substitutions, useLabels, decider);
    return jSONBaseModel;
  }


  /*
   * TODO zuordnungsregeln erweitern:
   * - labels berücksichtigen (mit und ohne leerzeichen)
   * - groß/kleinschreibung konfigurierbar
   * - benutzer kann selbst ein mapping übergeben
   * - parametrisierbarkeit dieser konfigurationsmöglichkeiten 
   */
  //TODO reflection cachen
  public void fillXynaObject(GeneralXynaObject xo, JSONObject job, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels, XynaObjectDecider decider) {
    Map<String, String> mapTransformations = new HashMap<String, String>();
    if (transformations != null) {
      for (ListToMapTransformation listToMapTransformation : transformations) {
        mapTransformations.put(listToMapTransformation.getPathToList(), listToMapTransformation.getKeyName());
      }
    }
    Map<String, String> mapSubstitutions = new HashMap<String, String>();
    if (substitutions != null) {
      for (MemberSubstitution memberSubstitution : substitutions) {
        mapSubstitutions.put(memberSubstitution.getJsonName(), memberSubstitution.getPathToMemberInDataType());
      }
    }
    fillXynaObjectRecursivly(xo, job, "", mapTransformations, mapSubstitutions, useLabels, decider);
  }
  
  @SuppressWarnings("unchecked")
  public void fillXynaObjectRecursivly(GeneralXynaObject xo, JSONObject job, String currentPath, Map<String, String> transformations, Map<String, String> substitutions, boolean useLabels, XynaObjectDecider decider) {
    if (xo == null) {
      return;
    }
    Map<String,String> varNamesOfXynaObject = getVarNames(xo, useLabels);
    for (JSONKeyValue e : job.getMembers()) {
      String varName = e.getKey();
      JSONValue value = e.getValue();

      String varNameInXyna = null;
      if (substitutions.containsKey(varName) &&
          substitutions.get(varName).substring(0, substitutions.get(varName).lastIndexOf('.')).equals(currentPath)) {
        varNameInXyna = substitutions.get(varName).substring(substitutions.get(varName).lastIndexOf('.') + 1);
      } else if( useLabels ) {
        for (Entry<String,String> ev : varNamesOfXynaObject.entrySet() ) {
          if (ev.getValue().equals(varName)) {
            varNameInXyna = ev.getKey();
            break;
          }
        }
      } else {
        if (varNamesOfXynaObject.containsKey(varName)) {
          varNameInXyna = varName;
        } else {
          for (String v : varNamesOfXynaObject.keySet()) {
            if (v.equalsIgnoreCase(varName)) {
              varNameInXyna = v;
              break;
            }
          }
        }
      }
      if (varNameInXyna == null) {
        logger.debug("parameter " + varName + " not found in " + xo);
        continue;
      }

      Class<?> typeOfField = null;
      Type genericType = null;
      boolean searchingClass = true;
      Class<?> currentClass = xo.getClass();
      while (searchingClass) {
        try {
          Field f = currentClass.getDeclaredField(varNameInXyna);
          if (!Modifier.isStatic(f.getModifiers())) {
            genericType = f.getGenericType();
            typeOfField = f.getType();
          }
          searchingClass = false;
        } catch (NoSuchFieldException ex) {
          currentClass = currentClass.getSuperclass();
          if ((currentClass == null) || (currentClass == XynaObject.class) || (currentClass == Object.class)) {
            searchingClass = false;
          } else {
            searchingClass = true;
          }
        }
      }
      if ((typeOfField == null) || (genericType == null)) {
        logger.debug("parameter " + varNameInXyna + " not found in " + xo);
        continue;
      }

      String newPath = currentPath.isEmpty() ? varNameInXyna : (currentPath + "." + varNameInXyna);

      try {
        switch (value.getType()) {
          case JSONVALTYPES.BOOLEAN :
            if (typeOfField == boolean.class || typeOfField == Boolean.class) {
              xo.set(varNameInXyna, value.getBooleanValue());
            } else if (typeOfField == String.class) {
              xo.set(varNameInXyna, String.valueOf(value.getBooleanValue()));
            } else {
              logger.debug("parameter " + varName + " has type " + typeOfField + " in " + xo + ", but is of type boolean in JSON.");
            }
            break;
          case JSONVALTYPES.NULL :
            if (typeOfField.isPrimitive()) {
              logger.debug("parameter " + varName + " is of primitive type (" + typeOfField + "), but null in JSON");
            } else {
              xo.set(varNameInXyna, null);
            }
            break;
          case JSONVALTYPES.STRING :
            if (typeOfField == Boolean.class || typeOfField == boolean.class) {
              if (value.getStringOrNumberValue().equalsIgnoreCase("true")) {
                xo.set(varNameInXyna, true);
              } else if (value.getStringOrNumberValue().equalsIgnoreCase("false")) {
                xo.set(varNameInXyna, false);
              } else {
                logger.debug("parameter " + varName + " can not be converted to field of type " + typeOfField + " in " + xo);
                continue;
              }
            }
          case JSONVALTYPES.NUMBER :
            if (typeOfField == String.class) {
              xo.set(varNameInXyna, value.getStringOrNumberValue());
            } else if (Number.class.isAssignableFrom(typeOfField) || typeOfField.isPrimitive()) {
              double d;
              try {
                d = Double.parseDouble(value.getStringOrNumberValue());
              } catch (NumberFormatException e1) {
                logger.debug("parameter " + varName + " can not be converted to field of type " + typeOfField + " in " + xo);
                continue;
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
            break;
          case JSONVALTYPES.ARRAY :
            //TODO support für vorinstanziierte listenelemente
            if (typeOfField == List.class) {
              if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type genType = pt.getActualTypeArguments()[0];
                if (genType instanceof Class) {
                  Class<?> genTypeClass = (Class<?>) genType;
                  if (XynaObject.class.isAssignableFrom(genTypeClass)) {
                    List<?> l = createList((Class<? extends XynaObject>) genTypeClass, value.getArrayValue(), newPath, transformations, substitutions, useLabels, decider);
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
            } else {
              logger.debug("parameter " + varName + " is of type " + typeOfField + " in " + xo + ", but of type array in JSON");
            }
            break;
          case JSONVALTYPES.OBJECT :
            Object o;
            try {
              o = xo.get(varNameInXyna);
            } catch (InvalidObjectPathException e1) {
              logger.debug("parameter " + varName + " not found in " + xo);
              continue;
            }
            if (transformations.containsKey(newPath) && 
                List.class.isAssignableFrom(typeOfField)) {
              ParameterizedType pt = (ParameterizedType) genericType;
              Type genType = pt.getActualTypeArguments()[0];
              if (genType instanceof Class) {
                Class<?> genTypeClass = (Class<?>) genType;
                if (XynaObject.class.isAssignableFrom(genTypeClass)) {
                  List<JSONValue> list = new ArrayList<JSONValue>();
                  list.addAll(value.getObjectValue().getMembers().stream().map(x -> x.getValue()).collect(Collectors.toList()));
                  List<?> l = createList((Class<? extends XynaObject>) genTypeClass, list, newPath, transformations, substitutions, useLabels, decider);
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
                o = createXynaObject((Class<GeneralXynaObject>) typeOfField, job, decider);
                xo.set(varNameInXyna, o);
              }
              if (o instanceof XynaObject) {
                fillXynaObjectRecursivly((XynaObject) o, value.getObjectValue(), newPath, transformations, substitutions, useLabels, decider);
              } else {
                logger.debug("skipping member " + varName + " in " + xo + ", because it is not of complex type");
              }
            }
            break;
          default :
            throw new RuntimeException("unexpected type : " + value.getType());
        }
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND ex) {
        logger.debug("parameter " + varName + " not found in " + xo);
        continue;
      }
    }
  }

  
  @SuppressWarnings("unchecked")
  private <A extends GeneralXynaObject> A createXynaObject(Class<A> genTypeClass, JSONObject obj, XynaObjectDecider decider) {
    try {
      return decider == null ? genTypeClass.getConstructor().newInstance() : (A) decider.decide(genTypeClass.getCanonicalName(), obj);
    } catch (InstantiationException e1) {
      throw new RuntimeException("Could not instantiate " + genTypeClass.getName(), e1);
    } catch (IllegalAccessException e1) {
      throw new RuntimeException(e1);
    } catch (IllegalArgumentException e1) {
      throw new RuntimeException(e1);
    } catch (InvocationTargetException e1) {
      throw new RuntimeException(e1);
    } catch (SecurityException e1) {
      throw new RuntimeException(e1);
    } catch (NoSuchMethodException e1) {
      throw new RuntimeException(e1);
    }
  }

  private <A extends GeneralXynaObject> List<A> createList(Class<A> genTypeClass, List<? extends JSONValue> array, String currentPath, Map<String, String> mapTransformations, Map<String, String> substitutions, boolean useLabels, XynaObjectDecider decider) {
    if (Modifier.isAbstract(genTypeClass.getModifiers())) {
      throw new RuntimeException("Can not instantiate list elements of abstract type " + genTypeClass + ".");
    }
    List<A> l = new ArrayList<A>();
    
    String newPath = currentPath.isEmpty() ? currentPath : currentPath+"[]";
    
    for (JSONValue jv : array) {
      if (JSONVALTYPES.OBJECT.equals(jv.getType())) {
        A listElement;
        try {
          listElement = (A) genTypeClass.getConstructor().newInstance();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        fillXynaObjectRecursivly(listElement, jv.getObjectValue(), newPath, mapTransformations, substitutions, useLabels, decider);
        l.add(listElement);
      } else {
        logger.debug("array element " + jv + " is not of object type");
      }
    }
    return l;
  }
  
  
  @SuppressWarnings("unchecked")
  private HashMap<String,String> getVarNames(GeneralXynaObject xo, boolean useLabels) {
    try {
      Method methodGetVarNames = xo.getClass().getMethod("getVariableNames");
      HashMap<String,String> ret = new HashMap<String,String>();
      for ( String v : (Set<String>)methodGetVarNames.invoke(xo) ) {
        if (useLabels) {
          ret.put(v, XOUtils.getLabelFor(xo, v));
        } else {
          ret.put(v, v);
        }
      }

      return ret;
    } catch (Exception ex) {
      // not expected
      throw new RuntimeException(ex);
    }
  }

  
  public Document writeJSON(GeneralXynaObject jSONBaseModel) {
    return writeJSON(jSONBaseModel, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(), false, OASScope.none);
  }

  public Document writeJSON(GeneralXynaObject jSONBaseModel, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels, OASScope scope) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    JSONObject job = createFromXynaObject(jSONBaseModel, transformations, substitutions, useLabels, scope);
    if (job != null) {
      d.setText(JSONObjectWriter.toJSON("", job));
    } else {
      d.setText("");
    }
    return d;
  }


  private static final XynaPropertyBoolean includeNulls = new XynaPropertyBoolean("xfmg.xfctrl.datamodel.json.createjson.includenulls",
                                                                                  false)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Determines whether JSON strings created from XMOM Data Types include null values for all members that are not set. \nExample: {\"name\":\"XYZ\", address:null}\nvs\n{\"name\":\"XYZ\"}");


  private JSONObject createFromXynaObject(GeneralXynaObject xo, List<? extends ListToMapTransformation> transformations,
                                          List<? extends MemberSubstitution> substitutions, boolean useLabels, OASScope scope) {
    Map<String, String> mapTransformations = new HashMap<String, String>();
    if (transformations != null) {
      for (ListToMapTransformation listToMapTransformation : transformations) {
        mapTransformations.put(listToMapTransformation.getPathToList(), listToMapTransformation.getKeyName());
      }
    }
    Map<String, String> mapSubstitutions = new HashMap<String, String>();
    if (substitutions != null) {
      for (MemberSubstitution memberSubstitution : substitutions) {
        mapSubstitutions.put(memberSubstitution.getPathToMemberInDataType(), memberSubstitution.getJsonName());
      }
    }
    return createFromXynaObjectRecursivly(xo, "", mapTransformations, mapSubstitutions, useLabels, scope);
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
  
  public JSONObject createFromXynaObjectRecursivly(GeneralXynaObject xo, String currentPath, Map<String, String> transformations, Map<String, String> substitutions, boolean useLabels, OASScope scope) {
    if (xo == null) {
      return null;
    }
    JSONObject job = new JSONObject();
    List<JSONKeyValue> members = new ArrayList<JSONKeyValue>();
    HashMap<String,String> varNamesOfXynaObject = getVarNames(xo, useLabels);
    for (String varNameInXyna : varNamesOfXynaObject.keySet()) {
      switch (scope) {
        case none :
          break;
        case request :
          if (isOASMarked(xo, varNameInXyna, true)) {
            continue;
          }
          break;
        case response :
          if (isOASMarked(xo, varNameInXyna, false)) {
            continue;
          }
          break;
      }
      String varName = varNameInXyna;
      String newPath = currentPath.isEmpty() ? varNameInXyna : currentPath + "." + varNameInXyna; 
      if (substitutions.containsKey(newPath)) {
        varName = substitutions.get(newPath);
      }
      try {
        Object val = xo.get(varNameInXyna);
        JSONValue value = new JSONValue();
        if (val == null) {
          if (includeNulls.get()) {
            value.unversionedSetType(JSONVALTYPES.NULL);
          } else {
            continue;
          }
        } else if (val instanceof XynaObject) {
          value.unversionedSetType(JSONVALTYPES.OBJECT);
          JSONObject obj = createFromXynaObjectRecursivly((XynaObject) val, newPath, transformations, substitutions, useLabels, scope);
          value.unversionedSetObjectValue(obj);
        } else if (val instanceof String) {
          value.unversionedSetType(JSONVALTYPES.STRING);
          value.unversionedSetStringOrNumberValue((String) val);
        } else if (val instanceof Boolean) {
          value.unversionedSetType(JSONVALTYPES.BOOLEAN);
          value.unversionedSetBooleanValue((Boolean) val);
        } else if (val instanceof List) {
          if (transformations.containsKey(newPath)) {
            String keyName = transformations.get(newPath);
            value.unversionedSetType(JSONVALTYPES.OBJECT);
            JSONObject map = new JSONObject();
            List<JSONKeyValue> list = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<? extends XynaObject> l = (List<? extends XynaObject>) val;
            for (XynaObject xoe: l) {
              JSONValue childValue = new JSONValue();
              JSONObject childJob = createFromXynaObjectRecursivly(xoe, newPath+"[]", transformations, substitutions, useLabels, scope);
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
                JSONObject childJob = createFromXynaObjectRecursivly((XynaObject) o, newPath+"[]", transformations, substitutions, useLabels, scope);
                jval.unversionedSetObjectValue(childJob);
                jval.unversionedSetType(JSONVALTYPES.OBJECT);
              } else if (o instanceof String) {
                jval.unversionedSetType(JSONVALTYPES.STRING);
                jval.unversionedSetStringOrNumberValue((String) o);
              } else if (o instanceof Boolean) {
                jval.unversionedSetType(JSONVALTYPES.BOOLEAN);
                jval.unversionedSetBooleanValue((Boolean) o);
              } else if (o instanceof Number) {
                jval.unversionedSetType(JSONVALTYPES.NUMBER);
                jval.unversionedSetStringOrNumberValue( o.toString());
              } else {
                logger.debug("Unsupported list element type: " + o);
              }
              arr.add(jval);
            }
            value.unversionedSetArrayValue(arr);
            value.unversionedSetType(JSONVALTYPES.ARRAY);
          }
        } else if (val instanceof Number) {
          value.unversionedSetType(JSONVALTYPES.NUMBER);
          value.unversionedSetStringOrNumberValue(val.toString());
        } else {
          logger.debug("Unsupported parameter type: " + val);
          continue;
        }
        members.add(new JSONKeyValue(varName, value));
      } catch (InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
    }
    job.unversionedSetMembers(members);
    return job;
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


  @SuppressWarnings("unchecked")
  @Override
  public List<GeneralXynaObject> parseListFromJSON(Document document, GeneralXynaObject xo) {
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(), false, null);
  }


  public Document writeJSONList(List<GeneralXynaObject> list) {
    return writeJSONList(list, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(), false, OASScope.none);
  }

  private Document writeJSONList(List<? extends GeneralXynaObject> list, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels, OASScope scope) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    if (list == null || list.isEmpty()) {
      d.setText("[]");
    } else {
      StringBuilder sb = new StringBuilder("[\n");
      int cnt = 0;
      for (GeneralXynaObject xo : list) {
        sb.append("  ");
        JSONObject job = createFromXynaObject(xo, transformations, substitutions, useLabels, scope);
        sb.append(JSONObjectWriter.toJSON("  ", job));
        if (++cnt < list.size()) {
          sb.append(",");
        }
        sb.append("\n");
      }
      sb.append("]");
      d.setText(sb.toString());
    }
    return d;
  }


  public List<? extends GeneralXynaObject> parseListFromJSONWithOptions(Document document, GeneralXynaObject xo,
                                                                    List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels, XynaObjectDecider decider) {
    
    String json = document.getText();
    if (json == null || json.isBlank()) {
      return new ArrayList<GeneralXynaObject>();
    }
    JSONTokenizer jt = new JSONTokenizer();
    List<JSONToken> tokens = jt.tokenize(json);
    JSONParser jp = new JSONParser(json);
    List<JSONValue> arr = new ArrayList<JSONValue>();
    jp.fillArray(tokens, 0, arr);

    Map<String, String> mapTransformations = new HashMap<String, String>();
    for (ListToMapTransformation listToMapTransformation : transformations) {
      mapTransformations.put(listToMapTransformation.getPathToList(), listToMapTransformation.getKeyName());
    }
    Map<String, String> mapSubstitutions = new HashMap<String, String>();
    for (MemberSubstitution memberSubstitution : substitutions) {
      mapSubstitutions.put(memberSubstitution.getJsonName(), memberSubstitution.getPathToMemberInDataType());
    }
    
    return createList(xo.getClass(), arr, "", mapTransformations, mapSubstitutions, useLabels, decider);
  }
  
  
  @SuppressWarnings("unchecked")
  @Override
  public List<GeneralXynaObject> parseListFromJSONWithOptions(Document document, GeneralXynaObject xo,
                                                                    JSONParsingOptions jSONParsingOptions) {
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, jSONParsingOptions.getListToMapTransformation(), jSONParsingOptions.getMemberSubstitution(), jSONParsingOptions.getUseLabels(), jSONParsingOptions.getObjectDecider());
  }

  @Override
  public GeneralXynaObject parseObjectFromJSONWithOptions(Document document, GeneralXynaObject jSONBaseModel, JSONParsingOptions jSONParsingOptions) {
    return parseObjectFromJSON(document, jSONBaseModel, jSONParsingOptions.getListToMapTransformation(), jSONParsingOptions.getMemberSubstitution(), jSONParsingOptions.getUseLabels(), jSONParsingOptions.getObjectDecider());
  }

  @Override
  public Document writeJSONListWithOptions(List<GeneralXynaObject> jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    return writeJSONList(jSONBaseModel, jSONWritingOptions.getListToMapTransformation(), jSONWritingOptions.getMemberSubstitution(), jSONWritingOptions.getUseLabels(), OASScope.valueOfOrNone(jSONWritingOptions.getOASMessageType()));
  }


  @Override
  public Document writeJSONWithOptions(GeneralXynaObject jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    return writeJSON(jSONBaseModel, jSONWritingOptions.getListToMapTransformation(), jSONWritingOptions.getMemberSubstitution(), jSONWritingOptions.getUseLabels(), OASScope.valueOfOrNone(jSONWritingOptions.getOASMessageType()));
  }



}
