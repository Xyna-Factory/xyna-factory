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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping;

import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;


/**
 *
 */
public class CreateXmlOptions {
  
  public static final StringParameter<String> ROOT_ELEMENT_NAME =
      StringParameter.typeString("rootElementName").
      label("Root Element Name").
      documentation(Documentation.
                    en("Root Element Name, only needed if no standard Root Element Name from XSD exists").
                    de("Name des RootElements, nur nötig, wenn es keinen Standard-RootElement-Name aus dem XSD gibt").
                    build()).
      build();

  public static final StringParameter<Boolean> OMIT_NULL_TAGS =
      StringParameter.typeBoolean("omitNullTags").
      label("Omit Null Tags").
      documentation(Documentation.
                    en("Omit null tag, even if they are not optional").
                    de("Weglassen leerer Tags, auch wenn diese nicht optional sind").
                    build()).
      defaultValue(false).build();
  
  public static final StringParameter<Boolean> OMIT_SINGLE_NAMESPACE_PREFIX =
      StringParameter.typeBoolean("omitSingleNamespacePrefix").
      label("Omit Single Namespace Prefix").
      documentation(Documentation.
                    en("Omit namespace prefix, if there is only one namespace").
                    de("Weglassen des NamespacePrefix, wenn es nur einen Namespace gibt").
                    build()).
      defaultValue(true).build();
  
  public static final StringParameter<Boolean> BOOLEAN_AS_INTEGER =
      StringParameter.typeBoolean("booleanAsInteger").
      label("Boolean As Integer").
      documentation(Documentation.
                    en("Write Booleans as 1/0 instead of true/false").
                    de("Ausgeben von Booleans als 1/0 statt true/false").
                    build()).
      defaultValue(false).build();
  
  public static final StringParameter<Boolean> INCLUDE_PI_ELEMENT =
      StringParameter.typeBoolean("includePIElement").
      label("Include PI Element").
      documentation(Documentation.
                    de("Processing Instruction Element <?xml> zu Beginn von XML erzeugen").
                    en("Create Processing Instruction Element <?xml>").build())
      .defaultValue(true).build();

  
  public static final List<StringParameter<?>> parameters = 
      StringParameter.asList( ROOT_ELEMENT_NAME, OMIT_NULL_TAGS, OMIT_SINGLE_NAMESPACE_PREFIX, BOOLEAN_AS_INTEGER, INCLUDE_PI_ELEMENT
                            );

  
  private String rootElementName;
  private boolean omitNullTags;
  private boolean omitSingleNamespacePrefix;
  private boolean booleanAsInteger;
  private boolean includePIElement;
  private NamespacePrefixCache namespacePrefixCache;
  
  public CreateXmlOptions() {
    namespacePrefixCache = new NamespacePrefixCache(omitSingleNamespacePrefix);
  }
  
  public static CreateXmlOptions create(Map<String, Object> map) {
    CreateXmlOptions cxo = new CreateXmlOptions();
    cxo.rootElementName = ROOT_ELEMENT_NAME.getFromMap(map);
    cxo.omitNullTags = OMIT_NULL_TAGS.getFromMap(map);
    cxo.omitSingleNamespacePrefix = OMIT_SINGLE_NAMESPACE_PREFIX.getFromMap(map);
    cxo.booleanAsInteger = BOOLEAN_AS_INTEGER.getFromMap(map);
    cxo.includePIElement = INCLUDE_PI_ELEMENT.getFromMap(map);
    cxo.namespacePrefixCache = new NamespacePrefixCache(cxo.omitSingleNamespacePrefix);
    return cxo;
  }
  
  public boolean omitNullTags() {
    return omitNullTags;
  }  
  public boolean booleanAsInteger() {
    return booleanAsInteger;
  }
  
  public String getRootElementName() {
    return rootElementName;
  }

  public boolean includePIElement() {
    return includePIElement;
  }

  public NamespacePrefixCache getNamespacePrefixCache() {
    return namespacePrefixCache;
  }

}
