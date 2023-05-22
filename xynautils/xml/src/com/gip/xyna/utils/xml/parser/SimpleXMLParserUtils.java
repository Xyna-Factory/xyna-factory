/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.xml.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleXMLParserUtils
 */
public class SimpleXMLParserUtils {

   public static class Range {
      int open = 0;
      int close = 0;

      public Range() {
      }

      /**
       * Setzt den Range so, dass alle Tags von sxp enthalten sind
       * 
       * @param sxp
       */
      public Range(SimpleXMLParser sxp) {
         close = sxp.getNum();
      }

      public Range(int open, int close) {
         this.open = open;
         this.close = close;
      }

      /**
       * @return Returns the open.
       */
      public int getOpen() {
         return open;
      }

      /**
       * @param open
       *              The open to set.
       */
      public void setOpen(int open) {
         this.open = open;
      }

      /**
       * @return Returns the close.
       */
      public int getClose() {
         return close;
      }

      /**
       * @param close
       *              The close to set.
       */
      public void setClose(int close) {
         this.close = close;
      }
   }

   /**
    * Liefert die Values zu allen Tags tagname, die sich zwischen den Positionen
    * first und last befinden
    * 
    * @param xml
    * @param tagname
    * @param first
    * @param last
    * @return
    */
   public static String[] extractStringArray(SimpleXMLParser xml,
         String tagname, int first, int last) {
      ArrayList<String> list = new ArrayList<String>();
      for (int pos = first; pos < last; ++pos) {
         if (xml.getKey(pos).equals(tagname)) {
            list.add(xml.getValue(pos));
         }
      }
      if (list.size() == 0) {
         return null; // keine Daten gefunden
      }
      return (String[]) list.toArray(new String[0]);
   }

   /**
    * Liefert die Values zu allen Tags tagname, die sich im Range range befinden
    * 
    * @param xml
    * @param tagname
    * @param range
    * @return
    */
   public static String[] extractStringArray(SimpleXMLParser xml,
         String tagname, Range range) {
      if (range == null) {
         return null; // kein Range, daher gibt es keine Daten
      }
      return extractStringArray(xml, tagname, range.getOpen(), range.getClose());
   }

   /**
    * Liefert die Values zu allen Tags tagname
    * 
    * @param xml
    * @param tagname
    * @return
    */
   public static String[] extractStringArray(SimpleXMLParser xml, String tagname) {
      return extractStringArray(xml, tagname, new Range(xml));
   }

   /**
    * Liefert die Values zu allen Tags tagname, die sich zwischen dem
    * einschlie�enden Tag between befinden Bei mehreren Tags between ist das
    * Ergebnis undefiniert
    * 
    * @param xml
    * @param tagname
    * @param between
    * @return
    */
   // public static String[] extractStringArray(SimpleXMLParser xml, String
   // tagname, String between ) {
   // Range range = findRange( xml, between, 0 );
   // return extractStringArray( xml, tagname, range );
   // }
   /**
    * gibt Position des letzten Tags an
    * 
    * @param xmlRequest
    * @param tagname
    * @return
    */
   public static int lastTag(SimpleXMLParser xmlRequest, String tagname) {
      for (int pos = xmlRequest.getNum() - 1; pos >= 0; --pos) {
         if (xmlRequest.getKey(pos).equals(tagname)) {
            return pos;
         }
      }
      return 0; // nicht gefunden
   }

   /**
    * gibt Position des ersten Tags an,
    * falls das Tag nicht gefunden wird, xmlRequest.getNum()
    * 
    * @param xmlRequest
    * @param tagname
    * @return
    */
   public static int firstTag(SimpleXMLParser xmlRequest, String tagname) {
      for (int pos = 0; pos < xmlRequest.getNum(); ++pos) {
         if (xmlRequest.getKey(pos).equals(tagname)) {
            return pos;
         }
      }
      return xmlRequest.getNum(); // nicht gefunden
   }

   /**
    * @param tagData
    * @return
    */
   public static boolean isEmpty(String tagData) {
      if (tagData == null)
         return true;
      if (tagData.length() == 0)
         return true;
      return false;
   }

   /**
    * Sucht das erste Intervall, das von dem Tag tagName begrenzt wird
    * 
    * @param xml
    * @param tagName
    * @param range
    * @return
    */
   public static Range findSubRange(SimpleXMLParser xml, String tagName,
         Range range) {
      Range subrange = findRange(xml, tagName, range.getOpen());
      if (subrange != null && subrange.getClose() > range.getClose()) {
         subrange = null;
      }
      return subrange;
   }

   /**
    * Sucht das erste Intervall ab startPos, das von tagName als �ffnendes und
    * schlie�endes Tag begrenzt wird
    * 
    * @param xml
    * @param tagName
    * @param startPos
    * @return Range oder null, wenn Range nicht existiert
    */
   public static Range findRange(SimpleXMLParser xml, String tagName,
         int startPos) {
      int open = startPos;
      for (; open < xml.getNum(); ++open) {
         if (xml.isOpenTag(open) && xml.getKey(open).equals(tagName)) {
            break;
         }
      }
      int close = open + 1;
      for (; close < xml.getNum(); ++close) {
         if (xml.isCloseTag(close) && xml.getKey(close).equals("/" + tagName)) {
            break;
         }
      }
      if (close >= xml.getNum()) {
         return null;
      }
      return new Range(open, close);
   }

   /**
    * Sucht den Value zu tagName im Intervall Range
    * 
    * @param xml
    * @param tagName
    * @param range
    * @return
    */
   public static String findTagInRange(SimpleXMLParser xml, String tagName,
         Range range) {
      for (int pos = range.getOpen() + 1; pos < range.getClose(); ++pos) {
         if (xml.getKey(pos).equals(tagName)) {
            return xml.getValue(pos);
         }
      }
      return null; // nicht gefunden
   }

   /**
    * Sucht die Values zu den Tags aus tags in dieser Reihenfolge im Intervall
    * range
    * 
    * @param parser
    * @param tags
    * @param range
    * @return
    */
   public static List<String> findTagsInRange(SimpleXMLParser parser,
         List<String> tags, Range range) {
      List<String> values = new ArrayList<String>();
      for (int t = 0; t < tags.size(); ++t) {
         values.add(findTagInRange(parser, (String)tags.get(t), range));
      }
      return values;
   }

   /**
    * Speichert in einem 2d-Array die Values zu den Tags aus tags aus jedem
    * Intervall, das von tagName begrenzt wird
    * 
    * @param xml
    * @param tagName
    * @param tags
    * @return
    */
   public static List<List<String>> xmlToArray(SimpleXMLParser xml,
         String tagName, List<String> tags) {
      List<List<String>> array = new ArrayList<List<String>>();
      Range range = new Range();
      range.setClose(0);
      while (range != null) {
         range = findRange(xml, tagName, range.getClose());
         if (range != null) {
            // neuer Range gefunden
            // System.err.println( "range: " + range.getOpen() + "-" +
            // range.getClose() );
            array.add(findTagsInRange(xml, tags, range));
         }
      }
      return array;
   }

   /**
    * Sucht die Position des Tags mit Wert value
    * 
    * @param xml
    * @param value
    * @param range
    * @return Position des Tags, -1 bei Nichtexistenz
    */
   public static int findPosForValueInRange(SimpleXMLParser xml, String value,
         Range range) {
      for (int pos = range.getOpen() + 1; pos < range.getClose(); ++pos) {
         if (xml.getValue(pos).equals(value)) {
            return pos;
         }
      }
      return -1;
   }

}
