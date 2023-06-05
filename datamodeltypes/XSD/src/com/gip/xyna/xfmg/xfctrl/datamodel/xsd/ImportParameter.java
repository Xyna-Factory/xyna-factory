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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.DefaultValueModifiable;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator.LabelCustomization;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator.PathPart;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;


public class ImportParameter implements GenerationParameter {
  
  private static InitialParameter defaults = new InitialParameter();

  public static final StringParameter<String> NAME = 
      StringParameter.typeString("name").
      label("Name").
      documentation(Documentation.
                    en("Unique Name of the data model").
                    de("Eindeutiger Name des Datenmodells").
                    build()).
      mandatory().build();
    
  public static final StringParameter<List<String>> WORKSPACES = 
      StringParameter.typeList(String.class, "workspaces").
      label("List of Workspaces").
      documentation(Documentation.
                    en("workspaces to generate or copy into").
                    de("Workspaces, in die die generierten Datentypen gelangen").
                    build()).
      defaultValue(defaults.getWorkspaces()).build();
  
  public static final StringParameter<String> BASE_PATH = 
      StringParameter.typeString(ImportDataModelParameters.BASE_PATH).
      label("Base Path").
      documentation(Documentation.
                    en("Base path of generated types").
                    de("Basispfad der generierten Datentypen").
                    build()).
      defaultValue(defaults.getBasePath()).build();
  
  public static final StringParameter<Boolean> OVERWRITE_MODEL = 
      StringParameter.typeBoolean("overwrite").
      label("Overwrite Data Model").
      documentation(Documentation.
                    en("Overwrites existing data model").
                    de("Überschreiben des bereits existierend Datenmodells").
                    build()).
      defaultValue(defaults.getOverwriteModel()).build();
  
  public static final StringParameter<Boolean> OVERWRITE_TYPES = 
      StringParameter.typeBoolean("overwriteDataTypes").
      label("Overwrite Data Types").
      documentation(Documentation.
                    en("Overwrites existing data types").
                    de("Überschreiben bereits existierender Datentypen").
                    build()).
      defaultValue(defaults.getOverwriteTypes()).build();
  
 
  public static final StringParameter<String> PATH_CUSTOMIZATION =
      StringParameter.typeEnumCombination(PathPart.class, "pathCustomization").
      label("Path Customization").
      documentation(Documentation.
                    en("Customization of xmom-path, e.g. '/#basepath/#name/#nshost/#nspath'").
                    de("Anpassung der XMOM-Pfade, z.B, '/#basepath/#name/#nshost/#nspath'").
                    build()).
      defaultValue(defaults.getPathCustomization()).build();
  
  public static final StringParameter<EnumMap<LabelCustomization,String>> LABEL_CUSTOMIZATION= 
      StringParameter.typeEnumMap(LabelCustomization.class, "labelCustomization").
      label("Label Customization").
      documentation(Documentation.
                    en("Customization of labels").
                    de("Anpassung der Labels").
                    build()).
      defaultValue(defaults.getLabelCustomization()).build();
  
  public static final StringParameter<EnumMap<GenerationOption,String>> GENERATION_OPTIONS = 
      StringParameter.typeEnumMap(GenerationOption.class, "generationOptions").
      label("Generation Options").
      documentation(Documentation.
                    en("Options to customize the type generation").
                    de("Optionen zur Anpassung der Typ-Generierung").
                    build()).
      defaultValue(defaults.getGenerationOption()).build();
  
  public enum GenerationOption implements DocumentedEnum {
    expandChoice(Documentation.en("expand choice to many optional member variables").
                 de("Expansion einer Choice in viele optionale Membervariablen").
                 build() ), 
    namedSimpleTypes(Documentation.en("defined simple types will be generated as objects").
                     de("definierte SimpleTypes werden eigene Objekte").
                     build() ),
    includeHiddenTypes(Documentation.en("include types not accesible from root elements").
                      de("inkludiere auch Typen, die nicht über Root-Elemente erreichbar sind").
                      build() ),
    ;

    private Documentation doc;

    private GenerationOption(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
  }
  
  public static final StringParameter<EnumSet<Information>> INFORMATIONS= 
      StringParameter.typeEnumSet(Information.class, "information").
      label("Information").
      documentation(Documentation.
                    en("Print informations").
                    de("Ausgabe von Informationen").
                    build()).
      defaultValue(defaults.getInformations()).
      build();
  
  public enum Information implements DocumentedEnum {
    ListXSDs(Documentation.en("list all used XSDs").
             de("Listet alle verwendeten XSDs").
             build() ), 
    ListNamespaces(Documentation.en("list namespaces and their generated xmom paths").
             de("Listet alle Namespaces und die daraus generierten XMOM-Pfade").
             build()), 
    ListDatatypes(Documentation.en("list all generated datatypes").
                  de("Listet alle generierten Datentypen").
                  build()),
    ListDatatypeTree(Documentation.en("list all generated datatypes as inheritance tree").
                  de("Listet alle generierten Datentypen als Vererbungsbaum").
                  build()),
                  
    ;

    private Documentation doc;

    private Information(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
  }
  
  public static final StringParameter<Boolean> NO_IMPORT =
      StringParameter.typeBoolean("noImport").
      label("No Import").
      documentation(Documentation.
                    en("No Import, only analysis of xsds").
                    de("Kein Import, nur Analyse der XSDs").
                    build()).
      defaultValue(false).build();

  public static final StringParameter<Boolean> REMOVE_USED =
      StringParameter.typeBoolean("removedUsed").
      label("Remove Used").
      documentation(Documentation.
                    en("Remove also if already in use").
                    de("Entfernen des DataModels, auch wenn es bereits verwendet wird").
                    build()).
      defaultValue(defaults.getRemoveUsed()).build();
  
  public static final StringParameter<Boolean> REMOVE_COMPLETE =
      StringParameter.typeBoolean("complete").
      label("Remove Complete").
      documentation(Documentation.
                    en("Remove from all workspaces").
                    de("Entfernen des DataModels aus allen Workspaces").
                    build()).
      defaultValue(defaults.getRemoveComplete()).build();


  public static final StringParameter<WorkspaceChangeMode> WORKSPACE_MODE= 
      StringParameter.typeEnum(WorkspaceChangeMode.class, "workspaceMode").
      label("Workspace Mode").
      documentation(Documentation.
                    en("Change mode of workspace list").
                    de("Änderungsmodus der Workspace-Liste").
                    build()).
      defaultValue(WorkspaceChangeMode.Target).
      build();
  
  public enum WorkspaceChangeMode implements DocumentedEnum {
    Target(Documentation.en("target configuration of workspaces ").
             de("Zielkonfiguration der Workspaces").
             build() ), 
    Add(Documentation.en("workspaces will be added").
             de("Workspaces werden hinzugefügt").
             build()), 
    Remove(Documentation.en("workspaces will be removed").
                  de("Workspaces werden entfernt").
                  build()),
    ;

    private Documentation doc;

    private WorkspaceChangeMode(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }
  }
  
  
  public static final StringParameter<Boolean> DISTRIBUTE_TO_WORKSPACES = 
                  StringParameter.typeBoolean("distributeToWorkspaces").
                  label("Distribute generated types into workspaces").
                  documentation(Documentation.
                                en("Distribute generated types into workspaces").
                                de("Verteilung der generierten Typen in Arbeitsbereiche").
                                build()).
                  defaultValue(defaults.getDistributeToWorkspaces()).build();
  
  
  public static final StringParameter<List<String>> XSD_STORAGE =
                  StringParameter.typeList(String.class, "xsdStorage").
                  label("XSD Storage").
                  documentation(Documentation.
                                en("XSD Storage").
                                de("XSD Speicher").
                                build()).build();
  
  public static final StringParameter<List<String>> XSD_FILENAMES =
                  StringParameter.typeList(String.class, "xsdFileNames").
                  label("XSD Filenames").
                  documentation(Documentation.
                                en("XSD Filenames").
                                de("XSD Dateiname").
                                build()).build();
  
  @Deprecated
  public static final StringParameter<String> XML_DOCUMENT_PATH = 
      StringParameter.typeString("xmlDocumentPath").
      label("XML-Document path").
      documentation(Documentation.
                    en("deprecated, now constant").
                    de("Deprecated, ist nun Konstante").
                    build()).
      defaultValue(Constants.getUtilPath()).build();

  @Deprecated
  public static final StringParameter<String> BASE_TYPE = 
      StringParameter.typeString(ImportDataModelParameters.BASE_TYPE_NAME).
      label("Base Type").
      documentation(Documentation.
                    en("deprecated, now constant").
                    de("Deprecated, ist nun Konstante").
                    build()).
      defaultValue(Constants.getBase_XmomType().getFQTypeName()).build();
  
  public static final List<StringParameter<?>> configurableParameters = 
      StringParameter.asList( WORKSPACES, BASE_PATH, PATH_CUSTOMIZATION, LABEL_CUSTOMIZATION, 
                              OVERWRITE_MODEL, OVERWRITE_TYPES, INFORMATIONS, REMOVE_COMPLETE, REMOVE_USED, DISTRIBUTE_TO_WORKSPACES
                            );

  public static final List<StringParameter<?>> importParameters = 
      StringParameter.asList( NAME, WORKSPACES, BASE_PATH, PATH_CUSTOMIZATION, LABEL_CUSTOMIZATION, 
                              OVERWRITE_MODEL, OVERWRITE_TYPES, NO_IMPORT, INFORMATIONS, GENERATION_OPTIONS, DISTRIBUTE_TO_WORKSPACES,
                              XML_DOCUMENT_PATH, BASE_TYPE
                            );
  
  public static final List<StringParameter<?>> storedImportParameters = 
      StringParameter.asList( WORKSPACES, BASE_PATH, PATH_CUSTOMIZATION, LABEL_CUSTOMIZATION, DISTRIBUTE_TO_WORKSPACES );

  public static final List<StringParameter<?>> modifyParameters = 
      StringParameter.asList( WORKSPACES, WORKSPACE_MODE, OVERWRITE_MODEL, OVERWRITE_TYPES, REMOVE_USED
                            );

  public static final List<StringParameter<?>> removeParameters = 
      StringParameter.asList(REMOVE_USED, REMOVE_COMPLETE);




  private String basePath;
  private EnumMap<LabelCustomization,String> labelCustomization;
  private String dataModelName;
  private List<String> workspaces;
  private String pathCustomization;
  private EnumSet<Information> informations;
  private boolean overwriteModel;
  private boolean overwriteTypes;
  @SuppressWarnings("unused")
  private Map<String,String> asStringMap;
  @SuppressWarnings("unused")
  private Map<String, Object> map;
  private boolean removeUsed;
  private boolean noImport;
  private boolean removeComplete;
  private WorkspaceChangeMode workspaceMode;
  private EnumMap<GenerationOption, String> generationOptions;
  private String dataModelTypeName;
  private boolean distributeToWorkspaces;
  private List<String> xsdStorage;
  private List<String> xsdFileNames;
  
  private ImportParameter(Map<String, Object> map) {
    this.map = map;
  }
  
  public static ImportParameter parameterForImport(String dataModelTypeName, Map<String, Object> map) {
    ImportParameter ip = new ImportParameter(map);
    ip.dataModelTypeName = dataModelTypeName;
    ip.dataModelName = NAME.getFromMap(map);
    ip.basePath = BASE_PATH.getFromMap(map);
    ip.pathCustomization = PATH_CUSTOMIZATION.getFromMap(map);
    ip.labelCustomization = LABEL_CUSTOMIZATION.getFromMap(map);
    ip.workspaces = WORKSPACES.getFromMap(map);
    ip.informations = INFORMATIONS.getFromMap(map);
    ip.overwriteModel = OVERWRITE_MODEL.getFromMap(map); 
    ip.overwriteTypes = OVERWRITE_TYPES.getFromMap(map);
    ip.noImport = NO_IMPORT.getFromMap(map);
    ip.generationOptions = GENERATION_OPTIONS.getFromMap(map);
    ip.distributeToWorkspaces = DISTRIBUTE_TO_WORKSPACES.getFromMap(map);
    
    ip.asStringMap = StringParameter.toStringMap(importParameters, map, true);
    return ip;
  }

  public static ImportParameter parameterForModify(String dataModelTypeName, Map<String, Object> map) {
    ImportParameter ip = new ImportParameter(map);
    ip.dataModelTypeName = dataModelTypeName;
    ip.workspaces = WORKSPACES.getFromMap(map);
    ip.workspaceMode = WORKSPACE_MODE.getFromMap(map);
    ip.informations = INFORMATIONS.getFromMap(map);
    ip.asStringMap = StringParameter.toStringMap(modifyParameters, map, true);
    return ip;
  }
  
  public static ImportParameter parameterForRemove(String dataModelTypeName, Map<String, Object> map) {
    ImportParameter ip = new ImportParameter(map);
    ip.dataModelTypeName = dataModelTypeName;
    ip.removeUsed = REMOVE_USED.getFromMap(map);
    ip.removeComplete = REMOVE_COMPLETE.getFromMap(map);
    ip.asStringMap = StringParameter.toStringMap(modifyParameters, map, true);
    return ip;
  }


  
  private static String normalizePath(String string) {
    String n = string.trim().replaceAll("\\.+", ".");
    if( n.endsWith(".") ) {
      return n.substring(0,n.length()-1);
    }
    return n;
  }

  
  //Implementierung GenerationParameter
  public String getBasePath() {
    return basePath;
  }
  public EnumMap<LabelCustomization, String> getLabelCustomization() {
    return labelCustomization;
  }
  public String getPathCustomization() {
    return pathCustomization;
  }
  public String getDataModelName() {
    return dataModelName;
  }
  public boolean isGenerationOptions_expandChoice() {
    String val = generationOptions.get(GenerationOption.expandChoice);
    if( val == null ) {
      return false;
    }
    return "true".equals(val.toLowerCase());
  }
  public boolean isGenerationOptions_namedSimpleTypes() {
    String val = generationOptions.get(GenerationOption.namedSimpleTypes);
    if( val == null ) {
      return false;
    }
    return "true".equals(val.toLowerCase());
  }
  public boolean isGenerationOptions_includeHiddenTypes() {
    String val = generationOptions.get(GenerationOption.includeHiddenTypes);
    if( val == null ) {
      return false;
    }
    return "true".equals(val.toLowerCase());
  }
  public boolean isOverwrite() {
    return overwriteTypes;
  }
  /*public String getFQModelName() {
    return baseType.getPath()+"."+dataModelName;
  }*/
  
  public boolean isGenerateDataModelInfo() {
    return true; //TODO Konfigurierbar machen?
  }

  public String getDataModelTypeName() {
    return dataModelTypeName;
  }
  
  
  
  //weitere Parameter
  public boolean isOverwriteModel() {
    return overwriteModel;
  }
  
  public boolean isOverwriteTypes() {
    return overwriteTypes;
  }

  public boolean isNoImport() {
    return noImport;
  }

  public List<String> getWorkspaces() {
    return workspaces;
  }

  public EnumSet<Information> getInformations() {
    return informations;
  }

  public boolean isRemoveUsed() {
    return removeUsed;
  }
  
  public boolean isRemoveComplete() {
    return removeComplete;
  }

  public WorkspaceChangeMode getWorkspaceMode() {
    return workspaceMode;
  }

  public String getModelNamePrefix() {
    return Constants.getBase_XmomType().getPath();
  }
  
  public boolean getDistributeToWorkspaces() {
    return distributeToWorkspaces;
  }
  
  public List<String> getXSDStorage() {
    return xsdStorage;
  }
  
  public void setXSDStorage(List<String> xsdStorage) {
    this.xsdStorage = xsdStorage;
  }
  
  public List<String> getXSDFilenames() {
    return xsdFileNames;
  }
  
  public void setXSDFilenames(List<String> xsdFileNames) {
    this.xsdFileNames = xsdFileNames;
  }
  

  private static class InitialParameter {
    
    private DefaultValueModifiable<List<String>> workspaces = 
        new DefaultValueModifiable<List<String>>(Arrays.asList(RevisionManagement.DEFAULT_WORKSPACE.getName()));
    private DefaultValueModifiable<String> basePath = new DefaultValueModifiable<String>("xdnc.model.xsd");
    private DefaultValueModifiable<Boolean> overwriteModel = new DefaultValueModifiable<Boolean>(false);
    private DefaultValueModifiable<Boolean> overwriteTypes = new DefaultValueModifiable<Boolean>(false);
    private DefaultValueModifiable<String> pathCustomization = new DefaultValueModifiable<String>("/#basepath/#name/#nshost/#nspath");
    private DefaultValueModifiable<EnumMap<LabelCustomization,String>> labelCustomization = new DefaultValueModifiable<EnumMap<LabelCustomization,String>>(new EnumMap<LabelCustomization,String>(LabelCustomization.class));
    private DefaultValueModifiable<EnumSet<Information>> informations = new DefaultValueModifiable<EnumSet<Information>>(EnumSet.allOf(Information.class));
    private DefaultValueModifiable<EnumMap<GenerationOption,String>> generationOption = new DefaultValueModifiable<EnumMap<GenerationOption,String>>(new EnumMap<GenerationOption,String>(GenerationOption.class));
    private DefaultValueModifiable<Boolean> removeComplete = new DefaultValueModifiable<Boolean>(true);
    private DefaultValueModifiable<Boolean> removeUsed = new DefaultValueModifiable<Boolean>(false);
    private DefaultValueModifiable<Boolean> distributeToWorkspaces = new DefaultValueModifiable<Boolean>(false);
    
    public DefaultValueModifiable<List<String>> getWorkspaces() {
      return workspaces;
    }
    
    public DefaultValueModifiable<Boolean> getRemoveComplete() {
      return removeComplete;
    }

    public DefaultValueModifiable<Boolean> getRemoveUsed() {
      return removeUsed;
    }

    public DefaultValueModifiable<EnumSet<Information>> getInformations() {
      return informations;
    }

    public DefaultValueModifiable<String> getBasePath() {
      return basePath;
    }

    public DefaultValueModifiable<Boolean> getOverwriteModel() {
      return overwriteModel;
    }
    
    public DefaultValueModifiable<Boolean> getOverwriteTypes() {
      return overwriteTypes;
    }
    
    public DefaultValueModifiable<String> getPathCustomization() {
      return pathCustomization;
    }
    
    public DefaultValueModifiable<EnumMap<LabelCustomization, String>> getLabelCustomization() {
      return labelCustomization;
    }

    public DefaultValueModifiable<Boolean> getDistributeToWorkspaces() {
      return distributeToWorkspaces;
    }

    public DefaultValueModifiable<EnumMap<GenerationOption, String>> getGenerationOption() {
      return generationOption;
    }

    public void initialize(Map<String,Object> initParamMap) {
      workspaces.setDefaultValue( WORKSPACES.getFromMap(initParamMap) );
      basePath.setDefaultValue( normalizePath( BASE_PATH.getFromMap(initParamMap) ) );
      overwriteModel.setDefaultValue( OVERWRITE_MODEL.getFromMap(initParamMap) );
      overwriteTypes.setDefaultValue( OVERWRITE_TYPES.getFromMap(initParamMap) );
      pathCustomization.setDefaultValue( PATH_CUSTOMIZATION.getFromMap(initParamMap) );
      labelCustomization.setDefaultValue( LABEL_CUSTOMIZATION.getFromMap(initParamMap) );
      informations.setDefaultValue( INFORMATIONS.getFromMap(initParamMap) );
      removeComplete.setDefaultValue( REMOVE_COMPLETE.getFromMap(initParamMap) );
      removeUsed.setDefaultValue( REMOVE_USED.getFromMap(initParamMap) );
    }

  }

  public static void initDefaults(Map<String,Object> initParamMap) {
    defaults.initialize(initParamMap);
  }

}
