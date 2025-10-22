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
package com.gip.xyna.xprc.xprcods.orderarchive.selectorder;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.collections.SubstringMap;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseEnum;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClauseString.Operator;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.WhereClausesConnection.Connect;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParameters;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParams;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SubstringMapIndex;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance.DynamicOrderInstanceReader;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



/**
 * definition einer gueltigen suchabfrage an das orderarchive oder orderdb. beispiel: OrderInstanceSelect ois = new
 * OrderInstanceSelect();
 * ois.selectAllForOrderInstance().whereId().isBiggerThan(3).and().where(ois.newWC().whereOrderType().isEqual("ot"));
 */
public class OrderInstanceSelect extends WhereClausesContainerImpl implements Selection, Serializable, OISelect {

  private static final Logger logger = CentralFactoryLogging.getLogger(OrderInstanceSelect.class);

  private static final long serialVersionUID = 762195134074810249L;
  
  private OrderInstance backingClass;
  
  public static enum OrderByDesignators {
    ASC, DESC;
  }
  
  private List<SerializablePair<OrderInstanceColumn, OrderByDesignators>> orderBy;

  private static final Comparator<OrderInstanceColumn> COMPARATOR = new Comparator<OrderInstanceColumn>() {

    @Override
    public int compare(OrderInstanceColumn o1, OrderInstanceColumn o2) {
      return o1.ordinal() - o2.ordinal();
    }

  };

  private Set<OrderInstanceColumn> selected;


  public OrderInstanceSelect() {
    super();
    backingClass = new OrderInstance(0l);
    selected = new HashSet<OrderInstanceColumn>();
    // Always select the primary key or an OrderArchive.search will fail (remove all but one order) when trying to remove duplicated orders 
    selected.add(OrderInstanceColumn.C_ID);
    orderBy = new ArrayList<>();
  }


  public OrderInstanceSelect select(OrderInstanceColumn column) {
    if (!selected.contains(column)) {
      selected.add(column);
    }
    return this;
  }


  public OrderInstanceSelect selectId() {
    selected.add(OrderInstanceColumn.C_ID);
    return this;
  }


  public OrderInstanceSelect selectParentId() {
    selected.add(OrderInstanceColumn.C_PARENT_ID);
    return this;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {

    StringBuffer sb = new StringBuffer();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }

    List<OrderInstanceColumn> sorted = new ArrayList<OrderInstanceColumn>(selected);
    Collections.sort(sorted, COMPARATOR);
    int count = 0;
    for (OrderInstanceColumn column : sorted) {
      sb.append(column.getColumnName());
      if (count < sorted.size() - 1) {
        sb.append(", ");
      }
      count++;
    }

    sb.append(" from ").append(backingClass.getTableName());
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }

    sb.append(super.getSelectString());
    if (orderBy.size() > 0) {
      sb.append(" ORDER BY ");
      Iterator<SerializablePair<OrderInstanceColumn, OrderByDesignators>> iterator = orderBy.iterator();
      while (iterator.hasNext()) {
        SerializablePair<OrderInstanceColumn, OrderByDesignators> next = iterator.next();
        sb.append(next.getFirst().getColumnName()).append(" ").append(next.getSecond().toString());
        if (iterator.hasNext()) {
          sb.append(", ");
        }
      }
    }
    return sb.toString();
  }
  
  
  public <O extends OrderInstance> void setBackingClass(O oi) {
    this.backingClass = oi;
  }


  public Parameter getParameter() {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list);
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }


  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    StringBuffer sb = new StringBuffer();
    sb.append("select count(*) from ");
    sb.append(backingClass.getTableName());
    boolean hasWhere = false;
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
      hasWhere = true;
    }
    sb.append(super.getSelectString());

    //FIXME folgende whereclause ist ein workaround für bugz 10999: in mysql inno db tabellen 
    //ist ein select count(*) über den pk sehr langsam und sollte über einen anderen index gehen.
    //use index(...) funktioniert unter oracle nicht.
    if (!hasWhere) {
      sb.append(" where ");
      sb.append(OrderInstanceColumn.C_LAST_UPDATE.getColumnName()).append(" > -100");
    }

    return sb.toString();
  }


  public OrderInstanceSelect selectAllForOrderInstance() {
    selectId().selectParentId().selectCustom0().selectCustom1().selectCustom2().selectCustom3().selectExceptions()
        .selectExecutionType().selectLastUpdate().selectMonitoringLevel().selectOrderType().selectPriority()
        .selectStartTime().selectStatus().selectApplicationName().selectWorkspaceName();
    return this;
  }


  public WhereClausesContainerImpl newWC() {
    return new WhereClausesContainerImpl();
  }


  public ResultSetReader<OrderInstance> getReader() {
    return new DynamicOrderInstanceReader(selected);
  }

  
  public OrderInstanceSelect selectApplicationName() {
    selected.add(OrderInstanceColumn.C_APPLICATIONNAME);
    selected.add(OrderInstanceColumn.C_VERISONNAME);
    return this;
  }

  public OrderInstanceSelect selectWorkspaceName() {
    selected.add(OrderInstanceColumn.C_WORKSPACENAME);
    return this;
  }
  
  public OrderInstanceSelect selectCustom0() {
    selected.add(OrderInstanceColumn.C_CUSTOM0);
    return this;
  }


  public OrderInstanceSelect selectCustom1() {
    selected.add(OrderInstanceColumn.C_CUSTOM1);
    return this;
  }


  public OrderInstanceSelect selectCustom2() {
    selected.add(OrderInstanceColumn.C_CUSTOM2);
    return this;
  }


  public OrderInstanceSelect selectCustom3() {
    selected.add(OrderInstanceColumn.C_CUSTOM3);
    return this;
  }


  public OrderInstanceSelect selectExceptions() {
    selected.add(OrderInstanceColumn.C_EXCEPTIONS);
    return this;
  }


  public OrderInstanceSelect selectExecutionType() {
    selected.add(OrderInstanceColumn.C_EXECUTION_TYPE);
    return this;
  }


  public OrderInstanceSelect selectLastUpdate() {
    selected.add(OrderInstanceColumn.C_LAST_UPDATE);
    return this;
  }


  public OrderInstanceSelect selectMonitoringLevel() {
    selected.add(OrderInstanceColumn.C_MONITORING_LEVEL);
    return this;
  }


  public OrderInstanceSelect selectOrderType() {
    selected.add(OrderInstanceColumn.C_ORDER_TYPE);
    return this;
  }


  public OrderInstanceSelect selectPriority() {
    selected.add(OrderInstanceColumn.C_PRIORITY);
    return this;
  }


  public OrderInstanceSelect selectStartTime() {
    selected.add(OrderInstanceColumn.C_START_TIME);
    return this;
  }


  public OrderInstanceSelect selectStatus() {
    selected.add(OrderInstanceColumn.C_STATUS);
    return this;
  }


  public OrderInstanceSelect selectStopTime() {
    selected.add(OrderInstanceColumn.C_STOP_TIME);
    return this;
  }


  public OrderInstanceSelect selectSessionId() {
    selected.add(OrderInstanceColumn.C_SESSION_ID);
    return this;
  }


  public OrderInstanceSelect selectStatusCompensation() {
    selected.add(OrderInstanceColumn.C_STATUS_COMPENSATE);
    return this;
  }


  public OrderInstanceSelect selectSuspensionStatus() {
    selected.add(OrderInstanceColumn.C_SUSPENSION_STATUS);
    return this;
  }


  public OrderInstanceSelect selectSuspensionCause() {
    selected.add(OrderInstanceColumn.C_SUSPENSION_CAUSE);
    return this;
  }


  public OrderInstanceSelect selectRootId() {
    selected.add(OrderInstanceColumn.C_ROOT_ID);
    return this;
  }


  public void substituteCustomFieldLikeConditions(SubstringMapIndex ... customFields) {
    if (isNotReady(customFields[0]) && isNotReady(customFields[1]) && isNotReady(customFields[2]) && isNotReady(customFields[3])) {
      return;
    }
    substituteCustomFieldLikeConditions(this, customFields);
  }
  
  private boolean isNotReady(SubstringMapIndex substringMapIndex) {
    return substringMapIndex == null || !substringMapIndex.complete;
  }


  private void substituteCustomFieldLikeConditions(WhereClausesContainerImpl wcContainer, SubstringMapIndex[] customFields) {
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = wcContainer.getWhereClauses();
    for (WhereClausesConnection<WhereClausesContainerImpl> wcConnection : wcs) {
      WhereClause<WhereClausesContainerImpl> wc = substituteCustomFieldLikeConditions(wcConnection.getConnectedObject(), customFields);
      if (wc != wcConnection.getConnectedObject()) {
        wcConnection.replaceWhereClause(wc);
      }
    }
  }
  
  private WhereClause<WhereClausesContainerImpl> substituteCustomFieldLikeConditions(WhereClause<WhereClausesContainerImpl> connectedObject,
                                                                                     SubstringMapIndex[] customFields) {
    if (connectedObject.getColumn() == null) {
      if (connectedObject instanceof WhereClauseBrace) {
        WhereClauseBrace wcb = (WhereClauseBrace) connectedObject;
        substituteCustomFieldLikeConditions(wcb.getInnerWC(), customFields);
      } else {
        logger.warn("unexpected whereclause connection: " + connectedObject);
      }
    } else if (connectedObject instanceof WhereClauseString
        && (connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM0.getColumnName())
            || connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM1.getColumnName())
            || connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM2.getColumnName())
            || connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM3.getColumnName()))) {
      int i = -1;
      if (connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM0.getColumnName())) {
        i = 0;
      } else if (connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM1.getColumnName())) {
        i = 1;
      } else if (connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM2.getColumnName())) {
        i = 2;
      } else if (connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_CUSTOM3.getColumnName())) {
        i = 3;
      }
      if (isNotReady(customFields[i])) {
        return connectedObject;
      }
      WhereClauseString<WhereClausesContainerImpl> wcs = (WhereClauseString<WhereClausesContainerImpl>) connectedObject;
      if (wcs.getOperator() == Operator.LIKE && wcs.getParameterValue().startsWith(SelectionParser.CHARACTER_WILDCARD)) {
        try {
          List<String> vals = findSuperStrings(wcs.getParameterValue(), customFields[i].map);
          if (vals == null) {
            return connectedObject;
          }
          if (vals.size() == 0) {
            return new WhereClausesContainerImpl().whereLastUpdate().isEqual(-1).getConnectedObject(); //false
          } else if (vals.size() == 1) {
            String v = vals.get(0);
            if (i == 0) {
              return new WhereClausesContainerImpl().whereCustom0().isEqual(v).getConnectedObject();
            } else if (i == 1) {
              return new WhereClausesContainerImpl().whereCustom1().isEqual(v).getConnectedObject();
            } else if (i == 2) {
              return new WhereClausesContainerImpl().whereCustom2().isEqual(v).getConnectedObject();
            } else if (i == 3) {
              return new WhereClausesContainerImpl().whereCustom3().isEqual(v).getConnectedObject();
            }
          } else if (vals.size() <= maxNumberOfExpansionsInWhereClause.get()) {
            // TODO wenn memory-pl IN -statement kann:
            //return new WhereClausesContainerImpl().whereOrderType().isIn(ots.toArray(new String[ots.size()])).getConnectedObject();
            WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause = null;
            for (String cv : vals) {
              WhereClausesContainerImpl wcci;
              if (innerWhereClause == null) {
                wcci = new WhereClausesContainerImpl();
              } else {
                wcci =innerWhereClause.or();
              }
              if (i == 0) {
                innerWhereClause = wcci.whereCustom0().isEqual(cv);
              } else if (i == 1) {
                innerWhereClause = wcci.whereCustom1().isEqual(cv);
              } else if (i == 2) {
                innerWhereClause = wcci.whereCustom2().isEqual(cv);
              } else if (i == 3) {
                innerWhereClause = wcci.whereCustom3().isEqual(cv);
              }
            }
            return new WhereClausesContainerImpl().where(innerWhereClause).getConnectedObject();
          } else {
            return connectedObject;
          }
        } catch (XNWH_WhereClauseBuildException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      //ignore
    }
    return connectedObject;
  }

  /*
   * ordertype suche
   *  like '%xxx%'
   *  umwandeln in
   *  ='axxx' or ='bxxxc' or = 'xxxx'
   *  
   * welche ordertypes kommen in frage?
   *  -> globaler datenraum vs pro application (muss aus query ausgelesen werden)
   *  
   * wie ist ordertype-datenhaltung aufgebaut?
   *  expiringmap<suchbegriff, liste<ordertypes>> enthält vorherige suchbegriffe.
   *  map<substring, liste<ordertypes>> enthält map aller substrings aller ordertypes auf die dazu passenden ordertypes, erstmal nur substrings der länge 2 pflegen, damit es nicht zu viele werden
   *  
   *  für eine anfrage der form like %x% bestimme für alle 2 langen substrings von x aus der obigen map die möglichen ordertypes
   *  nimm die liste die minimal ist (bei größe 0 vorzeitig abbrechen)
   *  - caseinsensitiv
   */
  public void substituteOrderTypeLikeConditions(SubstringMap orderTypes) {
    substituteOrderTypeLikeConditions(this, orderTypes);
  }

  private void substituteOrderTypeLikeConditions(WhereClausesContainerImpl wcContainer, SubstringMap orderTypes) {
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = wcContainer.getWhereClauses();
    for (WhereClausesConnection<WhereClausesContainerImpl> wcConnection : wcs) {
      WhereClause<WhereClausesContainerImpl> wc = substituteOrderTypeLikeConditions(wcConnection.getConnectedObject(), orderTypes);
      if (wc != wcConnection.getConnectedObject()) {
        wcConnection.replaceWhereClause(wc);
      }
    }
  }

  private static final XynaPropertyInt maxNumberOfExpansionsInWhereClause = new XynaPropertyInt("xprc.ods.orderarchive.queryexpansion.max", 30).
      setDefaultDocumentation(DocumentationLanguage.EN, "When executing orderarchive queries where like-queries are expanded to help index usage,"
          + " this is the maximum number of expanded values before returning to execute the like-query as is.");
  

  private WhereClause<WhereClausesContainerImpl> substituteOrderTypeLikeConditions(WhereClause<WhereClausesContainerImpl> connectedObject,
                                                                                   SubstringMap orderTypes) {
    if (connectedObject.getColumn() == null) {
      if (connectedObject instanceof WhereClauseBrace) {
        WhereClauseBrace wcb = (WhereClauseBrace) connectedObject;
        substituteOrderTypeLikeConditions(wcb.getInnerWC(), orderTypes);
      } else {
        logger.warn("unexpected whereclause connection: " + connectedObject);
      }
    } else if (connectedObject instanceof WhereClauseString
        && connectedObject.getColumn().equalsIgnoreCase(OrderInstanceColumn.C_ORDER_TYPE.getColumnName())) {
      WhereClauseString<WhereClausesContainerImpl> wcs = (WhereClauseString<WhereClausesContainerImpl>) connectedObject;
      if (wcs.getOperator() == Operator.LIKE && wcs.getParameterValue().startsWith(SelectionParser.CHARACTER_WILDCARD)) {
        try {
          List<String> ots = findSuperStrings(wcs.getParameterValue(), orderTypes);
          if (ots == null) {
            return connectedObject;
          } else {
            if (ots.size() == 0) {
              return new WhereClausesContainerImpl().whereLastUpdate().isEqual(-1).getConnectedObject(); //false
            } else if (ots.size() == 1) {
              return new WhereClausesContainerImpl().whereOrderType().isEqual(ots.get(0)).getConnectedObject();
            } else if (ots.size() <= maxNumberOfExpansionsInWhereClause.get()) {
              // TODO wenn memory-pl IN -statement kann:
              //return new WhereClausesContainerImpl().whereOrderType().isIn(ots.toArray(new String[ots.size()])).getConnectedObject();
              WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause = null;
              for (String ot : ots) {
                if (innerWhereClause == null) {
                  innerWhereClause = new WhereClausesContainerImpl().whereOrderType().isEqual(ot);
                } else {
                  innerWhereClause = innerWhereClause.or().whereOrderType().isEqual(ot);
                }
              }
              return new WhereClausesContainerImpl().where(innerWhereClause).getConnectedObject();
            } else {
              return connectedObject;
            }
          }
        } catch (XNWH_WhereClauseBuildException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      //ignore
    }
    return connectedObject;
  }


  private static class EscapeParamsStoringLikeParts implements EscapeParameters {

    List<String> parts = new ArrayList<String>();
    StringBuilder regexp = new StringBuilder();


    @Override
    public String escapeForLike(String s) {
      if (s != null && s.length() > 0) {
        parts.add(s);
        regexp.append(Pattern.quote(s));
        return "a";
      } else {
        return "";
      }
    }


    @Override
    public String getMultiCharacterWildcard() {
      regexp.append(".*");
      return "x";
    }


    @Override
    public String getSingleCharacterWildcard() {
      regexp.append(".");
      return "x";
    }

  }

  /**
   * 
   * @param parameterValue abc%def%geh%
   * @param substringMap
   * @return
   */
  public static List<String> findSuperStrings(String parameterValue, SubstringMap substringMap) {
    EscapeParamsStoringLikeParts ep = new EscapeParamsStoringLikeParts();
    String s = SelectionParser.escapeParams(parameterValue, true, ep);
    List<String> likeParts = ep.parts;
    Set<String> ret = null;
    for (String p : likeParts) {
      List<String> candidates = substringMap.getSuperStrings(p);
      if (ret == null) {
        ret = new HashSet<String>(candidates);
      } else {
        ret.retainAll(candidates);
      }
    }
    if (s.equals("x")) {
      return null;
    }
    if (s.equals("xax")) { //hinten %. dann ist substring alleine richtig
      return new ArrayList<String>(ret);
    } else if (s.equals("xa")) {
      //nur ordertypes berücksichtigen, die auch mit dem richtigen substring enden
      List<String> l = new ArrayList<String>();
      String end = likeParts.get(likeParts.size() - 1).toLowerCase();
      for (String r : ret) {
        if (r.toLowerCase().endsWith(end)) {
          l.add(r);
        }
      }
      return l;
    } else {
      //regulären ausdruck bauen, damit %a%b%c nicht von "bac" getroffen wird.
      List<String> l = new ArrayList<String>();
      Pattern patt = Pattern.compile(ep.regexp.toString(), Pattern.CASE_INSENSITIVE);
      for (String r : ret) {
        if (patt.matcher(r).matches()) {
          l.add(r);
        }
      }
      return l;
    }
  }


  /**
   * @return true, falls die query ergebnisse liefern kann, deren status finished oder failed ist.
   */
  public boolean doesQueryStatusFinishedOrFailed() {
    return queriesSomeFinishedOrFailed(this);
  }

  /**
   * @return true, falls diese whereclause ergebnisse liefern kann, deren status finished oder failed ist
   */
  private static boolean queriesSomeFinishedOrFailed(WhereClause<WhereClausesContainerImpl> connectedObject) {
    if (connectedObject.getColumn() == null) {
      if (connectedObject instanceof WhereClauseBrace) {
        WhereClauseBrace wcb = (WhereClauseBrace) connectedObject;
        if (wcb.isNegated()) {
          //achtung: "where not (id = 3)" kann genauso zeilen mit finished/failed zurückgeben wie "where (id = 3)".
          //die einzigen negierten anfagen, die keine finished/failed zeilen zurückgeben können, sind die,
          //die alle finished/failed zeilen beinhalten
          FinishedOrFailedBean fofb = queriesAllFinishedOrFailed(wcb.getInnerWC());
          return !(fofb.containsAllFailed && fofb.containsAllFinished);
        } else {
          return queriesSomeFinishedOrFailed(wcb.getInnerWC());
        }
      } else {
        logger.debug("unexpected whereclause connection: " + connectedObject);
      }
    } else if (connectedObject.getColumn().equals(OrderInstanceColumn.C_STATUS.getColumnName())) {
      if (connectedObject instanceof WhereClauseEnum) {
        WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus> wce =
            (WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus>) connectedObject;
        switch (wce.getOperator()) {
          case EQUALS :
            return wce.getParameterValue().isFinished();
            //TODO like.
          default :
            logger.debug("unexpected whereclauseoperator: " + wce.getOperator());
        }
      } else if (connectedObject instanceof WhereClauseString) {
        WhereClauseString<WhereClausesContainerImpl> wcs =
            (WhereClauseString<WhereClausesContainerImpl>) connectedObject;
        switch (wcs.getOperator()) {
          case EQUALS :
            String status = wcs.getParameterValue();
            if (status.startsWith("\"")) {
              status = status.substring(1, status.length() - 1);
            }
            OrderInstanceStatus ois = OrderInstanceStatus.fromString(status);
            return ois.isFinished();
            //TODO like.
          default :
            logger.debug("unexpected whereclauseoperator: " + wcs.getOperator());
        }
      } else {
        logger.debug("unexpected whereclauseobject: " + connectedObject);
      }
    }
    return true;
  }

  /**
   * @return true, falls dieser wc-container ergebnisse liefern kann, deren status finished oder failed ist 
   */
  private static boolean queriesSomeFinishedOrFailed(WhereClausesContainerImpl wcContainer) {
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = wcContainer.getWhereClauses();
    boolean queryStatusFinishedOrFailed = true;
    boolean nextConnectionIsAnd = true;
    for (WhereClausesConnection<WhereClausesContainerImpl> wcConnection : wcs) {
      boolean doesSubQueryStatusFinishedOrFailed = queriesSomeFinishedOrFailed(wcConnection.getConnectedObject());
      if (nextConnectionIsAnd) {
        queryStatusFinishedOrFailed &= doesSubQueryStatusFinishedOrFailed;
      } else {
        queryStatusFinishedOrFailed |= doesSubQueryStatusFinishedOrFailed;
      }
      nextConnectionIsAnd = wcConnection.getConnection() == Connect.AND;
    }
    return queryStatusFinishedOrFailed;
  }


  /**
   * @return true/true, falls alle finished/failed zeilen mit dieser query abgefragt werden. d.h. die finished/failed zeilen
   * dürfen mit dieser query (jeweils) nicht eingeschränkt werden.
   */
  private static FinishedOrFailedBean queriesAllFinishedOrFailed(WhereClausesContainerImpl wcContainer) {
    FinishedOrFailedBean result = new FinishedOrFailedBean();
    result.containsAllFailed = true;
    result.containsAllFinished = true;
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = wcContainer.getWhereClauses();
    boolean nextConnectionIsAnd = true;
    for (WhereClausesConnection<WhereClausesContainerImpl> wcConnection : wcs) {
      FinishedOrFailedBean fofb = queriesAllFinishedOrFailed(wcConnection.getConnectedObject());
      if (nextConnectionIsAnd) {
        result.containsAllFinished &= fofb.containsAllFinished;
        result.containsAllFailed &= fofb.containsAllFailed;
      } else {
        result.containsAllFinished |= fofb.containsAllFinished;
        result.containsAllFailed |= fofb.containsAllFailed;
      }
      nextConnectionIsAnd = wcConnection.getConnection() == Connect.AND;
    }
    return result;
  }

  /**
   * @return true/true falls alle finished/failed zeilen mit dieser query abgefragt werden. d.h. die finished/failed zeilen
   * dürfen mit dieser query (jeweils) nicht eingeschränkt werden.
   */
  private static FinishedOrFailedBean queriesAllFinishedOrFailed(WhereClause<WhereClausesContainerImpl> connectedObject) {
    FinishedOrFailedBean result = new FinishedOrFailedBean();
    result.containsAllFailed = false;
    result.containsAllFinished = false;
    if (connectedObject.getColumn() == null) {
      if (connectedObject instanceof WhereClauseBrace) {
        WhereClauseBrace wcb = (WhereClauseBrace) connectedObject;
        if (wcb.isNegated()) {
          //falls die innere bedingung keine finished/failed enthält, dann bedeutet die negation alle zu haben
          //falls die innere bedingung mindestens ein finished/failed enthält, dann bedeutet die negation nicht alle zu haben
          boolean containsSomeFinishedOrFailed = queriesSomeFinishedOrFailed(wcb.getInnerWC());
          result.containsAllFailed = !containsSomeFinishedOrFailed;
          result.containsAllFinished = !containsSomeFinishedOrFailed;
        } else {
          return queriesAllFinishedOrFailed(wcb.getInnerWC());
        }
      } else {
        logger.warn("unexpected whereclause connection: " + connectedObject);
      }
    } else if (connectedObject.getColumn().equals(OrderInstanceColumn.C_STATUS.getColumnName())) {
      if (connectedObject instanceof WhereClauseEnum) {
        WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus> wce =
            (WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus>) connectedObject;
        switch (wce.getOperator()) {
          case EQUALS :
            result.containsAllFailed = false;
            result.containsAllFinished = false;
            if (wce.getParameterValue().isSucceeded()) {//isFinished() wäre erfolgreich oder mit Fehler, hier wird unterschieden
              result.containsAllFinished = true;
            } else if (wce.getParameterValue().isFailed()) {
              result.containsAllFailed = true;
            }
            //TODO like.
            break;
          default :
            logger.warn("unexpected whereclauseoperator: " + wce.getOperator());
        }
      } else if (connectedObject instanceof WhereClauseString) {
        WhereClauseString<WhereClausesContainerImpl> wcs =
            (WhereClauseString<WhereClausesContainerImpl>) connectedObject;
        switch (wcs.getOperator()) {
          case EQUALS :
            result.containsAllFailed = false;
            result.containsAllFinished = false;
            OrderInstanceStatus ois = OrderInstanceStatus.fromString(wcs.getParameterValue());
            if (ois.isSucceeded()) { //isFinished() wäre erfolgreich oder mit Fehler, hier wird unterschieden
              result.containsAllFinished = true;
            } else if (ois.isFailed()) {
              result.containsAllFailed = true;
            }
            //TODO like.
            break;
          default :
            logger.warn("unexpected whereclauseoperator: " + wcs.getOperator());
        }
      } else {
        logger.warn("unexpected whereclauseobject: " + connectedObject);
      }
    }
    return result;
  }


  private static class FinishedOrFailedBean {

    private boolean containsAllFinished;
    private boolean containsAllFailed;
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    return (ResultSetReader<T>) getReader();
  }


  public void addOrderBy(OrderInstanceColumn cLastUpdate, OrderByDesignators desc) {
    if (orderBy == null) {
      orderBy = new ArrayList<>();
    }
    orderBy.add(SerializablePair.of(cLastUpdate, desc));
  }


  public String getTableName() {
    return backingClass.getTableName();
  }

}
