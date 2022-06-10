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
package com.gip.xyna.xprc.xfractwfe.formula;



import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.exceptions.InvalidTypeCastException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;



public class Functions implements SupportedFunctionStore {
  
  public static final String APPEND_TO_LIST_FUNCTION_NAME = "append";
  public static final String TYPE_OF_FUNCTION_NAME = "typeof";
  public static final String CAST_FUNCTION_NAME = "cast";
  public static final String NEW_FUNCTION_NAME = "new";

  private static Set<Function> functions; //registrierte funktionen
  static {
    functions = new HashSet<Function>();
    functions.add(new Function("null", TypeInfo.NULL, new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 0;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    }, "null"));
    FunctionParameterTypeDefinition oneStringParameter = new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return new TypeInfo(BaseType.STRING);
        }
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 1;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    };
    FunctionParameterTypeDefinition twoStringParameter = new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0 || parameterCnt == 1) {
          return new TypeInfo(BaseType.STRING);
        }
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 2;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    };
    FunctionParameterTypeDefinition threeStringParameter = new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt < 3) {
          return new TypeInfo(BaseType.STRING);
        }
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 3;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    };
    functions.add(new Function("concat", new TypeInfo(BaseType.STRING), new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        return new TypeInfo(BaseType.STRING);
      }


      public int numberOfParas() {
        return -1; //beliebig viele
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    }, Functions.class.getName() + ".fconcat"));
    functions.add(new Function("contains", new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), twoStringParameter,
                               Functions.class.getName() + ".fcontains"));
    functions.add(new Function("startswith", new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), twoStringParameter,
                               Functions.class.getName() + ".fstartswith"));
    functions.add(new Function("endswith", new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), twoStringParameter,
                               Functions.class.getName() + ".fendswith"));
    functions.add(new Function("matches", new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), twoStringParameter, Functions.class.getName()
        + ".fmatches"));
    functions.add(new Function("replaceall", new TypeInfo(BaseType.STRING), threeStringParameter, Functions.class.getName()
                               + ".freplaceall"));
    functions.add(new Function("substring", new TypeInfo(BaseType.STRING), new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return new TypeInfo(BaseType.STRING);
        } else if (parameterCnt == 1 || parameterCnt == 2) {
          return new TypeInfo(BaseType.INT_PRIMITIVE);
        }
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 2;
      }


      public int numberOfOptionalParas() {
        return 1;
      }
    }, Functions.class.getName()
                               + ".fsubstring"));
    functions.add(new Function("indexof", new TypeInfo(BaseType.INT_PRIMITIVE), twoStringParameter, Functions.class.getName()
                               + ".findexof"));
    functions.add(new Function("xpath", new TypeInfo(BaseType.LIST), new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0 || parameterCnt == 1) {
          return new TypeInfo(BaseType.STRING);
        } else if (parameterCnt == 2) {
          return new TypeInfo(BaseType.BOOLEAN_PRIMITIVE);
        }
        throw new RuntimeException();
      }


      public int numberOfParas() {
        return 2;
      }


      public int numberOfOptionalParas() {
        return 1;
      }
    }, Functions.class.getName() + ".fxpath"));
    functions.add(new Function("touppercase", new TypeInfo(BaseType.STRING), oneStringParameter, Functions.class
        .getName() + ".ftoUpperCase"));
    functions.add(new Function("tolowercase", new TypeInfo(BaseType.STRING), oneStringParameter, Functions.class
        .getName() + ".ftoLowerCase"));
    functions.add(new Function("length", new TypeInfo(BaseType.INT_PRIMITIVE), new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt != 0) {
          throw new RuntimeException();
        }
        return TypeInfo.ANY;
      }


      public int numberOfParas() {
        return 1;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    }, Functions.class.getName() + ".flength"));
    functions.add(new Function(TYPE_OF_FUNCTION_NAME, new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), new FunctionParameterTypeDefinition() {

      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return TypeInfo.ANY;
        }
        return new TypeInfo(BaseType.STRING);
      }


      public int numberOfParas() {
        return 2;
      }


      public int numberOfOptionalParas() {
        return 0;
      }
    }, Functions.class.getName() + ".ftypeof"));
    functions.add(new Function("concatlists", new TypeInfo(BaseType.LIST), new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 2;
      }
      
      
      public int numberOfOptionalParas() {
        return 0;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        return new TypeInfo(BaseType.LIST);
      }
    }, Functions.class.getName() + ".fconcatlists"));
    functions.add(new Function("deepequals", new TypeInfo(BaseType.BOOLEAN_PRIMITIVE), new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 2;
      }
      
      
      public int numberOfOptionalParas() {
        return 0;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        return TypeInfo.ANY;
      }
    }, Functions.class.getName() + ".fdeepequals"));
    functions.add(new Function(APPEND_TO_LIST_FUNCTION_NAME, new TypeInfo(BaseType.LIST), new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 2;
      }
      
      
      public int numberOfOptionalParas() {
        return 0;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return new TypeInfo(BaseType.LIST);
        } else if (parameterCnt == 1) {
          return TypeInfo.ANY;
        } else {
          throw new RuntimeException();
        }
      }
    }, Functions.class.getName() + ".fappendtolist"));
    functions.add(new Function(NEW_FUNCTION_NAME, TypeInfo.ANY, new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 1;
      }
      
      
      public int numberOfOptionalParas() {
        return 0;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return new TypeInfo(BaseType.STRING);
        } else {
          throw new RuntimeException();
        }
      }
    }, Functions.class.getName() + ".fnew"));
    functions.add(new Function(CAST_FUNCTION_NAME, TypeInfo.ANY, new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 1;
      }
      
      
      public int numberOfOptionalParas() {
        return 1;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        if (parameterCnt == 0) {
          return new TypeInfo(BaseType.STRING);
        } else if (parameterCnt == 1) {
          return TypeInfo.ANY;
        } else {
          throw new RuntimeException();
        }
      }
    }, Functions.class.getName() + ".fcast"));
    functions.add(new Function("asxflexpression", new TypeInfo(BaseType.STRING), new FunctionParameterTypeDefinition() {
      
      public int numberOfParas() {
        return 1;
      }
      
      
      public int numberOfOptionalParas() {
        return 0;
      }
      
      
      public TypeInfo getType(int parameterCnt) {
        return TypeInfo.ANY;
      }
    }, "", new FunctionVisitationPattern() {
      
      public void visit(FunctionExpression function, Visitor visitor) {
        if (visitor instanceof ModelledExpression.JavaCodeGeneratorVisitor) {
          IdentityCreationVisitor idVisitor = new IdentityCreationVisitor();
          Expression subExpr = function.getSubExpressions().get(0);
          subExpr.visit(idVisitor);
          String path = idVisitor.getXFLExpression();
          path = GenerationBase.escapeForCodeGenUsageInString(path);
          LiteralExpression pathLiteral = new LiteralExpression(path, path, -1, -1);
          try {
            pathLiteral.setTargetType(new TypeInfo(BaseType.STRING));  
          } catch (XPRC_InvalidVariableMemberNameException e) {
            // should never be thrown from a literal
            throw new RuntimeException(e);
          }
          pathLiteral.visit(visitor);
        } else {
          FunctionExpression.DEFAULT_VISITATION_PATTERN.visit(function, visitor);          
        }
      }
    }));
  }


  public void addFunction(Function f) {
    functions.add(f);
  }
  
  public static Function getFunction(String functionName) {
    for (Function function : functions) {
      if (function.getName().equals(functionName)) {
        return function;
      }
    }
    return null;
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static String fconcat(String... args) {
    if (args.length == 0) {
      return "";
    } else if (args.length == 1) {
      return args[0];
    }
    StringBuilder sb = new StringBuilder();
    for (String s : args) {
      sb.append(s);
    }
    return sb.toString();
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static boolean fcontains(String s, String t) {
    if (s == null) {
      return false;
    }
    return s.contains(t);
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static int flength(Object s) {
    if (s == null) {
      return -1;
    }
    if (s instanceof List) {
      return ((List<?>) s).size();
    }
    return String.valueOf(s).length();
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static boolean fstartswith(String s, String t) {
    if (s == null) {
      return false;
    }
    return s.startsWith(t);
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static boolean fendswith(String s, String t) {
    if (s == null) {
      return false;
    }
    return s.endsWith(t);
  }
  

  private static ClassLoaderDispatcher cld;
  
  //nur für abwärtskompatibilität
  private static final XynaPropertyBoolean toggleInstanceOfNullBehaviourToReturnTrue = new XynaPropertyBoolean("xprc.xfractwfe.formula.function.instanceof.null.returnstrue", false).setHidden(true);

  /**
   * ist objekt s vom mdm-type t (fqxmlname)
   */
  public static boolean ftypeof(Object s, String t) {
    if (s == null) {
      return toggleInstanceOfNullBehaviourToReturnTrue.get();
    }
    if (!(s instanceof GeneralXynaObject)) {
      return false;
    }
    try {
      t = GenerationBase.transformNameForJava(t);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    if (GenerationBase.isReservedServerObjectByFqClassName(t)) {
      try {
        return Class.forName(t).isAssignableFrom(s.getClass());
      } catch (ClassNotFoundException e) {
        return false;
      }
    }

    ClassLoader cl = s.getClass().getClassLoader();
    if (cl instanceof ClassLoaderBase) {
      long revision = ((ClassLoaderBase) cl).getRevision();
      Class<?> c;
      try {
        if (cld == null) {
          cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
        }
        c = cld.loadMDMClass(t, false, null, null, revision);
        if (c == null) {
          c = cld.loadExceptionClass(t, false, null, null, revision);
        }
        if (c == null) {
          return false; // ~ ClassNotFoundException
        }
      } catch (ClassNotFoundException e) {
        return false;
      }
      return c.isAssignableFrom(s.getClass());
    } else {
      try {
        return Class.forName(t).isAssignableFrom(s.getClass());
      } catch (ClassNotFoundException e) {
        return false;
      }
    }
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static boolean fmatches(String s, String regex) {
    //TODO performance: caching der regex, falls konstant?? evtl cache verwenden, wo objekte nach bestimmter zeit rausgeworfen werden
    if (s == null) {
      return false;
    }
    return s.matches(regex);
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static String ftoUpperCase(String s) {
    if (s == null) {
      return null;
    }
    return s.toUpperCase(); //FIXME locale woher?
  }


  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static String ftoLowerCase(String s) {
    if (s == null) {
      return null;
    }
    return s.toLowerCase(); //FIXME locale woher?
  }

  
  /**
   * wird vom generierten code aus aufgerufen 
   */
  public static List<String> fxpath(String xml, String xpath) {
    return fxpath(xml, xpath, false);
  }
  
  // TODO add another function for given namespace mappings
  public static List<String> fxpath(String xml, String xpath, boolean namespaceAware) {
    if (xml == null || xpath == null) {
      return Collections.emptyList();
    }
    List<String> resultList = new ArrayList<String>();
    try {
      Document doc = XMLUtils.parseString(xml, namespaceAware);
      XPathFactory factory = XPathFactory.newInstance();
      XPath xpathObj = factory.newXPath();
      if (namespaceAware) {
        xpathObj.setNamespaceContext(XMLUtils.getNamespaceContextForDocument(doc));
      }
      XPathExpression expr = xpathObj.compile(xpath);
      
      Object result = expr.evaluate(doc, XPathConstants.NODESET);
      NodeList nodes = (NodeList) result;
      for (int i = 0; i < nodes.getLength(); i++) {
        String nodeValue = nodes.item(i).getNodeValue();
        if (nodeValue == null) {
          resultList.add(XMLUtils.getXMLString((Element) nodes.item(i), true));
        } else {
          resultList.add(nodes.item(i).getNodeValue());
        }
      }
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException("Error encountered while trying to execute XPath function",e);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Error encountered while trying to execute XPath function",e);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("Error encountered while trying to execute XPath function",e);
    }
    return resultList;
  }
  
  
  public static String freplaceall(String basestring, String filter, String replacement) {
    if (basestring == null) {
      return null;
    }
    if (filter == null) {
      return basestring;
    }
    if (replacement == null) {
      replacement = "";
    }
    return basestring.replaceAll(filter, replacement);
  }
  
  
  public static int findexof(String basestring, String filter) {
    if (basestring == null || filter == null) {
      return -1;
    }
    return basestring.indexOf(filter);
  }
  
  
  public static String fsubstring(String basestring, int beginIndex) {
    if (basestring == null) {
      return null;
    }
    return fsubstring(basestring, beginIndex, basestring.length());
  }
  
  public static String fsubstring(String basestring, int beginIndex, int endIndex) {
    if (basestring == null) {
      return null;
    }
    int restrictedBeginIndex = beginIndex;
    if (beginIndex < 0) {
      restrictedBeginIndex = 0;
    }
    int restrictedEndIndex = endIndex;
    if (endIndex > basestring.length()) {
      restrictedEndIndex = basestring.length();
    }
    return basestring.substring(restrictedBeginIndex, restrictedEndIndex);
  }
  
  
  public static List<?> fconcatlists(List<?> a, List<?> b) {
    List ret = new ArrayList();
    if (a != null) {
      ret.addAll(a);
    }
    if (b != null) {
      ret.addAll(b);
    }
    return ret;
  }
  
  public static boolean fdeepequals(Object a, Object b) {
    if(a.getClass() != b.getClass()) {
      return false;
    }
    //Beide haben den selben typ, daher muss bei instanceof nur noch a geprüft werden
    if(a instanceof List) {
      return deepequalsList((List)a, (List)b);
    } else if(a instanceof XynaObject) {
      return deepequalsXynaObject((XynaObject)a, (XynaObject)b);
    } else {
      return Objects.equals(a, b);
    }
  }
  
  private static boolean deepequalsList(List<?> a, List<?> b) {
    if(a.size() != b.size()) {
      return false;
    }
    
    for(int i = 0; i < a.size(); i++) {
      if(!fdeepequals(a.get(i), b.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  private static boolean deepequalsXynaObject(XynaObject a, XynaObject b) {
    Set<String> varNames;
    //Beide Objekte sind schon von der selben KLasse, das wurde schon in fdeepequals() gecheckt
    Class<? extends XynaObject> objectClass = a.getClass();
    try {
      //Alle Variablennamen holen. Per Reflection, weil diese Methode nur generiert wird
      Method getVariableNames = objectClass.getMethod("getVariableNames");
      varNames = (Set<String>) getVariableNames.invoke(a);
      for(String varName:varNames) {
        Object varA = a.get(varName);
        Object varB = b.get(varName);
        if(!fdeepequals(varA, varB)) {
          return false;
        }
      }
    } catch (NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
    
    return true;
  }
  
  
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static List<?> fappendtolist(List<?> a, Object b) {
    List ret = new ArrayList();
    if (a != null) {
      ret.addAll(a);
    }
    if (b != null) {
      ret.add(b);
    }
    return ret;
  }
  
  
  public static <O extends GeneralXynaObject, I extends GeneralXynaObject> O fcast(Class<O> o, I i) throws XynaException {
    if (i == null) {
      return null;
    } else if (o.isInstance(i)) {
      return o.cast(i);
    } else {
      throw new InvalidTypeCastException(o.getName(), i.getClass().getName());
    }
  }
  
  
  public static Boolean fparsebooleanornull(Object value) {
    return value == null ? null : Boolean.parseBoolean(String.valueOf(value));
  }
  
  
  public static Integer fparseintegerornull(Object value) {
    return value == null ? null : Integer.parseInt(String.valueOf(value));
  }
  
  
  public static Long fparselongrnull(Object value) {
    return value == null ? null : Long.parseLong(String.valueOf(value));
  }
  
  
  public static Float fparsefloatornull(Object value) {
    return value == null ? null : Float.parseFloat(String.valueOf(value));
  }
  
  
  public static Double fparsedoubleornull(Object value) {
    return value == null ? null : Double.parseDouble(String.valueOf(value));
  }
  

  public Set<Function> getSupportedFunctions() {
    return functions;
  }
  
  
  // called from code generated by ModelledExpression
  public final static String ADJUST_VALUE_METHOD_NAME = "adjustValueToTargetType";

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <T> T adjustValueToTargetType(Class<T> targetType, Object sourceValue) {
    Object result = null;
    if (sourceValue == null) {
      if (targetType == int.class) {
        result = 0;
      } else if (targetType == long.class) {
        result = 0l;
      } else if (targetType == double.class) {
        result = 0d;
      } else if (targetType == boolean.class) {
        result = false;
      } else {
        return null;
      }
    } else {
      // String target is easy, handle first
      if (targetType == String.class) {
        result = sourceValue.toString();
      } else if (targetType == List.class) {
        if (sourceValue instanceof List) {
          result = sourceValue;
        }
      } else if (targetType == Object.class) {
        result = sourceValue;
      } else {
        Class<?> sourceType = sourceValue.getClass();
        // sourceType is always boxed!
        if (sourceType == String.class) {
          // from String
          if (targetType == int.class ||
              targetType == Integer.class) {
            result = Integer.parseInt((String) sourceValue);
          } else if (targetType == long.class ||
                     targetType == Long.class) {
            result = Long.parseLong((String) sourceValue);
          } else if (targetType == boolean.class ||
                     targetType == Boolean.class) {
            result = Boolean.parseBoolean((String) sourceValue);
          } else if (targetType == double.class ||
                     targetType == Double.class) {
            result = Double.parseDouble((String) sourceValue);
          }
        } else if (sourceType == Integer.class) {
          // from Integer
          if (targetType == int.class ||
              targetType == Integer.class) {
            result = sourceValue;
          } else if (targetType == long.class ||
                     targetType == Long.class) {
            result = ((Integer) sourceValue).longValue();
          } else if (targetType == boolean.class ||
                     targetType == Boolean.class) {
            result = false;
          } else if (targetType == double.class ||
                     targetType == Double.class) {
            result = ((Integer) sourceValue).doubleValue();
          }
        } else if (sourceType == Long.class) {
          // from Long
          if (targetType == int.class ||
              targetType == Integer.class) {
            result = ((Long) sourceValue).intValue();
          } else if (targetType == long.class ||
                     targetType == Long.class) {
            result = sourceValue;
          } else if (targetType == boolean.class ||
                     targetType == Boolean.class) {
            result = false;
          } else if (targetType == double.class ||
                     targetType == Double.class) {
            result = ((Long) sourceValue).doubleValue();
          }
        } else if (sourceType == Boolean.class) {
          // from Boolean
          if (targetType == int.class ||
              targetType == Integer.class ||
              targetType == long.class ||
              targetType == Long.class ||
              targetType == double.class ||
              targetType == Double.class) {
            throw new RuntimeException("Parsing error!");
          } else if (targetType == boolean.class ||
                     targetType == Boolean.class) {
            result = sourceValue;
          }
        } else if (sourceType == Double.class) {
          // from Double
          if (targetType == int.class ||
              targetType == Integer.class) {
            result = ((Double) sourceValue).intValue();
          } else if (targetType == long.class ||
                     targetType == Long.class) {
            result = ((Double) sourceValue).longValue();
          } else if (targetType == boolean.class ||
                     targetType == Boolean.class) {
            result = false;
          } else if (targetType == double.class ||
                     targetType == Double.class) {
            result = sourceValue;
          }
        }
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Source value '" + String.valueOf(sourceValue) + "' could not be adjusted to " + targetType.getName());  
    } else {
      if (targetType.isPrimitive()) {
        return (T) result;
      } else {
        return targetType.cast(result);
      }
    }
  }
  

}
