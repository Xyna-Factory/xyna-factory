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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModelSpecific;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listdatamodels;



public class ListdatamodelsImpl extends XynaCommandImplementation<Listdatamodels> {

  public void execute(OutputStream statusOutputStream, Listdatamodels payload) throws XynaException {
    
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    
    String dataModelType = payload.getDatamodeltype();
    
    List<DataModel> dataModels = null;
    if( dataModelType != null ) {
      dataModels = dmm.listDataModels(dataModelType);
    } else {
      dataModels = dmm.listDataModels();
    }
  
    if( dataModels == null || dataModels.isEmpty() ) {
      writeLineToCommandLine(statusOutputStream, "No data model is registered.");
      return;
    } else if( dataModels.size() == 1 ) {
      writeLineToCommandLine(statusOutputStream, "Registered data model:");
    } else {
      writeLineToCommandLine(statusOutputStream,  dataModels.size()+" registered data models:");
    }
    
    StringBuilder output = new StringBuilder();
    if( dataModelType != null ) {
      appendDataModels( output, dataModels, payload.getVerbose() );
    } else {
      Map<String, ArrayList<DataModel>> grouped = CollectionUtils.group(dataModels, new TypeGrouping() );
      List<String> dataModelTypes = new ArrayList<String>(grouped.keySet());
      Collections.sort(dataModelTypes);
    
      for( String dmt : dataModelTypes ) {
        output.append(dmt).append(":\n");
        appendDataModels( output, grouped.get(dmt), payload.getVerbose() );
      }
    }
    
    writeToCommandLine( statusOutputStream, output.toString() );
  }

  private void appendDataModels(StringBuilder output, List<DataModel> dataModels, boolean verbose) {
    Collections.sort(dataModels, new DataModelComparator() );
    for( DataModel dm : dataModels ) {
      output.append("  ").append( dm.getType().getLabel() ).append(" - ").append(dm.getVersion()).append("\n");
      if( verbose ) {
        appendVerbose( output, dm, "      ");
      }
    }
  }


  private void appendVerbose(StringBuilder output, DataModel dm, String indent) {
    if( dm.getDocumentation() != null && dm.getDocumentation().length() != 0 ) {
      output.append(indent).append(dm.getDocumentation()).append("\n");
    }
    if( dm.getXmomTypeCount() != null ) {
      output.append(indent).append(dm.getXmomTypeCount());
      if( dm.getDeployable() ) {
        output.append(" deployable");
      }
      output.append(" datatypes\n");
    }
    
    if( dm.getDataModelSpecifics() == null ) {
      return;
    }
    
    Map<String, ArrayList<DataModelSpecific>> grouped = CollectionUtils.group(dm.getDataModelSpecifics(), new GetLabel() );
    List<String> labels = new ArrayList<String>(grouped.keySet());
    Collections.sort(labels);
    for( String label : labels ) {
      List<DataModelSpecific> list = grouped.get(label);
      Collections.sort(list);
      output.append(indent).append(label);
      String sep = "=";
      for (DataModelSpecific dms : list) {
        output.append(sep).append(dms.getValue());
        sep = ", ";
      }
      output.append("\n");
    }
  }

  private static class GetLabel implements Transformation<DataModelSpecific,String> {
    public String transform(DataModelSpecific from) {
      return from.getLabel();
    }
  }
  

  private static class DataModelComparator implements Comparator<DataModel> {

    public int compare(DataModel o1, DataModel o2) {
      int c = o1.getDataModelType().compareTo(o2.getDataModelType());
      return c;
    }
    
  }
  
  private static class TypeGrouping implements Transformation<DataModel, String> {

    public String transform(DataModel from) {
      return from.getDataModelType();
    }
    
  }
  
}
