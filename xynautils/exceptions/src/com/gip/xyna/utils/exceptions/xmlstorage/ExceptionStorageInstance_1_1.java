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
package com.gip.xyna.utils.exceptions.xmlstorage;



import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.exceptions.ExceptionStorage;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer;
import com.gip.xyna.utils.exceptions.utils.codegen.FqClassNameAdapter;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidClassNameException;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaClass;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaGenUtils;



public class ExceptionStorageInstance_1_1 extends ExceptionStorageInstance {

  private static final String FIELD_MAP_NAME = "fieldMap";
  private static final String FIELD_GETTER_METHOD_NAME = "getField";
  
  private List<ExceptionStorageInstance> importedStorages = new ArrayList<ExceptionStorageInstance>();  
  private FqClassNameAdapter fqClassNameAdapter = new FqClassNameAdapter();
  
  
  @Override
  public JavaClass[] generateJavaClasses(boolean loadFromResource, ExceptionEntryProvider provider, String xmlFile)
                  throws InvalidValuesInXMLException {
    try {
      ArrayList<JavaClass> list = new ArrayList<JavaClass>();
      for (ExceptionEntry entry : getEntries()) {
        if (!(entry instanceof ExceptionEntry_1_1)) {
          throw new InvalidValuesInXMLException(xmlFile, "found incompatible Exception Entry: " + entry.getCode());
        }
      }
      for (ExceptionEntry entryi : getEntries()) {
        ExceptionEntry_1_1 entry = (ExceptionEntry_1_1) entryi;
        List<ExceptionParameter> superClassParameter = collectParameterOfSuperClasses(entry, provider);

        String baseClassName = XynaException.class.getName();
        if (entry.getBaseExceptionName() != null) {
          baseClassName = entry.getBaseExceptionPath() + "." + entry.getBaseExceptionName();
        }

        JavaClass jc = new JavaClass(entry.getPath(), entry.getName());
        jc.setFqClassNameAdapter(fqClassNameAdapter);
        
        jc.addImport(List.class.getName());
        
        baseClassName = jc.setSuperClass(baseClassName);
        if (entry.isAbstract()) {
          jc.setIsAbstract(true);
        }


     /*   static {
          ExceptionStorage.loadFromResource("Exceptions.xml", HTTPTrigger.class.getClassLoader());
        }*/
        if (loadFromResource) {
          String exstorageClassName = jc.addImport(ExceptionStorage.class.getName());
          CodeBuffer staticinit = new CodeBuffer("Utils");
          staticinit.addLine(exstorageClassName + ".loadFromResource(\"" + getXmlFile() + "\", " //TODO konfigurierbar new File(getXmlFile()).getName()
                          + jc.getSimpleClassName() + ".class.getClassLoader())");
          jc.addStaticInitBlock(staticinit);
        }
        
        //constructor mit parametern: parameter lokal speichern, tostring nach unten weitergeben.
        CodeBuffer constructorAllParasNoException = new CodeBuffer("Utils");
        CodeBuffer constructorAllParasWithException = new CodeBuffer("Utils");
     
        constructorAllParasNoException.add("public " + jc.getSimpleClassName() + "(");
        constructorAllParasWithException.add("public " + jc.getSimpleClassName() + "(");

        for (ExceptionParameter para : superClassParameter) {
          String paraTypeClassName = null;
          if (para.isReference()) {
            paraTypeClassName = jc.addImport(para.getTypePath(), para.getTypeName());
          } else {
            paraTypeClassName = para.getJavaType();
          }
          if (para.isList()) {
            String listClassName = jc.addImport(List.class.getName());
            paraTypeClassName = createListType(listClassName, paraTypeClassName);
          }
          constructorAllParasNoException.addListElement(paraTypeClassName + " " + para.getVarName());
          constructorAllParasWithException.addListElement(paraTypeClassName + " " + para.getVarName());
          
        }

        for (ExceptionParameter para : entry.getParameter()) {
          String paraTypeClassName = null;
          if (para.isReference()) {
            paraTypeClassName = jc.addImport(para.getTypePath(), para.getTypeName());
          } else {
            paraTypeClassName = para.getJavaType();
          }
          if (para.isList()) {
            String listClassName = jc.addImport(List.class.getName());
            paraTypeClassName = createListType(listClassName, paraTypeClassName);
          }
          constructorAllParasNoException.addListElement(paraTypeClassName + " " + para.getVarName());
          constructorAllParasWithException.addListElement(paraTypeClassName + " " + para.getVarName());
          jc.addMemberVar("private " + paraTypeClassName + " " + para.getVarName());

          if (!skipGettersAndSetters) {
            // setter methoden
            CodeBuffer setter = new CodeBuffer("Utils");
            setter.addLine("public void " + JavaGenUtils.getSetterFor(para.getVarName()) + "(" + paraTypeClassName + " "
                + para.getVarName() + ") {");
            setter.addLine("this." + para.getVarName() + " = " + para.getVarName());
            setter.addLine("}");
            jc.addMethod(setter);

            //getter methoden
            CodeBuffer getter = new CodeBuffer("Utils");
            getter.addLine("public " + paraTypeClassName + " " + JavaGenUtils.getGetterFor(para.getVarName()) + "() {");
            getter.addLine("return " + para.getVarName());
            getter.addLine("}");
            jc.addMethod(getter);
          }
        }


        int nParameter = superClassParameter.size() + entry.getParameter().size();
        if (nParameter > 0) {
          CodeBuffer privateEmptyConstructor = new CodeBuffer("Utils");
          privateEmptyConstructor.addLine("private ", jc.getSimpleClassName(), "() {");
          String paraString = "";
          for (int i = 0; i<nParameter; i++) {
            paraString += ", null";
          }
          privateEmptyConstructor.addLine("super(new String[] {\"", entry.getCode(), "\"", paraString, "})");
          privateEmptyConstructor.addLine("}");
          jc.addConstructor(privateEmptyConstructor);
        } //else: public konstruktor hat bereits keine parameter

        constructorAllParasWithException.addListElement("Throwable cause");
        
        constructorAllParasNoException.add(") {").addLB();
        constructorAllParasWithException.add(") {").addLB();
        constructorAllParasNoException.add("super(new String[]{");
        constructorAllParasWithException.add("super(new String[]{");
        CodeBuffer refreshArgsMethod = new CodeBuffer("Utils"); //args müssen nachträgliche änderungen nach dem konstruktor sinnvoll mitbekommen
        refreshArgsMethod.addLine("protected void refreshArgs() {");
        if (entry.getBaseExceptionName() != null) {
          refreshArgsMethod.addLine("super.refreshArgs()");
        }
        refreshArgsMethod.addLine("String[] args = getArgs()");
        
        if (!entry.isAbstract()) {
          constructorAllParasNoException.addListElement("\"" + entry.getCode() + "\"");
          constructorAllParasWithException.addListElement("\"" + entry.getCode() + "\"");
        }
        int paraCnt = 0;
        for (ExceptionParameter para : superClassParameter) {
          paraCnt++;
          constructorAllParasNoException.addListElement(para.getVarName() + " + \"\"");
          constructorAllParasWithException.addListElement(para.getVarName() + " + \"\"");
        }
        for (ExceptionParameter para : entry.getParameter()) {
          refreshArgsMethod.addLine("args[" + (paraCnt++), "] = ", para.getVarName(), " + \"\"");
          constructorAllParasNoException.addListElement(para.getVarName() + " + \"\"");
          constructorAllParasWithException.addListElement(para.getVarName() + " + \"\"");
        }
                
        refreshArgsMethod.addLine("}");
        jc.addMethod(refreshArgsMethod);

        constructorAllParasNoException.add("})").addLB();
        constructorAllParasWithException.add("}, cause)").addLB();
        for (ExceptionParameter para : superClassParameter) {
          constructorAllParasNoException.addLine(JavaGenUtils.getSetterFor(para.getVarName()) + "(" + para.getVarName() + ")");
          constructorAllParasWithException.addLine(JavaGenUtils.getSetterFor(para.getVarName()) + "(" + para.getVarName() + ")");
        }
        for (ExceptionParameter para : entry.getParameter()) {
          constructorAllParasNoException.addLine(JavaGenUtils.getSetterFor(para.getVarName()) + "(" + para.getVarName() + ")");
          constructorAllParasWithException.addLine(JavaGenUtils.getSetterFor(para.getVarName()) + "(" + para.getVarName() + ")");
        }
        constructorAllParasNoException.addLine("}");
        constructorAllParasWithException.addLine("}");

        List<ExceptionParameter> allParameters = new ArrayList<ExceptionParameter>();
        allParameters.addAll(superClassParameter);
        allParameters.addAll(entry.getParameter());

        int nParameterLength = calcLengthOfParameters(allParameters);

        if (nParameterLength < 255) {
          jc.addConstructor(constructorAllParasNoException);
        }
        if (nParameterLength < 254) {
          jc.addConstructor(constructorAllParasWithException);
        }


        //protected constructor mit allen parametern für vererbung
        CodeBuffer constructorOnlyStringsNoException = new CodeBuffer("Utils");
        CodeBuffer constructorOnlyStringsWithException = new CodeBuffer("Utils");
        constructorOnlyStringsNoException.addLine("protected " + jc.getSimpleClassName() + "(String[] args) {");
        constructorOnlyStringsWithException.addLine("protected " + jc.getSimpleClassName() + "(String[] args, Throwable cause) {");
        constructorOnlyStringsNoException.addLine("super(args)");
        constructorOnlyStringsWithException.addLine("super(args, cause)");
        constructorOnlyStringsNoException.addLine("}");
        constructorOnlyStringsWithException.addLine("}");
        jc.addConstructor(constructorOnlyStringsNoException);
        jc.addConstructor(constructorOnlyStringsWithException);

        CodeBuffer initCause = new CodeBuffer("Utils");
        initCause.addLine("public " + jc.getSimpleClassName() + " initCause(" + Throwable.class.getSimpleName()
                        + " t) {");
        initCause.addLine("return (" + jc.getSimpleClassName() + ") super.initCause(t)");
        initCause.addLine("}");

        jc.addMethod(initCause);
        
        /*
         * getField for typeresistant mappings
         */
        jc.addImport(Class.class.getName());
        jc.addImport(Field.class.getName());
        jc.addImport(ConcurrentMap.class.getName());
        jc.addImport(ConcurrentHashMap.class.getName());
        jc.addImport(NoSuchFieldException.class.getName());
        jc.addImport(IllegalArgumentException.class.getName());
        
        jc.addMemberVar("private static " + ConcurrentMap.class.getSimpleName() + "<" + String.class.getSimpleName() + ", " + Field.class.getSimpleName() + "> " + FIELD_MAP_NAME + 
                          " = new " + ConcurrentHashMap.class.getSimpleName() + "()");
        
        CodeBuffer getFieldMethod = new CodeBuffer("Utils");
        generateJavaFieldCache(getFieldMethod, entry);
        jc.addMethod(getFieldMethod);
        
        list.add(jc);
      }

      return list.toArray(new JavaClass[0]);
    } catch (InvalidClassNameException e) {
      throw new InvalidValuesInXMLException(xmlFile, e.getMessage()).initCause(e);
    }
  }


  private String createListType(String listClassName, String paraTypeClassName) {
    String paraTypeClassNameAsObject = paraTypeClassName;
    if (paraTypeClassName.equals("int")) {
      paraTypeClassNameAsObject = "Integer";
    } else if (paraTypeClassName.equals("long")) {
      paraTypeClassNameAsObject = "Long";
    } else if (paraTypeClassName.equals("double")) {
      paraTypeClassNameAsObject = "Double";
    } else if (paraTypeClassName.equals("boolean")) {
      paraTypeClassNameAsObject = "Boolean";
    } else if (paraTypeClassName.equals("float")) {
      paraTypeClassNameAsObject = "Float";
    } 
    return listClassName + "<" + paraTypeClassNameAsObject + ">";
  }


  /**
   * reihenfolge = root-klasse parameter zuerst, kinder-klassen parameter danach
   * @param entry
   * @param provider
   * @return
   * @throws InvalidValuesInXMLException
   */
  private List<ExceptionParameter> collectParameterOfSuperClasses(ExceptionEntry_1_1 entry,
                                                                  ExceptionEntryProvider provider)
                  throws InvalidValuesInXMLException {
    List<ExceptionParameter> paras = new ArrayList<ExceptionParameter>();
    while (entry.getBaseExceptionName() != null) {
      String path = entry.getBaseExceptionPath();
      String name = entry.getBaseExceptionName();
      entry = (ExceptionEntry_1_1) provider.get(path, name);
      if (entry == null) {
        throw new InvalidValuesInXMLException(getXmlFile(), "Did not find information to exception " + path + "." + name);
      }
      //vorne einfügen
      paras.addAll(0, entry.getParameter());
    }
    return paras;
  }

  
  //every long and every double counts twice
  protected int calcLengthOfParameters(List<ExceptionParameter> list) {
    long longDoubles = list.stream()
        .filter(x -> !x.isList() && (long.class.getName().equals(x.getTypeName()) || double.class.getName().equals(x.getTypeName())))
        .count();
    return (int) (list.size() + longDoubles);
  }

  public void addImport(ExceptionStorageInstance impEsi) {
    importedStorages.add(impEsi);
  }


  public List<ExceptionStorageInstance> getImports() {
    return importedStorages;
  }
  

  public void generateJavaFieldCache(CodeBuffer cb, ExceptionEntry_1_1 entry) {
    final String fieldGetterMethodName = FIELD_GETTER_METHOD_NAME;
    final String superFieldGetterMethodName = FIELD_GETTER_METHOD_NAME;
    final String inputParamName = "target_fieldname";
    final String resultParamName = "foundField";
    cb.addLine("public static ", Field.class.getSimpleName(), " ", fieldGetterMethodName, "(", String.class.getSimpleName()," ",inputParamName, ") {");
    cb.addLine(Field.class.getSimpleName(), " ", resultParamName, " = null");
    cb.addLine(resultParamName, " = ", FIELD_MAP_NAME, ".get(", inputParamName, ")");
    cb.addLine("if (", resultParamName, " != null) {");
    cb.addLine("return ", resultParamName);
    cb.addLine("}");
    cb.addLine("try {");
    cb.addLine(resultParamName, " = ", entry.getName(), ".class.getDeclaredField(", inputParamName,")");
    cb.addLine("} catch (", NoSuchFieldException.class.getSimpleName(), " e) {");
    cb.addLine("}");
    cb.addLine("if (", resultParamName, " == null) {");
    if (entry.getBaseExceptionName() != null) {
      cb.addLine("return ", entry.getBaseExceptionName(), ".", superFieldGetterMethodName, "(", inputParamName, ")");
    } else {
      cb.addLine("throw new ", IllegalArgumentException.class.getSimpleName(), "(\"Parameter '" + inputParamName + "' not found\")");
    }
    cb.addLine("} else {");
    cb.addLine(resultParamName, ".setAccessible(true)");
    cb.addLine(FIELD_MAP_NAME, ".put(", inputParamName, ", ", resultParamName, ")");
    cb.addLine("return ", resultParamName);
    cb.addLine("}");
    cb.addLine("}");
    cb.addLB();
  }


  public void setFqClassNameAdapter(FqClassNameAdapter fqClassNameAdapter) {
    this.fqClassNameAdapter = fqClassNameAdapter;
  }

}
