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
package com.gip.xyna.utils.xynatree;

import com.gip.xyna.utils.xml.schema.XynaSchemaNode;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Klasse für Bäume von Objekten
 */
public class XynaNode implements JSONObject, XMLObject {

   private Object value;
   private String name;
   private Vector<XynaNode> children = new Vector<XynaNode>();
   private Hashtable<String, String> attributes = new Hashtable<String, String>();

   public XynaNode(String name) {
      this.name = name;
   }

   /**
    * Setzt den Wert.<br>
    * falls das Objekt JSONObject implementiert, wird bei der JSON-Erzeugung die
    * toJSON() Methode des Objektes aufgerufen
    * 
    * @param value
    */
   public void setValue(Object value) {
      this.value = value;
   }

   public Object getValue() {
      return value;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void appendChild(XynaNode child) throws Exception {
      children.add(child);
   }

   public void removeChild(XynaNode child) throws Exception {
      if (children.indexOf(child) > -1) {
         children.remove(child);
      } else {
         throw new Exception("Knoten nicht gefunden.");
      }
   }

   public void insertChild(XynaNode newChild, XynaNode refChild)
         throws Exception {
      if (children.indexOf(refChild) > -1) {
         children.insertElementAt(newChild, children.indexOf(refChild));
      } else {
         throw new Exception("Knoten " + refChild.getName() + "nicht gefunden.");
      }
   }

   /**
    * value = null => attribut wird gelöscht
    * 
    * @param name
    * @param value
    */
   public void setAttribute(String name, String value) {
      if (value == null) {
         attributes.remove(name);
      } else {
         attributes.put(name, value);
      }
   }

   public String getAttribute(String name) {
      return attributes.get(name);
   }

   public Hashtable<String, String> getAttributes() {
      return attributes;
   }

   public XynaNode[] getChildren() throws Exception {
      return (XynaNode[]) children.toArray(new XynaNode[] {});
   }

   /**
    * Erzeugt im JSON-Format einen String der den Baum beschreibt, der zu dieser
    * Node gehört (Kurz!). Ist ein Feld nicht gesetzt, wird "null" in den
    * JSON-String geschrieben. <br>
    * Struktur: <br> { &lt;name&gt; : &lt;value&gt;, <br>
    * &lt;attributkey1&gt; : &lt;attributvalue1&gt;, <br>
    * &lt;attributkey2&gt; : &lt;attributvalue2&gt;, <br>
    * children : [ {...}, <br>
    * {...} ] <br> }
    * 
    * @return JSON-String
    * @throws Exception
    */
   public String toJSON() throws Exception {
      String json = "{ " + getName() + " : ";
      if (getValue() instanceof JSONObject) {
         json += ((JSONObject) getValue()).toJSON();
      } else {
         json += getValue();
      }
      if (getAttributes().keySet().size() > 0) {
         for (Iterator<String> it = getAttributes().keySet().iterator(); it
               .hasNext();) {
            json += ", " + CR;
            String key = it.next();
            json += key + " : " + getAttribute(key);
         }
      }
      if (getChildren().length > 0) {
         json += ", " + CR;
         json += "children : [ " + CR;
         XynaNode[] children = getChildren();
         for (int i = 0; i < children.length; i++) {
            if (i > 0) {
               json += ", " + CR;
            }
            json += children[i].toJSON();
         }
         json += CR + "]";
      }
      json += CR + "}";
      return json;
   }

   /**
    * Erzeugt im JSON-Format einen String der den Baum beschreibt, der zu dieser
    * Node gehört (Ausführliche 1:1 Abbildung der Felder dieser Klasse). Ist ein
    * Feld nicht gesetzt, wird "null" in den JSON-String geschrieben. <br>
    * Struktur: <br> { name : &lt;name&gt;, <br>
    * value : &lt;value&gt;, <br>
    * &lt;attributkey1&gt; : &lt;attributvalue1&gt;, <br>
    * &lt;attributkey2&gt; : &lt;attributvalue2&gt;, <br>
    * children : [ {...}, <br>
    * {...} ] <br> }
    * 
    * @return JSON-String
    * @throws Exception
    */
   public String toJSONExtended() throws Exception {
      String json = "{ name : " + getName() + ", " + CR;
      json += "value : ";
      if (getValue() instanceof JSONObject) {
         json += ((JSONObject) getValue()).toJSON();
      } else {
         json += getValue();
      }
      json += ", " + CR + " attributes : { ";
      if (getAttributes().keySet().size() > 0) {
         int i = 0;
         for (Iterator<String> it = getAttributes().keySet().iterator(); it
               .hasNext();) {
            if (i > 0) {
               json += ", " + CR;
            }
            i++;
            String key = it.next();
            json += key + " : " + getAttribute(key);
         }
      }
      json += CR + "}";
      if (getChildren().length > 0) {
         json += ", " + CR;
         json += "children : [ " + CR;
         XynaNode[] children = getChildren();
         for (int i = 0; i < children.length; i++) {
            if (i > 0) {
               json += ", " + CR;
            }
            json += children[i].toJSONExtended();
         }
         json += CR + "]";
      }
      json += CR + "}";
      return json;
   }

   public String toXML() throws Exception {
      String xml = "<" + name;
      if (getAttributes().keySet().size() > 0) {
         int i = 0;
         for (Iterator<String> it = getAttributes().keySet().iterator(); it
               .hasNext();) {
            i++;
            String key = it.next();
            xml += " " + key + "=\"" + getAttribute(key) + "\"";
         }
      }
      xml += ">" + CR;
      if (getValue() != null) {
         if (getValue() instanceof XMLObject) {
            xml += ((XMLObject) getValue()).toXML();
         } else {
            xml += getValue();
         }
         xml += CR;
      }
      for (int i = 0; i < getChildren().length; i++) {
         xml += getChildren()[i].toXML() + CR;
      }
      xml += "</" + name + ">";
      return xml;
   }

   public String getType() throws Exception {
      if (value != null) {
         if (value instanceof XMLObject) {
            return ((XMLObject) value).getType();
         }
         String classname = getValue().getClass().getName();
         // TODO ggfs hier erweitern für beliebige objekte mittels
         // reflection?!
         return XynaSchemaNode.getXSDTypeOfClassname(classname);
      }
      return XynaSchemaNode.XSD_STRING;
   }
}
