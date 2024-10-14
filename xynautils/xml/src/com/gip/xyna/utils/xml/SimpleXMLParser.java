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
package com.gip.xyna.utils.xml;

import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Einfacher Parser zum Parsen eines XML-Texts
 * 
 * Aufruf:<br>
 * 
 * <pre>
 * {@code
 *  String xml = &quot;&lt;?xml version=\&quot;1.0\&quot; encoding=\&quot;UTF-8\&quot;?&gt;\n&quot;
 *  + &quot;&lt;testXML&gt;\n&quot;
 *  + &quot;  &lt;tag1&gt;wert1&lt;/tag1&gt;\n&quot;
 *  + &quot;  &lt;tag2&gt;&lt;/tag2&gt;\n&quot;
 *  + &quot;  &lt;tag3 /&gt;\n&quot;
 *  + &quot;  &lt;tag4 attribut=\&quot;test\&quot;&gt;wert4&lt;/tag4&gt; \n&quot;
 *  + &quot;&lt;/testXML&gt;\n&quot;;
 *  SimpleXMLParser sxp = new SimpleXMLParser();
 *  sxp.parse(xml);
 *  System.out.println( sxp.getNum() );
 *  System.out.println( sxp.getValue(1) );
 *  System.out.println( sxp.getTagData(&quot;tag1&quot;) );
 *  System.out.println( sxp.getTagData(&quot;tag2&quot;) );
 *  System.out.println( sxp.getTagData(&quot;tag3&quot;) );
 *  System.out.println( sxp.getKey(4) + &quot; -&gt; &quot; + sxp.getValue(4) );
 *  System.out.println( sxp.getValue(5) );
 * </pre>
 * 
 * Liefert:<br>
 * 
 * <pre>
 *  6
 *  testXML -&gt; OPEN
 *  wert1
 *  wert1
 * 
 *  null
 *  tag4 attribut=&quot;test&quot; -&gt; wert4
 *  /testXML -&gt; CLOSE
 * </pre>
 * 
 * @bug SimpleXMLParser kann nicht mit Kommentaren umgehen
 * @deprecated user com.gip.xyna.utils.xml.parser.SimpleXMLParser instead. Will
 *             be removed in a later release!
 */
public class SimpleXMLParser {

   private final static String OPENTAG = "OPEN";
   private final static String CLOSETAG = "CLOSE";

   private static Pattern patternAmp = Pattern.compile("&amp;");
   private static Pattern patternApos = Pattern.compile("&apos;");
   private static Pattern patternGt = Pattern.compile("&gt;");
   private static Pattern patternLt = Pattern.compile("&lt;");
   private static Pattern patternQuot = Pattern.compile("&quot;");

   private Vector keys = new Vector();
   private Vector values = new Vector();

   /**
    * Liefert die Anzahl gefundener Tags (maximal darf pos-1 an getKey bzw.
    * getValue übergeben werden)
    * 
    * @return Anzahl der gefundenen Tags
    */
   public int getNum() {
      return keys.size();
   }

   /**
    * liefert Key an Position pos
    * 
    * @param pos
    * @return Key an Position pos
    */
   public String getKey(int pos) {
      return (String) keys.get(pos);
   }

   /**
    * liefert Value an Position pos
    * 
    * @param pos
    * @return Value an Position pos
    */
   public String getValue(int pos) {
      return (String) values.get(pos);
   }

   /**
    * Sucht nach den Daten, die zum Tag gehören Achtung: nur das erste Datum
    * wird gefunden, weitere Tags mit gleichem Namen werden nicht gefunden
    * 
    * @param tagname
    * @return Daten des Tags tagname
    */
   public String getTagData(String tag) {
      // TODO besser HashMap bauen
      for (int i = 0; i < keys.size(); ++i) {
         if (tag.equals(keys.get(i))) {
            return (String) values.get(i);
         }
      }
      return null;
   }

   /**
    * Ist dieses Tag ein öffnendes Tag?
    * 
    * @param pos
    * @return true, wenn Tag öffnet
    */
   public boolean isOpenTag(int pos) {
      return values.get(pos) == OPENTAG; // == ist richtig!
   }

   /**
    * Ist dieses Tag einschließendes Tag?
    * 
    * @param pos
    * @return true, wenn Tag schließt
    */
   public boolean isCloseTag(int pos) {
      return values.get(pos) == CLOSETAG;// == ist richtig!
   }

   private void addKeyValue(String key, String value) {
      value = decodeString(value);
      keys.add(key);
      values.add(value);
      if (false) {
         do {
            if (value == OPENTAG) {
               System.out.println("OPEN " + key);
               break;
            }
            if (value == CLOSETAG) {
               System.out.println("CLOSE " + key);
               break;
            }
            System.out.println("TAG '" + key + "' DATA '" + value + "'");
         } while (false);
      }
   }

   /**
    * Dekodiert alle im XML nicht erlaubten Zeichen (Entity-Reference) &amp;
    * &gt; &lt; &apos; &quote;
    * 
    * @param string
    * @return neuer String mit ursprünglichen Zeichen
    */
   public String decodeString(String string) {
      if (string == null || string.length() == 0) {
         return string;
      }
      string = patternApos.matcher(string).replaceAll("'");
      string = patternGt.matcher(string).replaceAll(">");
      string = patternLt.matcher(string).replaceAll("<");
      string = patternQuot.matcher(string).replaceAll("\"");
      string = patternAmp.matcher(string).replaceAll("&");
      return string;
   }

   /**
    * Parst den XML-Text
    * 
    * @param xml
    *              XML-Text
    */
   public void parse(String xml) {
      // bisherige Daten verwerfen
      keys = new Vector();
      values = new Vector();

      Vector vector = new Vector();
      String[] arr = xml.split("[<>]");
      boolean lastEmpty = false;
      for (int i = 0; i < arr.length; ++i) {
         String str = arr[i].trim();
         if (str.length() == 0) {
            if (!lastEmpty) {
               vector.add(str);
            }
            lastEmpty = true;
         } else {
            vector.add(str);
            lastEmpty = false;
         }
         // System.out.println( arr[i] );
      }
      vector.add(""); // verhindert ArrayIndexOutOfBoundException, einfacher
      // Algorithmus
      int p = 0;
      for (; p < vector.size(); ++p) {
         String str = (String) vector.get(p);
         if (str.length() != 0 && !str.startsWith("?xml")) {
            break; // erstes sinnvolles Tag gefunden
         }
      }
      /*
       * for( int p2=p; p2<vector.size(); ++p2 ) { String tag =
       * (String)vector.get(p2); System.out.println( tag ); }
       */
      for (; p < vector.size(); ++p) {
         String tag = (String) vector.get(p);
         if (tag.length() == 0)
            continue; // uninteressante Zeile
         if (((String) vector.get(p + 1)).length() == 0) {
            // Tag ohne direkte Daten gefunden
            if (tag.startsWith("/")) {
               addKeyValue(tag, CLOSETAG);
            } else {
               if (tag.endsWith("/")) {
                  tag = tag.substring(0, tag.length() - 1).trim(); // " /"
                  // abschneiden
                  addKeyValue(tag, null);
               } else {
                  // Nun kann noch ein Leerstring enthalten sein, dann das
                  // schließende Tag
                  String closeTag = ((String) vector.get(p + 2));
                  if (closeTag.startsWith("/")
                        && tag.equals(closeTag.substring(1))) {
                     addKeyValue(tag, "");
                     p += 2;
                  } else {
                     // es ist nur öffnendes Tag
                     addKeyValue(tag, OPENTAG);
                  }
               }
            }
         } else {
            // jetzt sollten direkt Daten folgen, danach das schließende Tag
            String closeTag = ((String) vector.get(p + 2));
            if (!closeTag.startsWith("/")) {
               // kein CloseTag
               String test = ((String) vector.get(p + 1));
               if (test.length() != 0 && test.startsWith("/")
                     && tag.equals(test.substring(1))) {
                  addKeyValue(tag, "");
                  // CloseTag doch gefunden
                  p += 2;
               } else {
                  System.err.println("CloseTag erwartet!");
               }
            } else {
               if (!tag.equals(closeTag.substring(1))) {
                  System.err.println("CloseTag passt nicht zu OpenTag!");
               }
               // OK, einschliessendes Tag gefunden
               addKeyValue(tag, (String) vector.get(p + 1));
               p += 3;
            }
         }
      }
   }

   /**
    * @return Returns the keys.
    */
   // TODO: key vector should be private
   public Vector getKeys() {
      return keys;
   }

   /**
    * @param keys
    *              The keys to set.
    */
   public void setKeys(Vector keys) {
      this.keys = keys;
   }

   /**
    * @return Returns the values.
    */
   // TODO: value vector should be private
   public Vector getValues() {
      return values;
   }

   /**
    * @param values
    *              The values to set.
    */
   public void setValues(Vector values) {
      this.values = values;
   }

}