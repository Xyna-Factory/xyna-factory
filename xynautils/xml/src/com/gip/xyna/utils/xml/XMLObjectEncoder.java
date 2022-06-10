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
package com.gip.xyna.utils.xml;

import java.io.StringWriter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.apache.log4j.Logger;


public class XMLObjectEncoder {

  private static Logger logger = Logger.getLogger("xyna.utils.xml");

  /**
   * erstellt ein xml aus dem objekt, wobei jedes xmlelement einem bean-feld in der klasse des objekts
   * entspricht. es werden nur felder der klasse berücksichtigt, die auch dazugehörige gettermethoden
   * besitzen. <p>
   * es werden folgende getterarten unterstützt:
   * boolean a; => isA
   * int test => getTest
   * int _test => getTest
   * int Test => getTest
   * nicht primitive typen werden derzeit nur string, date und calendar direkt in string-werte
   * umgewandelt. date und calendar werden im format yyyy-MM-dd'T'HH:mm:ss'Z' ins xml geschrieben.<p>
   * gibt &lt;empty /&gt; zurück, falls o null ist.
   * 
   * beispiel: 
   * <code>
   class MyClass {
        private boolean b = true;
        public boolean getB() { return b; }
        private MyClass mc;
        public MyClass getMc() { return mc; }
        public void setMc(MyClass mc) {this.mc=mc; }
        public static void main(String[] args) throws Exception {
          MyClass my = new MyClass();
          my.setMc(new MyClass());
          System.out.println(XMLObjectEncoder.getObjectAsXMLString(my));
        }
      }
   * </code>
   * gibt aus:
   * {@code 
<MyClass>
   <Mc>
      <B>true</B>
   </Mc>
   <B>true</B>
</MyClass>
   * }
   * @param o
   * @return
   * @throws Exception
   */
  public static String getObjectAsXMLString(Object o) throws Exception {
    if (o==null) {
      return "<empty />";
    }
    XMLDocument xmldoc = new XMLDocument();
    String className = o.getClass() == null ? "unknownElement" : o.getClass().getSimpleName();
    XMLElement root = (XMLElement)xmldoc.createElement(className);
    xmldoc.appendChild(root);
    addObjectAsXML(o, root, 0);
    StringWriter sw = new StringWriter();
    xmldoc.print(sw);
    return sw.toString();
  }

  private static final String[] GETTERPREFIXES = new String[] { "get", "is" };
  private static final Class[] PRIMITIVE_TYPES =
    new Class[] { Integer.TYPE, Long.TYPE, Byte.TYPE, Float.TYPE, Double.TYPE, Boolean.TYPE,
      Short.TYPE };
  private static final Class[] PRIMITIVE_CLASSTYPES =
    new Class[] { Integer.class, Long.class, Byte.class, Float.class, Double.class, Boolean.class,
      Short.class };

  /**
   * Ändert einen String ab
   */
  private interface GetterVariation {
    public String vary(String baseName);
  }

  private static class VaryIdent implements GetterVariation {

    public String vary(String baseName) {
      return baseName;
    }
  }

  private static class VaryUnderScore implements GetterVariation {

    public String vary(String baseName) {
      return "_" + baseName;
    }
  }

  private static class VaryLowerCase implements GetterVariation {

    public String vary(String baseName) {
      if (baseName != null && baseName.length() > 0) {
        return baseName.substring(0, 1).toLowerCase() + baseName.substring(1);
      } else {
        return baseName;
      }
    }
  }
  
  /**
   * abwandlungen von strings, die bei propertys in beans vorkommen im vergleich zu ihren getter-namen
   */
  private static GetterVariation[] variations =
    new GetterVariation[] { new VaryLowerCase(), new VaryIdent(), new VaryUnderScore() };

  private static void addObjectAsXML(Object o, XMLElement parentNode, int depth) throws Exception {
    if (depth > 30) {
      logger.warn("While building XML from Object found a problem: Depth too high (>20). Propably a recursion in the object-structure");
      return;
    }
    if (o == null) {
      return;
    }
    for (Method m: o.getClass().getMethods()) {
      String methodName = m.getName();
      for (String getterPrefix: GETTERPREFIXES) {
        if (methodName.startsWith(getterPrefix)) {
          //check ob property existiert mit diesem namen
          String fieldName = methodName.substring(getterPrefix.length());
          Field f = null;
          //getField() zeigt nur public fields => rekursiv suchen
          for (GetterVariation getterVariation: variations) {
            f = findFieldByName(o.getClass(), getterVariation.vary(fieldName));
            if (f != null) {
              break;
            }
          }
          if (f == null) {
            break;
          }
          //found bean property
          Class c = f.getType();
          //   logger.debug(c.getName());
          if (!f.isAccessible()) {
            f.setAccessible(true);
          }
          Object got = f.get(o);
          if (got == null) {
            return;
          } else if (c.isArray()) {
            //array
            Class compClass = c.getComponentType();
            for (int i = 0; i < Array.getLength(got); i++) {
              Object comp = Array.get(got, i);
              if (comp == o) {
                //selbstreferenz ignorieren
              } else {
                addElementAndRecurse(compClass, comp, fieldName, parentNode, depth);
              }
            }
          } else {
            if (got == o) {
              //selbstreferenz ignorieren
            } else {
              addElementAndRecurse(c, got, fieldName, parentNode, depth);
            }
          }
          break; //nur ein prefix pro methodname
        }
      }
    }
  }

  private static Field findFieldByName(Class c, String fieldName) {
    Field[] fs = c.getDeclaredFields();
    for (Field fl: fs) {
      if (fl.getName().equals(fieldName)) {
        return fl;
      }
    }
    //nicht gefunden
    Class superClass = c.getSuperclass();
    if (superClass != null) {
      return findFieldByName(superClass, fieldName);
    }
    return null;
  }

  private static void addElementAndRecurse(Class c, Object o, String fieldName,
    XMLElement parentNode, int depth) throws Exception {
    if (o == null) {
        return;
    }
    XMLDocument xmldoc = parentNode.getDocument();
    XMLElement element = (XMLElement)xmldoc.createElement(fieldName);
    parentNode.appendChild(element);
    boolean found = false;
    for (Class primitive: PRIMITIVE_TYPES) {
      if (c == primitive) {
        element.setTextContent("" + o);
        found = true;
        break;
      }
    }
    if (found)
      return;
    found = false;
    for (Class primitiveCl: PRIMITIVE_CLASSTYPES) {
      if (c.isAssignableFrom(primitiveCl)) {
        element.setTextContent("" + o);
        found = true;
        break;
      }
    }
    if (found)
      return;
    if (c.isAssignableFrom(String.class)) {
      element.setTextContent("" + o);
    } else if (c.isAssignableFrom(Date.class)) {
      Date d = (Date)o;      
      element.setTextContent(getFormattedTime(d));
    } else if (c.isAssignableFrom(Calendar.class)) {
      Calendar d = (Calendar)o;
      element.setTextContent(getFormattedTime(d.getTime()));
    } else {
      addObjectAsXML(o, element, depth+1);
    }
  }
  
  private static String getFormattedTime(Date d) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf.format(d);
  }
  
}
