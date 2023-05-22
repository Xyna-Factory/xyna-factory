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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException.Reason;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;


/**
 * StringParameter ist eine komfortable L�sung, um Konfigurationsparameter als String-Liste einzulesen,
 * zu validieren und einfachen Zugriff darauf zu erhalten.
 * Innerhalb dieser StringParameter-Klasse existieren viele Hilfsklassen, die zusammenwirken.
 * <br><br>
 * Aufgerufen werden sollten (in den meisten F�llen) nur die statischen Methoden, die auch im 
 * folgenden Beispiel verwendet werden:<pre>
 * public enum Testenum { abc, def, ghi };
 * public String parse(List&lt;StringParameter&lt;?&gt;&gt; allParameters, List&lt;String&gt; params) {
 *   try {
 *     Map&lt;String, Object&gt; map = StringParameter.parse(params).with(allParameters);
 *     return map.toString();
 *   } catch (StringParameter.StringParameterParsingException e) {
 *     return "exception=\""+e.getMessage()+"\"";
 *   }
 * }
 * public void main( String[] args ) {
 *   StringParameter&lt;Testenum&gt; sp1 = StringParameter.typeEnum(Testenum.class, "testEnum").
 *       description("beliebige Enums sind m�glich").build();
 *   StringParameter&lt;Boolean&gt; sp3 = StringParameter.typeBoolean("testBoolean").
 *      description("nur true und false sind caseInsensitive zugelassen").build();
 *   StringParameter&lt;String&gt; sp2 = StringParameter.typeString("testMandatoryFor").
 *       mandatoryFor(sp1, Testenum.def).
 *       mandatoryFor(sp1, Testenum.ghi).
 *       mandatoryFor(sp3).
 *       description("Parameter muss angegeben werden, "+
 *           "falls 'testBoolean' gesetzt wird oder 'testEnum' auf 'def' oder 'ghi' gesetzt wird").
 *       build();
 *   StringParameter&lt;Integer&gt; sp4 = StringParameter.typeInteger("testMandatory").
 *       mandatory().description("dies ist ein Pflichtparameter").build();
 *   StringParameter&lt;Long&gt; sp5 = StringParameter.typeLong("testDefaultUndValidierung").
 *       defaultValue(1234L).pattern("[1-9]\\d\\d\\d+").
 *       description("hat einen Default-Parameter und eine Validierung (&gt;=1000)").build();
 *   
 *   List&lt;StringParameter&lt;?&gt;&gt; allParameters = StringParameter.asList(sp1,sp2,sp3,sp4,sp5);
 *   List&lt;String&gt; params = null;
 *   
 *   params = Arrays.asList( "testMandatory=5" );
 *   try {
 *     Map&lt;String, Object&gt; map = StringParameter.paramListToMap(allParameters, params);
 *     Assert.assertEquals( "{testMandatory=5}", map.toString() );
 *     Testenum testEnum = sp1.getFromMap(map);
 *     Assert.assertNull(testEnum);
 *     String testMandatoryFor = sp2.getFromMap(map);
 *     Assert.assertNull(testMandatoryFor);
 *     Boolean testBoolean = sp3.getFromMap(map);
 *     Assert.assertNull(testBoolean);
 *     int testMandatory = sp4.getFromMap(map);
 *     Assert.assertEquals(5, testMandatory);
 *     long testDefaultUndValidierung = sp5.getFromMap(map);
 *     Assert.assertEquals(1234, testDefaultUndValidierung); //default, obwohl nicht in Map und in Parametern
 *   } catch( StringParameterParsingException e ) {
 *     Assert.fail("No exception expected: "+e.getMessage() );
 *   }
 *   
 *   params = Arrays.asList( "testEnum=abc" );
 *   Assert.assertEquals( "exception=\"testMandatory is mandatory\"", parse( allParameters, params ) );
 *      
 *   params = Arrays.asList( "testMandatory=1", "testEnum=abc" );
 *   Assert.assertEquals( "{testMandatory=1, testEnum=abc}", parse( allParameters, params ) );
 *   
 *   params = Arrays.asList( "testMandatory=2", "testEnum=abc", "testBoolean=true" );
 *   Assert.assertEquals( "exception=\"testMandatoryFor is mandatory for testBoolean\"", parse( allParameters, params) );
 *   
 *   params = Arrays.asList( "testMandatory=3", "testEnum=def" );
 *   Assert.assertEquals( "exception=\"testMandatoryFor is mandatory for testEnum with value def\"", parse( allParameters, params) );
 *
 *   params = Arrays.asList( "testMandatory=4",  "testEnum=ghi", "testBoolean=true", "testMandatoryFor=string" );
 *   Assert.assertEquals( "{testMandatory=4, testMandatoryFor=string, testBoolean=true, testEnum=ghi}", parse( allParameters, params ) );
 *
 *   params = Arrays.asList( "testMandatory=4", "testDefaultUndValidierung" );
 *   Assert.assertEquals( "{testMandatory=4, testDefaultUndValidierung=1234}", parse( allParameters, params ) );
 *       
 *   params = Arrays.asList( "testMandatory=4", "testDefaultUndValidierung=999" );
 *   Assert.assertEquals( "exception=\"testDefaultUndValidierung does not match pattern\"", parse( allParameters, params) );
 *
 *   params = Arrays.asList( "testMandatory=4", "testDefaultUndValidierung=1000" );
 *   Assert.assertEquals( "{testMandatory=4, testDefaultUndValidierung=1000}", parse( allParameters, params ) );
 * }
 * </pre>
 */
public abstract class StringParameter<T> implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(\\w+)=(.*)");

  
  protected SerializableDefaultValue<T> defaultValue;
  protected String name;
  protected String label;
  protected Pattern pattern;
  protected boolean optional;
  protected boolean hasDefault;
  protected ArrayList<SerializablePair<String, SerializableMandatoryValue>> mandatoryFor;
  protected Documentation documentation;

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }
  
  @Deprecated
  public String getDescription() {
    return documentation.get(DocumentationLanguage.EN);
  }
  
  public boolean isOptional() {
    return optional;
  }
  
  public boolean isMandatory() {
    return ! optional;
  }
  
  public boolean isMandatoryFor() {
    return mandatoryFor != null;
  }

  public T getDefaultValue() {
    if( defaultValue == null ) {
      return null;
    }
    return defaultValue.getDefaultValue();
  }

  public String getDefaultValueAsString() {
    if( defaultValue == null ) {
      return null;
    }
    return asString( defaultValue.getDefaultValue() );
  }
  
  public List<SerializablePair<String, Serializable>> getMandatoryFor() {
    List<SerializablePair<String, Serializable>> list = new ArrayList<SerializablePair<String,Serializable>>();
    for (SerializablePair<String, SerializableMandatoryValue> serializablePair : mandatoryFor) {
      list.add(SerializablePair.<String, Serializable>of(serializablePair.getFirst(), serializablePair.getSecond().getValueAsString()));
    }
    return list;
  }
  
  public boolean hasDefault() {
    return hasDefault;
  }
  
  public String documentation(DocumentationLanguage lang) {
    if (documentation != null) {
      return documentation.get(lang);  
    } else {
      return "";
    }
  }
  
  public String getSimpleTypeString() {
    return getTypeClass().getSimpleName();
  }
  
  /**
   * Parsen des String in den gew�nschten Typ
   * @param string
   * @return
   * @throws Exception
   */
  public abstract T parseString(String string) throws Exception;
  
  /**
   * Parsen des Object in den gew�nschten Typ
   * @param object
   * @return
   * @throws Exception
   */
  public abstract T parseObject(Object object) throws Exception;
  
  /**
   * Ausgabe als parsebarer String
   * @param value
   * @return
   */
  public abstract String asString(T value);
  
  /**
   * R�ckgabe des Typs
   * @return
   */
  public abstract Class<T> getTypeClass();

  /**
   * Ausgabe als Key-Value-Paar "name=value"
   * @param value
   * @return
   */
  @SuppressWarnings("unchecked")
  @Deprecated
  public String toNamedParameter(Object value) {
    if( value instanceof String ) {
      return name+"="+value;
    } else {
      return name+"="+asString((T)value);
    }
  }
  /*
   *  FIXME sch�ner w�re hier ein 
   * public String toNamedParameter(T value) {
   *   return name+"="+asString(value);
   * }
   * dies geht aber nicht, da existierende Trigger toNamedParameter mit String aufrufen!
   */
  

  /**
   * Ausgabe als Key-Value-Paar "name=value"
   * @param value
   * @return
   */
   public String toNamedParameterObject(T value) {
     return name+"="+asString(value);
   }
  
  /**
   * Ausgabe als Key-Value-Paar "name=value"
   * @param valueAsString
   * @return
   */
  public String toNamedParameterString(String valueAsString) {
    return name+"="+valueAsString;
  }
  
  
  /**
   * Hilfsfunktion, um einfache eine unmodifiable-List der StringParameter zu erstellen
   * @param parameters
   * @return
   */
  public static List<StringParameter<?>> asList(StringParameter<?> ... parameters ) {
    return Collections.unmodifiableList( Arrays.asList(parameters));
  }

  /**
   * Selektion des Parameters aus der Object-Map: Falls Eintrag in der Map fehlt, wird defaultValue zur�ckgegeben
   * @param map
   * @return
   */
  public T getFromMap(Map<String, Object> map) {
    Object o = map.get(name);
    if( o != null ) {
      return getTypeClass().cast(o);
    } else {
      return getDefaultValue();
    }
  }
  
  /**
   * Selektion des Parameters aus der Object-Map: Falls Eintrag in der Map fehlt, wird defaultValue zur�ckgegeben
   * @param map
   * @return
   */
  public String getFromMapAsString(Map<String, Object> map) {
    Object o = map.get(name);
    if( o != null ) {
      return asString(getTypeClass().cast(o));
    } else {
      return getDefaultValueAsString();
    }
  }
 
  @Override
  public String toString() {
    return getClass().getSimpleName()+"("+name+")";
  }
  
  /**
   * Parsen in den gew�nschten Typ.
   * (N�tig, da alter Code gegen parse(String) kompiliert ist, 
   * ansonsten w�rde parse(Object) parse(String) ersetzen k�nnen.)
   * @param string
   * @return
   * @throws StringParameterParsingException
   */
  public T parse(String string) throws StringParameterParsingException {
    if( string == null ) {
      return useDefaultValue();
    }
    if( ! validateString(string) ) {
      return useDefaultValue();
    }
    try {
      return parseString(string);
    } catch( StringParameterParsingException e ) {
      throw e;
    } catch( Exception e ) {
      throw new StringParameterParsingException(name, Reason.Parsing, getTypeClass(), e );
    }
  }
  /**
   * Parsen in den gew�nschten Typ.
   * @param object
   * @return
   * @throws StringParameterParsingException
   */
  public T parse(Object object) throws StringParameterParsingException {
    if( object == null ) {
      return useDefaultValue();
    }
    try {
      if( object instanceof String ) {
        String string = (String)object;
        if( ! validateString(string) ) {
          return useDefaultValue();
        }
        return parseString(string);
      } else {
        return parseObject(object);
      }
    } catch( StringParameterParsingException e ) {
      throw e;
    } catch( Exception e ) {
      throw new StringParameterParsingException(name, Reason.Parsing, getTypeClass(), e );
    }
  }

  private boolean validateString(String string) throws StringParameterParsingException {
    //Validierung
    if( pattern != null ) {
      //Auch Leerstrings sollen gegen Pattern gepr�ft werden
      Matcher m = pattern.matcher(string);
      if( ! m.matches() ) {
        throw new StringParameterParsingException(name, Reason.Pattern );
      }
    } else {
      //Leerstrings ohne Pattern werden nicht mehr geparst: useDefaultValue
      //falls Leerstrings erlaubt sein sollen, pattern verwenden!
      if( string.length() == 0 ) {
        return false;
      }
    }
    return true;
  }

  private T useDefaultValue() throws StringParameterParsingException {
    if( optional ) {
      return getDefaultValue();
    } else {
      throw new StringParameterParsingException(name, Reason.Mandatory);
    }
  }


  /**
   * Ist der StringParameter im Context aller StringParameter valide?
   * @param paramMap
   */
  public void validate(Map<String, Object> paramMap) throws StringParameterParsingException {
    if( isMandatory() ) {
      if( paramMap.containsKey(name) ) {
        //ist enthalten, daher Regel erf�llt
      } else {
        throw new StringParameterParsingException(name, Reason.Mandatory);
      }
    } else if( isMandatoryFor() ) {
      if( paramMap.containsKey(name) ) {
        //ist enthalten, daher Regel erf�llt
      } else {
        //nicht enthalten, ist einer der Anforderer enthalten?
        for( SerializablePair<String, SerializableMandatoryValue> pair : mandatoryFor ) {
          if( pair.getSecond() == null ) {
            if( paramMap.containsKey(pair.getFirst() ) ) {
              throw new StringParameterParsingException(name, Reason.MandatoryFor, pair);
            }
          } else {
            Object valueAsCorrectType = pair.getSecond().getValueAs(paramMap.get(pair.getFirst()));
            if(valueAsCorrectType != null &&
               valueAsCorrectType.equals( paramMap.get(pair.getFirst() ) ) ) {
              throw new StringParameterParsingException(name, Reason.MandatoryFor, pair);
            }
          }
        }
      }
    }
  }
  
  /**
   * Umwandlung der ParameterListe in eine Map&lt;String,Object&gt; (Nach weiterem Aufruf von with(...)
   * @param params
   * @return
   */
  public static Parser parse(List<String> params) {
     return new Parser(params);
  }
  
  /**
   * Umwandlung der Parameter-Map in eine Map&lt;String,Object&gt; (Nach weiterem Aufruf von with(...)
   * @param params
   * @return
   */
  public static Parser parse(Map<String,? extends Object> params) {
    return new Parser(params);
  }
  
  
  public static enum Unmatched { Keep, Ignore, Error }
  public static enum Unparseable { Keep, Ignore, Null, Error }
  
  public static class Parser {

    private Unmatched unmatched = Unmatched.Error;
    private Unparseable unparseable = Unparseable.Error;
    private Map<String, ? extends Object> params;
    private Map<String,StringParameter<?>> allStringParameter;
    private Map<String,Object> paramMap;
    
    public Parser(List<String> paramList) {
      Map<String, String> map = new HashMap<String, String>();
      for( String p : paramList ) {
        Matcher m = KEY_VALUE_PATTERN.matcher(p);
        if( m.matches() ) {
          map.put( m.group(1), m.group(2));
        } else {
          map.put( p, null);
        }
      }
      this.params = map;
    }
    
    public Parser(Map<String,? extends Object> params) {
      this.params = params;
    }
    
    public Parser unmatchedKey(Unmatched unmatched) {
      this.unmatched = unmatched;
      return this;
    }
    
    public Parser unparseableValue(Unparseable unparseable) {
      this.unparseable = unparseable;
      return this;
    }
    
    public Parser silent(boolean silent) {
      this.unmatched = Unmatched.Keep; //TODO so behalten?
      this.unparseable = Unparseable.Null; //TODO so behalten?
      return this;
    }

    public Map<String,Object> with(StringParameter<?> ... allStringParameter) throws StringParameterParsingException {
      this.allStringParameter = toMap(Arrays.asList(allStringParameter));
      return parse();
    }
    
    public Map<String,Object> with(List<StringParameter<?>> allStringParameter) throws StringParameterParsingException {
      this.allStringParameter = toMap(allStringParameter);
      return parse();
    }
    
    public Map<String,Object> with(Map<String,StringParameter<?>> allStringParameter) throws StringParameterParsingException {
      this.allStringParameter = allStringParameter;
      return parse();
    }
    
    
    private Map<String, Object> parse() throws StringParameterParsingException {
      paramMap = new HashMap<String, Object>();
      if( params != null ) {
        for( Map.Entry<String,? extends Object> entry : params.entrySet() ) {
          String name = entry.getKey();
          Object value = entry.getValue();
          StringParameter<?> sp = findStringParameter(name,value);
          if( sp != null ) {
            parseValue( sp, value );
          }
        }
      }
      
      //TODO optional?
      for( StringParameter<?> sp :allStringParameter.values() ) {
        sp.validate( paramMap );
      }
      return paramMap;
    }
    
    
    private StringParameter<?> findStringParameter(String name, Object value) throws StringParameterParsingException {
      StringParameter<?> sp = allStringParameter.get(name);
      if( sp == null ) {
        switch( unmatched ) {
          case Keep:
            paramMap.put( name, value );
            break;
          case Ignore:
            break;
          case Error:
            throw new StringParameterParsingException( name, Reason.StringParameter );
        }
      }
      return sp;
    }

    private void parseValue(StringParameter<?> sp, Object value) throws StringParameterParsingException {
      try {
        paramMap.put( sp.getName(), sp.parse( value) );
      } catch( StringParameterParsingException e ) {
        switch( unparseable ) {
          case Error:
            throw e;
          case Ignore:
            break;
          case Keep:
            paramMap.put( sp.getName(), value);
            break;
          case Null:
            paramMap.put( sp.getName(), null);
            break;
        }
      }
    }
    
    private Map<String,StringParameter<?>> toMap(List<StringParameter<?>> allStringParameter) {
      Map<String,StringParameter<?>> allSPs = new HashMap<String,StringParameter<?>>();
      for(StringParameter<?> sp : allStringParameter ) {
        allSPs.put(sp.getName(), sp );
      }
      return allSPs;
    }
    
  }
  
  /**
   * Umwandlung der ParameterListe in eine Map&lt;String,Object&gt;
   * @param allStringParameter alle StringParameter
   * @param params ParameterListe
   * @return
   * @throws StringParameterParsingException
   * @deprecated
   */
  @Deprecated
  public static Map<String,Object> paramListToMap( List<StringParameter<?>> allStringParameter, List<String> params) throws StringParameterParsingException {
    return parse(params).with(allStringParameter);
  }

  
 
  
  
  /**
   * Umwandlung der ParameterListe in eine Map&lt;String,String&gt; ohne Validierung
   * @param params ParameterListe
   * @return
   * @deprecated
   */
  @Deprecated
  public static Map<String,String> paramListToMap( List<String> params) {
    HashMap<String, String> paramMap = new HashMap<String, String>();
    for( String p : params ) {
      Matcher m = KEY_VALUE_PATTERN.matcher(p);
      if( m.matches() ) {
        paramMap.put( m.group(1), m.group(2));
      } else {
        paramMap.put( p, null);
      }
    }
    return paramMap;
  }

  /**
   * Umwandlung einer Map&lt;String,String&gt; in eine Liste mit "key=value" Paaren
   * @param paramMap ParameterListe
   * @return
   */
  public static List<String> paramStringMapToList(Map<String,String> paramMap) {
    if (paramMap == null) {
      return null;
    }
    
    List<String> params = new ArrayList<String>();
    for(Map.Entry<String,String> entry : paramMap.entrySet()) {
      params.add(entry.getKey()+"="+entry.getValue());
    }
    return params;
  }

  
  public static StringParameterBuilder<Integer> typeInteger(String name) {
    StringParameterBuilder<Integer> spb = new StringParameterBuilder<Integer>( new StringParameterInteger() );
    return spb.name(name);
  }
  
  public static StringParameterBuilder<Long> typeLong(String name) {
    StringParameterBuilder<Long> spb = new StringParameterBuilder<Long>( new StringParameterLong() );
    return spb.name(name);
  }
  
  public static StringParameterBuilder<Duration> typeDuration(String name) {
    StringParameterBuilder<Duration> spb = new StringParameterBuilder<Duration>( new StringParameterDuration() );
    return spb.name(name);
  }

  public static StringParameterBuilder<Boolean> typeBoolean(String name) {
    StringParameterBuilder<Boolean> spb = new StringParameterBuilder<Boolean>( new StringParameterBoolean() );
    return spb.name(name);
  }

  public static StringParameterBuilder<String> typeString(String name) {
    StringParameterBuilder<String> spb = new StringParameterBuilder<String>( new StringParameterString() );
    return spb.name(name);
  }

  public static <E extends Enum<E>> StringParameterBuilder<E> typeEnum(Class<E> enumClass, String name) {
    StringParameterBuilder<E> spb = new StringParameterBuilder<E>( new StringParameterEnum<E>(enumClass) );
    return spb.name(name);
  }
  
  public static <E extends Enum<E>> StringParameterBuilder<E> typeEnum(Class<E> enumClass, String name, boolean caseInsensitive) {
    StringParameterBuilder<E> spb = new StringParameterBuilder<E>( new StringParameterEnum<E>(enumClass, caseInsensitive) );
    return spb.name(name);
  }
  
  public static <E extends Enum<E>> StringParameterBuilder<EnumSet<E>> typeEnumSet(Class<E> enumClass, String name) {
    StringParameterBuilder<EnumSet<E>> spb = new StringParameterBuilder<EnumSet<E>>( new StringParameterEnumSet<E>(enumClass) );
    return spb.name(name);
  }
  
  public static <E extends Enum<E>> StringParameterBuilder<EnumSet<E>> typeEnumSet(Class<E> enumClass, String name, boolean caseInsensitive) {
    StringParameterBuilder<EnumSet<E>> spb = new StringParameterBuilder<EnumSet<E>>( new StringParameterEnumSet<E>(enumClass, caseInsensitive) );
    return spb.name(name);
  }

  public static <E extends Enum<E>> StringParameterBuilder<EnumMap<E,String>> typeEnumMap(Class<E> enumClass, String name) {
    StringParameterBuilder<EnumMap<E,String>> spb = new StringParameterBuilder<EnumMap<E,String>>( new StringParameterEnumMap<E>(enumClass) );
    return spb.name(name);
  }
  
  public static <E extends Enum<E>> StringParameterBuilder<EnumMap<E,String>> typeEnumMap(Class<E> enumClass, String name, boolean caseInsensitive) {
    StringParameterBuilder<EnumMap<E,String>> spb = new StringParameterBuilder<EnumMap<E,String>>( new StringParameterEnumMap<E>(enumClass,caseInsensitive) );
    return spb.name(name);
  }
  
  public static <E extends Enum<E>> StringParameterBuilder<String> typeEnumCombination(Class<E> enumClass, String name) {
    StringParameterBuilder<String> spb = new StringParameterBuilder<String>( new StringParameterEnumCombination<E>(enumClass,true) );
    return spb.name(name);
  }

  
  public static <T> StringParameterBuilder<List<T>> typeList(Class<T> typeClass, String name) {
    StringParameterBuilder<List<T>> spb = new StringParameterBuilder<List<T>>( new StringParameterList<T>(typeClass) );
    return spb.name(name);
  }
  
  public static <T> StringParameterBuilder<List<T>> typeList(Class<T> typeClass, String name, ListSeparator listSeparator) {
    StringParameterBuilder<List<T>> spb = new StringParameterBuilder<List<T>>( new StringParameterList<T>(typeClass,listSeparator,true) );
    return spb.name(name);
  }
  
  public static <T> StringParameterBuilder<List<T>> typeList(Class<T> typeClass, String name, ListSeparator listSeparator, boolean trim) {
    StringParameterBuilder<List<T>> spb = new StringParameterBuilder<List<T>>( new StringParameterList<T>(typeClass,listSeparator,trim) );
    return spb.name(name);
  }

  
  public static class StringParameterBuilder<T> {
    StringParameter<T> sp;

    public StringParameterBuilder(StringParameter<T> StringParameter) {
      sp = StringParameter;
      sp.optional = true;
    }

    @Deprecated
    public StringParameterBuilder<T> description(String description) {
      sp.documentation = Documentation.en(description).build();
      return this;
    }
    
    public StringParameterBuilder<T> documentation(Documentation doc) {
      sp.documentation = doc;
      return this;
    }
   
    public StringParameterBuilder<T> name(String name) {
      sp.name = name;
      return this;
    }

    public StringParameterBuilder<T> label(String label) {
      sp.label = label;
      return this;
    }

    public StringParameterBuilder<T> defaultValue(T value) {
      sp.hasDefault = true;
      sp.defaultValue = new SerializableDefaultValue<T>(sp,value);
      return this;
    }
    
    public StringParameterBuilder<T> defaultValue(DefaultValue<T> defaultValue) {
      sp.hasDefault = true;
      if( defaultValue != null ) {
        sp.defaultValue = new SerializableDefaultValue<T>(sp,defaultValue);
      }
      return this;
    }

    public StringParameter<T> build() {
      return sp;
    }

    public StringParameterBuilder<T> pattern(Pattern pattern) {
      sp.pattern = pattern;
      return this;
    }
    
    public StringParameterBuilder<T> pattern(String pattern) {
      sp.pattern = Pattern.compile(pattern);
      return this;
    }

    /**
     * parameter is optional (default)
     */
    public StringParameterBuilder<T> optional() {
      sp.optional = true;
      return this;
    }
    /**
     * parameter is mandatory
     */
    public StringParameterBuilder<T> mandatory() {
      sp.optional = false;
      return this;
    }

    /**
     * parameter is mandatory if another StringParameter is set (repeated call possible)
     */
    public StringParameterBuilder<T> mandatoryFor(StringParameter<?> StringParameter) {
      return mandatoryFor(StringParameter,null);
    }
    
    /**
     * parameter is mandatory if another StringParameter is set to the given value (repeated call possible)
     */
    public StringParameterBuilder<T> mandatoryFor(StringParameter<?> StringParameter, Object value) {
      if( sp.mandatoryFor == null ) {
        sp.mandatoryFor = new ArrayList<SerializablePair<String,SerializableMandatoryValue>>();
      }
      sp.mandatoryFor.add(SerializablePair.of(StringParameter.getName(), new SerializableMandatoryValue(value)));
      return this;
    }
   
  }

  public static interface DefaultValue<T> extends Serializable {
    T getDefaultValue();
  }  
  
  public static class DefaultValueFixed<T> implements DefaultValue<T> {

    private static final long serialVersionUID = 1L;
    
    private transient T defaultValue;
 
    public DefaultValueFixed(T value) {
      this.defaultValue = value;
     }

    public T getDefaultValue() {
      return defaultValue;
    }

  }
  
  public static class DefaultValueModifiable<T> implements DefaultValue<T> {

    private static final long serialVersionUID = 1L;

    private transient T defaultValue;

    public DefaultValueModifiable(T value) {
      this.defaultValue = value;
    }
    
    public void setDefaultValue(T defaultValue) {
      this.defaultValue = defaultValue;
    }
 
    public T getDefaultValue() {
      return defaultValue;
    }
    
  }
 
  public static class SerializableDefaultValue<T> implements DefaultValue<T> {

    private static final long serialVersionUID = 1L;
    private String defaultValueAsString;
    private StringParameter<T> stringParameter;
    private transient DefaultValue<T> defaultValue;
    private transient T defaultValueValue;
    private transient boolean needsRestore;
    
    public SerializableDefaultValue(StringParameter<T> stringParameter, DefaultValue<T> defaultValue) {
      this.stringParameter = stringParameter;
      this.defaultValue = defaultValue;
    }
 
    public SerializableDefaultValue(StringParameter<T> stringParameter, T value) {
      this.stringParameter = stringParameter;
      this.defaultValueValue = value;
    }

    public T getDefaultValue() {
      if( defaultValue != null ) {
        return defaultValue.getDefaultValue();
      } else if( needsRestore ) {
        try {
          defaultValueValue = stringParameter.parse(defaultValueAsString);
          return defaultValueValue;
        } catch (StringParameterParsingException e) {
          throw new IllegalStateException("Could not restore DefaultValue from String");
        }
      } else {
        return defaultValueValue;
      }
    }
    
    private void writeObject(ObjectOutputStream stream) throws IOException {
      if( defaultValue != null ) {
        this.defaultValueAsString = stringParameter.asString(defaultValue.getDefaultValue()); 
      } else {
        this.defaultValueAsString = stringParameter.asString(defaultValueValue); 
      }
      stream.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      ois.defaultReadObject();
      needsRestore = true;
    }

  }
 
  
  public static class SerializableMandatoryValue implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String valueAsString;
    
    @SuppressWarnings("rawtypes")
    public SerializableMandatoryValue(Object value) {
      if (value instanceof String) {
        valueAsString = (String) value;
      } else if (value instanceof StringSerializable) {
        valueAsString = ((StringSerializable)value).serializeToString();
      } else if (value instanceof Enum) {
        valueAsString = String.valueOf(value);
      }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <O> O getValueAs(O instance) {
      if (instance instanceof String) {
        return instance;
      } else if (instance instanceof StringSerializable) {
        return (O) ((StringSerializable)instance).deserializeFromString(valueAsString);
      } else if (instance instanceof Enum) {
        return (O) Enum.valueOf((Class<? extends Enum>)instance.getClass(), valueAsString);
      } else {
        return null;
      }
    }
    
    private String getValueAsString() {
      return valueAsString;
    }
    
  }
  
  
  
  public static class StringParameterParsingException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public enum Reason {
      Pattern(" does not match pattern"),
      Mandatory(" is mandatory"),
      MandatoryFor(" is mandatory") {
        public String getMessage(Object additional) {
          if( additional instanceof Pair<?,?> ) {
            Pair<?,?> pair = (Pair<?,?>)additional;
            if( pair.getSecond() == null ) {
              return message +" for "+pair.getFirst();
            } else {
              return message +" for "+pair.getFirst()+" with value "+pair.getSecond();
            }
          }
          return message;
        }
      },
      Parsing(" could not be parsed") {
        public String getMessage(Object additional) {
          if( additional instanceof Class<?> ) {
            return message +" to "+((Class<?>)additional).getSimpleName();
          }
          return message;
        }
      },
      StringParameter( " is no StringParameter");
      
      protected String message;

      private Reason(String message) {
        this.message = message;
      }
     
      protected String getMessage(Object additional) {
        return message;
      }
      
      public String getConstantMessagePart() {
        return message;
      }

    }

    private Reason reason;
    private String parameterName;
    private Object additional;
    
    public StringParameterParsingException(String parameterName, Reason reason) {
      super(parameterName + reason.getMessage(null) );
      this.parameterName = parameterName;
      this.reason = reason;
    }
    
    public StringParameterParsingException(String parameterName, Reason reason, Object additional, Throwable cause) {
      super(parameterName + reason.getMessage(additional), cause );
      this.parameterName = parameterName;
      this.reason = reason;
      this.additional = additional;
    }

    
    public StringParameterParsingException(String parameterName, Reason reason, Object additional) {
      super(parameterName + reason.getMessage(additional) );
      this.parameterName = parameterName;
      this.reason = reason;
      this.additional = additional;
    }

     public Reason getReason() {
      return reason;
    }
     
    public String getParameterName() {
      return parameterName;
    } 
    
    public Object getAdditional() {
      return additional;
    }
    
  }

  public static class StringParameterString extends StringParameter<String> {
    private static final long serialVersionUID = 1L;

    public String parseString(String string) {
      return string;
    }
    public String parseObject(Object object) {
      return String.valueOf(object);
    }
    public String asString(String value) {
      return value;
    }
    public Class<String> getTypeClass() {
      return String.class;
    }
  }
  
  public static class StringParameterInteger extends StringParameter<Integer> {
    private static final long serialVersionUID = 1L;
    
    public Integer parseString(String string) {
      return Integer.valueOf(string);
    }
    public Integer parseObject(Object object) {
      return (Integer)object;
    }
    public String asString(Integer value) {
      return value==null?null:value.toString();
    }
    public Class<Integer> getTypeClass() {
      return Integer.class;
    }
  }
  
  public static class StringParameterLong extends StringParameter<Long> {
    private static final long serialVersionUID = 1L;

    public Long parseString(String string) {
      return Long.valueOf(string);
    }
    public Long parseObject(Object object) {
      return (Long)object;
    }
    public String asString(Long value) {
      return value==null?null:value.toString();
    }
    public Class<Long> getTypeClass() {
      return Long.class;
    }
  }
  
  public static class StringParameterDuration extends StringParameter<Duration> {
    private static final long serialVersionUID = 1L;

    public Duration parseString(String string) {
      return Duration.valueOf(string);
    }
    public Duration parseObject(Object object) {
      return (Duration)object;
    }
    public String asString(Duration value) {
      return value==null?null:value.toString();
    }
    public Class<Duration> getTypeClass() {
      return Duration.class;
    }
  }
  
  public static class StringParameterBoolean extends StringParameter<Boolean> {
    private static final long serialVersionUID = 1L;

    public Boolean parseString(String string) throws StringParameterParsingException {
      if( string.equalsIgnoreCase("true") ) {
        return Boolean.TRUE;
      } else if( string.equalsIgnoreCase("false") ) {
        return Boolean.FALSE;
      } else {
        throw new StringParameterParsingException(name, Reason.Parsing, Boolean.class );
      }
    }
    public Boolean parseObject(Object object) throws StringParameterParsingException {
      return (Boolean)object;
    }
    
    public String asString(Boolean value) {
      return value==null?null:value.toString();
    }
    public Class<Boolean> getTypeClass() {
      return Boolean.class;
    }
  }
  

  public static abstract class StringParameterWithEnum<T, E extends Enum<E>> extends StringParameter<T> {
    
    private static final long serialVersionUID = 1L;
    protected SerializableEnumClass<E> enumClass;
    protected boolean caseInsensitive;
    private String simpleTypeString;
    
    protected StringParameterWithEnum(Class<E> enumClass, boolean caseInsensitive, String simpleTypeString) {
      this.enumClass = new SerializableEnumClass<E>(enumClass);
      this.caseInsensitive = caseInsensitive;
      this.simpleTypeString = simpleTypeString;
    }
    
    public boolean hasDocumentedEnum() {
      return enumClass.isEnumDocumented();
    }

    public String getSimpleTypeString() {
      return simpleTypeString;
    }
    
    public List<SerializablePair<String, Documentation>> getEnumDocumentation() {
      return enumClass.getDocumentation();
    }

    public List<String> getEnumConstantsAsStrings() {
      return enumClass.getEnumConstantsAsStrings();
    }
    
    protected E toEnum(Object object) {
      if( object instanceof String ) {
        return enumClass.toEnum( (String)object, caseInsensitive);
      } else {
        return enumClass.getEnumClass().cast(object);
      }
    }

  }
  
  public static class StringParameterEnum<E extends Enum<E>> extends StringParameterWithEnum<E,E> {
    private static final long serialVersionUID = 1L;

    protected StringParameterEnum(Class<E> enumClass) {
      super(enumClass, false, "Enum");
    }
    protected StringParameterEnum(Class<E> enumClass, boolean caseInsensitive) {
      super(enumClass, caseInsensitive, "Enum");
    }
    
    public Class<E> getTypeClass() {
      return enumClass.getEnumClass();
    }

    public E parseString(String string) {
      return toEnum(string);
    }
    @SuppressWarnings("unchecked")
    public E parseObject(Object object) {
      return (E)object;
    }
    public String asString(E value) {
      return value==null?null:value.name();
    }

    public String getSimpleTypeString() {
      return "Enum";
    }
  }
  
  public static class StringParameterEnumSet<E extends Enum<E>> extends StringParameterWithEnum<EnumSet<E>,E> {
    private static final long serialVersionUID = 1L;
    
    private ListSeparator listSeparator;
    
    protected StringParameterEnumSet(Class<E> enumClass) {
      super(enumClass, false, "EnumSet");
      listSeparator = ListSeparator.COMMA_WHITESPACE;
    }
    protected StringParameterEnumSet(Class<E> enumClass, boolean caseInsensitive) {
      super(enumClass, caseInsensitive, "EnumSet");
      listSeparator = ListSeparator.COMMA_WHITESPACE;
    }
    
    public EnumSet<E> parseString(String string) {
      EnumSet<E> set = EnumSet.noneOf(enumClass.getEnumClass());
      for( String p : listSeparator.split(string) ) {
        set.add( toEnum(p) );
      }
      return set;
    }
    @Override
    public EnumSet<E> parseObject(Object object) throws Exception {
      EnumSet<E> set = EnumSet.noneOf(enumClass.getEnumClass());
      if( object instanceof Set ) {
        for( Object o : (Set<?>) object ) {
          set.add( toEnum( o ) );
        }
      } else if( object instanceof Map ) {
        for( Map.Entry<?, ?> e : ((Map<?,?>) object).entrySet() ) {
          E enumVal = toEnum( e.getKey() );
          String val = String.valueOf(e.getValue());
          if( "false".equals(val) ) {
            //Enum nicht eintragen
          } else {
            set.add( enumVal );
          }
        }
      } else if( object == null ) {
        return null;
      } else {
        throw new ClassCastException(object.getClass()+" can not be cast to EnumSet");
      }
      return set;
    }
    
    public String asString(EnumSet<E> value) {
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for( E e : value) {
        sb.append(sep).append(e.name());
        sep = listSeparator.getSeparator();
      }
      return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public Class<EnumSet<E>> getTypeClass() {
      Class<?> clazz = EnumSet.class;
      return (Class<EnumSet<E>>) clazz;
    }
  }
  
  public static class StringParameterEnumMap<E extends Enum<E>> extends StringParameterWithEnum<EnumMap<E,String>,E> {
    private static final long serialVersionUID = 1L;
    
    protected StringParameterEnumMap(Class<E> enumClass) {
      super(enumClass, false, "EnumMap");
    }
    protected StringParameterEnumMap(Class<E> enumClass, boolean caseInsensitive) {
      super(enumClass, caseInsensitive, "EnumMap");
    }
    public EnumMap<E,String> parseString(String string) {
      EnumMap<E,String> map = new EnumMap<E,String>(enumClass.getEnumClass());
      for( String p : CsvUtils.iterate(string) ) {
        Matcher m = KEY_VALUE_PATTERN.matcher(p);
        if( m.matches() ) {
          map.put( toEnum(m.group(1)), m.group(2) );
        } else {
          map.put( toEnum(p), null );
        }
      }
      return map;
    }
    @Override
    public EnumMap<E, String> parseObject(Object object) throws Exception {
      if( object instanceof Map ) {
        EnumMap<E,String> map = new EnumMap<E,String>(enumClass.getEnumClass());
        for( Map.Entry<?, ?> e : ((Map<?,?>) object).entrySet() ) {
          E enumVal = toEnum( e.getKey() );
          String val = String.valueOf(e.getValue());
          map.put( enumVal, val );
        }
        return map;
      } else if( object == null ) {
        return null;
      } else {
        throw new ClassCastException(object.getClass()+" can not be cast to EnumMap");
      }
    }
    public String asString(EnumMap<E,String> value) {
      if( value == null ) {
        return null;
      }
      List<String> kvpairs = new ArrayList<String>();
      for( Map.Entry<E,String> e : value.entrySet() ) {
        kvpairs.add( e.getKey().name()+"="+e.getValue());
      }
      return CsvUtils.toCSV(kvpairs);
    }
       
    @SuppressWarnings("unchecked")
    public Class<EnumMap<E,String>> getTypeClass() {
      Class<?> clazz = EnumMap.class;
      return (Class<EnumMap<E,String>>) clazz;
    }

  }
  
  public static class StringParameterEnumCombination<E extends Enum<E>> extends StringParameterWithEnum<String,E> {
    private static final long serialVersionUID = 1L;
    
    protected StringParameterEnumCombination(Class<E> enumClass, boolean caseInsensitive) {
      super(enumClass, caseInsensitive, "EnumCombination");
      
      this.enumClass = new SerializableEnumClass<E>(enumClass);
      this.caseInsensitive = caseInsensitive;
    }
    
    public String parseString(String string) { //TODO Validierung?
      return string;
    }
    public String parseObject(Object object) throws Exception {
      return String.valueOf(object);
    }
    public String asString(String string) {
      return string;
    }
    
    public Class<String> getTypeClass() {
      return String.class;
    }
  
  }

  
  
  public static class ListSeparator implements Serializable {
    private static final long serialVersionUID = 1L;
    public final static ListSeparator WHITESPACE = 
        new ListSeparator(" ", "\\s+", Documentation.en("whitespace separated").de("whitespace-separiert").build());
    public final static ListSeparator COMMA_WHITESPACE = 
        new ListSeparator(", ", ",\\s*", Documentation.en("comma whitespace separated").de("komma-whitespace-separiert").build());
    public final static ListSeparator COMMA = 
        new ListSeparator(",", ",", Documentation.en("comma separated").de("komma-separiert").build());
    
    private String separator;
    private Pattern separatorPattern;
    private Documentation documentation;
    
    public ListSeparator(String separator, String separatorPattern, Documentation documentation) {
      this.separator = separator;
      this.separatorPattern = Pattern.compile(separatorPattern);
      this.documentation = documentation;
    }

    public String[] split(String string) {
      return separatorPattern.split(string);
    }
    
    public String getSeparator() {
      return separator;
    }
    
    public Documentation getDocumentation() {
      return documentation;
    }
    
  }
  

  

  public static class StringParameterList<T> extends StringParameter<List<T>> {
    private static final long serialVersionUID = 1L;
    
    private transient Constructor<T> constructor;
    private transient Class<T> typeClass;
    private boolean trim;

    private ListSeparator listseparator;
    
    public StringParameterList(Class<T> typeClass) {
      this(typeClass, ListSeparator.COMMA_WHITESPACE, true);
    }
   
    public StringParameterList(Class<T> typeClass, ListSeparator listseparator, boolean trim) {
      this.listseparator = listseparator;
      this.typeClass = typeClass;
      try {
        constructor = typeClass.getConstructor(String.class);
      } catch (Exception e) { //SecurityException NoSuchMethodException
        throw new UnsupportedOperationException("Type "+typeClass.getSimpleName()+" has no constructor from string.");
      }
      
      this.trim = trim;
    }
    
    public List<T> parseString(String string) throws Exception {
      List<T> list = new ArrayList<T>();
      boolean first = true;
      for(String p : listseparator.split(string) ) {
        if( first && trim ) {
          if( p == null || p.trim().length() == 0 ) { 
            first = false;
            continue;
          }
        }
        first = false;
        if( trim ) {
          p = p.trim();
        }
        list.add( constructor.newInstance(p));
      }
      return list;
    }
    
    public List<T> parseObject(Object object) throws Exception {
      List<T> list = new ArrayList<T>();
      for( Object o : (List<?>)object ) {
        if( o instanceof String ) {
          String p = (String)o;
          if( trim ) {
            p = p.trim();
          }
          list.add( constructor.newInstance(p));
        } else {
          /*
          public T cast(Object obj) {
            if (obj != null && !isInstance(obj))
                throw new ClassCastException();
            return (T) obj;
              }
          */
          //list.add(typeClass.cast(o)); hat keine sch�ne Exception (Message ist leer)
          if( o == null ) {
            list.add(null);
          } else {
            if( typeClass.isInstance(o) ) {
              list.add(typeClass.cast(o));
            } else {
              throw new ClassCastException(o.getClass() +" cannot be cast to "+typeClass);
            }
          }
        }
      }
      return list;
    }
    
    public String asString(List<T> value) {
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for( T t : value) {
        sb.append(sep).append(t);
        sep = listseparator.getSeparator();
      }
      return sb.toString();
    }
    
    @SuppressWarnings("unchecked")
    public Class<List<T>> getTypeClass() {
      @SuppressWarnings("rawtypes")
      Class c = List.class;
      return (Class<List<T>>) c;
    }

  }


  public static List<String> toList(List<StringParameter<?>> stringParameters, Map<String,Object> map, boolean includeDefaults) {
    List<String> list = new ArrayList<String>();
    if( map == null ) {
      if( includeDefaults ) {
        for( StringParameter<?> sp : stringParameters ) {
          @SuppressWarnings("unchecked")
          StringParameter<Object> spo = (StringParameter<Object>)sp;
          if( spo.hasDefault() ) {
            list.add( spo.toNamedParameterString(spo.getDefaultValueAsString()) );
          }
        }
      }
    } else {
      for( StringParameter<?> sp : stringParameters ) {
        @SuppressWarnings("unchecked")
        StringParameter<Object> spo = (StringParameter<Object>)sp;
        
        if( map.containsKey(spo.getName() ) ) {
          list.add( spo.toNamedParameterString(sp.getFromMapAsString(map)) );
        } else {
          if( includeDefaults && spo.hasDefault()) {
            list.add( spo.toNamedParameterString(spo.getDefaultValueAsString()) );
          }
        }
      }
    }
    return list;
  }
 
  public static String toString(List<StringParameter<?>> stringParameters, Map<String,Object> map, boolean includeDefaults) {
    List<String> list = toList(stringParameters, map, includeDefaults);
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( String s : list ) {
      sb.append(sep).append(s);
      sep = " ";
    }
    return sb.toString();
  }

  public static Map<String, String> toStringMap(List<StringParameter<?>> stringParameters, Map<String, Object> map,
                                                boolean includeDefaults) {
    Map<String,String> stringMap = new HashMap<String,String>();
    if( map == null ) {
      if( includeDefaults ) {
        for( StringParameter<?> sp : stringParameters ) {
          @SuppressWarnings("unchecked")
          StringParameter<Object> spo = (StringParameter<Object>)sp;
          if( spo.hasDefault() ) {
            stringMap.put( spo.getName(), spo.getDefaultValueAsString() );
          }
        }
      }
    } else {
      for( StringParameter<?> sp : stringParameters ) {
        @SuppressWarnings("unchecked")
        StringParameter<Object> spo = (StringParameter<Object>)sp;
        
        if( map.containsKey(spo.getName() ) ) {
          stringMap.put( spo.getName(), sp.getFromMapAsString(map) );
        } else {
          if( includeDefaults && spo.hasDefault()) {
            stringMap.put( spo.getName(), spo.getDefaultValueAsString() );
          }
        }
      }
    }
    return stringMap;
  }

  public static Map<String, String> listToMap(List<String> params) {
    HashMap<String, String> paramMap = new HashMap<String, String>();
    for( String p : params ) {
      Matcher m = KEY_VALUE_PATTERN.matcher(p);
      if( m.matches() ) {
        paramMap.put(m.group(1), m.group(2));
      } else {
        paramMap.put(p, null);
      }
    }
    return paramMap;
  }

  public static ObjectMapBuilder buildObjectMap() {
    return new ObjectMapBuilder();
  }

  public static class ObjectMapBuilder {
    Map<String, Object> map = new HashMap<String, Object>();
    
    public <T> ObjectMapBuilder put(StringParameter<T> sp, T value) {
      map.put(sp.getName(), value);
      return this;
    }

    public Map<String, Object> build() {
      return map;
    }
    
  }

 

}
