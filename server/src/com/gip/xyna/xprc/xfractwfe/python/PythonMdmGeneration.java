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
import java.util.Collection;
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
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.python.PythonGeneration.MethodInformation;
import com.gip.xyna.xprc.xfractwfe.python.PythonGeneration.XynaObjectInformation;
import com.gip.xyna.xprc.xfractwfe.python.PythonThreadManagement.PythonKeywordsThread;



public class PythonMdmGeneration {


  public static final String LOAD_MODULE_SNIPPET = setupLoadModuleSnippet();
  public final List<String> pythonKeywords = new ArrayList<String>();


  /**
   * contains mdm.py with implementations, but without typeHints
   */
  private Map<Long, String> cache = new HashMap<Long, String>();


  public void invalidateRevision(Collection<Long> revisions) {
    for(Long revision : revisions) {
      cache.remove(revision);
    }
  }
  
  private void loadPythonKeywords() {
    try {
      PythonKeywordsThread thread = PythonThreadManagement.createPythonKeywordThread(PythonMdmGeneration.class.getClassLoader());
      thread.start();
      thread.join();
      if (thread.wasSuccessful()) {
        pythonKeywords.clear();
        pythonKeywords.addAll(thread.getResult());
      } else {
        throw new RuntimeException(thread.getException());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static String setupLoadModuleSnippet() {
    StringBuilder sb = new StringBuilder();
    sb.append("import importlib\n");
    sb.append("import sys\n");
    sb.append("import types\n");
    setupStringLoader(sb);
    setupStringFinder(sb);
    sb.append("def _load_module(modules): \n");
    sb.append("  finder = StringFinder(StringLoader(modules))\n");
    sb.append("  sys.meta_path.append(finder)\n");
    return sb.toString();
  }

  private static void setupStringLoader(StringBuilder sb) {
    sb.append("class StringLoader(importlib.abc.Loader):\n");
    sb.append("  def __init__(self, modules):\n");
    sb.append("    self._modules = modules\n");
    sb.append("  def has_module(self, fullname):\n");
    sb.append("    return (fullname in self._modules)\n");
    sb.append("  def create_module(self, spec):\n");
    sb.append("    if self.has_module(spec.name):\n");
    sb.append("      module = types.ModuleType(spec.name)\n");
    sb.append("      exec(self._modules[spec.name], module.__dict__)\n");
    sb.append("      return module\n");
    sb.append("  def exec_module(self, module):\n");
    sb.append("    pass\n\n");
  }
  
  private static void setupStringFinder(StringBuilder sb) {
    sb.append("class StringFinder(importlib.abc.MetaPathFinder):\n");
    sb.append("  def __init__(self, loader):\n");
    sb.append("    self._loader = loader\n");
    sb.append("  def find_spec(self, fullname, path, target=None):\n");
    sb.append("    if self._loader.has_module(fullname):\n");
    sb.append("      return importlib.machinery.ModuleSpec(fullname, self._loader)\n\n");
  }


  public String createPythonMdm(Long revision, boolean withImpl, boolean typeHints) {
    if(withImpl == true && typeHints == false ) {
      return cache.computeIfAbsent(revision, x -> createPythonMdmString(x, withImpl, typeHints));
    }
    return createPythonMdmString(revision, withImpl, typeHints);
  }
  
  private String createPythonMdmString(Long revision, boolean withImpl, boolean typeHints) {
    if (pythonKeywords.isEmpty()) {
      loadPythonKeywords();
    }
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
      if (info != null) {
        map.put(info.fqn, info);
      }
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
    
    //can't use match yet - python version might be too old
    sb.append("    if type(value).__name__ == \"HashMap\":\n");
    sb.append("      return convert_to_python_object(value)\n");
    sb.append("    if type(value) is list: \n");
    sb.append("      if multiple:\n");
    sb.append("        result = ()\n");
    sb.append("        for v in value:\n");
    sb.append("          result = result + (v,)\n");
    sb.append("        return result\n");
    sb.append("      else:\n");
    sb.append("        return _convert_list(value)\n");
    sb.append("    else:\n");
    sb.append("      return value\n\n");
    
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
    sb.append("  fqn = obj[\"_fqn\"].replace('.', '_')\n");
    sb.append("  result = eval(f\"{fqn}()\")\n");
    sb.append("  for f in obj:\n");
    sb.append("    _set_field(result, f, obj)\n");
    sb.append("  return result\n\n");
  }


  //can't use match yet - python version might be too old
  private void fillConvertList(StringBuilder sb) {
    sb.append("def _convert_list(values):\n");
    sb.append("  result = []\n");
    sb.append("  for value in values:\n");
    sb.append("    if type(value).__name__ == \"HashMap\":\n");
    sb.append("      result.append(convert_to_python_object(value))\n");
    sb.append("    elif type(value) is list:\n");
    sb.append("      result.append(_convert_list(value))\n");
    sb.append("    else:\n");
    sb.append("      result.append(value)\n\n");
    sb.append("  return result\n\n");
  }


  //can't use match yet - python version might be too old
  private void fillSetField(StringBuilder sb) {
    fillConvertList(sb);
    sb.append("def _set_field(object_to_set, fieldName, data):\n");
    sb.append("  value = data[fieldName]\n");
    sb.append("  if type(value).__name__ == \"HashMap\":\n");
    sb.append("    object_to_set.set(fieldName, convert_to_python_object(value))\n");
    sb.append("  elif type(value) is list:\n");
    sb.append("    object_to_set.set(fieldName, _convert_list(value))\n");
    sb.append("  else:\n");
    sb.append("    object_to_set.set(fieldName, value)\n\n");
  }


  private void typeHint(StringBuilder sb, String type, boolean typeHints) {
    if (!typeHints) {
      return;
    }
    sb.append(type);
  }


  private void addXynaObjectToMdm(StringBuilder sb, XynaObjectInformation info, boolean withImpl, boolean typeHints) {
    sb.append("class ");
    sb.append(PythonGeneration.convertToPythonFqn(info.fqn));
    sb.append("(");
    sb.append(PythonGeneration.convertToPythonFqn(info.parent));
    sb.append("):\n");
    sb.append("  def __init__(self):\n");
    sb.append("    super().__init__(\"");
    sb.append(info.fqn); //original FQN
    sb.append("\")\n");

    if (info.members != null && !info.members.isEmpty()) {
      for (Pair<String, String> member : info.members) {
        sb.append("    self.");
        sb.append(member.getFirst());
        if (pythonKeywords.contains(member.getFirst())) {
          sb.append("_");  // append _ if the member variable is a python keyword
        }
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
        result.methods = PythonGeneration.loadOperations(((DOM) doe).getOperations());
      }
    } catch (Exception e) {
      return null;
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
    return new Pair<String, String>(avar.getVarName(), PythonGeneration.getPythonTypeOfVariable(avar));
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


  public void exportPythonMdm(Long revision, String destination) throws Exception {
    String data = createPythonMdm(revision, false, true);
    try (PrintWriter bos = new PrintWriter(destination + "/mdm.py")) {
      bos.write(data);
    }
  }
  
  public List<String> getPythonKeywords() {
    if (pythonKeywords.isEmpty()) {
      loadPythonKeywords();
    }
    return pythonKeywords;
  }
}
