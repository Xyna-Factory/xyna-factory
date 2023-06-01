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
package com.gip.xyna.utils.misc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;


/**
 *
 */
public class SerializableEnumClass<E extends Enum<E>> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private transient Class<E> enumClass;
  private String fqEnumClassName;
  private List<String> enumConstantsAsStrings;
  private boolean isEnumDocumented;
  private List<SerializablePair<String,Documentation>> documentation;

  public SerializableEnumClass(Class<E> enumClass) {
    this.enumClass = enumClass;
    this.isEnumDocumented = DocumentedEnum.class.isAssignableFrom(enumClass);
  }

 
  public boolean isEnumDocumented() {
    return isEnumDocumented;
  }
  
  private void writeObject(ObjectOutputStream stream) throws IOException {
    //Anlegen der benötigten Daten durch die Getter
    getFqEnumClassName();
    getEnumConstantsAsStrings();
    getDocumentation();
    
    //Serialisieren der Daten, die die enumClass ersetzen
    stream.defaultWriteObject();
  }

  public List<SerializablePair<String,Documentation>> getDocumentation() {
    if( documentation == null ) {
      if( isEnumDocumented ) {
        E[] ecs = enumClass.getEnumConstants();
        List<SerializablePair<String,Documentation>> list = new ArrayList<SerializablePair<String,Documentation>>(ecs.length);
        for( E e : ecs ) {
          list.add(SerializablePair.of(e.name(),((DocumentedEnum)e).getDocumentation() ));
        }
        documentation = Collections.unmodifiableList(list);
      } else {
        documentation = Collections.emptyList();
      }
    }
    return documentation;
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois)
      throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    try {
      //FIXME nur über ClassLoaderDispatcher zu finden
      enumClass = (Class<E>)Class.forName(fqEnumClassName);
    } catch( ClassNotFoundException e ) {
      Logger.getLogger(SerializableEnumClass.class).trace("", e );
      //dann halt nicht wiederherstellen
    }
  }
  
  /**
   * 
   */
  public String getFqEnumClassName() {
    if( fqEnumClassName == null ) {
      fqEnumClassName = enumClass.getName();
    }
    return fqEnumClassName;
  }

  
  public Class<E> getEnumClass() {
    return enumClass;
  }

  public List<String> getEnumConstantsAsStrings() {
    if( enumConstantsAsStrings == null ) {
      E[] ecs = enumClass.getEnumConstants();
      List<String> list = new ArrayList<String>(ecs.length);
      for( E e : ecs ) {
        list.add(e.name());
      }
      enumConstantsAsStrings = Collections.unmodifiableList(list);
    }
    return enumConstantsAsStrings;
  }

  public E toEnum(String string, boolean caseInsensitive) {
    if( enumClass == null ) {
      throw new IllegalStateException("Enum does not exist after deserialization");
    }
    if( caseInsensitive ) {
      for( E e : enumClass.getEnumConstants() ) {
        if( e.name().equalsIgnoreCase(string) ) {
          return e;
        }
      }
      return Enum.valueOf(enumClass, string); //produziert Fehler, da nicht caseInsensitive gefunden
    } else {
      return Enum.valueOf(enumClass, string);
    }
  }

}
