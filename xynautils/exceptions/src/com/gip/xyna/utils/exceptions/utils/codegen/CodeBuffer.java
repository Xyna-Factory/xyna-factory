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
package com.gip.xyna.utils.exceptions.utils.codegen;


import java.util.ArrayList;



public class CodeBuffer {

  private String header;

  private ArrayList<Part> parts = new ArrayList<Part>();


  private static class Part {

    public static final int LINE = 0;
    public static final int LB = 1;
    public static final int LINEPART = 2;
    public static final int LISTELEMENT = 3;
    private String s;
    private int type;


    public Part(String s, int type) {
      this.s = s;
      this.type = type;
    }


    public String getContent() {
      return s;
    }


    public int getType() {
      return type;
    }
  }


  public CodeBuffer(String departmentName) {
    setGIPSourceHeader(departmentName);
  }

  /**
   * erhöht intendation um 1, falls zeile mit { endet.
   * verringert intendation um 1, falls zeile mit } beginnt.
   * hängt semikolon an, falls zeile nicht mit einem der zeichen "//", "/*", "*" 
   * beginnt oder mit einem der zeichen "{", "}", ";", "," endet.
   * 
   * mehrere strings werden einfach verkettet, dann zeilenende. 
   *   verhält sich also genauso wie mehrfaches add(..) und dann addLB().
   * @param s
   * @return
   */
  public CodeBuffer addLine(String ... s) {
    if (s.length == 1) {
      parts.add(new Part(s[0], CodeBuffer.Part.LINE));
    } else {
      for (int i = 0; i<s.length; i++) {
        parts.add(new Part(s[i], CodeBuffer.Part.LINEPART));
      }
      addLB();
    }
    return this;
  }
  
  
  public CodeBuffer addLine(String s) {
    parts.add(new Part(s, CodeBuffer.Part.LINE));
    return this;
  }

  /**
   * linebreak adden
   */
  public CodeBuffer addLB() {
    parts.add(new Part(null, CodeBuffer.Part.LB));
    return this;
  }

  /**
   * verringert die intendation um 1, falls der string mit "}" beginnt, und dies entweder der erste methodenaufruf auf den codebuffer ist
   * oder der vorherige methodenaufruf einer mit zeilenumbruch (addLine, addLB) gewesen ist.
   * erhöht die intendation um 1, falls der string mit "{" endet und danach ein zeilenumbruch folgt.
   * fügt intendation nur an, falls dies entweder der erste methodenaufruf auf den codebuffer ist
   * oder der vorherige methodenaufruf einer mit zeilenumbruch (addLine, addLB) gewesen ist.
   * fügt semikolon nur an, falls die zeile nicht mit einem der zeichen "}", "{", ",", ";" endet und falls danach ein zeilenumbruch folgt
   * @param s
   * @return
   */
  public CodeBuffer add(String ... ss) {
    for (String s : ss) {
      parts.add(new Part(s, CodeBuffer.Part.LINEPART));
    }
    return this;
  }
  
  public CodeBuffer add(String s) {
    parts.add(new Part(s, CodeBuffer.Part.LINEPART));
    return this;
  }
  
  public CodeBuffer add(CodeBuffer cb) {
    for (Part p : cb.parts) {
      parts.add(p);
    }
    return this;
  }
  
  /**
   * fügt "<content of s escaped>" ein, d.h. es werden anführungszeichen um den string gewrappt, und alle sonderzeichen
   * im string behandelt, damit der entstehende code sinnvoll ist.
   */
  public CodeBuffer addString(String s) {
    add("\"");
    String t = s.replaceAll("\\n", "\\\\n");
    t = t.replaceAll("\"", "\\\\\"");
//    t = t.replaceAll("\\", "\\\\");
    add(t);
    add("\"");
    return this;
  }


  /**
   * mehrere addLB()
   * 
   * @param i
   * @return
   */
  public CodeBuffer addLB(int i) {
    for (int j = 0; j < i; j++) {
      parts.add(new Part(null, CodeBuffer.Part.LB));
    }
    return this;
  }


  /**
   * kommaseparierte listen erstellen.
   * 
   * fügt die intendation nur an, falls vorher ein zeilenumbruch hinzugefügt wurde (addLine, addLB).
   * fügt ein ", " an, falls der nächste codebufferpart wieder ein listenelement ist.
   * 
   * @param s
   */
  public CodeBuffer addListElement(String s) {
    parts.add(new Part(s, CodeBuffer.Part.LISTELEMENT));
    return this;
  }


  private static final String LB = System.getProperty("line.separator");

  public String toString() {
    return toString(true);
  }

  public String toString(boolean withHeader) {
    Part pNow = null;
    Part pBefore = null;
    Part pAfter = null;
    StringBuffer sb = new StringBuffer();
    if (withHeader) {
      if (header != null) {
        sb.append(header);
      }
    }
    Indentation intendation = new Indentation();
    for (int i = 0; i < parts.size(); i++) {
      pBefore = pNow;
      pNow = parts.get(i);
      if (i < parts.size() - 1) {
        pAfter = parts.get(i + 1);
      } else {
        pAfter = null;
      }

      switch (pNow.getType()) {
        case CodeBuffer.Part.LINE :
          if (pNow.getContent().startsWith("}")) {
            intendation.dec();
          }
          intendation.appendIndentation(sb);
          sb.append(pNow.getContent());
          if (!(pNow.getContent().endsWith("{") || pNow.getContent().endsWith("}") || pNow.getContent().endsWith(";")
              || pNow.getContent().endsWith(",") || pNow.getContent().trim().startsWith("//")
              || pNow.getContent().trim().startsWith("*") || pNow.getContent().trim().startsWith("/*") || pNow
              .getContent().trim().startsWith("@"))) {
            sb.append(";");
          } else {
            if (pNow.getContent().endsWith("{")) {
              intendation.inc();
            }
          }
          sb.append(LB);
          break;
        case CodeBuffer.Part.LB :
          sb.append(LB);
          break;
        case CodeBuffer.Part.LINEPART :
          if (pNow.getContent() != null
                          && pNow.getContent().startsWith("}")
                          && (pBefore == null || (pBefore.getType() != CodeBuffer.Part.LISTELEMENT && pBefore.getType() != CodeBuffer.Part.LINEPART))) {
            intendation.dec();
          }
          if (pBefore == null || pBefore.getType() == CodeBuffer.Part.LINE || pBefore.getType() == CodeBuffer.Part.LB) {
            intendation.appendIndentation(sb);
          }
          sb.append(pNow.getContent());
          if (pAfter != null
              && pAfter.getType() == CodeBuffer.Part.LB
              && pNow.getContent() != null
              && !(pNow.getContent().endsWith("{") || pNow.getContent().endsWith("}")
                  || pNow.getContent().endsWith(";") || pNow.getContent().endsWith(","))) {
            sb.append(";");
          } else if (pAfter != null && pAfter.getType() == CodeBuffer.Part.LB && pNow.getContent() != null && pNow.getContent().endsWith("{")) {
            intendation.inc();
          }

          break;
        case CodeBuffer.Part.LISTELEMENT :
          if (pBefore == null || pBefore.getType() == CodeBuffer.Part.LINE || pBefore.getType() == CodeBuffer.Part.LB) {
            intendation.appendIndentation(sb);
          }
          sb.append(pNow.getContent());
          if (pAfter != null && pAfter.getType() == CodeBuffer.Part.LISTELEMENT) {
            sb.append(", ");
          }
          break;
        default :
      }
    }
    return sb.toString();
  }


  public void setGIPSourceHeader(String departmentName) {
    header =   "/*\n"
             + " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n"
             + " * Copyright 2022 GIP SmartMercial GmbH, Germany\n"
             + " *\n"
             + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
             + " * you may not use this file except in compliance with the License.\n"
             + " * You may obtain a copy of the License at\n"
             + " *\n"
             + " *  http://www.apache.org/licenses/LICENSE-2.0\n"
             + " *\n"
             + " * Unless required by applicable law or agreed to in writing, software\n"
             + " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
             + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
             + " * See the License for the specific language governing permissions and\n"
             + " * limitations under the License.\n"
             + " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n"
             + " */\n";
  }

}
