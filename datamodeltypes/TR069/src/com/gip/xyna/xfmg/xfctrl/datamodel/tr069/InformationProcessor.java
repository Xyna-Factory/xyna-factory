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

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.console.Colorizer;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.ImportParameter.Information;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRComponent;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRDataModelDefinition;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRModel;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObject;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObjectContainer;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRObjectReference;
import com.gip.xyna.xfmg.xfctrl.datamodel.types.TRParameter;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;


/**
 *
 */
public class InformationProcessor {

  private TR069Import tr069Import;
  private TR069Reader tr069Reader;
  private ImportParameter parameter;
  private EnumSet<Information> informations;
  
  public InformationProcessor(TR069Import tr069Import, ImportParameter parameter) {
    this.tr069Import = tr069Import;
    this.tr069Reader = tr069Import.getTr069Reader();
    this.parameter = parameter;
    this.informations = parameter.getInformations();
  }


  public void process(DataModelResult dataModelResult) {
    Colorizer colorizer = Colorizer.colorize( informations.contains(Information.Colorize) );
    boolean changes = informations.contains(Information.Changes);
    
    
    if( informations.contains(Information.Documents) ) {
      documents(dataModelResult,colorizer);
    }
    if( informations.contains(Information.ObjectModelTree) ) {
      modelTree(dataModelResult, colorizer, false, changes);
    }
    if( informations.contains(Information.FullModelTree) ) {
      modelTree(dataModelResult, colorizer, true, changes);
    }
  }

  private void documents(DataModelResult dataModelResult, Colorizer colorizer) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nDocuments:\n");
    for( TRDataModelDefinition document: tr069Reader.getDocuments() ) {
      if( document.getModels().size() == 0 ) {
        sb.append("Document ").append(document.getName()).append("\n");
      } else {
        sb.append(colorizer.getColor(document.getFileName()));
        sb.append("Document ").append(document.getName()).append(" has:");
        for( TRModel model : document.getModels() ) {
          sb.append("\n  Model ").append(model.getFqName()).append(" with ");
          sb.append(model.getObjectCount()).append(" objects" );
          int components = model.getComponentNames().size();
          if( components != 0 ) {
            sb.append(" and ").append(components).append(" components" ); 
          }
        }
        for( TRComponent component : document.getComponents() ) {
          sb.append("\n  Component ").append(component.getName()).append(" with ");
          sb.append(component.getObjectCount()).append(" objects " );
        }
        sb.append(colorizer.normal()).append("\n");
      }
    }
    dataModelResult.info( sb.toString() );
  }
  
  private void modelTree(DataModelResult dataModelResult, Colorizer colorizer, boolean withParameter, boolean changes) {
    for( String modelName : tr069Import.getSelectedModels() ) {
      StringBuilder sb = new StringBuilder();
      TRModel model = tr069Import.getTr069Reader().getModel(modelName);
      sb.append("\nModel ").append(model.getFqName());
      sb.append(" (").append(model.getDocument().getFileName() ).append("):\n");
      for( TRObject root : model.getRootObjects() ) {
        printObjectsRecursively(sb, "", model, root, colorizer, withParameter, changes);
      }
      dataModelResult.info( sb.toString() );
    }
  }


  private void printObjectsRecursively(StringBuilder sb, String parent, TRObjectContainer container, TRObject object, 
                                       Colorizer colorizer, boolean withParameter, boolean changes) {
    String objectName = object.getName()+"."+(object.isListInParent()?"{i}.":"");
    String parentObjectName = parent + colorizer.getColor(object.getFileName()) + objectName;
    
    if( changes ) {
      sb.append(parent).append(colorizer.getColor(object.getFileNameOfLastChange())).append(objectName);
    } else {
      sb.append(parentObjectName);
    }
    sb.append(colorizer.normal()).append("\n");
    
    if( withParameter ) {
      for( TRParameter param : object.getParameters() ) {
        String fileName = changes ? param.getFileNameOfLastChange() : param.getFileName();
        sb.append(parentObjectName).append(colorizer.getColor(fileName));
        sb.append( param.getName() ).append(" ").append(param.getType());
        sb.append(colorizer.normal()).append("\n");
      }
    }
    for( TRObjectReference childRef : object.getChildren() ) {
      Pair<TRObjectContainer,TRObject> child = childRef.getObjectFrom(container);
      if( child != null ) {
        printObjectsRecursively(sb, parentObjectName, child.getFirst(), child.getSecond(), colorizer, withParameter, changes);
      } else {
        System.err.println("###No child "+ childRef); //FIXME
      }
    }
  }  
  
  public boolean proceedImport() {
    return ! informations.contains(Information.NoImport);
  }

}
