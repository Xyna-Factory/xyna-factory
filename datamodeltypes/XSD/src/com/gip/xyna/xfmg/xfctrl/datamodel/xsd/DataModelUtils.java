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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.RandomAccessArrayList;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator.LabelCustomization;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel.Builder;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType;


public class DataModelUtils {
  
  public static DataModel getDataModel(ImportParameter importParameter, String dataModelType) {
    Builder dataModelBuilder = new Builder();
    
    dataModelBuilder.baseType( Constants.getBase_XmomType().toXynaObject() );
    dataModelBuilder.dataModelType(dataModelType);
    String version = "0";
    dataModelBuilder.version( version );
    dataModelBuilder.documentation("");
    dataModelBuilder.deployable(true);
    dataModelBuilder.type(getXMOMType(importParameter.getDataModelName(), importParameter.getModelNamePrefix(), version));
    DataModel dataModel = dataModelBuilder.instance();
    
    ArrayList<DataModelSpecific>  storedImportParameters = listStoredImportParameter(importParameter);
    dataModel.setDataModelSpecifics(storedImportParameters);
    
    return dataModel;
  }
  
  
  public static XmomType getXMOMType(String dataModelName, String modelNamePrefix, String version) {
    return new XmomType(modelNamePrefix + ".v" + version.replaceAll("\\.", "_"), dataModelName, dataModelName);
  }

  
  private static ArrayList<DataModelSpecific> listStoredImportParameter(ImportParameter importParameter) {
    ArrayList<DataModelSpecific> dmsList = new ArrayList<DataModelSpecific>();
    StringParameter<?> sp;
    
    sp = ImportParameter.BASE_PATH;
    appendDataModelSpecific( dmsList, sp, null, importParameter.getBasePath(), true);
    
    sp = ImportParameter.PATH_CUSTOMIZATION;
    appendDataModelSpecific( dmsList, sp, null, importParameter.getPathCustomization(), false);
    
    sp = ImportParameter.DISTRIBUTE_TO_WORKSPACES;
    appendDataModelSpecific( dmsList, sp, null, Boolean.toString(importParameter.getDistributeToWorkspaces()), false);
    
    sp = ImportParameter.LABEL_CUSTOMIZATION;
    for( Map.Entry<LabelCustomization,String> entry : importParameter.getLabelCustomization().entrySet() ) {
      appendDataModelSpecific( dmsList, sp, entry.getKey(), entry.getValue(), false);
    }
    
    return dmsList;
  }

  public static ArrayList<DataModelSpecific> listWorkspaces(List<String> workspaces) {
    ArrayList<DataModelSpecific> dmsList = new ArrayList<DataModelSpecific>();
    StringParameter<?> sp = ImportParameter.WORKSPACES;
    for( int i=0; i<workspaces.size(); ++i ) {
      appendDataModelSpecific( dmsList, sp, i, workspaces.get(i), false);
    }
    return dmsList;
  }
  
  
  public static ArrayList<DataModelSpecific> listXSDStorage(List<String> xsdStorage) {
    ArrayList<DataModelSpecific> dmsList = new ArrayList<DataModelSpecific>();
    StringParameter<?> sp = ImportParameter.XSD_STORAGE;
    for( int i=0; i<xsdStorage.size(); ++i ) {
      appendDataModelSpecific( dmsList, sp, i, xsdStorage.get(i), true);
    }
    return dmsList;
  }
  
  
  public static List<DataModelSpecific> listXSDFilenames(List<String> xsdFilenames) {
    ArrayList<DataModelSpecific> dmsList = new ArrayList<DataModelSpecific>();
    StringParameter<?> sp = ImportParameter.XSD_FILENAMES;
    for( int i=0; i<xsdFilenames.size(); ++i ) {
      appendDataModelSpecific( dmsList, sp, i, xsdFilenames.get(i), true);
    }
    return dmsList;
  }

 
  
  
  
  private static void appendDataModelSpecific(ArrayList<DataModelSpecific> dmsList, StringParameter<?> sp,
                                              Object keyExtension, String value, boolean serverOnly) {
    StringBuilder key = new StringBuilder();
    key.append(serverOnly ? "server." : "%0%.");
    key.append(sp.getName());
    if( keyExtension != null ) {
      if( keyExtension instanceof Integer ) {
        key.append("[\"").append(keyExtension).append("\"]");
      } else {
        key.append(".").append(keyExtension);
      }
    }
    dmsList.add(new DataModelSpecific(key.toString(), value, sp.getLabel() ));
  }


  public static Set<String> getWorkspaces(DataModel dataModel) {
    Set<String> workspaces = new HashSet<String>();
    String prefix = "%0%."+ ImportParameter.WORKSPACES.getName();
    for( DataModelSpecific dms : dataModel.getDataModelSpecifics() ) {
      if( dms.getKey() != null && dms.getKey().startsWith(prefix) ) {
        workspaces.add( dms.getValue());
      }
    }
    return workspaces;
  }


  public static List<DataModelSpecific> extractWorkspaces(DataModel dataModel) {
    List<DataModelSpecific> workspaces = new ArrayList<DataModelSpecific>();
    String prefix = "%0%."+ ImportParameter.WORKSPACES.getName();
    for( DataModelSpecific dms : dataModel.getDataModelSpecifics() ) {
      if( dms.getKey().startsWith(prefix) ) {
        workspaces.add( dms );
      }
    }
    return workspaces;
  }
  
  
  private final static Pattern XSD_STORAGE_INDEX_EXTRACTION = Pattern.compile("server." + ImportParameter.XSD_STORAGE.getName()+"\\[\"([0-9])+\"\\]");
  
  public static List<String> extractXSDStorage(DataModel dataModel) {
    List<String> xsdStorage = new RandomAccessArrayList<String>();
    for( DataModelSpecific dms : dataModel.getDataModelSpecifics() ) {
      Matcher matcher = XSD_STORAGE_INDEX_EXTRACTION.matcher(dms.getKey());
      if (matcher.matches()) {
        xsdStorage.set(Integer.parseInt(matcher.group(1)), dms.getValue());
      }
    }
    return xsdStorage;
  }
  
  
  private final static Pattern XSD_FILENAMES_INDEX_EXTRACTION = Pattern.compile("server." + ImportParameter.XSD_FILENAMES.getName()+"\\[\"([0-9])+\"\\]");
  
  public static List<String> extractXSDFileNames(DataModel dataModel) {
    List<String> xsdFilenames = new RandomAccessArrayList<String>();
    for( DataModelSpecific dms : dataModel.getDataModelSpecifics() ) {
      Matcher matcher = XSD_FILENAMES_INDEX_EXTRACTION.matcher(dms.getKey());
      if (matcher.matches()) {
        xsdFilenames.set(Integer.parseInt(matcher.group(1)), dms.getValue());
      }
    }
    return xsdFilenames;
  }


  public static Set<String> listDatatypes(DataModel dataModel) {
    Set<String> dataTypes = new HashSet<String>();
    for( XmomType type : dataModel.getXmomTypes() ) {
      dataTypes.add( type.getFqName() );
    }
    return dataTypes;
  }
  
  
  public static boolean isApplication(DataModel dataModel) {
    String prefix = "%0%."+ ImportParameter.DISTRIBUTE_TO_WORKSPACES.getName();
    for( DataModelSpecific dms : dataModel.getDataModelSpecifics() ) {
      if( dms.getKey().startsWith(prefix) ) {
        return dms.getValue().equals("false");
      }
    }
    return false;
  }
  


  

}
