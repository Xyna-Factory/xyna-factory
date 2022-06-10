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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

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
  
  public static final StringParameter<Versioning> VERSIONING = 
      StringParameter.typeEnum(Versioning.class, "versioning").
      documentation(Documentation.
                    en("Algorithm to extract version from MIB module").
                    de("Algorithmus zur Extraktion der Version des MIB-Modules").
                    build()).
      defaultValue(defaults.getVersioning()).build();

  public static final StringParameter<List<String>> OID_RESTRICTIONS = 
      StringParameter.typeList(String.class, "oids").
      documentation(Documentation.
                    en("OID restrictions to import partial mib, oids separated by whitespace").
                    de("Restriktionen zum teilweisen Import, OIDs werden mit Whitespace getrennt").
                    build()).
      optional().build();
  
  public static final StringParameter<EnumSet<Information>> INFORMATION= 
      StringParameter.typeEnumSet(Information.class, "information").
      documentation(Documentation.
                    en("Print informations").
                    de("Ausgabe von Informationen").
                    build()).
      defaultValue(defaults.getInformations()).build();

  public static final StringParameter<Boolean> NO_IMPORT =
      StringParameter.typeBoolean("noImport").
      label("No Import").
      documentation(Documentation.
                    en("No Import, only analysis of mibs").
                    de("Kein Import, nur Analyse der MIBs").
                    build()).
      defaultValue(false).build();

  
   
  public static final List<StringParameter<?>> configurableParameters = 
      StringParameter.asList( BASE_PATH, BASE_TYPE, OVERWRITE, VERSIONING
                            );
  
  public static final List<StringParameter<?>> importParameters = 
      StringParameter.asList( BASE_PATH, BASE_TYPE, INFORMATION, OID_RESTRICTIONS, OVERWRITE, NO_IMPORT, VERSIONING
                            );

  public enum Information implements DocumentedEnum {
    ListMIBs(Documentation.en("not implemented yet").
             de("noch nicht implementiert").
             build() ), 
    ModuleStatistics(Documentation.en("not implemented yet").
             de("noch nicht implementiert").
             build());

    private Documentation doc;

    private Information(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
  }
  
  private String basePath;
  private XmomType baseType;
  private List<String> oidRestrictions;
  private boolean overwrite;
  private String dataModelTypeName;
  private Versioning versioning;
  private EnumSet<Information> informations;
  private boolean noImport;
  @SuppressWarnings("unused")
  private Map<String, Object> map;
  
  private ImportParameter(Map<String, Object> map) {
    this.map = map;
  }

  public static ImportParameter parameterForImport(Map<String, Object> map, String dataModelTypeName) {
    ImportParameter ip = new ImportParameter(map);
    ip.dataModelTypeName = dataModelTypeName;
    ip.basePath = normalizePath( BASE_PATH.getFromMap(map) );
    ip.baseType = XmomType.ofFQTypeName(BASE_TYPE.getFromMap(map));
    ip.oidRestrictions = OID_RESTRICTIONS.getFromMap(map);
    ip.overwrite = OVERWRITE.getFromMap(map);
    ip.versioning = VERSIONING.getFromMap(map);
    ip.informations = INFORMATION.getFromMap(map);
    ip.noImport = NO_IMPORT.getFromMap(map);
    return ip;
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

  public boolean isNoImport() {
    return noImport;
  }
  
  public EnumSet<Information> getInformations() {
    return informations;
  }
    
  public List<String> getOidRestrictions() {
    return oidRestrictions;
  }

  public boolean hasOidRestrictions() {
    return oidRestrictions != null && ! oidRestrictions.isEmpty();
  }

  public boolean getOverwrite() {
    return overwrite;
  }
  
  public String getDataModelTypeName() {
    return dataModelTypeName;
  }
  
  public Versioning getVersioning() {
    return versioning;
  }
  

  
  private static class InitialParameter {
    
    private DefaultValueModifiable<String> basePath = new DefaultValueModifiable<String>("xdnc.model.mib");
    private DefaultValueModifiable<String> baseType = new DefaultValueModifiable<String>("xdnc.model.mib.MIBBaseModel");
    private DefaultValueModifiable<Boolean> overwrite = new DefaultValueModifiable<Boolean>(false);
    private DefaultValueModifiable<Versioning> versioning = new DefaultValueModifiable<Versioning>(Versioning.CountRevisions);
    private DefaultValueModifiable<EnumSet<Information>> informations = new DefaultValueModifiable<EnumSet<Information>>(EnumSet.allOf(Information.class));
    
    public DefaultValueModifiable<String> getBaseType() {
      return baseType;
    }

    public DefaultValueModifiable<String> getBasePath() {
      return basePath;
    }

    public DefaultValueModifiable<Boolean> getOverwrite() {
      return overwrite;
    }
      
    public DefaultValueModifiable<Versioning> getVersioning() {
      return versioning;
    }
    
    public DefaultValueModifiable<EnumSet<Information>> getInformations() {
      return informations;
    }

    public void initialize(Map<String,Object> initParamMap) {
      basePath.setDefaultValue( normalizePath( BASE_PATH.getFromMap(initParamMap) ) );
      baseType.setDefaultValue( BASE_TYPE.getFromMap(initParamMap) );

      overwrite.setDefaultValue( OVERWRITE.getFromMap(initParamMap) );
      versioning.setDefaultValue( VERSIONING.getFromMap(initParamMap) );
    }
    
  }

  public static void initDefaults(Map<String,Object> initParamMap) {
    defaults.initialize(initParamMap);
  }
  
}
