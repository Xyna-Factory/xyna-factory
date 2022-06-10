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
package xdev.xtestfactory.util.persistence.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceOperations;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.IFormula.Accessor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;

import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.SecurityException;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import xdev.xtestfactory.util.persistence.ExternalJoinQueryParameter;
import xdev.xtestfactory.util.persistence.ExternalJoinResult;
import xnwh.persistence.ExtendedParameter;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;
import xnwh.persistence.XMOMStorableAccessException;
import xdev.xtestfactory.util.persistence.PersistenceUtilsServiceOperation;


public class PersistenceUtilsServiceOperationImpl implements ExtendedDeploymentTask, PersistenceUtilsServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
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

  public List<? extends ExternalJoinResult> externalJoinQuery(XynaOrderServerExtension xose, SelectionMask selectionMask, FilterCondition filterCondition, ExternalJoinQueryParameter externalJoinQueryParameter, ExtendedParameter extendedParameter) throws XMOMStorableAccessException {
    String formula = externalJoinQueryParameter.getEquiJoinCondition().getFormula();
    Long revision = xose.getRootOrder().getRevision();
    final XMOMStorableStructureInformation rootInfo = getStorableStructureInformation(selectionMask.getRootType(), revision);
    final XMOMStorableStructureInformation joinedInfo = getStorableStructureInformation(externalJoinQueryParameter.getJoinedType(), revision).generateMergedClone(revision);
    String[] accessPaths = formula.split("==");
    Stack<StorableColumnInformation> rootJoinColumnStack = resolveColumnStack(rootInfo, accessPaths[0]);
    Stack<StorableColumnInformation> joinJoinColumnStack = resolveColumnStack(joinedInfo, accessPaths[1]);
    if (joinJoinColumnStack.size() > 1) {
      throw new RuntimeException("Joining on nested tables currently not supported!");
    }
    StorableColumnInformation joinJoinColumn = joinJoinColumnStack.pop();
    
    XMOMStorableStructureInformation newRootInfo = rootInfo.generateMergedClone(revision); 
    StorableStructureInformation currentInfo = newRootInfo;
    for (StorableColumnInformation sci : rootJoinColumnStack) {
      StorableColumnInformation newColumn = sci.clone(currentInfo);
      getColumnInformation(currentInfo).put(newColumn.getColumnName(), newColumn);
      if (newColumn.isStorableVariable()) {
        currentInfo = newColumn.getStorableVariableInformation();
      }
    }
    
    final StorableColumnInformation newColumn = currentInfo.getColumnInfo(rootJoinColumnStack.peek().getVariableName());
    linkColumn(newColumn, joinJoinColumn); 
    
    final Map<Object, List<GeneralXynaObject>> joins = new HashMap<Object, List<GeneralXynaObject>>();
    final ResultSetReader<?> relevantReader = currentInfo.getResultSetReaderForDatatype();
    final StorableStructureInformation relevantInfo = currentInfo;
    
    try {
      final Class<? extends GeneralXynaObject> classOfJoinedType = (Class<? extends GeneralXynaObject>) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .getMDMClassLoader(externalJoinQueryParameter.getJoinedType(), revision, true).loadClass(externalJoinQueryParameter.getJoinedType(), true);
    
      ResultSetReader<Object> resultReader = new ResultSetReader<Object>() {
  
        public Object read(ResultSet rs) throws SQLException {
          Object result = relevantReader.read(rs);
          StorableColumnInformation parentFKCol = relevantInfo.getColInfoByVarType(VarType.EXPANSION_PARENT_FK);
          if (parentFKCol == null) {
            parentFKCol = relevantInfo.getColInfoByVarType(VarType.PK);
          }
          Object parentFK = readFromResultSet(parentFKCol, rs);
          
          List<GeneralXynaObject> joinsForPk = joins.get(parentFK);
          if (joinsForPk == null) {
            joinsForPk = new ArrayList<GeneralXynaObject>();
            joins.put(parentFK, joinsForPk);
          }
          Object possibleJoin = rs.getObject(newColumn.getColumnName());
          if (possibleJoin instanceof GeneralXynaObject) {
            if (classOfJoinedType.isInstance(possibleJoin)) {
              joinsForPk.add((GeneralXynaObject) possibleJoin);
            }
          }
          return result;
        }
  
        private Object readFromResultSet(StorableColumnInformation parentFKCol, ResultSet rs) throws SQLException {
          return parentFKCol.getPrimitiveType().fromString(rs.getString(parentFKCol.getColumnName()));
        }
      };
      currentInfo.setResultSetReaderForDatatype(resultReader);
    
    } catch (XFMG_MDMObjectClassLoaderNotFoundException | ClassNotFoundException e1) {
      throw new RuntimeException(e1);
    }
    
    try {
      List<? extends GeneralXynaObject> rootResult = 
          (List<? extends GeneralXynaObject>) getOperationsImpl().query(null, // null order to prevent reResolve of columns
                                                                        Conversions.convertSelectionMask(selectionMask), new FilterCond(filterCondition),
                                                                        Conversions.convertQueryParameter(externalJoinQueryParameter), revision, 
                                                                        Conversions.convertExtendedParameter(extendedParameter), newRootInfo);
      List<ExternalJoinResult> resultt = new ArrayList<ExternalJoinResult>();
      for (GeneralXynaObject gxo : rootResult) {
        Object rootPk = newRootInfo.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER).getFromDatatype((XynaObject) gxo);
        if (joins.containsKey(rootPk)) {
          if (joins.get(rootPk).size() > 0) {
            Collections.reverse(joins.get(rootPk));
            for (GeneralXynaObject join : joins.get(rootPk)) {
              resultt.add(new ExternalJoinResult((Storable) gxo, (Storable) join));
            }  
          } else {
            resultt.add(new ExternalJoinResult((Storable) gxo, null));
          }
        } else {
          resultt.add(new ExternalJoinResult((Storable) gxo, null));
        }
      }
      return resultt;
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private Map<String, StorableColumnInformation> getColumnInformation(StorableStructureInformation ssi) {
    try {
      Field field = StorableStructureInformation.class.getDeclaredField("columnInformation");
      field.setAccessible(true);
      return (Map<String, StorableColumnInformation>) field.get(ssi);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException("Failed to access columnInformation",e);
    }
  }
  
  private void linkColumn(StorableColumnInformation sourceColumn, StorableColumnInformation joinTargetColumn) {
    for (StorableColumnInformationFields scif : StorableColumnInformationFields.values()) {
      scif.adjustField(sourceColumn, joinTargetColumn);
    }
  }
  
  
  private Stack<StorableColumnInformation> resolveColumnStack(XMOMStorableStructureInformation rootInfo, String string) {
    Stack<StorableColumnInformation> stack = new Stack<StorableColumnInformation>();
    String[] parts = string.split("\\.");
    StorableStructureInformation structure = rootInfo;
    for (int i = 1; i < parts.length; i++) {
      StorableColumnInformation column = structure.getColumnInfo(parts[i]);
      if (column == null) {
        StorableStructureInformation current = structure;
        while (current.hasSuper()) {
          current = current.getSuperEntry().getInfo();
          column = current.getColumnInfo(parts[i]);
          if (column != null) {
            break;
          }
        }
      }
      stack.push(column);
      if (i+1 == parts.length) {
        return stack;
      }
      if (column.isStorableVariable()) {
        structure = column.getStorableVariableInformation();
      } else {
        structure = null;
      }
    }
    throw new RuntimeException("Failed to resolve column");
  }
  

  private XMOMStorableStructureInformation getStorableStructureInformation(String roottype, Long revision) {
    try {
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(roottype, revision);
      return getStructureCache(rev).getStructuralInformation(GenerationBase.transformNameForJava(roottype));
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static XMOMStorableStructureCache getStructureCache(Long revision) {
    return XMOMStorableStructureCache.getInstance(revision);
  }
  
  private XMOMPersistenceOperations getOperationsImpl() {
    return XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
  }



}
