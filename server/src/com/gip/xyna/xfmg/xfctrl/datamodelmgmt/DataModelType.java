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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt;

import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_ParseStringToDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_WriteDataModelToStringException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Path.PathCreationVisitor;


/**
 * DataModelType sammelt die Methoden, die alle konkreten DataModelType-Implementierungen erfüllen müssen.
 * 
 * Wird implementiert von MIB, TR069...
 *
 */
public interface DataModelType {


  public void importDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage,
                                         Map<String, Object> importParamMap, List<String> files);


  public void modifyDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              DataModel dataModel, Map<String, Object> paramMap);


  public void removeDataModel(DataModelResult dataModelResult, DataModelStorage dataModelStorage, 
                              DataModel dataModel, Map<String, Object> paramMap);


  /**
   * Initialisierung des DataModelTypes, Rückgabe der initialen PluginDescription
   * @param name Name des Datenmodells
   * @return PluginDescription
   * @throws XynaException
   */
  public PluginDescription init(String name) throws XynaException ;
  
  /**
   * Konfiguration der Default-Parameter, Rückgabe der angepassten PluginDescription
   * @param paramMap
   * @return PluginDescription
   */
  public PluginDescription configureDefaults(Map<String, Object> paramMap);
  
  
  /**
   * Anzeige der PluginDescription.
   * Diese sollte vom DataModelType gecacht werden, da showDescription() häufiger gerufen werden kann
   * @return PluginDescription
   */
  public PluginDescription showDescription();
  
  
  public void shutdown();

  
  public PathCreationVisitor getPathCreationVisitor(DOM dom);


  /**
   * Schreiben des XynaObjects in einen String
   * @param dataModelStorage
   * @param dataModelName
   * @param paramMap
   * @param data
   * @param revision
   * @return
   * @throws XFMG_WriteDataModelToStringException
   */
  public String writeDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                               Map<String, Object> paramMap, XynaObject data, long revision
                               ) throws XFMG_WriteDataModelToStringException;


  /**
   * Lesen des Strings und Erzeugen eines XynaObjects in angegebener Revision
   * @param dataModelStorage
   * @param dataModelName
   * @param paramMap
   * @param data
   * @param revision
   * @return
   * @throws XFMG_ParseStringToDataModelException
   */
  public XynaObject parseDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                                   Map<String, Object> paramMap, String data, long revision
                                   ) throws XFMG_ParseStringToDataModelException;


  

  
}
