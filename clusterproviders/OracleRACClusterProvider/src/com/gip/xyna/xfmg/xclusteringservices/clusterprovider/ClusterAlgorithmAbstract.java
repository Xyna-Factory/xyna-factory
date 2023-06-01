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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;


public abstract class ClusterAlgorithmAbstract implements ClusterAlgorithm {

  protected static Logger logger = CentralFactoryLogging.getLogger(ClusterAlgorithm.class);
  
  void setStateInternally(SQLUtils sqlUtils, int binding, ClusterState state, boolean online) {
    sqlUtils.executeDML(SQLStrings.UPDATE_STATE_FOR_BINDING_SQL,
      new ExtendedParameter(state.toString(), new BooleanWrapper(online), binding) );
  }

  
  public static class ClusterSetupRowsForUpdate {
    
    private ArrayList<XynaClusterSetup> rows;
    private ArrayList<XynaClusterSetup> othersRows;
    private XynaClusterSetup ownRow;
    private int ownBinding;

    public ClusterSetupRowsForUpdate(SQLUtils sqlUtils) {
      read(sqlUtils);
    }
    
    public ClusterSetupRowsForUpdate(SQLUtils sqlUtils, int ownBinding) {
      read(sqlUtils);
      this.ownBinding = ownBinding;
      extractOwn();
    }

    public List<XynaClusterSetup> getAll() {
      return rows;
    }

    public XynaClusterSetup getOwn() {
      if( othersRows == null ) { //othersRows ist nach extractOwn zumindest leeres Array, daher != null
        extractOwn();
      }
      return ownRow;
    }

    public int read(SQLUtils sqlUtils) {
      rows = sqlUtils.query(SQLStrings.SELECT_ALL_BINDINGS_FOR_UPDATE_SQL, new Parameter(), XynaClusterSetup.reader);
      if( rows == null ) {
        rows = new ArrayList<XynaClusterSetup>(); //Fehler: rows konnten nicht gelesen werden, 
        //dieser Fehler wird hier nun unterdrückt
      }
      return rows.size();
    }

    public List<XynaClusterSetup> getOthers() {
      if( othersRows == null ) {
        extractOwn();
      }
      return othersRows;
    }

    private void extractOwn() {
      othersRows = new ArrayList<XynaClusterSetup>();
      for( XynaClusterSetup csr : rows ) {
        if( csr.getBinding() == ownBinding ) {
          ownRow = csr;
        } else {
          othersRows.add(csr);
        }
      }
    }

    public int size() {
      return rows.size();
    }

    public void setOwnBinding(int ownBinding) {
      this.ownBinding = ownBinding;
    }

    public int getMaxBinding() {
      //rows ist sortiert nach Binding, daher ist letzter Eintrag der mit größtem Binding
      return rows.get(rows.size() - 1).getBinding();
    }
    
    public ClusterAlgorithm checkAndGetClusterAlgorithm() {
      int cnt = size();
      if (cnt == 0 ) {
        throw new RuntimeException("Central cluster setup table is empty!");
      }
      if (getOwn() == null ) {
        throw new RuntimeException("Central cluster setup table does not contain local binding");
      }

      if (cnt == 1) {
        return new ClusterAlgorithmSingleNode();
      } else if (cnt == 2) {
        return new ClusterAlgorithmTwoNodes();
      } else {
        return new ClusterAlgorithmMultiNodes();
      }
    }

    
  }


  
  
}
