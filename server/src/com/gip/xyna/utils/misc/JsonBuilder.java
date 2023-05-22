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
package com.gip.xyna.utils.misc;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.gip.xyna.utils.ByteUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;

public class JsonBuilder {

  private static final String PROPERTY_KEY_WHITESPACES = "debug.whitespaceInJsons";

  public static final XynaPropertyBoolean WHITESPACE_IN_JSONS = new XynaPropertyBoolean(PROPERTY_KEY_WHITESPACES, false).
      setDefaultDocumentation(DocumentationLanguage.DE, "Legt fest, ob in den aus XMOMs generierten JSONs Whitespace enthalten sein soll.").
      setDefaultDocumentation(DocumentationLanguage.EN, "Determins whether JSON that are generated from XMOMs should contain whitespace.");


  private LinkedList<StackEntry> context = new LinkedList<>();
  private StackEntry current;
  private boolean nextObjectAsAttribute;
  
  private Writer writer;
  
  private boolean whitespaceInJsons = false;
  
  
  public JsonBuilder() {
    this(new StringWriter());
  }
  
  public JsonBuilder(Writer writer) {

    whitespaceInJsons = WHITESPACE_IN_JSONS.get();

    this.writer = writer;
    current = new StackEntry(0, whitespaceInJsons);
    context.addLast(current);
  }
  
  @Override
  public String toString() {
    if(writer instanceof StringWriter) {
      return ((StringWriter)writer).toString();
    }
    throw new UnsupportedOperationException("toString not possible when used with writer");
  }
  
  public void nextObjectAsAttribute(String label) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\"").append(whitespaceInJsons ? " :" : ":");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    nextObjectAsAttribute = true;
  }
  
  public void startObject() {
    try {
      if( nextObjectAsAttribute ) {
        nextObjectAsAttribute = false;
      } else {
        current.nextObject(writer);
      }
      writer.append('{');
      current = new StackEntry(current.indent+1, whitespaceInJsons);
      context.addLast(current);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void endObject() {
    context.pollLast();
    current = context.getLast();
    try {
      current.endLine(writer);
      current.indent(writer);
      writer.append('}');
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void addNullObject() {
    try {
      if( nextObjectAsAttribute ) {
        nextObjectAsAttribute = false;
      } else {
        current.nextObject(writer);
      }
      writer.append("null");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void addNullAttribute(String label) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " null" : "null");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addStringAttribute(String label, String value) {
    if( value == null ) {
      addNullAttribute(label);
    } else {
      try {
        current.nextAttribute(writer);
        writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " \"" : "\"");
        encode(value);
        writer.append("\"");
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

  private void encode(String string) {
    try {
      for (int i = 0; i < string.length(); i++) {
        writer.append( encodeChar(string.charAt(i) ) );
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  private String encodeChar(char c) {
    switch (c) {
    case '\\' : return "\\\\";
    case '"' :  return "\\\"";
    case '\b' : return "\\b";
    case '\f' : return "\\f";
    case '\n' : return "\\n";
    case '\r' : return "\\r";
    case '\t' : return "\\t";
    default :
      if ((int) c < 32) {
        return "\\u00" + ByteUtils.toHexString((byte) c, false);
      } else {
        /*
         * andere unicode charaktere zu escapen ist optional: http://www.ietf.org/rfc/rfc4627.txt

           char = unescaped /
                  escape (
                      %x22 /          ; "    quotation mark  U+0022
                      %x5C /          ; \    reverse solidus U+005C
                      %x2F /          ; /    solidus         U+002F
                      %x62 /          ; b    backspace       U+0008
                      %x66 /          ; f    form feed       U+000C
                      %x6E /          ; n    line feed       U+000A
                      %x72 /          ; r    carriage return U+000D
                      %x74 /          ; t    tab             U+0009
                      %x75 4HEXDIG )  ; uXXXX                U+XXXX

           escape = %x5C              ; \

           quotation-mark = %x22      ; "

           unescaped = %x20-21 / %x23-5B / %x5D-10FFFF
         */
        return String.valueOf(c);
      }
    }
  }

  public void addOptionalStringAttribute(String label, String value) {
    if( value != null ) {
      addStringAttribute(label, value);
    }
  }

  public void addIntegerAttribute(String label, Integer value) {
    addNumberAttribute(label, value);
  }
  public void addOptionalIntegerAttribute(String label, Integer value) {
    addOptionalNumberAttribute(label, value);
  }
  
  public void addNumberAttribute(String label, Number value) {
    current.nextAttribute(writer);
    try {
      writer.append("\"").append(label).append("\"").append(whitespaceInJsons ? ": " : ":").append(value.toString());
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  public void addOptionalNumberAttribute(String label, Number value) {
    if( value != null ) {
      addNumberAttribute(label, value);
    }
  }

  public <E extends Enum<E>> void addEnumAttribute(String label, Enum<E> enumValue) {
    current.nextAttribute(writer);
    try {
      writer.append("\"").append(label).append("\"").append(whitespaceInJsons ? ": \"" : ":\"").append(enumValue.toString()).append("\"");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  public <E extends Enum<E>> void addOptionalEnumAttribute(String label, Enum<E> enumValue) {
    if( enumValue != null ) {
      addEnumAttribute(label, enumValue);
    }
  }
  
  public void addBooleanAttribute(String label, boolean value) {
    current.nextAttribute(writer);
    try {
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " " : "").append(value ? "true" : "false");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addOptionalBooleanAttribute(String label, boolean value) {
    if( value ) {
      addBooleanAttribute(label, value);
    }
  }
  
  public void addObjectAttribute(String label, JsonSerializable value) {
    addObjectAttribute(label);{
      value.toJson(this);
    }endObject();
  }
  
  public void addOptionalObjectAttribute(String label, JsonSerializable value) {
    if( value != null ) {
      addObjectAttribute(label, value);
    }
  }
  
  public void addAttribute(String label) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " " : "");
      current = new StackEntry(current.indent+1, whitespaceInJsons);
      context.add(current);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void addObjectAttribute(String label) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " {" : "{");
      current = new StackEntry(current.indent+1, whitespaceInJsons);
      context.add(current);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addListAttribute(String label) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " [" : "[");
      current = new StackEntry(current.indent+1, whitespaceInJsons);
      context.add(current);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addStringListAttribute(String label, List<String> list) {
    try {
      current.nextAttribute(writer);
      writer.append("\"").append(label).append("\":").append(whitespaceInJsons ? " [" : "[");
      String sep = "";
      for( String v : list ) {
        writer.append(sep).append("\"");
        encode(v);
        writer.append("\"");
        if(whitespaceInJsons) {
          sep = ", ";
        } else {
          sep = ",";
        }
      }
      writer.append("]");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addObjectListAttribute(String listLabel, Collection<? extends JsonSerializable> jsonObjects) {
    addListAttribute(listLabel);{
      for( JsonSerializable js : jsonObjects ) {
        startObject();{
          js.toJson(this);
        }endObject();
      }
    }endList();
  }
  public void addOptionalObjectListAttribute(String listLabel, Collection<? extends JsonSerializable> jsonObjects) {
    if( jsonObjects != null ) {
      addObjectListAttribute(listLabel, jsonObjects);
    }
  }
  
  public void startList() {
    if (nextObjectAsAttribute) {
      nextObjectAsAttribute = false;
    }
    try {
      writer.append("[");
      current = new StackEntry(current.indent+1, whitespaceInJsons);
      context.add(current);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void endList() {
    context.pollLast();
    current = context.getLast();
    try { 
      current.endLine(writer);
      current.indent(writer);
      writer.append("]");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * [,]"&lt;value&gt;" oder null
   * @param value
   */
  public void addStringListElement(String value) {
    current.nextElement(writer);
    try {
      if (value == null) {
        writer.append("null");
      } else {
        writer.append("\"");
        encode(value);
        writer.append("\"");
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  /**
   * [,]&lt;value&gt; 
   * value ist entweder
   * "true", "false", "null", "&lt;zahl&gt;"
   * @param value
   */
  public void addPrimitiveListElement(String value) {
    if (value == null) {
      throw new RuntimeException();
    }
    current.nextElement(writer);
    try {
      writer.append(value);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void addObjectListElement(JsonSerializable jsonObject) {
    startObject();{
      jsonObject.toJson(this);
    }endObject();
  }
  
  private static class StackEntry {
    private final int indent;
    private boolean first;
    private boolean whitespaceInJsons;
    
    public StackEntry(int indent, boolean whitespaceInJsons) {
      this.indent = indent;
      this.first = true;
      this.whitespaceInJsons = whitespaceInJsons;
    }
    

    public void nextObject(Writer writer) {
      try {
        if( first ) {
          if(whitespaceInJsons) {
            writer.append('\n');
          }
          first = false;
        } else {
          writer.append(",").append(whitespaceInJsons ? "\n" : "");
        }
        indent(writer);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    @Override
    public String toString() {
      return "StackEntry("+indent+")";
    }
    
    public void nextAttribute(Writer writer) {
      try {
        if( first ) {
          if(whitespaceInJsons) {
            writer.append('\n');
          }
          first = false;
        } else {
            writer.append(',').append(whitespaceInJsons ? "\n" : "");
        }
        indent(writer);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    
    public void nextElement(Writer writer) {
      try {
        if( first ) {
          if(whitespaceInJsons) {
            writer.append('\n');
          }
          first = false;
        } else {
            writer.append(',').append(whitespaceInJsons ? "\n" : "");
        }
        indent(writer);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    
    public void endLine(Writer writer) {
      if(whitespaceInJsons) {
        try {
          writer.append("\n");
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    }
    
    public void indent(Writer writer) throws IOException {
      if(whitespaceInJsons) {
        for( int i=0; i<indent; ++i ) {
          writer.append("  ");
        }
      }
    }    
  }
  
}
