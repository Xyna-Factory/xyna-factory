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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.DefaultValueModifiable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class ImportParameter {

  private static InitialParameter defaults = new InitialParameter();
  
  public static final StringParameter<String> BASE_PATH = 
      StringParameter.typeString(ImportDataModelParameters.BASE_PATH).
      documentation(Documentation.
                    en("Base path of generated types").
                    de("Basispfad der generierten Datentypen").
                    build()).
      defaultValue(defaults.getBasePath()).build();
  
  public static final StringParameter<String> BASE_TYPE = 
      StringParameter.typeString(ImportDataModelParameters.BASE_TYPE_NAME).
      documentation(Documentation.
                    en("Base type of generated types").
                    de("Basistype der generierten Datentypen").
                    build()).
      defaultValue(defaults.getBaseType()).build();
    
  public static final StringParameter<Boolean> OVERWRITE = 
      StringParameter.typeBoolean("overwrite").
      documentation(Documentation.
                    en("Overwrites existing types").
                    de("Überschreiben bereits existierender Datentypen").
                    build()).
      defaultValue(defaults.getOverwrite()).build();
  
  public static final StringParameter<List<String>> MODEL_RESTRICTIONS = 
      StringParameter.typeList(String.class, "modelRestrictions").
      documentation(Documentation.
                    en("Restrictions to import partial TR069, models separated by whitespace").
                    de("Restriktionen zum teilweisen Import, Modelle werden mit Whitespace getrennt").
                    build()).
      optional().build();
    
  public static final StringParameter<EnumSet<Information>> INFORMATION= 
      StringParameter.typeEnumSet(Information.class, "information").
      documentation(Documentation.
                    en("Print informations").
                    de("Ausgabe von Informationen").
                    build()).
      defaultValue(defaults.getInformation()).build();
  
  public static final List<StringParameter<?>> importParameters = 
      StringParameter.asList( BASE_PATH, BASE_TYPE, OVERWRITE, MODEL_RESTRICTIONS, INFORMATION
                            );
  
  public static final List<StringParameter<?>> configurableParameters = 
      StringParameter.asList( BASE_PATH, BASE_TYPE, OVERWRITE, INFORMATION
                            );
  
 

  public enum Information implements DocumentedEnum {
    NoImport(Documentation.
             en("no import, only informations" ).
             de("keinen Import durchführen").
             build()), 
    Colorize(Documentation.
             en("colorized presentation").
             de("farbige Darstellung").
             build()), 
    ObjectModelTree(Documentation.
                    en("tree representation of imported objects").
                    de("Baumdarstellung der importierten Objekte").
                    build()), 
    FullModelTree(Documentation.
                  en("tree representation of imported objects with their attributes").
                  de("Baumdarstellung der importierten Objekte samt ihrer Attribute").
                  build()),
    Documents(Documentation.
              en("show imported documents" ).
              de("Anzeige der importierten Dokumente").
              build()), 
    Changes(Documentation.
            en("show changes between datamodel versions").
            de("Anzeige der Änderungen zwischen den Datenmodellen-Versionen").
            build());
    
    private Documentation doc;

    private Information( Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
  }
  
  
  private String basePath;
  private XmomType baseType;
  private boolean overwrite;
  private String dataModelTypeName;
  private List<String> modelRestrictions;
  private EnumSet<Information> informations;
  
  public ImportParameter(Map<String, Object> map, String dataModelTypeName) {
    this.dataModelTypeName = dataModelTypeName;
    basePath = normalizePath( BASE_PATH.getFromMap(map) );
    baseType = XmomType.ofFQTypeName(BASE_TYPE.getFromMap(map));
    overwrite = OVERWRITE.getFromMap(map);
    modelRestrictions = MODEL_RESTRICTIONS.getFromMap(map);
    informations = INFORMATION.getFromMap(map);
  }

  
  private static String normalizePath(String string) {
    String n = string.trim().replaceAll("\\.+", ".");
    if( n.endsWith(".") ) {
      return n.substring(0,n.length()-1);
    }
    return n;
  }


  public XmomType getBaseType() {
    return baseType;
  }

  public String getBasePath() {
    return basePath;
  }

  public boolean getOverwrite() {
    return overwrite;
  }
  
  public String getDataModelTypeName() {
    return dataModelTypeName;
  }
  
  public boolean hasModelRestrictions() {
    return modelRestrictions != null && ! modelRestrictions.isEmpty();
  }
  
  public List<String> getModelRestrictions() {
    return modelRestrictions;
  }
    
  public EnumSet<Information> getInformations() {
    return informations;
  }

  public boolean hasInformations() {
    return informations != null && ! informations.isEmpty();
  }
  
  private static class InitialParameter {
    
    private DefaultValueModifiable<String> basePath = new DefaultValueModifiable<String>("xdnc.model.tr069");
    private DefaultValueModifiable<String> baseType = new DefaultValueModifiable<String>("xdnc.model.tr069.TR069BaseModel");
    private DefaultValueModifiable<Boolean> overwrite = new DefaultValueModifiable<Boolean>(false);
    private DefaultValueModifiable<EnumSet<Information>> information = new DefaultValueModifiable<EnumSet<Information>>(EnumSet.noneOf(Information.class));
    
    public DefaultValueModifiable<String> getBaseType() {
      return baseType;
    }

    public DefaultValueModifiable<String> getBasePath() {
      return basePath;
    }

    public DefaultValueModifiable<Boolean> getOverwrite() {
      return overwrite;
    }
      
    public DefaultValueModifiable<EnumSet<Information>> getInformation() {
      return information;
    }

    public void initialize(Map<String, Object> initParamMap) {
      basePath.setDefaultValue( normalizePath( BASE_PATH.getFromMap(initParamMap) ) );
      baseType.setDefaultValue( BASE_TYPE.getFromMap(initParamMap) );

      overwrite.setDefaultValue( OVERWRITE.getFromMap(initParamMap) );
      information.setDefaultValue( INFORMATION.getFromMap(initParamMap) );
    }

  }

  public static void initDefaults(Map<String, Object> initParamMap) {
    defaults.initialize(initParamMap);
  }

}
