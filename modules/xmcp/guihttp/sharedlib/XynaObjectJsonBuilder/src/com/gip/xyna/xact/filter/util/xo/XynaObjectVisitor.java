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
package com.gip.xyna.xact.filter.util.xo;



import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


/*
 * "stringvar" : "asd"
 * "numbervar" : 123
 * "stringnull" : null
 * "numbernull" : null
 * "booleanvar" : false
 * "booleannull" : null
 * "complex" : { 
 *        "$meta" : {
 *              "fqn" : "bla.blubb"
 *              "rdc" : {"workspace" :"asd"}
 *                  }
 *        "complexmember" : "asd"
 *             }
 * "complexlist" : { //complexListWrapper
 *        "$meta" : {
 *              "fqn" : "bla.blubb"
 *                  }
 *        "$list" : [
 *              {...} , {...}
 *                  ]
 *             }
 * "primitivelist" : [
 *            true, true, null, false, -1.234e12
 *                   ]
 * 
 */
public class XynaObjectVisitor extends EmptyJsonVisitor<GeneralXynaObject> {

  public final static String META_TAG = "$meta";
  public final static String WRAPPED_LIST_TAG = "$list";
  public final static String OBJECT_TAG = "$object";
  public final static String PRIMITIVE_TAG = "$primitive";
  public final static String META_LABEL_TAG = "$label";
  public final static String META_METHOD_TAG = "$method";
  public final static String LABEL_TAG = "label";
  public final static String DOCU_TAG = "docu";
  public final static String DOLLAR_DOCU_TAG = "$docu";
  public final static String IS_ABSTRACT_TAG = "abstract";
  public static final String OUTPUTS_LIST_TAG = "returns";
  public static final String INPUTS_LIST_TAG = "params";
  

  private MetaInfo info;
  private GeneralXynaObject object;
  private boolean isComplexListWrapper = false;



  public XynaObjectVisitor() {
  }

  //subobject oder komplexes listenelement (und dann ist das label das label der liste)
  public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
    if (label.equals(META_TAG)) {
      return MetaInfo.getJsonVisitor();
    } else if (label.equals(WRAPPED_LIST_TAG)) {
      this.isComplexListWrapper = true; //wir befinden uns in dem complexlist-fall in dem objekt mit $meta und $list
      return new XynaObjectVisitor(); //ein komplexes listenelement
    } else {
      return new XynaObjectVisitor(); //komplexe membervariable oder das künstliche json-objekt für die complexlist (welches $meta und $list enthält)
    }
  }


  @Override
  public void attribute(String label, String value, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
      throws UnexpectedJSONContentException {
    adjustValue(getObject(), label, value);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void list(String label, List<String> values, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
      throws UnexpectedJSONContentException {
    //primitive liste
    Field field = findField(getObject().getClass(), label);
    if (field != null) {
      field.setAccessible(true);
      try {
        Class<?> listElType = XynaObjectJsonBuilder.getGenericTypeOfList(field);
        //FIXME eigtl müsste man den typ über generationbase herausbekommen. hier kann man boxed/unboxed listen nicht unterscheiden
        PrimitiveType primitiveType = PrimitiveType.createOrNull(listElType.getSimpleName());
        if (primitiveType != null) {
          List typedList = new ArrayList();
          for (String value : values) {
            if (value == null) {
              if (primitiveType.isObject()) {
                typedList.add(null);
              } else {
                if (primitiveType == PrimitiveType.BOOLEAN) {
                  typedList.add(false);
                } else { //zahl
                  typedList.add(primitiveType.fromString("0"));
                }
              }
            } else {
              typedList.add(primitiveType.fromString(value));
            }
          }
          try {
            getObject().set(label, typedList);
          } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
            throw new UnexpectedJSONContentException("Unexpected attribute: " + label);
          }
        } else {
          throw new UnexpectedJSONContentException("Unknown Type: " + label);
        }
      } finally {
        field.setAccessible(false);
      }
    } else {
      throw new UnexpectedJSONContentException("Unexpected attribute: " + label);
    }
  }


  private void adjustValue(GeneralXynaObject gxo, String label, String value) throws UnexpectedJSONContentException {
    Field field = findField(gxo.getClass(), label);
    if (field != null) {
      field.setAccessible(true);
      try {
        PrimitiveType primitiveType = PrimitiveType.createOrNull(field.getType().getSimpleName());
        if (primitiveType != null) {
          gxo.set(label, primitiveType.fromString(value));
        } else {
          throw new UnexpectedJSONContentException("Unexpected attribute: " + label);
        }
      } catch (NumberFormatException e) {
        throw new UnexpectedJSONContentException(label, new RuntimeException("Field <" + label + "> has invalid value <" + value + ">.", e));
      } catch (IllegalArgumentException | XDEV_PARAMETER_NAME_NOT_FOUND e) {
        throw new RuntimeException(e);        
      } finally {
        field.setAccessible(false);
      }
    }
  }

  protected Field oFindField(Class<? extends GeneralXynaObject> clazz, String label) {
    return findField(clazz, label);
  }


  public static Field findField(Class<? extends GeneralXynaObject> clazz, String label) {
    try {
      return (Field) clazz.getDeclaredMethod("getField", String.class).invoke(clazz, label);
    } catch (Exception e) {
      return null;
    }
  }


  public void object(String label, Object value) throws UnexpectedJSONContentException {
    if (label.equals(META_TAG)) {
      info = (MetaInfo) value;
    } else {
      try {
        getObject().set(label, value);
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
    }
  }


  private static ClassLoaderType deriveClassLoader(XMOMType type) {
    switch (type) {
      case DATATYPE :
        return ClassLoaderType.MDM;
      case EXCEPTION :
        return ClassLoaderType.Exception;
      default :
        return null;
    }
  }


  private GeneralXynaObject getObject() {
    if (object == null) {
      if (info == null) {
        throw new RuntimeException("MetaInfo not present!");
      }
      Class<?> clazz = null;
      try {
        clazz = deriveClassFromInfo();
        object = (GeneralXynaObject) clazz.newInstance();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | Ex_FileAccessException | XPRC_XmlParsingException | ClassNotFoundException
          | InstantiationException | IllegalAccessException | XPRC_InvalidPackageNameException e) {
        
        try{
          java.lang.reflect.Constructor c = getConstructor(clazz);
          c.setAccessible(true);
          object = (GeneralXynaObject)c.newInstance();
        }
        catch(Exception e2){
          throw new RuntimeException(e);
        }
      }
    }
    return object;
  }
  
  private java.lang.reflect.Constructor getConstructor(Class<?> clazz){
    java.lang.reflect.Constructor[] candidates = clazz.getDeclaredConstructors();
    
    for(int i=0; i<candidates.length; i++){
      if(candidates[i].getParameterCount() == 0)
        return candidates[i];
    }
    return null;
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
    //liste der complextypes
    if (isComplexListWrapper) {
      object = new XynaObjectList(values, info.getFqName());
    } else {
      //TODO was ist das für ein fall?
      try {
        Field field = oFindField(getObject().getClass(), label);
        if (field != null) {
          getObject().set(label, new ArrayList(values));
        }
      } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
        throw new RuntimeException(e);
      }
    }
  }


  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void emptyList(String label) throws UnexpectedJSONContentException {
    /*
     * listen leer initialisieren
     * falls complexListWrapper, ist das boolean-flag leider nicht auf true gesetzt, weil dazu hätte man einem listenelement begegnen müssen (gibt aber ja dann keine)
     * => man muss jetzt herausfinden, ob es sich um eine complexe oder eine simple list handelt.
     * 
     * fall1: complexe liste
     *   dann zeigt metainfo auf den typ der listenelemente
     *   label = $list
     * fall2: simple liste
     *   dann zeigt metainfo auf den typ des complexen typs, der die simple liste enthält
     *   label = <membervarname der simple-list>
     */
    if (WRAPPED_LIST_TAG.equals(label)) {
      isComplexListWrapper = true;
      objectList(label, new ArrayList());
    } else {
      list(label, new ArrayList<String>(), null);
    }
  }


  public GeneralXynaObject get() {
    return getObject();
  }


  public GeneralXynaObject getAndReset() {
    return getObject();
  }
  
  private Class<?> deriveClassFromInfo() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, ClassNotFoundException {
    String fqClassName = GenerationBase.transformNameForJava(info.getFqName());
    if (GenerationBase.isReservedServerObjectByFqClassName(fqClassName)) {
      return GenerationBase.getReservedClass(info.getFqName());
    }
    Long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
        .getRevision(info.getRuntimeContext());
    revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getRevisionDefiningXMOMObjectOrParent(info.getFqName(), revision);
    XMOMType type = XMOMType.getXMOMTypeByRootTag(GenerationBase.retrieveRootTag(info.getFqName(), revision));
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    ClassLoaderBase cl = cld.getClassLoaderByType(deriveClassLoader(type), fqClassName, revision);
    if (cl == null) {
      throw new RuntimeException("No classloader found for " + info.getFqName() + " in " + info.getRuntimeContext().getGUIRepresentation());
    } else {
      return cl.loadClass(fqClassName);
    }
  }

}