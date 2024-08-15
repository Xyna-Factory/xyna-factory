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



import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;

import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;



public class PythonMdmGeneration {

  private static final Map<PrimitiveType, String> primitive_types_mapping = setupPrimitiveTypes();


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


  public String createPythonMdm(Long revision, boolean withImpl, boolean typeHints) {
    StringBuilder sb = new StringBuilder();
    fillDefaults(sb, withImpl, typeHints);
    XMOMDatabaseSearchResult objects = searchXmomDbForObjects(revision);
    List<XynaObjectInformation> objectsSorted = convertAndSortSearchResult(objects.getResult(), revision);
    for (XynaObjectInformation object : objectsSorted) {
      addXynaObjectToMdm(sb, object, withImpl, typeHints);
    }
    return sb.toString();
  }
  

  private Map<String, XynaObjectInformation> loadObjects(List<XMOMDatabaseSearchResultEntry> objects, Long revision) {
    GenerationBaseCache cache = new GenerationBaseCache();
    Map<String, XynaObjectInformation> map = new HashMap<>();
    map.put("XynaObject", null);
    map.put("XynaException", null);
    for (XMOMDatabaseSearchResultEntry obj : objects) {
      if (obj.getType().equals(XMOMDatabaseType.SERVICEGROUP)) {
        String dtFqn = obj.getFqName().substring(0, obj.getFqName().lastIndexOf('.'));
        if (objects.stream().anyMatch(x -> x.getFqName().equals(dtFqn))) {
          continue; // we discover all methods by examining the datatype
        } else {
          obj.setFqName(dtFqn);
        }
      }
      XynaObjectInformation info = loadXynaObjectInfo(revision, obj.getFqName(), obj.getType().equals(XMOMDatabaseType.EXCEPTION), cache);
      map.put(info.fqn, info);
    }
    return map;
  }
  
  private List<XynaObjectInformation> convertAndSortSearchResult(List<XMOMDatabaseSearchResultEntry> objects, Long revision) {
    Map<String, XynaObjectInformation> map = loadObjects(objects, revision);
    List<XynaObjectInformation> result = new ArrayList<XynaObjectInformation>(objects.size());

    for (String fqn: map.keySet()) {
      XynaObjectInformation obj = map.get(fqn);
      if (obj == null) {
        continue;
      }
      addObjectToResult(obj, map, result);
    }

    return result;
  }
  

  private void addObjectToResult(XynaObjectInformation obj, Map<String, XynaObjectInformation> map, List<XynaObjectInformation> result) {
    Stack<XynaObjectInformation> hierarchy = new Stack<>();
    hierarchy.push(obj);

    while (map.get(hierarchy.peek().parent) != null) {
      hierarchy.push(map.get(hierarchy.peek().parent));
    }

    if (!map.containsKey(hierarchy.peek().parent)) {
      throw new RuntimeException("Unknown Object reference in " + hierarchy.peek().fqn + " to object: " + hierarchy.peek().parent);
    }

    while(!hierarchy.isEmpty()) {
      XynaObjectInformation info = hierarchy.pop();
      result.add(info);
      map.put(info.fqn, null);
      
    }
  }


  private void fillDefaults(StringBuilder sb, boolean withImpl, boolean typeHints) {
    fillImports(sb);
    fillBaseObject(sb, "XynaObject", "DATATYPE", null, withImpl, typeHints);
    fillBaseObject(sb, "XynaException", "EXCEPTION", "Exception", withImpl, typeHints);
    fillConvertToPythonObject(sb, withImpl, typeHints);
    fillConvertToPythonValue(sb, withImpl, typeHints);
  }


  private void fillConvertToPythonValue(StringBuilder sb, boolean withImpl, boolean typeHints) {
    sb.append("def convert_to_python_value(value");
    typeHint(sb, ": any", typeHints);
    sb.append(", multiple");
    typeHint(sb, ": bool", typeHints);
    sb.append(")");
    typeHint(sb, " -> any", typeHints);
    sb.append(":\n");
    if(!withImpl) {
      sb.append("  pass\n\n");
      return;
    }
    
    sb.append("    match(value):\n");
    sb.append("      case dict():\n");
    sb.append("        return convert_to_python_object(value)\n");
    sb.append("      case list():\n");
    sb.append("        if multiple:\n");
    sb.append("          result = ()\n");
    sb.append("          for v in value:\n");
    sb.append("            result = result + (v,)\n");
    sb.append("          return result\n");
    sb.append("        else:\n");
    sb.append("          return _convert_list(value)\n");
    sb.append("      case _:\n");
    sb.append("        return value\n\n");
    
  }


  private void fillImports(StringBuilder sb) {
    sb.append("import decimal\n");
    sb.append("from com.gip.xyna import XynaFactory # type: ignore\n\n");
  }


  private void fillBaseObject(StringBuilder sb, String name, String xynaType, String parent, boolean withImpl, boolean typeHints) {
    sb.append("class ");
    sb.append(name);
    if (parent != null) {
      sb.append("(");
      sb.append(parent);
      sb.append(")");
    }
    sb.append(":\n");
    sb.append("  _context = None\n");
    sb.append("  def __init__(self, fqn");
    typeHint(sb, ": str", typeHints);
    sb.append("):\n");
    sb.append("    self._fqn = fqn\n");
    sb.append("    self._xynatype = \"");
    sb.append(xynaType);
    sb.append("\"\n\n");

    sb.append("  def set(self, field_name");
    typeHint(sb, ": str", typeHints);
    sb.append(", value");
    typeHint(sb, ": any", typeHints);
    sb.append(")");
    typeHint(sb, " -> None", typeHints);
    sb.append(":\n");
    sb.append("    if field_name != \"_fqn\" and field_name !=\"_xynatype\":\n");
    sb.append("      setattr(self, field_name, value)\n\n");
  }


  private void fillConvertToPythonObject(StringBuilder sb, boolean withImpl, boolean typeHints) {
    if (withImpl) {
      fillSetField(sb);
    }
    sb.append("def convert_to_python_object(obj");
    typeHint(sb, ": dict[str, any]", typeHints);
    sb.append(")");
    typeHint(sb, " -> XynaObject | XynaException", typeHints);
    sb.append(":\n");
    if (!withImpl) {
      sb.append("  pass\n\n");
      return;
    }
    sb.append("  fqn = obj[\"_fqn\"]\n");
    sb.append("  result = eval(f\"{fqn}()\")\n");
    sb.append("  for f in obj:\n");
    sb.append("    _set_field(result, f, obj)\n");
    sb.append("  return result\n\n");
  }


  private void fillConvertList(StringBuilder sb) {
    sb.append("def _convert_list(values):\n");
    sb.append("  result = []\n");
    sb.append("  for value in values:\n");
    sb.append("    match(value):\n");
    sb.append("      case dict():\n");
    sb.append("        result.append(convert_to_python_object(value))\n");
    sb.append("      case list():\n");
    sb.append("        result.append(_convert_list(value))\n");
    sb.append("      case _:\n");
    sb.append("        result.append(value)\n\n");
    sb.append("  return result\n\n");
  }


  private void fillSetField(StringBuilder sb) {
    fillConvertList(sb);
    sb.append("def _set_field(object_to_set, fieldName, data):\n");
    sb.append("  value = data[fieldName]\n");
    sb.append("  match value:\n");
    sb.append("    case dict():\n");
    sb.append("      object_to_set.set(fieldName, convert_to_python_object(value))\n");
    sb.append("    case list():\n");
    sb.append("      object_to_set.set(fieldName, _convert_list(value))\n");
    sb.append("    case _:\n");
    sb.append("      object_to_set.set(fieldName, value)\n\n");
  }


  private void typeHint(StringBuilder sb, String type, boolean typeHints) {
    if (!typeHints) {
      return;
    }
    sb.append(type);
  }


  private void addXynaObjectToMdm(StringBuilder sb, XynaObjectInformation info, boolean withImpl, boolean typeHints) {
    sb.append("class ");
    sb.append(convertToPythonFqn(info.fqn));
    sb.append("(");
    sb.append(convertToPythonFqn(info.parent));
    sb.append("):\n");
    sb.append("  def __init__(self):\n");
    sb.append("    super().__init__(\"");
    sb.append(info.fqn); //original FQN
    sb.append("\")\n");

    if (info.members != null && !info.members.isEmpty()) {
      for (Pair<String, String> member : info.members) {
        sb.append("    self.");
        sb.append(member.getFirst());
        typeHint(sb, ": " + member.getSecond(), typeHints);
        sb.append(" = None\n");
      }
    }

    sb.append("\n");

    if (info.methods != null && !info.methods.isEmpty()) {
      for (MethodInformation method : info.methods) {
        addXynaObjectMethod(sb, info.fqn, method, withImpl, typeHints);
      }
    }
    sb.append("\n");
  }


  private void addXynaObjectMethod(StringBuilder sb, String fqn, MethodInformation info, boolean withImpl, boolean typeHints) {
    sb.append("  def ");
    sb.append(info.name);
    sb.append("(");
    if (!info.isStatic) {
      sb.append("self");
    }
    if (info.argumentsWithTypes != null && !info.argumentsWithTypes.isEmpty()) {
      if (!info.isStatic) {
        sb.append(", ");
      }
      for (Pair<String, String> argument : info.argumentsWithTypes) {
        sb.append(argument.getFirst());
        typeHint(sb, ": " + argument.getSecond(), typeHints);
        sb.append(", ");
      }
      sb.setLength(sb.length() - 2); //remove last ", "
    }
    sb.append(")");
    if (info.returnType != null) {
      typeHint(sb, " -> " + info.returnType, typeHints);
    }
    sb.append(":\n");
    if (!withImpl) {
      sb.append("    pass\n\n");
      return;
    }

    sb.append("    ");
    
    if (info.returnType != null) {
      sb.append("return convert_to_python_value(");
    }
    sb.append("XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement().");
    if (info.isStatic) {
      sb.append("invokeService(");
    } else {
      sb.append("invokeInstanceService(");
    }
    sb.append("XynaObject._context, ");
    if (info.isStatic) {
      sb.append("\"" + fqn + "\"");
      sb.append(", ");
    } else {
      sb.append("self, ");
    }
    sb.append("\"" + info.name + "\"");
    sb.append(", [");
    if (info.argumentsWithTypes != null && !info.argumentsWithTypes.isEmpty()) {
      for (Pair<String, String> argument : info.argumentsWithTypes) {
        sb.append(argument.getFirst());
        sb.append(", ");
      }
      sb.setLength(sb.length() - 2); //remove last ", "
    }
    sb.append("]");
    if (info.returnType != null) {
      sb.append("), ");
      sb.append(info.returnType.startsWith("tuple[") ? "True" : "False");
    }
    sb.append(")\n\n");
  }


  private XynaObjectInformation loadXynaObjectInfo(Long revision, String fqn, boolean isException, GenerationBaseCache cache) {
    XynaObjectInformation result = new XynaObjectInformation();
    result.fqn = fqn;
    try {
      DomOrExceptionGenerationBase doe =  isException ? ExceptionGeneration.getOrCreateInstance(fqn, cache, revision) : DOM.getOrCreateInstance(fqn, cache, revision);
      doe.parse(false);
      result.parent = doe.getSuperClassGenerationObject() != null ? doe.getSuperClassGenerationObject().getOriginalFqName() : null;
      result.members = doe.getMemberVars().stream().map(this::toMemberInfo).collect(Collectors.toList());
      if (!isException) {
        result.methods = loadOperations(((DOM) doe).getOperations());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (result.parent == null) {
      result.parent = isException ? "XynaException" : "XynaObject";
    }
    
    //special case XynaException (inherits from java.lang.Exception
    if (fqn.equals("core.exception.XynaException")) {
      result.parent = "XynaException";
    }

    return result;
  }


  private Pair<String, String> toMemberInfo(AVariable avar) {
    return new Pair<String, String>(avar.getVarName(), getPythonTypeOfVariable(avar));
  }


  private List<MethodInformation> loadOperations(List<Operation> operations) {
    if (operations == null || operations.isEmpty()) {
      return null;
    }
    List<MethodInformation> result = new ArrayList<MethodInformation>();
    for (Operation op : operations) {
      MethodInformation info = new MethodInformation();
      info.isStatic = op.isStatic();
      info.name = op.getNameWithoutVersion();
      info.returnType = createReturnTypeFromOutputVars(op.getOutputVars());
      info.argumentsWithTypes = createArgumentsWithTypes(op.getInputVars());
      result.add(info);
    }
    return result;
  }


  private List<Pair<String, String>> createArgumentsWithTypes(List<AVariable> inputVars) {
    List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();
    if (inputVars == null || inputVars.isEmpty()) {
      return null;
    }
    for (AVariable avar : inputVars) {
      result.add(new Pair<String, String>(avar.getVarName(), getPythonTypeOfVariable(avar)));
    }
    return result;
  }


  private String getPythonTypeOfVariable(AVariable avar) {
    String type;
    if (avar.isJavaBaseType()) {
      type = primitive_types_mapping.getOrDefault(avar.getJavaTypeEnum(), "any");
    } else {
      type = "'" + convertToPythonFqn(avar.getOriginalPath() + "." + avar.getOriginalName()) + "'";
    }
    if(avar.isList()) {
      type = "list[" + type + "]";
    }
    return type;
  }


  private String createReturnTypeFromOutputVars(List<AVariable> vars) {
    if (vars.isEmpty()) {
      return null;
    }
    if (vars.size() == 1) {
      AVariable avar = vars.get(0);
      return getPythonTypeOfVariable(avar);
    }
    return String.format("tuple[%s]", String.join(", ", vars.stream().map(this::getPythonTypeOfVariable).collect(Collectors.toList())));
  }


  private XMOMDatabaseSearchResult searchXmomDbForObjects(Long revision) {
    XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    XMOMDatabaseSelect select = new XMOMDatabaseSelect();
    select.addAllDesiredResultTypes(Arrays.asList(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION, XMOMDatabaseType.SERVICEGROUP));
    try {
      return xmomDB.searchXMOMDatabase(Arrays.asList(select), Integer.MAX_VALUE, revision);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String convertToPythonFqn(String fqn) {
    return fqn.replace('.', '_');
  }

  public void exportPythonMdm(Long revision, String destination) throws Exception {
    String data = createPythonMdm(revision, false, true);
    try (PrintWriter bos = new PrintWriter(destination + "/mdm.py")) {
      bos.write(data);
    }
  }


  private static class XynaObjectInformation {

    private String fqn; //original
    private String parent; //original
    private List<Pair<String, String>> members;
    private List<MethodInformation> methods;
  }

  private static class MethodInformation {

    private String name;
    private String returnType;
    private boolean isStatic;
    private List<Pair<String, String>> argumentsWithTypes;
  }
}
