/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package xdev.xtestfactory.infrastructure.gui.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;

import base.Text;
import xdev.xtestfactory.infrastructure.gui.UtilsServiceOperation;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;
import xnwh.persistence.Storable;
import xprc.xpce.datatype.NamedVariableMember;


public class UtilsServiceOperationImpl implements ExtendedDeploymentTask, UtilsServiceOperation {

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  @Override
  public Text labelToJavaName(Text text) {
    return new Text(com.gip.xyna.xprc.xfractwfe.generation.xml.Utils.labelToJavaName(text.getText(), true));
  }
  
  @Override
  public List<? extends Storable> filterLimitAndSortStorable(List<? extends Storable> storables, TableInfo tableInfo) {
    if(storables == null || storables.isEmpty())
      return storables;
    TableHelper<Storable, TableInfo> tableHelper = createTableHelper(storables.get(0), tableInfo);
    List<Storable> storableList = storables.stream().filter(tableHelper.filter()).collect(Collectors.toList());
    tableHelper.sort(storableList);
    return tableHelper.limit(storableList);
  }
  
  @Override
  public List<? extends Storable> limitStorable(List<? extends Storable> storables, TableInfo tableInfo) {
    if(storables == null || storables.isEmpty())
      return storables;
    TableHelper<Storable, TableInfo> tableHelper = createTableHelper(storables.get(0), tableInfo);
    List<Storable> storableList = new ArrayList<>(storables.size());
    storableList.addAll(storables);
    return tableHelper.limit(storableList);
    }  
  
  @Override
  public List<? extends Storable> sortStorable(List<? extends Storable> storables, TableInfo tableInfo) {
    if(storables == null || storables.isEmpty() || storables.size() == 1)
      return storables; 
    
    TableHelper<Storable, TableInfo> tableHelper = createTableHelper(storables.get(0), tableInfo);
    List<Storable> storableList = new ArrayList<>(storables.size());
    storableList.addAll(storables);
    tableHelper.sort(storableList);
    return storableList;
  }
  
  @Override
  public List<? extends Storable> filterStorable(List<? extends Storable> storables, TableInfo tableInfo) {
    if(storables == null || storables.isEmpty())
      return storables;
    
    TableHelper<Storable, TableInfo> tableHelper = createTableHelper(storables.get(0), tableInfo);
    return storables.stream().filter(tableHelper.filter()).collect(Collectors.toList());
  }
  
  private TableHelper<Storable, TableInfo> createTableHelper(Storable storable, TableInfo tableInfo){
    TableHelper<Storable, TableInfo> helper = TableHelper.<Storable, TableInfo>init(tableInfo);
    Set<String> variableNames = storable.getVariableNames();
    for (String variableName : variableNames) {
      helper.addSelectFunction(variableName, s -> {
        try {
          return s.get(variableName);
        } catch (InvalidObjectPathException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      });
    }
    return helper.limitConfig(TableInfo::getLimit)
    .sortConfig(ti -> {
      for (TableColumn tc : ti.getColumns()) {
        TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
        if(sort != null)
          return sort;
      }
      return null;
    })
    .filterConfig(ti -> 
      ti.getColumns().stream()
      .filter(tableColumn -> 
        !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null
      )
      .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
      .collect(Collectors.toList())
    );
  }

  @Override
  public List<? extends NamedVariableMember> filterVariableMembers(List<? extends NamedVariableMember> members, List<? extends Text> namesToRemove) {
    List<NamedVariableMember> filterdMembers = new ArrayList<NamedVariableMember>();
    for (NamedVariableMember member : members) {
      boolean keepMember = true;
      for (Text nameToRemove : namesToRemove) {
        if (nameToRemove != null && nameToRemove.getText() != null && nameToRemove.getText().equals(member.getVarName())) {
          keepMember = false;
          break;
        }
      }

      if (keepMember) {
        filterdMembers.add(member);
      }
    }

    return filterdMembers;
  }

}
