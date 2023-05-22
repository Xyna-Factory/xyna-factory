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
package com.gip.xyna.xfmg.xods.configuration;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;

import com.gip.xyna.utils.collections.SerializableWrappedMap;
import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.db.types.StringSerializable;



public class DocumentationMap extends SerializableWrappedMap<DocumentationLanguage, String> implements StringSerializable<DocumentationMap>, Serializable {

  private static final long serialVersionUID = 1L;

  public DocumentationMap() {
    super( new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class) );
  }

  public DocumentationMap(Map<DocumentationLanguage, String> map) {
    super( new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class) );
    putAll(map);
  }

  public static DocumentationMap valueOf(String string) {
    if( string == null || string.isEmpty() ) {
      return new DocumentationMap();
    }
    StringSerializableList<String> ssl = StringSerializableList.separator(String.class);
    ssl = ssl.deserializeFromString(string);
    
    EnumMap<DocumentationLanguage, String> map = new EnumMap<>(DocumentationLanguage.class);
    for( String entry : ssl ) {
      String[] parts = entry.split(" => ", 2);
      DocumentationLanguage lang = DocumentationLanguage.valueOf(parts[0]);
      map.put(lang, unescapeComma(parts[1]));
    }
    return new DocumentationMap(map);
  }

  public DocumentationMap deserializeFromString(String string) {
    return valueOf(string);
  }
  
  public String serializeToString() {
    StringSerializableList<String> ssl = StringSerializableList.separator(String.class);
    for( Map.Entry<DocumentationLanguage, String> entry : wrapped.entrySet() ) {
      ssl.add( entry.getKey()+" => "+ escapeComma(entry.getValue()) );
    }
    return ssl.serializeToString();
  }
  
  
  private static String escapeComma(String value){
    return value.replaceAll(Matcher.quoteReplacement("\\"), Matcher.quoteReplacement("\\\\")).replaceAll(", ", Matcher.quoteReplacement(",\\ "));
  }
  
  private static String unescapeComma(String escapedValue){
    String unescapedValue = escapedValue.replaceAll(Matcher.quoteReplacement(",\\ "), ", ").replaceAll(Matcher.quoteReplacement("\\\\"), Matcher.quoteReplacement("\\"));
    
    return unescapedValue;
  }

}
