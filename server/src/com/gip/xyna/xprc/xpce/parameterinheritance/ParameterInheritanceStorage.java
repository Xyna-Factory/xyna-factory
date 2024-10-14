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
package com.gip.xyna.xprc.xpce.parameterinheritance;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.storables.InheritanceRuleStorable;


public class ParameterInheritanceStorage {

  private static Logger logger = CentralFactoryLogging.getLogger(ParameterInheritanceStorage.class);

  
  private final ODSImpl ods;
  private final PreparedQueryCache queryCache;
  
  private static final String QUERY_GET_INHERITANCE_RULE_CHILD = "select * from " + InheritanceRuleStorable.TABLENAME + " where "
                  + InheritanceRuleStorable.COL_PARAMETERTYPE + " = ? and " + InheritanceRuleStorable.COL_ORDERTYPE + " = ? and "
                  + InheritanceRuleStorable.COL_REVISION + " = ? and " + InheritanceRuleStorable.COL_CHILDFILTER + " = ? for update";
  private static final String QUERY_GET_INHERITANCE_RULE_OWN = "select * from " + InheritanceRuleStorable.TABLENAME + " where "
                  + InheritanceRuleStorable.COL_PARAMETERTYPE + " = ? and " + InheritanceRuleStorable.COL_ORDERTYPE + " = ? and "
                  + InheritanceRuleStorable.COL_REVISION + " = ? and (" 
                  + InheritanceRuleStorable.COL_CHILDFILTER + " is null or childFilter = '') for update";
  private static final String QUERY_GET_INHERITANCE_RULES_FOR_ORDERTYPE = "select * from " + InheritanceRuleStorable.TABLENAME + " where "
                  + InheritanceRuleStorable.COL_ORDERTYPE + " = ? and "
                  + InheritanceRuleStorable.COL_REVISION + " = ? for update";
  
  public ParameterInheritanceStorage() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    
    ods.registerStorable(InheritanceRuleStorable.class);
    
    queryCache = new PreparedQueryCache();
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
  
  /**
   * Ändert eine bestehende InheritanceRule oder legt sie neu an, falls sie noch nicht existiert.
   * @param parameterType
   * @param dk
   * @param inheritanceRule
   * @throws PersistenceLayerException
   * @throws XFMG_NoSuchRevision
   */
  public void persistInheritanceRule(ParameterType parameterType, DestinationKey dk, InheritanceRule inheritanceRule) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(dk.getRuntimeContext().toString(), e);
    }
    createOrUpdate(parameterType, dk.getOrderType(), revision, inheritanceRule);
  }

  /**
   * Löscht eine InheritanceRule.
   * @param dk
   * @param parameterType
   * @param childFilter
   * @throws PersistenceLayerException
   * @throws XFMG_NoSuchRevision
   */
  public void deleteInheritanceRule(DestinationKey dk, ParameterType parameterType, String childFilter) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(dk.getRuntimeContext().toString(), e);
    }
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      InheritanceRuleStorable existingStorable = getInheritanceRuleForUpdate(con, parameterType, dk.getOrderType(), revision, childFilter);
      if (existingStorable != null) {
        con.deleteOneRow(existingStorable);
        con.commit();
      }
    } finally {
      con.closeConnection();
    }
  }
  
  
  /**
   * Löscht alle InheritanceRules für einen OrderType.
   * @param dk
   * @throws PersistenceLayerException
   * @throws XFMG_NoSuchRevision
   */
  public void deleteInheritanceRulesForOrderType(DestinationKey dk) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(dk.getRuntimeContext().toString(), e);
    }
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<InheritanceRuleStorable> pq =
                      queryCache.getQueryFromCache(QUERY_GET_INHERITANCE_RULES_FOR_ORDERTYPE, con, InheritanceRuleStorable.reader);
      List<InheritanceRuleStorable> list = con.query(pq, new Parameter(dk.getOrderType(), revision), -1);
      con.delete(list);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  
  public Collection<InheritanceRuleStorable> getAllInheritanceRules() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(InheritanceRuleStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  private void createOrUpdate(ParameterType parameterType, String orderType, Long revision, InheritanceRule inheritanceRule) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      InheritanceRuleStorable storable = getInheritanceRuleForUpdate(con, parameterType, orderType, revision, inheritanceRule.getChildFilter());
      if (storable != null) {
        storable.setValue(inheritanceRule.getUnevaluatedValue());
        storable.setPrecedence(inheritanceRule.getPrecedence());
      } else {
        storable = new InheritanceRuleStorable(orderType, revision, parameterType, inheritanceRule.getChildFilter(), inheritanceRule.getUnevaluatedValue(), inheritanceRule.getPrecedence());
      }
      
      con.persistObject(storable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private InheritanceRuleStorable getInheritanceRuleForUpdate(ODSConnection con, ParameterType parameterType, String orderType, Long revision, String childFilter) throws PersistenceLayerException {
    String sql = childFilter == null || childFilter.length() == 0 ? QUERY_GET_INHERITANCE_RULE_OWN : QUERY_GET_INHERITANCE_RULE_CHILD;
    PreparedQuery<InheritanceRuleStorable> pq =
                    queryCache.getQueryFromCache(sql, con, InheritanceRuleStorable.reader);
    List<InheritanceRuleStorable> list =
                    con.query(pq,
                              new Parameter(parameterType.toString(), orderType, revision,
                                            childFilter), -1);
    if (list.size() > 1) {
      throw new RuntimeException("InheritanceRule of type '" + parameterType + "' for orderType '" + orderType + "' and childFilter '" + childFilter + "' not unique.");
    }
    
    if (list.size() == 1) {
      return list.get(0);
    }

    return null;
  }
  
}
