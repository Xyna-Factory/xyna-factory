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

import java.lang.reflect.Constructor;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.db.types.StringSerializable;



/**
 * Implementierungen von CollectionUtils.Transformation für Transformationen 
 * zwischen String und anderen Datentypen.
 * <br>
 * Aufruf:
 * <ul>
 * <li>trafo = StringTransformation.toString(Example.class);</li>
 * <li>trafo = StringTransformation.toValue(Example.class);</li>
 * </ul>
 * <br>
 * vollständiges Beispiel:
 * <pre>
 *  List&lt;String&gt; from = Arrays.asList("1","2","3");
 *  List&lt;Integer&gt; intList = CollectionUtils.transform(from, StringTransformation.toValue(Integer.class) );
 * </pre>
 */
public class StringTransformation {

  private StringTransformation() {}
  
  public static final Transformation<String,String> IDENTITY = new Transformation<String,String>() {

    @Override
    public String transform(String from) {
      return from;
    }
    
  };
  
  public static final Transformation<StringSerializable<?>,String> STRING_SERIALIZABLE_TO_STRING = new Transformation<StringSerializable<?>,String>() {

    @Override
    public String transform(StringSerializable<?> from) {
      return from.serializeToString();
    }
    
  };
  
  public static final Transformation<Object,String> OBJECT_TO_STRING = new Transformation<Object,String>() {

    @Override
    public String transform(Object from) {
      return String.valueOf(from);
    }
    
  };
  
  @SuppressWarnings("unchecked")
  public static <V> Transformation<V,String> toString(Class<V> valueClass ) {
    if( valueClass.equals(String.class) ) {
      return (Transformation<V,String>)IDENTITY;
    } else if( StringSerializable.class.isAssignableFrom(valueClass) ) {
      return (Transformation<V,String>)STRING_SERIALIZABLE_TO_STRING;
    } else {
      return (Transformation<V,String>)OBJECT_TO_STRING;
    }
  }

  @SuppressWarnings("unchecked")
  public static <V> Transformation<String, V> toValue(Class<V> valueClass) {
    if( valueClass.equals(String.class) ) {
      return (Transformation<String,V>)IDENTITY;
    } else if( StringSerializable.class.isAssignableFrom(valueClass) ) {
      try {
        StringSerializable<V> instance = (StringSerializable<V>)valueClass.getConstructor().newInstance();
        return new StringToStringSerializableTransformation<>(instance);
      } catch (Exception e) {
        throw new IllegalArgumentException("no default constructor found");
      }
    } else {
      try {
        return new StringToObjectTransformation<>( valueClass.getConstructor(String.class) );
      } catch (Exception e) {
        throw new IllegalArgumentException("no constructor from String found");
      }
    }
  }

  public static class StringToStringSerializableTransformation<V> implements Transformation<String,V> {

    private final StringSerializable<V> instance;
    
    public StringToStringSerializableTransformation(StringSerializable<V> instance) {
      this.instance = instance;
    }
    
    @Override
    public V transform(String from) {
      return instance.deserializeFromString(from);
    }
    
  }
  
  public static class StringToObjectTransformation<V> implements Transformation<String,V> {
    private final Constructor<V> constructor;
    
    public StringToObjectTransformation(Constructor<V> constructor) {
      this.constructor = constructor;
    }
    
    @Override
    public V transform(String from) {
      try {
        return constructor.newInstance(from);
      } catch (Exception e) {
        throw new IllegalStateException("constructor from String does not work", e);
      }
    }
    
  }

}
