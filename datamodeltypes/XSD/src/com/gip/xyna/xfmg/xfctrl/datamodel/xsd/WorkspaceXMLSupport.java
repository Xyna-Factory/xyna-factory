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

import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_ParseStringToDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_WriteDataModelToStringException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.WorkspaceHelper.AdditionalWorkspaceOperations;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.GeneralParseXMLException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.GeneralWriteXMLException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.ParseXMLCreateXynaObjectException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.TypeMapperInstantiationParseException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.exceptions.TypeMapperInstantiationWriteException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.GenerationParameter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.XMLDocumentUtilsGenerator;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.XMLParserWriterCache;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.XMLParserWriterCache.XMLParserWriter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.TypeMapperCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XmlCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.exceptions.XynaObjectCreationException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelStorage;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionMap;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionMap.ValueCreator;


public class WorkspaceXMLSupport implements AdditionalWorkspaceOperations, ValueCreator<XMLParserWriterCache> {

  
  private String dataModelTypeName;
  private RevisionMap<XMLParserWriterCache> xmlParserWriterCaches;
  
  public WorkspaceXMLSupport(String dataModelTypeName) {
    this.dataModelTypeName = dataModelTypeName;
    this.xmlParserWriterCaches = new RevisionMap<XMLParserWriterCache>(this);
  }

  public void initialize(DataModelResult dataModelResult, GenerationParameter generationParameter, long revision) {
    XMLDocumentUtilsGenerator xmlDocumentUtilsGenerator = new XMLDocumentUtilsGenerator(generationParameter);
    xmlDocumentUtilsGenerator.createXMLUtils(dataModelResult, revision);
  }
  
  public void clean(DataModelResult dataModelResult, String dataModelName, long revision) {
    XMLParserWriterCache xmlParserWriterCache = xmlParserWriterCaches.getOrCreate(revision);
    xmlParserWriterCache.remove(dataModelName);
  }

  public String writeDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                               Map<String, Object> paramMap, XynaObject xynaObject, long revision
                               ) throws XFMG_WriteDataModelToStringException {
    XMLParserWriterCache xmlParserWriterCache = xmlParserWriterCaches.getOrCreate(revision);
    try {
      XMLParserWriter xmlParserWriter = xmlParserWriterCache.getOrCreate(dataModelStorage,dataModelName,dataModelVersion);
      return xmlParserWriter.writeDataModel(paramMap, xynaObject);
    } catch (XynaException e) {
      throw new GeneralWriteXMLException(e.getMessage(), e);
    } catch (TypeMapperCreationException e) {
      throw new TypeMapperInstantiationWriteException(e.getMessage(), e);
    } catch (XmlCreationException e) {
      throw new RuntimeException(e);
    }
  }
  
  public XynaObject parseDataModel(DataModelStorage dataModelStorage, String dataModelName, String dataModelVersion,
                                   Map<String, Object> paramMap, String data, long revision
                                   ) throws XFMG_ParseStringToDataModelException {
    XMLParserWriterCache xmlParserWriterCache = xmlParserWriterCaches.getOrCreate(revision);
    try {
      XMLParserWriter xmlParserWriter = xmlParserWriterCache.getOrCreate(dataModelStorage,dataModelName,dataModelVersion);
      return xmlParserWriter.parseDataModel(paramMap, data);
    } catch (XynaException e) {
      throw new GeneralParseXMLException(e.getMessage(), e);
    } catch (TypeMapperCreationException e) {
      throw new TypeMapperInstantiationParseException(e.getMessage(), e);
    } catch (XynaObjectCreationException e) {
      throw new ParseXMLCreateXynaObjectException( e.getType(), e.getMessage(), e);
    }
  }

  public XMLParserWriterCache construct(long revision) {
    return new XMLParserWriterCache(revision, dataModelTypeName);
  }

  public void destruct(long revision, XMLParserWriterCache value) {
    //nichts zu tun
  }


}
