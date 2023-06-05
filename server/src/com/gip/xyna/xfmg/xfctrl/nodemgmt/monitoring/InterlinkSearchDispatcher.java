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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.CredentialsCache;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InfrastructureLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParams;


public class InterlinkSearchDispatcher extends FunctionGroup {

  public InterlinkSearchDispatcher() throws XynaException {
    super();
  }


  private static final Logger logger = CentralFactoryLogging.getLogger(InterlinkSearchDispatcher.class);


  private class SearchJob implements Runnable {

    public volatile CyclicBarrier cb;
    private final String factoryNode;
    private final SearchRequestBean searchRequest;
    private final SearchResult<?> sr;


    public SearchJob(String factoryNode, SearchRequestBean srb, SearchResult<?> sr) {
      this.factoryNode = factoryNode;
      searchRequest = new SearchRequestBean(srb); //kopie erstellen, damit jeder job unabhängige nodefilter haben kann
      searchRequest.clearFactoryNodesFilter(); //nicht beim job wieder dispatchen
      this.sr = sr;
    }


    @Override
    public void run() {
      try {
        if (factoryNode.equals(NodeManagement.FACTORYNODE_LOCAL)) {
          merge(((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal()).search(searchRequest));
        } else {
          FactoryNode nodeByName = getNodeManagement().getNodeByName(factoryNode);
          if (nodeByName == null) {
            throw new RuntimeException("Factory Node " + factoryNode + " unknown.");
          }
          MonitoringLinkProfile profile =
              (MonitoringLinkProfile) nodeByName.getInterFactoryLink().getProfile(InterFactoryLinkProfileIdentifier.Monitoring);
          SearchResult<?> result = profile.search(checkConnectivityAndAccess(nodeByName), searchRequest);
          merge(result);
        }
      } catch (Throwable t) {
        addException(t);
      } finally {
        try {
          cb.await();
        } catch (InterruptedException e) {
          //dann halt nicht warten
        } catch (BrokenBarrierException e) {
          logger.warn(null, e);
        }
      }
    }


    private void addException(Throwable t) {
      synchronized (sr) {
        sr.addException(t);
      }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private void merge(SearchResult<?> result) {
      synchronized (sr) {
        int diff;
        if (factoryNode.equals(NodeManagement.FACTORYNODE_LOCAL)) {
          diff = result.getResult().size();
          List<?> oldResult = sr.getResult();
          if (oldResult != null) {
            //passiert nur, wenn der gleiche knoten mehrfach angegeben ist. sollte eigtl nicht passieren, aber schadet nichts, das hier zu behandeln.
            diff -= oldResult.size();
          }
          sr.setResult((List) result.getResult());
        } else {
          diff = result.getCount();
          List<?> oldResult = sr.getRemoteResults().get(factoryNode);
          if (oldResult != null) {
            //passiert nur, wenn der gleiche knoten mehrfach angegeben ist. sollte eigtl nicht passieren, aber schadet nichts, das hier zu behandeln.
            diff -= oldResult.size();
          }
          sr.addResult(factoryNode, (List) result.getResult());
        }
        sr.setCount(sr.getCount() + diff);
      }
    }

  }


  private CredentialsCache cache;

  private final ThreadPoolExecutor tpe = new ThreadPoolExecutor(10, 20, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

  @Override
  public String getDefaultName() {
    return "InterlinkDispatcher";
  }


  @Override
  protected void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(InterlinkSearchDispatcher.class, getDefaultName()).after(XynaProperty.class).execAsync(new Runnable() {

      @Override
      public void run() {
        cache = CredentialsCache.getInstance();
      }
      
    });
  }
  
  public void shutdown() {
    if (cache != null) {
      cache.shutdown();
    }
    tpe.shutdown();
  }


  public SearchResult<?> dispatch(SearchRequestBean searchRequest) throws XynaException {
    if (searchRequest.isLocal()) {
      return searchLocal(searchRequest);
    }
    SearchResult<?> sr = getEmptySearchResult(searchRequest);
    SearchJob[] jobs = getSearchJobs(searchRequest, sr);
    CyclicBarrier cb = new CyclicBarrier(jobs.length + 1); //jeder job, plus dieser thread
    for (SearchJob job : jobs) {
      job.cb = cb;
      try {
        tpe.execute(job);
      } catch (RejectedExecutionException e) {
        //dann im eigenen thread laufen lassen
        job.run();
      }
    }
    try {
      cb.await();
    } catch (InterruptedException e) {
      //trotzdem sr zurückgeben, evtl ist da ja bereits was vernünftiges drin
      synchronized (sr) {
        sr.addException(e);
      }
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
    return sr;
  }


  @SuppressWarnings("rawtypes")
  private static SearchResult<?> getEmptySearchResult(SearchRequestBean searchRequest) {
    return new SearchResult();
  }


  private SearchJob[] getSearchJobs(SearchRequestBean request,  SearchResult<?> sr) {
    String[] nodes = getFactoryNodes(request.getFactoryNodesFilter());
    List<SearchJob> result = new ArrayList<SearchJob>(nodes.length);
    for (String n : nodes) {
      result.add(new SearchJob(n, request, sr));
    }
    return result.toArray(new SearchJob[0]);
  }


  private static final EscapeParams escapeIntoRegexp = new EscapeParams() {

    public String escapeForLike(String toEscape) {
      if (toEscape == null || toEscape.length() == 0) {
        return toEscape;
      }

      return Pattern.quote(toEscape);
    }


    public String getWildcard() {
      return ".*";
    }

  };
  
  private static final EscapeParams unescapeToString = new EscapeParams() {

    public String escapeForLike(String toEscape) {
       return toEscape;
    }


    public String getWildcard() {
      return "";
    }

  };


  private static String[] getFactoryNodes(List<String> factoryNodesFilter) {
    Set<String> output = new HashSet<String>();
    for (String node : factoryNodesFilter) {
      if (node.equals(NodeManagement.FACTORYNODE_LOCAL)) {
        output.add(node);
      } else if (node.contains("%")) { //FIXME wenn man nach einem escapten % sucht, findet man keinen knoten und es gibt keinen fehler.
        //TODO caching? die anfragen kommen ja evtl häufiger
        String regexp = SelectionParser.escapeParams(node, true, escapeIntoRegexp);
        Pattern pattern = Pattern.compile(regexp);
        boolean found = false;
        for (FactoryNodeStorable fn : getNodeManagement().getAllFactoryNodes()) {
          if (pattern.matcher(fn.getName()).matches()) {
            output.add(fn.getName());
            found = true;
          }
        }
        if (!found) {
          logger.info("no matching factory node found for filter '" + node + "'");
        }
      } else {
        output.add(SelectionParser.escapeParams(node, false, unescapeToString));
      }
    }
    return output.toArray(new String[0]);
  }


  private static SearchResult<?> searchLocal(SearchRequestBean searchRequest) throws XynaException {
    searchRequest.clearFactoryNodesFilter();
    return ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal()).search(searchRequest);
  }


  private XynaCredentials checkConnectivityAndAccess(FactoryNode node) throws XFMG_NodeConnectException {
    String nodeName = node.getNodeInformation().getName();
    InfrastructureLinkProfile infrastructure =
        node.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.Infrastructure);
    XynaCredentials credentials = cache.getCredentials(nodeName, infrastructure);
    try {
      infrastructure.getExtendedStatus(credentials);
    } catch (XFMG_NodeConnectException e) {
      cache.clearSession(nodeName);
      credentials = cache.getCredentials(nodeName, infrastructure);
      infrastructure.getExtendedStatus(credentials);
    }
    return credentials;
  }


  private static NodeManagement getNodeManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
  }



}
