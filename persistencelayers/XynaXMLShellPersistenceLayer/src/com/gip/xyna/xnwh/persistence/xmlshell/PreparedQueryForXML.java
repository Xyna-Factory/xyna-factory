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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmlshell.ResultNode.OPERATOR;


public class PreparedQueryForXML<E> implements PreparedQuery<E> {

  private static Logger logger = CentralFactoryLogging.getLogger(PreparedQueryForXML.class);
  
  private Query<E> query;
  private List<List<String>> whereClauseList;
  private ResultNode root;
  private List<String> selection;
  private String tableSuffix;
  private int grepTimoutInSeconds;
  
  private static Pattern SELECT_FROM  = Pattern.compile("(?<=select)(.)*(?=from)", Pattern.CASE_INSENSITIVE);
  private static Pattern WHERE        = Pattern.compile("(?<=where ).*(?=$|order by)", Pattern.CASE_INSENSITIVE);
  private static Pattern SELECT_FROM2 = Pattern.compile("^select (.)* from (.)*$", Pattern.CASE_INSENSITIVE);
  
  
  
  public PreparedQueryForXML(Query<E> query, String tableSuffix, int grepTimoutInSeconds) throws SQLException {
    this.query = query;
    root = new ResultNode();
    this.tableSuffix = tableSuffix;
    whereClauseList = new ArrayList<List<String>>();
    this.grepTimoutInSeconds = grepTimoutInSeconds;
    parseStatement(query.getSqlString());
  }
  
  public ResultSetReader<? extends E> getReader() {
    return query.getReader();
  }

  public String getTable() {
    return query.getTable();
  }
  
  public List<String> getSelection() {
    return selection;
  }
  
  public Set<String> execute(Parameter parameter, Set<String> allSet) throws SQLException, PersistenceLayerException, GrepException {

      Set<String> ret = Collections.synchronizedSortedSet(new TreeSet<String>(XynaXMLShellPersistenceLayer.reverseOrder)); 
      if (parameter == null || parameter.size() == 0) { //no parameters, return the allSet
        logger.debug("no parameter, returning allSet");
        return allSet;
      }
      
      //clear the tree
      ResultNode workingRoot = root.generateWorkingCopy(allSet); //traverseAndReset(allSet);
      List<ResultNode> setNodes = new ArrayList<ResultNode>();
      if (workingRoot.next() != null) {
        workingRoot.next().gatherSetNodes(setNodes);
      } else {
        return ret;
      }
      //Traverse the tree an fill with parameter...or the mapping^^
      logger.debug("creating latch with size: "+setNodes.size());
      CountDownLatch latch = new CountDownLatch(setNodes.size());
      List<GrepCommand> greps = new ArrayList<GrepCommand>();
      int x = 0;
      for (int i = 0; i < parameter.size(); i++) {
        GrepCommand newGrep = new GrepCommand(whereClauseList.get(x)); 
        greps.add(newGrep);
        List<Object> params = new ArrayList<Object>();
        for (int j = 0; j < newGrep.getNumParams(); j++) {
          params.add(parameter.get(i+j));          
        }
        newGrep.fillParameter(params, XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator + getTable() + tableSuffix + Constants.fileSeparator, latch);
        i += params.size() - 1;
        XynaXMLShellPersistenceLayer.grepExecutor.execute(newGrep);
        x++;
      }      
      
      try {
        if (logger.isDebugEnabled()) {
          logger.debug(new StringBuilder().append("waiting ").append(grepTimoutInSeconds).append("sec for greps to finish").toString());
        }
        latch.await(grepTimoutInSeconds, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        logger.error("PreparedQueryForXML timedout while waiting for searchThreads, returning empty result");
        return ret;
      }

      for (int i = 0; i < setNodes.size(); i++) {
        setNodes.get(i).setResult(greps.get(i).retrieveOutput());
      }
      if (workingRoot.next() == null) {
        return ret;
      }
      for (String s : workingRoot.next().process()) {
        ret.add(new String(s));
      }
      return ret;

  }
  
  
  private void parseStatement(String statement) throws SQLException {
    //cut of the selection
    System.err.println(statement);
    Matcher mat = SELECT_FROM.matcher(statement);
    selection = new ArrayList<String>();
    if (mat.find()) {
      String selects = statement.substring(mat.start(), mat.end());
      for (String column : selects.split(",")) {
        selection.add(column.trim());
      }
    }
    
    //cut out the whereclause
    mat = WHERE.matcher(statement);
    String whereClauses = statement;
    if (mat.find()) { //throw something if not found?    
      whereClauses = statement.substring(mat.start(), mat.end());
    } else {
      // no whereClause...are there no params?
      mat = SELECT_FROM2.matcher(statement);
      if (mat.find()) {
        return;
      }
    }
    
    parseWhereClauses(whereClauses, root, null, null);
  }
  
  private void parseWhereClauses(String whereClauses, ResultNode previous, ResultNodeHolder holder, OPERATOR carriedOperator) throws SQLException {
    if (whereClauses.startsWith("and ")) {
      parseWhereClauses(whereClauses.substring(4).trim(), previous, holder, OPERATOR.AND);
      return;
    }
    if (whereClauses.startsWith("or ")) {
      parseWhereClauses(whereClauses.substring(3).trim(), previous, holder, OPERATOR.OR);
      return;
    }
    if (whereClauses.startsWith("not (")) {
      int index = whereClauses.indexOf(')');
      if (index == -1) {
        throw new SQLException("Error parsing: " + whereClauses + ", no closing bracket found.");
      }
      ResultNodeHolder nodeHolder = new ResultNodeHolder(carriedOperator, true);
      previous.setNext(nodeHolder);
      parseWhereClauses(whereClauses.substring(5, index).trim(), null, nodeHolder, null);
      parseWhereClauses(whereClauses.substring(index+1).trim(), nodeHolder, null, null);
      return;
    }
    if (whereClauses.startsWith("(")) {
      int index = whereClauses.indexOf(')');
      if (index == -1) {
        throw new SQLException("Error parsing: " + whereClauses + ", no closing bracket found.");
      }
      
      ResultNodeHolder nodeHolder = new ResultNodeHolder(carriedOperator);
      previous.setNext(nodeHolder);
      parseWhereClauses(whereClauses.substring(1, index).trim(), null, nodeHolder, carriedOperator);
      parseWhereClauses(whereClauses.substring(index+1).trim(), nodeHolder, holder, null);
      return;
    }
    
    String s = "[a-zA-Z0-9]+ (=|LIKE|>|<) \\?";
    Pattern pat = Pattern.compile(s);
    Matcher mat = pat.matcher(whereClauses);
    if (mat.find()) {
      ResultNode node = new ResultNode(carriedOperator);
      if (previous != null) {
        previous.setNext(node);
      } else if (holder != null) {
        holder.setContent(node);
      }
      //setNodes.add(node);
      
      String select = whereClauses.substring(0, mat.end());
      String rest = whereClauses.substring(mat.end()).trim();
      List<String> selects = new ArrayList<String>();
      selects.add(select);
      while (rest.startsWith("or ")) {
        String column = select.split(" ")[0];
        if (rest.substring(3).trim().startsWith(column)) {
          mat = pat.matcher(rest.substring(3));
          if (mat.find()) {
            select = rest.substring(3, mat.end()+3);
            rest = rest.substring(mat.end()+3).trim();
            selects.add(select);
          }
        }
      }
      whereClauseList.add(selects);
      parseWhereClauses(rest, node, null, null);
      
      return;   
    }
  }
  
  
  @Override
  public String toString() {
    return query.getSqlString();
  }

}
