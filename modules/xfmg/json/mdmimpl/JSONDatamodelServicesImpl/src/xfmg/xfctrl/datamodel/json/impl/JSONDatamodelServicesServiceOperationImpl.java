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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import xact.templates.Document;
import xact.templates.JSON;
import xfmg.xfctrl.datamodel.json.JSONDatamodelServicesServiceOperation;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONObject;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValue;
import xfmg.xfctrl.datamodel.json.impl.JSONParser.JSONValueType;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONTokenType;
import xfmg.xfctrl.datamodel.json.parameter.JSONParsingOptions;
import xfmg.xfctrl.datamodel.json.parameter.JSONWritingOptions;
import xfmg.xfctrl.datamodel.json.parameter.ListToMapTransformation;
import xfmg.xfctrl.datamodel.json.parameter.MemberSubstitution;



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

  
  public GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject jSONBaseModel) {
    return parseObjectFromJSON(document, jSONBaseModel, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(),false);
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
    value.stringOrNumberValue = doc.getText();
    value.type = JSONValueType.STRING;
    String jsonString = value.toJSON("");
    return new Document(doc.getDocumentType(), jsonString.substring(1, jsonString.length() - 1));
  }

  
  public GeneralXynaObject parseObjectFromJSON(Document document, GeneralXynaObject jSONBaseModel, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels) {
    String json = document.getText();
    if (json == null) {
      return null;
    }
    JSONTokenizer jt = new JSONTokenizer();
    List<JSONToken> tokens = jt.tokenize(json);
    JSONParser jp = new JSONParser(json);
    JSONObject job = new JSONObject();
    jp.fillObject(tokens, 0, job);

    fillXynaObject(jSONBaseModel, job, transformations, substitutions, useLabels);
    return jSONBaseModel;
  }


  /*
   * TODO zuordnungsregeln erweitern:
   * - labels ber�cksichtigen (mit und ohne leerzeichen)
   * - gro�/kleinschreibung konfigurierbar
   * - benutzer kann selbst ein mapping �bergeben
   * - parametrisierbarkeit dieser konfigurationsm�glichkeiten 
   */
  //TODO reflection cachen
  public void fillXynaObject(GeneralXynaObject xo, JSONObject job, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels) {
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
    fillXynaObjectRecursivly(xo, job, "", mapTransformations, mapSubstitutions, useLabels);
  }
  
  public void fillXynaObjectRecursivly(GeneralXynaObject xo, JSONObject job, String currentPath, Map<String, String> transformations, Map<String, String> substitutions, boolean useLabels) {
    if (xo == null) {
      return;
    }
    Map<String,String> varNamesOfXynaObject = getVarNames(xo, useLabels);
    for (Entry<String, JSONValue> e : job.objects.entrySet()) {
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
      Class currentClass = xo.getClass();
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
        switch (value.type) {
          case BOOLEAN :
            if (typeOfField == boolean.class || typeOfField == Boolean.class) {
              xo.set(varNameInXyna, value.booleanValue);
            } else if (typeOfField == String.class) {
              xo.set(varNameInXyna, String.valueOf(value.booleanValue));
            } else {
              logger.debug("parameter " + varName + " has type " + typeOfField + " in " + xo + ", but is of type boolean in JSON.");
            }
            break;
          case NULL :
            if (typeOfField.isPrimitive()) {
              logger.debug("parameter " + varName + " is of primitive type (" + typeOfField + "), but null in JSON");
            } else {
              xo.set(varNameInXyna, null);
            }
            break;
          case STRING :
            if (typeOfField == Boolean.class || typeOfField == boolean.class) {
              if (value.stringOrNumberValue.equalsIgnoreCase("true")) {
                xo.set(varNameInXyna, true);
              } else if (value.stringOrNumberValue.equalsIgnoreCase("false")) {
                xo.set(varNameInXyna, false);
              } else {
                logger.debug("parameter " + varName + " can not be converted to field of type " + typeOfField + " in " + xo);
                continue;
              }
            }
          case NUMBER :
            if (typeOfField == String.class) {
              xo.set(varNameInXyna, value.stringOrNumberValue);
            } else if (Number.class.isAssignableFrom(typeOfField) || typeOfField.isPrimitive()) {
              double d;
              try {
                d = Double.parseDouble(value.stringOrNumberValue);
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
          case ARRAY :
            //TODO support f�r vorinstanziierte listenelemente
            if (typeOfField == List.class) {
              if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                Type genType = pt.getActualTypeArguments()[0];
                if (genType instanceof Class) {
                  Class<?> genTypeClass = (Class<?>) genType;
                  if (XynaObject.class.isAssignableFrom(genTypeClass)) {
                    List<?> l = createList((Class<? extends XynaObject>) genTypeClass, value.arrayValue, newPath, transformations, substitutions, useLabels);
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == String.class) {
                    List<String> l = new ArrayList<String>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING || jv.type == JSONValueType.NUMBER) {
                        l.add(jv.stringOrNumberValue);
                      } else if (jv.type == JSONValueType.BOOLEAN) {
                        l.add(String.valueOf(jv.booleanValue));
                      } else if (jv.type == JSONValueType.NULL) {
                        l.add(null);
                      } else {
                        logger.debug("array element " + jv + " is not of string-compatible type, but parameter " + varName
                            + " contains a list of " + genType);
                      }
                    }
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == Boolean.class) {
                    List<Boolean> l = new ArrayList<Boolean>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING) {
                        l.add(Boolean.valueOf(jv.stringOrNumberValue));
                      } else if (jv.type == JSONValueType.BOOLEAN) {
                        l.add(jv.booleanValue);
                      } else if (jv.type == JSONValueType.NULL) {
                        l.add(null);
                      } else {
                        logger.debug("array element " + jv + " is not of boolean-compatible type, but parameter " + varName
                            + " contains a list of " + genType);
                      }
                    }
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == Integer.class) {
                    List<Integer> l = new ArrayList<Integer>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING || jv.type == JSONValueType.NUMBER) {
                        try {
                          l.add(Integer.valueOf(jv.stringOrNumberValue));
                        } catch (NumberFormatException ex) {
                          logger.warn("Skipped array element " + jv.stringOrNumberValue + ", because it is not a " + genTypeClass
                              + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                        }
                      } else if (jv.type == JSONValueType.NULL) {
                        l.add(null);
                      } else {
                        logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                            + " contains a list of " + genType);
                      }
                    }
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == Long.class) {
                    List<Long> l = new ArrayList<Long>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING || jv.type == JSONValueType.NUMBER) {
                        try {
                          l.add(Long.valueOf(jv.stringOrNumberValue));
                        } catch (NumberFormatException ex) {
                          logger.warn("Skipped array element " + jv.stringOrNumberValue + ", because it is not a " + genTypeClass
                              + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                        }
                      } else if (jv.type == JSONValueType.NULL) {
                        l.add(null);
                      } else {
                        logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                            + " contains a list of " + genType);
                      }
                    }
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == Double.class) {
                    List<Double> l = new ArrayList<Double>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING || jv.type == JSONValueType.NUMBER) {
                        try {
                          l.add(Double.valueOf(jv.stringOrNumberValue));
                        } catch (NumberFormatException ex) {
                          logger.warn("Skipped array element " + jv.stringOrNumberValue + ", because it is not a " + genTypeClass
                              + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                        }
                      } else if (jv.type == JSONValueType.NULL) {
                        l.add(null);
                      } else {
                        logger.debug("array element " + jv + " is not of number-compatible type, but parameter " + varName
                            + " contains a list of " + genType);
                      }
                    }
                    xo.set(varNameInXyna, l);
                  } else if (genTypeClass == Float.class) {
                    List<Float> l = new ArrayList<Float>();
                    for (JSONValue jv : value.arrayValue) {
                      if (jv.type == JSONValueType.STRING || jv.type == JSONValueType.NUMBER) {
                        try {
                          l.add(Float.valueOf(jv.stringOrNumberValue));
                        } catch (NumberFormatException ex) {
                          logger.warn("Skipped array element " + jv.stringOrNumberValue + ", because it is not a " + genTypeClass
                              + ", that is expected for parameter " + varNameInXyna + " in " + xo);
                        }
                      } else if (jv.type == JSONValueType.NULL) {
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
          case OBJECT :
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
                  list.addAll(value.objectValue.objects.values());
                  List<?> l = createList((Class<? extends XynaObject>) genTypeClass, list, newPath, transformations, substitutions, useLabels);
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
                try {
                  o = typeOfField.getConstructor().newInstance();
                  xo.set(varNameInXyna, o);
                } catch (InstantiationException e1) {
                  throw new RuntimeException("Could not instantiate " + typeOfField.getName(), e1);
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
              if (o instanceof XynaObject) {
                fillXynaObjectRecursivly((XynaObject) o, value.objectValue, newPath, transformations, substitutions, useLabels);
              } else {
                logger.debug("skipping member " + varName + " in " + xo + ", because it is not of complex type");
              }
            }
            break;
          default :
            throw new RuntimeException("unexpected type : " + value.type);
        }
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND ex) {
        logger.debug("parameter " + varName + " not found in " + xo);
        continue;
      }
    }
  }


  private <A extends GeneralXynaObject> List<A> createList(Class<A> genTypeClass, List<JSONValue> array, String currentPath, Map<String, String> mapTransformations, Map<String, String> substitutions, boolean useLabels) {
    if (Modifier.isAbstract(genTypeClass.getModifiers())) {
      throw new RuntimeException("Can not instantiate list elements of abstract type " + genTypeClass + ".");
    }
    List<A> l = new ArrayList<A>();
    
    String newPath = currentPath.isEmpty() ? currentPath : currentPath+"[]";
    
    for (JSONValue jv : array) {
      if (jv.type == JSONValueType.OBJECT) {
        A listElement;
        try {
          listElement = (A) genTypeClass.getConstructor().newInstance();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        fillXynaObjectRecursivly(listElement, jv.objectValue, newPath, mapTransformations, substitutions, useLabels);
        l.add(listElement);
      } else {
        logger.debug("array element " + jv + " is not of object type");
      }
    }
    return l;
  }
  
  
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
    return writeJSON(jSONBaseModel, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(),false);
  }

  public Document writeJSON(GeneralXynaObject jSONBaseModel, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    JSONObject job = createFromXynaObject(jSONBaseModel, transformations, substitutions, useLabels);
    if (job != null) {
      d.setText(job.toJSON(""));
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
                                          List<? extends MemberSubstitution> substitutions, boolean useLabels) {
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
    return createFromXynaObjectRecursivly(xo, "", mapTransformations, mapSubstitutions, useLabels);
  }
  
  public JSONObject createFromXynaObjectRecursivly(GeneralXynaObject xo, String currentPath, Map<String, String> transformations, Map<String, String> substitutions, boolean useLabels) {
    if (xo == null) {
      return null;
    }
    JSONObject job = new JSONObject();
    HashMap<String,String> varNamesOfXynaObject = getVarNames(xo, useLabels);
    for (String varNameInXyna : varNamesOfXynaObject.keySet()) {
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
            value.type = JSONValueType.NULL;
          } else {
            continue;
          }
        } else if (val instanceof XynaObject) {
          value.type = JSONValueType.OBJECT;
          value.objectValue = createFromXynaObjectRecursivly((XynaObject) val, newPath, transformations, substitutions, useLabels);
        } else if (val instanceof String) {
          value.type = JSONValueType.STRING;
          value.stringOrNumberValue = (String) val;
        } else if (val instanceof Boolean) {
          value.type = JSONValueType.BOOLEAN;
          value.booleanValue = (Boolean) val;
        } else if (val instanceof List) {
          if (transformations.containsKey(newPath)) {
            String keyName = transformations.get(newPath);
            value.type = JSONValueType.OBJECT;
            JSONObject map = new JSONObject();
            List<? extends XynaObject> l = (List<? extends XynaObject>) val;
            for (XynaObject xoe: l) {
              JSONValue childValue = new JSONValue();
              JSONObject childJob = createFromXynaObjectRecursivly(xoe, newPath+"[]", transformations, substitutions, useLabels);
              childValue.objectValue = childJob;
              childValue.type = JSONValueType.OBJECT;
              map.objects.put(xoe.get(keyName).toString(), childValue);
            }
            value.objectValue = map;
          } else {
            List<JSONValue> arr = new ArrayList<JSONValue>();
            List<?> l = (List<?>) val;
            for (Object o : l) {
              JSONValue jval = new JSONValue();
              if (o == null) {
                jval.type = JSONValueType.NULL;
              } else if (o instanceof XynaObject) {
                JSONObject childJob = createFromXynaObjectRecursivly((XynaObject) o, newPath+"[]", transformations, substitutions, useLabels);
                jval.objectValue = childJob;
                jval.type = JSONValueType.OBJECT;
              } else if (o instanceof String) {
                jval.type = JSONValueType.STRING;
                jval.stringOrNumberValue = (String) o;
              } else if (o instanceof Boolean) {
                jval.type = JSONValueType.BOOLEAN;
                jval.booleanValue = (Boolean) o;
              } else if (o instanceof Number) {
                jval.type = JSONValueType.NUMBER;
                jval.stringOrNumberValue = o.toString();
              } else {
                logger.debug("Unsupported list element type: " + o);
              }
              arr.add(jval);
            }
            value.arrayValue = arr;
            value.type = JSONValueType.ARRAY;
          }
        } else if (val instanceof Number) {
          value.type = JSONValueType.NUMBER;
          value.stringOrNumberValue = val.toString();
        } else {
          logger.debug("Unsupported parameter type: " + val);
          continue;
        }
        job.objects.put(varNamesOfXynaObject.get(varName), value);
      } catch (InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
    }
    return job;
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<GeneralXynaObject> parseListFromJSON(Document document, GeneralXynaObject xo) {
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(), false);
  }


  public Document writeJSONList(List<GeneralXynaObject> list) {
    return writeJSONList(list, Collections.<ListToMapTransformation>emptyList(), Collections.<MemberSubstitution>emptyList(),false);
  }

  private Document writeJSONList(List<? extends GeneralXynaObject> list, List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels) {
    Document d = new Document();
    d.setDocumentType(new JSON());
    if (list == null || list.isEmpty()) {
      d.setText("[]");
    } else {
      StringBuilder sb = new StringBuilder("[\n");
      int cnt = 0;
      for (GeneralXynaObject xo : list) {
        sb.append("  ");
        JSONObject job = createFromXynaObject(xo, transformations, substitutions, useLabels);
        sb.append(job.toJSON("  "));
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
                                                                    List<? extends ListToMapTransformation> transformations, List<? extends MemberSubstitution> substitutions, boolean useLabels) {
    
    String json = document.getText();
    if (json == null) {
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
    
    return createList(xo.getClass(), arr, "", mapTransformations, mapSubstitutions, useLabels);
  }
  
  
  public List<GeneralXynaObject> parseListFromJSONWithOptions(Document document, GeneralXynaObject xo,
                                                                    JSONParsingOptions jSONParsingOptions) {
    return (List<GeneralXynaObject>) parseListFromJSONWithOptions(document, xo, jSONParsingOptions.getListToMapTransformation(), jSONParsingOptions.getMemberSubstitution(), jSONParsingOptions.getUseLabels());
  }


  public GeneralXynaObject parseObjectFromJSONWithOptions(Document document, GeneralXynaObject jSONBaseModel, JSONParsingOptions jSONParsingOptions) {
    return parseObjectFromJSON(document, jSONBaseModel, jSONParsingOptions.getListToMapTransformation(), jSONParsingOptions.getMemberSubstitution(), jSONParsingOptions.getUseLabels());
  }

  @Override
  public Document writeJSONListWithOptions(List<GeneralXynaObject> jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    return writeJSONList(jSONBaseModel, jSONWritingOptions.getListToMapTransformation(), jSONWritingOptions.getMemberSubstitution(), jSONWritingOptions.getUseLabels());
  }


  public Document writeJSONWithOptions(GeneralXynaObject jSONBaseModel, JSONWritingOptions jSONWritingOptions) {
    return writeJSON(jSONBaseModel, jSONWritingOptions.getListToMapTransformation(), jSONWritingOptions.getMemberSubstitution(), jSONWritingOptions.getUseLabels());
  }



}
