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
package com.gip.xyna.utils.misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException.Cause;

public class JsonParser {
  
  public interface JsonVisitor<T> {

    public enum Type { Null, String, Boolean, Number, Object, Mixed /*Achtung, wenn eine Liste Mixed ist, dann ist es nicht mehr möglich pro Element die Typinformation wiederherzustellen*/ }
    
    void currentPosition(Position position);

    JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException;

    void attribute(String label, String value, Type type) throws UnexpectedJSONContentException;
    
    void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException;
    
    void object(String label, Object value) throws UnexpectedJSONContentException;
    
    void objectList(String label, List<Object> values) throws UnexpectedJSONContentException;
    
    void emptyList(String label) throws UnexpectedJSONContentException;
    
    T get();
    
    T getAndReset();

  }
  
  public static abstract class EmptyJsonVisitor<T> implements JsonVisitor<T> {

    @Override
    public void currentPosition(Position position) {
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    
    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    @Override
    public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    @Override
    public void object(String label, Object value) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
      throw newUnexpectedJSONContentException(label);
    }
    
    protected UnexpectedJSONContentException newUnexpectedJSONContentException(String label) {
      UnexpectedJSONContentException ujce = new UnexpectedJSONContentException(label);
      ujce.setClass( getClass() );
      return ujce;
    }

  }
   
  public interface JsonTokenVisitor {

    void currentPosition(Position position);
    
    void curlyBraceOpen() throws UnexpectedJSONContentException;

    void curlyBraceClose() throws UnexpectedJSONContentException;

    void squareBracketOpen();

    void squareBracketClose() throws UnexpectedJSONContentException;

    void comma();

    void colon();
    
    void booleanValue(boolean value) throws UnexpectedJSONContentException;

    void nullValue() throws UnexpectedJSONContentException;

    void numberValue(String value) throws UnexpectedJSONContentException;

    void stringValue(String value) throws UnexpectedJSONContentException;
    
  }
  
  public static class Position implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String json;
    private int line;
    private int pos;
    private int index;

    public Position(String json) {
      this.json = json;
      this.line = 0;
      this.pos = 0;
      this.index = -1;
    }

    @Override
    public String toString() {
      return "Position("+line+","+pos+","+index+")";
    }
    
    public int getIndex() {
      return index;
    }
    
    public int getPos() {
      return pos;
    }
    
    public int getLine() {
      return line;
    }

    protected void incrementLine() {
      line ++;
      pos = 0;
    }

    protected void increment(int inc) {
      pos += inc;
      index += inc;
    }
    
    public String getJsonSubstring(int beginIndex, int endIndex) {
      return json.substring(beginIndex, endIndex);
    }
  }
  
  public static class InvalidJSONException extends Exception {
    private static final long serialVersionUID = 1L;

    private Position position;

    public InvalidJSONException(Position position, String message) {
      super(message + " at "+position);
      this.position = position;
    }
    
    public InvalidJSONException(String message) {
      super(message);
    }

    public Position getPosition() {
      return position;
    }

  }

  public static class UnexpectedJSONContentException extends Exception {
    private static final long serialVersionUID = 1L;

    private Position position;
    private String label;
    private Class<?> throwingClass;
    
    public static enum Cause {
      nullValue("null");
      
      private String message;

      private Cause(String message) {
        this.message = message;
      }
      
      public String getMessage() {
        return message;
      }
      
    }

    public UnexpectedJSONContentException(String label) {
      super("Unexpected label \""+label+"\"");
      this.label = label;
    }

    public void setClass(Class<?> throwingClass) {
      this.throwingClass = throwingClass;
    }

    public UnexpectedJSONContentException(String label, Throwable cause) {
      super("Unexpected value for label \""+label+"\"", cause);
      this.label = label;
    }

    public UnexpectedJSONContentException(String label, String expected) {
      super("Unexpected value (expected: \""+expected+"\") for label \""+label+"\"" );
      this.label = label;
    }

    public UnexpectedJSONContentException(String label, Cause cause) {
      super("Unexpected value ("+cause.getMessage()+") for label \""+label+"\"");
      this.label = label;
    }

    public Position getPosition() {
      return position;
    }

    @Override
    public String getMessage() {
      if( throwingClass == null ) {
        return super.getMessage() +" at "+position;
      } else {
        return super.getMessage() +" at "+position+" in "+throwingClass.getName();
      }
    }

    public void setPosition(Position position) {
      this.position = position;
    }
    
    public String getLabel() {
      return label;
    }
    
  }

  private boolean validate;
  private String json;
  private int length;
  private Position position;
  
  public JsonParser() {
    this(true);
  }
  
  public JsonParser(boolean validate) {
    this.validate = validate;
  }
  
  public <T> T parse( String json, JsonVisitor<T> visitor ) throws InvalidJSONException, UnexpectedJSONContentException {
    parse(json, new JsonTokenVisitorStackImpl(visitor) );
    return visitor.get();
  }
  
  public void parse( String json, JsonTokenVisitor visitor ) throws InvalidJSONException, UnexpectedJSONContentException {
    this.json  = json;
    this.length = json.length();
    this.position = new Position(json);
    visitor.currentPosition(position);
    try {
      tokenize(visitor);
    } catch( UnexpectedJSONContentException e ) {
      e.setPosition(position);
      throw e;
    }
  }
  
  
  
  
  private void tokenize( JsonTokenVisitor visitor) throws InvalidJSONException, UnexpectedJSONContentException {
    int c = -1;
    while ( (c = readNextChar() ) != -1 ) {
      switch (c) {
        case '\"' :
          String string = readString();
          visitor.stringValue(string);
          break;
        case '\n':
          incrementLine();
          break;
        case '{' :
          visitor.curlyBraceOpen();
          break;
        case '}' :
          visitor.curlyBraceClose();
          break;
        case '[' :
          visitor.squareBracketOpen();
          break;
        case ']' :
          visitor.squareBracketClose();
          break;
        case ',' :
          visitor.comma();
          break;
        case ':' :
          visitor.colon();
          break;
        case '0' :
        case '1' :
        case '2' :
        case '3' :
        case '4' :
        case '5' :
        case '6' :
        case '7' :
        case '8' :
        case '9' :
        case '-' :
          String number = readNumber();
          visitor.numberValue(number);
          break;
        case 't' :
          readValue("true");
          visitor.booleanValue(true);
          break;
        case 'f' :
          readValue("false");
          visitor.booleanValue(false);
          break;
        case 'n' :
          readValue("null");
          visitor.nullValue();
          break;
        case -1:
          return;
        default :
          //ignore. zeilenumbrüche, whitespaces, etc
          if (validate) {
            if (!Character.isWhitespace(c)) {
              throw new InvalidJSONException(position, "Unexpected character \'"+c+"\'");
            }
          }
      }
    }
  }


  private void readValue(String value) throws InvalidJSONException {
    int len = value.length();
    if (validate) {
      String found = json.substring(position.getIndex(), position.getIndex() + len);
      if (! found.equals(value)) {
        throw new InvalidJSONException(position, "Expected \""+value+"\", found \""+ found+"\"");
      }
    }
    incrementPos(len-1);
  }

  private void incrementLine() {
    position.incrementLine();
  }

  private void incrementPos(int inc) {
    position.increment(inc);
  }
  
  private int readNextChar() {
    position.increment(1);
    if( position.getIndex()<length ) {
      return json.charAt(position.getIndex());
    } else {
      return -1;
    }
  }

  private String readNumber() throws InvalidJSONException {
    StringBuilder sb = new StringBuilder();
    sb.append( json.charAt(position.getIndex() ));
    boolean end = false;
    int c = -1;
    while (! end && (c=readNextChar() ) != -1 ) {
      switch (c) {
        case ' ' :
        case ',' :
        case '}' :
        case ']' :
        case '\n' :
        case '\r' :
        case '\t' :
        case '\b' :
          incrementPos(-1);
          end = true;
          break;
        default :
         sb.append((char)c);
      }
    }
    if( validate ) {
      try {
        @SuppressWarnings("unused")
        Double d = Double.valueOf(sb.toString());
      } catch (NumberFormatException e) {
        throw new InvalidJSONException(position, "Text \""+sb.toString()+"\" could not be parsed as a number.");
      }
    }
    return sb.toString();
  }


  private String readString() throws InvalidJSONException {
    StringBuilder sb = new StringBuilder();
    int c = -1;
    while ( (c = readNextChar() ) != -1 ) {
      switch (c) {
        case '\n':
          sb.append('\n');
          incrementLine();
        case '\\' :
          //escaping
          sb.append( readEscaped() );
          break;
        case '\"' :
          return sb.toString();
        default :
          sb.append((char)c);
      }
    }
    return sb.toString();
  }


  private char readEscaped() throws InvalidJSONException {
    int c = readNextChar();
    switch (c) {
      case '"' :
        return '"';
      case '\\' :
        return '\\';
      case 'n' :
        return '\n';
      case 'r' :
        return '\r';
      case 't' :
        return '\t';
      case '/' :
        return '/';
      case 'b' :
        return '\b';
      case 'f' :
        return '\f';
      case 'u' : //unicode
        incrementPos(1);
        String hex = json.substring(position.getIndex(), position.getIndex()+4);
        try {
          int n = Integer.parseInt(hex, 16);
          incrementPos(3);
          return ((char)n);
        } catch( NumberFormatException e ) {
          if( validate ) {
            throw new InvalidJSONException(position, "Expected unicode code point (4 hexadecimal digits)");
          } else {
            return '?';
          }
        }
      default :
        if (validate) {
          throw new InvalidJSONException(position,
                                         "Backslash may not be used in strings except to escape a fixed set of characters.");
        }
        return '?';
    }
  }

  private static class JsonTokenVisitorStackImpl implements JsonTokenVisitor {
    
    private JsonTokenVisitorImpl current;
    private Position position;
    private Stack<JsonTokenVisitorImpl> stack;
    
    public JsonTokenVisitorStackImpl(JsonVisitor<?> visitor) {
      current = new JsonTokenVisitorImpl(visitor);
      stack = new Stack<JsonTokenVisitorImpl>();
    }
   
    @Override
    public void currentPosition(Position position) {
      this.position = position;
      current.currentPosition(position);
    }

    @Override
    public void curlyBraceOpen() throws UnexpectedJSONContentException {
      //System.err.println("curlyBraceOpen "+position +" "+current.isInObjectList()+ " "+stack.size() );
      if( current.isInObjectList() ) {
        System.err.println("iol");
        return;
      }
      String label = current.getLabel();
      if( label == null && !current.isInList()) {
        //System.out.println(" -> no new visitor");
        return;
      }
      
      JsonVisitor<?> next = current.getVisitor().objectStarts(label);
      if( next == null ) {
        //System.out.println(" -> no new visitor");
        current.incrementObjectDepth();
        return;
      }
      
      next.currentPosition(position);
      
      boolean inObjectList = false;
      if( current.isInList() ) {
        inObjectList = true;
      }
      
      //System.err.println(" -> new visitor " +next + (inObjectList?" in object list ":""));
      stack.push(current);
      current = new JsonTokenVisitorImpl(next);
      //current.setInObjectList(inObjectList); //FIXME raus, da sonst Fehler bei komplexen Objekten in Liste. 
      //FIXME Unschön: Für jedes Listenelement wird nach JsonVisitor gefragt
    }

    @Override
    public void curlyBraceClose() throws UnexpectedJSONContentException {
      //System.err.println("curlyBraceClose "+position +" "+current.isInObjectList()+ " "+ stack.size() );
      if( stack.size() > 0 ) {
        if( current.getObjectDepth() == 0 ) {
          stack.peek().objectValue( current.getVisitor().getAndReset() );
          if( ! current.isInObjectList() ) {
            current = stack.pop();
          }
        } else {
          current.decrementObjectDepth();
        }
      }
    }

    @Override
    public void squareBracketOpen() {
      current.squareBracketOpen();
    }

    @Override
    public void squareBracketClose() throws UnexpectedJSONContentException {
      if( current.isInObjectList() ) {
        //System.err.println( current.getLabel() +" "+current.getVisitor() +" " + stack );
        current.setInObjectList(false);
        current = stack.pop();
      }
      current.squareBracketClose();
    }

    @Override
    public void comma() {
      current.comma();
    }

    @Override
    public void colon() {
      current.colon();
    }

    @Override
    public void booleanValue(boolean value) throws UnexpectedJSONContentException {
      current.booleanValue(value);
    }

    @Override
    public void nullValue() throws UnexpectedJSONContentException {
      current.nullValue();
    }

    @Override
    public void numberValue(String value) throws UnexpectedJSONContentException {
      current.numberValue(value);
    }

    @Override
    public void stringValue(String value) throws UnexpectedJSONContentException {
      current.stringValue(value);
    }
    
  }
  
  private static class JsonTokenVisitorImpl implements JsonTokenVisitor {

    private JsonVisitor<?> visitor;
    private String currentLabel = null;
    private boolean expectLabel = true;
    private boolean inList;
    private Type listType;
    private List<String> primitiveList;
    private List<Object> objectList;
    private boolean inObjectList;
    private int objectDepth = 0;
    
    public JsonTokenVisitorImpl(JsonVisitor<?> visitor) {
      if( visitor == null ) {
        throw new IllegalArgumentException("Visitor cannot be null");
      }
      this.visitor = visitor;
      //this.visitor.setCaller(this);
    }
    @Override
    public String toString() {
      return "JsonTokenVisitorImpl("+currentLabel+","+")";
    }

    public void incrementObjectDepth() {
      expectLabel = true;
      ++objectDepth;
    }
    public void decrementObjectDepth() {
      --objectDepth;
    }
    public int getObjectDepth() {
      return objectDepth;
    }


    public boolean isInObjectList() {
      return inObjectList;
    }

    public void setInObjectList(boolean inObjectList) {
      this.inObjectList = inObjectList;
    }

    public boolean isInList() {
      return inList;
    }

    public JsonVisitor<?> getVisitor() {
      return visitor;
    }

    public String getLabel() {
      return currentLabel;
    }

    @Override
    public void currentPosition(Position position) {
      visitor.currentPosition(position);
    }

    @Override
    public void curlyBraceOpen() {
    }

    @Override
    public void curlyBraceClose() {
    }

    @Override
    public void squareBracketOpen() {
      this.inList = true;
    }

    @Override
    public void squareBracketClose() throws UnexpectedJSONContentException {
      this.inList = false;
      expectLabel = true;
      //System.err.println( "squareBracketClose "+ listType +" "+currentLabel);
      if( primitiveList != null ) {
        visitor.list( currentLabel, primitiveList, listType);
      } else if( objectList != null ) {
        visitor.objectList( currentLabel, objectList);
      } else {
        visitor.emptyList(currentLabel);
      }
      listType = null;
      primitiveList = null;
      objectList = null;
    }

    @Override
    public void comma() {
    }

    @Override
    public void colon() {
    }

    @Override
    public void booleanValue(boolean value) throws UnexpectedJSONContentException {
      if( inList ) {
        if( primitiveList == null ) {
          listType = Type.Boolean;
          primitiveList = new ArrayList<String>();
        }
        if (listType == Type.Null) {
          listType = Type.Boolean;
        } else if (listType != Type.Boolean) {
          listType = Type.Mixed;
        }
        primitiveList.add(String.valueOf(value));
      } else {
        visitor.attribute( currentLabel, String.valueOf(value), Type.Boolean);
        expectLabel = true;
      }
    }

    @Override
    public void nullValue() throws UnexpectedJSONContentException {
      if( inList ) {
        if(listType == Type.Object) {
          if( objectList == null ) {
            objectList = new ArrayList<Object>();
          }
          objectList.add(null);
        }
        else {
          if( primitiveList == null ) {
            listType = Type.Null;
            primitiveList = new ArrayList<String>();
          }
          primitiveList.add(null);
        }
      } else {
        visitor.attribute( currentLabel, null, Type.Null);
        expectLabel = true;
      }
    }

    @Override
    public void numberValue(String value) throws UnexpectedJSONContentException {
      if( inList ) {
        if( primitiveList == null ) {
          listType = Type.Number;
          primitiveList = new ArrayList<String>();
        }
        if (listType == Type.Null) {
          listType = Type.Number;
        } else if (listType != Type.Number) {
          listType = Type.Mixed;
        }
        primitiveList.add(value);
      } else {
        visitor.attribute( currentLabel, value, Type.Number);
        expectLabel = true;
      }
    }

    @Override
    public void stringValue(String value) throws UnexpectedJSONContentException {
      if( inList ) {
        if( primitiveList == null ) {
          listType = Type.String;
          primitiveList = new ArrayList<String>();
        }
        if (listType == Type.Null) {
          listType = Type.String;
        } else if (listType != Type.String) {
          listType = Type.Mixed;
        }
        primitiveList.add(value);
      } else {
        if( expectLabel ) {
          this.currentLabel = value;
          expectLabel = false;
        } else {
          visitor.attribute( currentLabel, value, Type.String);
          expectLabel = true;
        }
      }
    }
    
    public void objectValue(Object object) throws UnexpectedJSONContentException {
      //System.err.println( "addObject " + currentLabel +" " + inList +" " + object);
      if( inList ) {
        if( objectList == null ) {
          objectList = new ArrayList<Object>();
          listType = Type.Object;
        }
        
        //we need to add all null values we put into our primitiveList in here!
        if(primitiveList != null) {
          for(int i=0; i<primitiveList.size(); i++) {
            if(primitiveList.get(i) == null)
              objectList.add(null);
          }
          primitiveList = null;
        }
        
        if (listType == Type.Null) {
          listType = Type.Object;
        } else if (listType != Type.Object) {
          listType = Type.Mixed;
        }
        objectList.add(object);
      } else {
        visitor.object(currentLabel, object );
        expectLabel = true;
      }
      
    }
    
  }
  
  public static class JsonStringVisitor implements JsonVisitor<String> {

    private Position position;
    private int start;

    @Override
    public String get() {
      int end = position.getIndex();
      return position.getJsonSubstring(start,end+1);
    }

    @Override
    public String getAndReset() {
      int end = position.getIndex();
      return position.getJsonSubstring(start,end+1);
    }
    
    @Override
    public void currentPosition(Position position) {
      this.position = position;
      this.start = position.getIndex();
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      return null;
    }

    @Override
    public void attribute(String label, String value, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
        throws UnexpectedJSONContentException {
    }

    @Override
    public void list(String label, List<String> values, com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type type)
        throws UnexpectedJSONContentException {
    }

    @Override
    public void object(String label, Object value) throws UnexpectedJSONContentException {
    }

    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
    }

    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
    }
    
    
  }

  public static class JsonParserUtils {

    public static <E extends Enum<E>> E parseEnum(Class<E> enumType, String label, String value) throws UnexpectedJSONContentException {
      try {
        return Enum.valueOf(enumType, value);
      } catch( Exception e ) {
        throw new UnexpectedJSONContentException(label, e);
      }
    }

    public static <E extends Enum<E>> void expect(String label, String value, E expected) throws UnexpectedJSONContentException {
      expect(label,value,expected.toString());
    }
    public static <E extends Enum<E>> void expect(String label, String value, String expected) throws UnexpectedJSONContentException {
      if( value != null ) {
        if( value.equals(expected) ) {
          return;
        }
      } else {
        if( expected == null ) {
          return;
        }
      }
      throw new UnexpectedJSONContentException(label, expected); 
    }
    
    public static void checkNotNull(String label, String value) throws UnexpectedJSONContentException {
      if( value == null ) {
        throw new UnexpectedJSONContentException(label, Cause.nullValue ); 
      }
    }
    
    public static void checkAllowedLabels(List<String> allowedLabels, String label) throws UnexpectedJSONContentException {
      if( ! allowedLabels.contains(label) ) {
        throw new UnexpectedJSONContentException(label);
      }
    }

    public static JsonVisitor<JsonSerializable> ignore() {
      return new IgnoreJsonVisitor();
    }
    
    public static class IgnoreJsonVisitor implements JsonVisitor<JsonSerializable> {

      @Override
      public JsonSerializable get() {
        return null;
      }

      @Override
      public JsonSerializable getAndReset() {
        return null;
      }
      
      @Override
      public void currentPosition(Position position) {
      }

      @Override
      public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
        return null;
      }
      
      @Override
      public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      }
      @Override
      public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
      }
      @Override
      public void object(String label, Object value) throws UnexpectedJSONContentException {
      }
      @Override
      public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      }

      @Override
      public void emptyList(String label) throws UnexpectedJSONContentException {
      }

      
    }
    
    
  }
  
}
