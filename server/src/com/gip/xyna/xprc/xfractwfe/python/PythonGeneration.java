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
package com.gip.xyna.xprc.xfractwfe.python;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;



public class PythonGeneration {

  public static class XynaObjectInformation {

    public String fqn; //original
    public String parent; //original
    public List<Pair<String, String>> members;
    public List<MethodInformation> methods;
  }

  public static class MethodInformation {

    public String name;
    public String returnType;
    public boolean isStatic;
    public List<Pair<String, String>> argumentsWithTypes;
  }


  public static final Map<PrimitiveType, String> PRIMITIVE_TYPES_MAPPING = setupPrimitiveTypes();


  private static Map<PrimitiveType, String> setupPrimitiveTypes() {
    Map<PrimitiveType, String> result = new HashMap<>();
    result.put(PrimitiveType.BOOLEAN, "bool");
    result.put(PrimitiveType.BOOLEAN_OBJ, "bool");
    result.put(PrimitiveType.BYTE, "bytes");
    result.put(PrimitiveType.BYTE_OBJ, "bytes");
    result.put(PrimitiveType.DOUBLE, "decimal.Decimal");
    result.put(PrimitiveType.DOUBLE_OBJ, "decimal.Decimal");
    result.put(PrimitiveType.EXCEPTION, "XynaException");
    result.put(PrimitiveType.INT, "int");
    result.put(PrimitiveType.INTEGER, "int");
    result.put(PrimitiveType.LONG, "int");
    result.put(PrimitiveType.LONG_OBJ, "int");
    result.put(PrimitiveType.STRING, "str");
    result.put(PrimitiveType.VOID, "None");
    return result;
  }


  public static List<MethodInformation> loadOperations(List<Operation> operations) {
    return loadOperations(operations, false);
  }


  public static List<MethodInformation> loadOperations(List<Operation> operations, boolean addMdmPath) {
    if (operations == null || operations.isEmpty()) {
      return Collections.emptyList();
    }
    List<MethodInformation> result = new ArrayList<MethodInformation>();
    for (Operation op : operations) {
      MethodInformation info = new MethodInformation();
      info.isStatic = op.isStatic();
      info.name = op.getNameWithoutVersion();
      info.returnType = createReturnTypeFromOutputVars(op.getOutputVars(), addMdmPath);
      info.argumentsWithTypes = createArgumentsWithTypes(op.getInputVars(), addMdmPath);
      result.add(info);
    }
    return result;
  }


  public static String createReturnTypeFromOutputVars(List<AVariable> vars, boolean addMdmPath) {
    if (vars.isEmpty()) {
      return null;
    }
    if (vars.size() == 1) {
      AVariable avar = vars.get(0);
      return getPythonTypeOfVariable(avar, addMdmPath);
    }
    return String.format("tuple[%s]",
                         String.join(", ", vars.stream().map(PythonGeneration::getPythonTypeOfVariable).collect(Collectors.toList())));
  }


  public static String getPythonTypeOfVariable(AVariable avar) {
    return getPythonTypeOfVariable(avar, false);
  }


  public static String getPythonTypeOfVariable(AVariable avar, boolean addMdmPath) {
    String type;
    if (avar.isJavaBaseType()) {
      type = PRIMITIVE_TYPES_MAPPING.getOrDefault(avar.getJavaTypeEnum(), "any");
    } else {
      if (addMdmPath) {
        type = "mdm." + convertToPythonFqn(avar.getOriginalPath() + "." + avar.getOriginalName());
      } else {
        type = "'" + convertToPythonFqn(avar.getOriginalPath() + "." + avar.getOriginalName()) + "'";
      }
    }
    if (avar.isList()) {
      type = "list[" + type + "]";
    }
    return type;
  }


  public static List<Pair<String, String>> createArgumentsWithTypes(List<AVariable> inputVars, boolean addMdmPath) {
    List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    if (inputVars == null || inputVars.isEmpty()) {
      return null;
    }
    for (AVariable avar : inputVars) {
      result.add(new Pair<String, String>(avar.getVarName().replaceAll("\\s+", ""), getPythonTypeOfVariable(avar, addMdmPath)));
    }
    return result;
  }


  public static String getDateSuffix() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    return sdf.format(new Date());
  }


  public static String convertToPythonFqn(String fqn) {
    return fqn.replace('.', '_');
  }

}