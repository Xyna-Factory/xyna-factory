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

import java.io.Serializable;
import java.util.EnumMap;

import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;


/**
 *
 */
public class Documentation implements Serializable {

  private static final long serialVersionUID = 1L;
  
  
  /**
   * Interface, welches die dokumentierten Enums erf�llen m�ssen
   *
   */
  public static interface DocumentedEnum {
    Documentation getDocumentation();
  }
  

  
  private EnumMap<DocumentationLanguage, String> documentation;
    
  private Documentation(DocumentationBuilder documentationBuilder) {
    documentation = new EnumMap<DocumentationLanguage, String>(documentationBuilder);
  }
  
  public String get(DocumentationLanguage lang) {
    return documentation.get(lang);
  }
  

  
  
  
  
  
  
  
  public static DocumentationBuilder de(String value) {
    return new DocumentationBuilder(DocumentationLanguage.DE, value);
  }
  public static DocumentationBuilder en(String value) {
    return new DocumentationBuilder(DocumentationLanguage.EN, value);
  }
  
  public static class DocumentationBuilder extends EnumMap<DocumentationLanguage, String> {
    private static final long serialVersionUID = 1L;
    
    public DocumentationBuilder(DocumentationLanguage lang, String value) {
      super(DocumentationLanguage.class);
      put(lang, value);
    }
    
    public DocumentationBuilder de(String value) {
      put(DocumentationLanguage.DE, value);
      return this;
    }
    public DocumentationBuilder en(String value) {
      put(DocumentationLanguage.EN, value);
      return this;
    }
    public DocumentationBuilder lang(DocumentationLanguage lang, String value) {
      put(lang, value);
      return this;
    }
    public Documentation build() {
      return new Documentation(this);
    }
  }

  
}
