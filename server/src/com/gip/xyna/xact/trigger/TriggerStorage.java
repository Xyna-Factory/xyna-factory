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

package com.gip.xyna.xact.trigger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;


public class TriggerStorage {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TriggerStorage.class);

  private ODSImpl ods;
  
  public TriggerStorage() throws PersistenceLayerException {
    
    ods = ODSImpl.getInstance();
    
    ods.registerStorable(TriggerStorable.class);
    ods.registerStorable(TriggerInstanceStorable.class);
    ods.registerStorable(FilterStorable.class);
    ods.registerStorable(FilterInstanceStorable.class);
    ods.registerStorable(TriggerConfigurationStorable.class);
  }
  
  
  private static class TriggerByRevisionFilter implements Filter<TriggerStorable> {

    private Long revision;

    public TriggerByRevisionFilter(Long revision) {
      this.revision = revision;
    }

    public boolean accept(TriggerStorable value) {
      return value.getRevision().equals(revision);
    }
  }

  private static class TriggerByFqClassNameFilter implements Filter<TriggerStorable> {
    
    private String fqClassName;
    private Long revision;
    
    public TriggerByFqClassNameFilter(String fqClassName, Long revision) {
      this.fqClassName = fqClassName;
      this.revision = revision;
    }
    
    public boolean accept(TriggerStorable value) {
      return value.getFqTriggerClassName().equals(fqClassName) && value.getRevision().equals(revision);
    }
  }
  
  private static class TriggerByTriggerNameFilter implements Filter<TriggerStorable> {
    
    private final String triggerName;
    private final Set<Long> revisions;

    public TriggerByTriggerNameFilter(String triggerName, Long revision, boolean followRuntimeContextDependencies) {
      this.triggerName = triggerName;
      revisions = new HashSet<Long>(1);
      if (followRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getDependenciesRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(TriggerStorable value) {
      return value.getTriggerName().equals(triggerName) && revisions.contains(value.getRevision());
    }
  }
  
  private static class FilterByRevisionFilter implements Filter<FilterStorable> {

    private Long revision;

    public FilterByRevisionFilter(Long revision) {
      this.revision = revision;
    }

    public boolean accept(FilterStorable value) {
      return value.getRevision().equals(revision);
    }
  }

  private static class FilterByTriggerNameFilter implements Filter<FilterStorable> {
    
    private String triggerName;
    private Set<Long> revisions;


    public FilterByTriggerNameFilter(String triggerName, Long revision, boolean followRuntimeContextBackwardDependencies) {
      this.triggerName = triggerName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextBackwardDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getParentRevisionsRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(FilterStorable value) {
      return value.getTriggerName().equals(triggerName) && revisions.contains(value.getRevision());
    }
  }

  private static class FilterByNameFilter implements Filter<FilterStorable> {

    private String filterName;
    private Set<Long> revisions;


    public FilterByNameFilter(String filterName, Long revision, boolean followRuntimeContextDependencies) {
      this.filterName = filterName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getDependenciesRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }


    public boolean accept(FilterStorable value) {
      return value.getFilterName().equals(filterName) && revisions.contains(value.getRevision());
    }
  }

  
  private static class TriggerInstanceByTriggerNameFilter implements Filter<TriggerInstanceStorable> {
    
    private final String triggerName;
    private final Set<Long> revisions;


    public TriggerInstanceByTriggerNameFilter(String triggerName, Long revision, boolean followRuntimeContextDependencies) {
      this.triggerName = triggerName;
      revisions = new HashSet<Long>(1);
      if (followRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getParentRevisionsRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(TriggerInstanceStorable value) {
      return value.getTriggerName().equals(triggerName) && revisions.contains(value.getRevision());
    }
  }
  
  private static class TriggerInstanceByNameFilter implements Filter<TriggerInstanceStorable> {
    
    private final String triggerInstanceName;
    private final Set<Long> revisions;
    
    public TriggerInstanceByNameFilter(String triggerInstanceName, long revision, boolean followRuntimeContextDependencies) {
      this.triggerInstanceName = triggerInstanceName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getDependenciesRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(TriggerInstanceStorable value) {
      return value.getTriggerInstanceName().equals(triggerInstanceName) && revisions.contains(value.getRevision());
    }
  }
  
  private static class FilterInstanceByRevisionFilter implements Filter<FilterInstanceStorable> {
    
    private Long revision;
    
    public FilterInstanceByRevisionFilter(Long revision) {
      this.revision = revision;
    }
    
    public boolean accept(FilterInstanceStorable value) {
      return value.getRevision().equals(revision);
    }
  }

  private static class TriggerInstanceByRevisionFilter implements Filter<TriggerInstanceStorable> {
    
    private Long revision;
    
    public TriggerInstanceByRevisionFilter(Long revision) {
      this.revision = revision;
    }
    
    public boolean accept(TriggerInstanceStorable value) {
      return value.getRevision().equals(revision);
    }
  }

  private static class FilterInstanceByTriggerInstanceNameFilter implements Filter<FilterInstanceStorable> {
    
    private String triggerInstanceName;
    private final Set<Long> revisions;
    
    public FilterInstanceByTriggerInstanceNameFilter(String triggerInstanceName, Long revision, boolean followRuntimeContextBackwardDependencies) {
      this.triggerInstanceName = triggerInstanceName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextBackwardDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getParentRevisionsRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(FilterInstanceStorable value) {
      return value.getTriggerInstanceName().equals(triggerInstanceName) && revisions.contains(value.getRevision());
    }
  }
  
  
  private static class FilterInstanceByNameFilter implements Filter<FilterInstanceStorable> {
    
    private final String filterInstanceName;
    private final Set<Long> revisions;
    
    private FilterInstanceByNameFilter(String filterInstanceName, long revision, boolean followRuntimeContextDependencies) {
      this.filterInstanceName = filterInstanceName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getDependenciesRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(FilterInstanceStorable value) {
      return value.getFilterInstanceName().equals(filterInstanceName) && revisions.contains(value.getRevision());
    }
  }
  
  private static class FilterInstanceByFilterNameFilter implements Filter<FilterInstanceStorable> {
    
    private String filterName;
    private final Set<Long> revisions;
    
    public FilterInstanceByFilterNameFilter(String filterName, Long revision, boolean followRuntimeContextBackwardDependencies) {
      this.filterName = filterName;
      this.revisions = new HashSet<Long>();
      if (followRuntimeContextBackwardDependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getParentRevisionsRecursivly(revision, revisions);
      }
      revisions.add(revision);
    }
    
    public boolean accept(FilterInstanceStorable value) {
      return value.getFilterName().equals(filterName) && revisions.contains(value.getRevision());
    }
  }
  
  private Comparator<TriggerStorable> triggerStorableComparator = new Comparator<TriggerStorable>() {

    public int compare(TriggerStorable o1, TriggerStorable o2) {
      return o1.getTriggerName().compareTo(o2.getTriggerName());
    }
  };


  private Comparator<TriggerInstanceStorable> triggerInstanceStorableComparator = new Comparator<TriggerInstanceStorable>() {

    public int compare(TriggerInstanceStorable o1, TriggerInstanceStorable o2) {
      int triggernameComparison = o1.getTriggerName().compareTo(o2.getTriggerName());
      if (triggernameComparison == 0) {
        return o1.getTriggerInstanceName().compareTo(o2.getTriggerInstanceName());
      }
      return triggernameComparison;
    }
  };


  private Comparator<FilterInstanceStorable> filterInstanceStorableComparator = new Comparator<FilterInstanceStorable>() {

    public int compare(FilterInstanceStorable o1, FilterInstanceStorable o2) {
      int triggernameComparison = o1.getTriggerInstanceName().compareTo(o2.getTriggerInstanceName());
      if (triggernameComparison == 0) {
        int filternameComparison = o1.getFiltereName().compareTo(o2.getFiltereName());
        if (filternameComparison == 0) {
          return o1.getFilterInstanceName().compareTo(o2.getFilterInstanceName());
        }
        return filternameComparison;
      }
      return triggernameComparison;
    }
  };


  private Comparator<FilterStorable> filterStorableComparator = new Comparator<FilterStorable>() {

    public int compare(FilterStorable o1, FilterStorable o2) {
      int triggernameComparison = o1.getTriggerName().compareTo(o2.getTriggerName());
      if (triggernameComparison == 0) {
        return o1.getFilterName().compareTo(o2.getFilterName());
      }
      return triggernameComparison;
    }
  };


  public List<TriggerStorable> getTriggersByRevision(Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerStorable> result = CollectionUtils.filter(con.loadCollection(TriggerStorable.class), new TriggerByRevisionFilter(revision));
      Collections.sort(result, triggerStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }

  public TriggerStorable getTriggerByName(String triggerName, Long revision, boolean followRuntimeContextDependencies) throws PersistenceLayerException, XACT_TriggerNotFound {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerStorable> storables = CollectionUtils.filter(con.loadCollection(TriggerStorable.class), new TriggerByTriggerNameFilter(triggerName, revision, followRuntimeContextDependencies));
      if (storables.size() == 0) {
        throw new XACT_TriggerNotFound(triggerName);
      } else {
        return storables.get(0);
      }
    } finally {
      finallyClose(con);
    }
  }
  
  public TriggerStorable getTriggerByFqClassName(String fqClassName, Long revision) throws PersistenceLayerException, XACT_TriggerNotFound {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerStorable> storables = CollectionUtils.filter(con.loadCollection(TriggerStorable.class), new TriggerByFqClassNameFilter(fqClassName, revision));
      if (storables.size() == 0) {
        throw new XACT_TriggerNotFound(fqClassName);
      } else {
        return storables.get(0);
      }
    } finally {
      finallyClose(con);
    }
  }
  
  public TriggerInstanceStorable getTriggerInstanceByName(String triggerInstanceName, Long revision, boolean followRuntimeContextDependencies) throws PersistenceLayerException,  XACT_TriggerInstanceNotFound {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerInstanceStorable> storables = CollectionUtils.filter(con.loadCollection(TriggerInstanceStorable.class), new TriggerInstanceByNameFilter(triggerInstanceName, revision, followRuntimeContextDependencies));
      if (storables.size() == 0) {
        throw new XACT_TriggerInstanceNotFound(triggerInstanceName);
      } else {
        return storables.get(0);
      }
    } finally {
      finallyClose(con);
    }
  }


  public List<TriggerInstanceStorable> getTriggerInstancesByTriggerName(String triggerName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerInstanceStorable> result =
          CollectionUtils.filter(con.loadCollection(TriggerInstanceStorable.class),
                                 new TriggerInstanceByTriggerNameFilter(triggerName, revision, followRuntimeContextBackwardDependencies));
      Collections.sort(result, triggerInstanceStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }
  

  public List<FilterStorable> getFiltersByRevision(Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterStorable> result = CollectionUtils.filter(con.loadCollection(FilterStorable.class), new FilterByRevisionFilter(revision));
      Collections.sort(result, filterStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }

  public List<FilterStorable> getFiltersByTriggerName(String triggerName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterStorable> result = CollectionUtils.filter(con.loadCollection(FilterStorable.class), new FilterByTriggerNameFilter(triggerName, revision, followRuntimeContextBackwardDependencies));
      Collections.sort(result, filterStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }


  public FilterStorable getFilterByName(String filterName, Long revision, boolean followRuntimeContextDependencies) throws PersistenceLayerException, XACT_FilterNotFound {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterStorable> storables = CollectionUtils.filter(con.loadCollection(FilterStorable.class), new FilterByNameFilter(filterName, revision, followRuntimeContextDependencies));
      if (storables.size() == 0) {
        throw new XACT_FilterNotFound(filterName);
      } else {
        return storables.get(0);
      }
    } finally {
      finallyClose(con);
    }
  }
  

  public List<FilterInstanceStorable> getFilterInstancesByRevision(Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterInstanceStorable> result =
          CollectionUtils.filter(con.loadCollection(FilterInstanceStorable.class), new FilterInstanceByRevisionFilter(revision));
      Collections.sort(result, filterInstanceStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }


  public Collection<TriggerInstanceStorable> getTriggerInstancesByRevision(long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<TriggerInstanceStorable> result =
          CollectionUtils.filter(con.loadCollection(TriggerInstanceStorable.class), new TriggerInstanceByRevisionFilter(revision));
      Collections.sort(result, triggerInstanceStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }

  public FilterInstanceStorable getFilterInstanceByName(String filterInstanceName, Long revision, boolean followRuntimeContextDependencies) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterInstanceStorable> storables = CollectionUtils.filter(con.loadCollection(FilterInstanceStorable.class), new FilterInstanceByNameFilter(filterInstanceName, revision, followRuntimeContextDependencies));
      if (storables.size() == 0) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(filterInstanceName, "filterinstance");
      } else {
        return storables.get(0);
      }
    } finally {
      finallyClose(con);
    }
  }


  public List<FilterInstanceStorable> getFilterInstancesByTriggerInstanceName(String triggerInstanceName, Long revision, boolean followRuntimeContextBackwardDependencies)
      throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterInstanceStorable> result =
          CollectionUtils.filter(con.loadCollection(FilterInstanceStorable.class),
                                 new FilterInstanceByTriggerInstanceNameFilter(triggerInstanceName, revision, followRuntimeContextBackwardDependencies));
      Collections.sort(result, filterInstanceStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }

  public List<FilterInstanceStorable> getFilterInstancesByFilterName(String filterName, Long revision, boolean followRuntimeContextBackwardDependencies) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      List<FilterInstanceStorable> result =
          CollectionUtils.filter(con.loadCollection(FilterInstanceStorable.class),
                                 new FilterInstanceByFilterNameFilter(filterName, revision, followRuntimeContextBackwardDependencies));
      Collections.sort(result, filterInstanceStorableComparator);
      return result;
    } finally {
      finallyClose(con);
    }
  }
  
  
  public TriggerConfigurationStorable getTriggerConfigurationByTriggerInstanceName(String triggerInstanceName, Long revision) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    TriggerConfigurationStorable storable = new TriggerConfigurationStorable(triggerInstanceName, revision);
    try {
      con.queryOneRow(storable);
      return storable;
    } finally {
      con.closeConnection();
    }
  }
  
  
  public void setTriggerInstanceState(String triggerInstanceName, Long revision, TriggerInstanceState state) throws PersistenceLayerException, XACT_TriggerInstanceNotFound {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    TriggerInstanceStorable tis = new TriggerInstanceStorable(triggerInstanceName, revision);
    try {
      con.queryOneRowForUpdate(tis);
      tis.setState(state);
      con.persistObject(tis);
      con.commit();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XACT_TriggerInstanceNotFound(triggerInstanceName, e);
    } finally {
      finallyClose(con);
    }
  }


  public void setFilterInstanceState(String filterInstanceName, Long revision, FilterInstanceState state) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    FilterInstanceStorable fis = new FilterInstanceStorable(filterInstanceName, revision);
    try {
      con.queryOneRowForUpdate(fis);
      fis.setState(state);
      con.persistObject(fis);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }
  
  
  public void setTriggerError(String triggerName, Long revision, Throwable t) {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    TriggerStorable storable = new TriggerStorable(triggerName, revision);
    try {
      con.queryOneRowForUpdate(storable);
      storable.setError(t);
      con.persistObject(storable);
      con.commit();
    } catch (XynaException e) {
      logger.warn("Could not persist error state of trigger " + triggerName, e);
    } finally {
      finallyClose(con);
    }
  }

  public void setFilterError(String filterName, Long revision, Throwable t) {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    FilterStorable storable = new FilterStorable(filterName, revision);
    try {
      con.queryOneRowForUpdate(storable);
      storable.setError(t);
      con.persistObject(storable);
      con.commit();
    } catch (XynaException e) {
      logger.warn("Could not persist error state of filter " + filterName, e);
    } finally {
      finallyClose(con);
    }
  }

  public void setTriggerInstanceError(String triggerInstanceName, Long revision, Throwable t) {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    TriggerInstanceStorable storable = new TriggerInstanceStorable(triggerInstanceName, revision);
    try {
      con.queryOneRowForUpdate(storable);
      storable.setError(t);
      con.persistObject(storable);
      con.commit();
    } catch (XynaException e) {
      logger.warn("Could not persist error state of triggerInstance " + triggerInstanceName, e);
    } finally {
      finallyClose(con);
    }
  }

  
  public void deleteTrigger(String triggerName, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      List<TriggerInstanceStorable> instances = getTriggerInstancesByTriggerName(triggerName, revision, false);
      con.delete(instances);
      TriggerStorable triggerStorable = new TriggerStorable(triggerName, revision);
      con.deleteOneRow(triggerStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  public void deleteTriggerInstance(String triggerInstanceName, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      TriggerInstanceStorable triggerInstanceStorable = new TriggerInstanceStorable(triggerInstanceName, revision);
      con.deleteOneRow(triggerInstanceStorable);
      
      TriggerConfigurationStorable tcs = new TriggerConfigurationStorable(triggerInstanceName, revision);
      con.deleteOneRow(tcs);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  public void deleteFilter(String filterName, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      Collection<FilterInstanceStorable> instances = getFilterInstancesByFilterName(filterName, revision, false);
      con.delete(instances);
      
      FilterStorable filterStorable = new FilterStorable(filterName, revision);
      con.deleteOneRow(filterStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  public void deleteFilterInstance(String filterInstanceName, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      FilterInstanceStorable toBeDeleted = new FilterInstanceStorable(filterInstanceName, revision);
      con.deleteOneRow(toBeDeleted);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  public void persistObject(Storable<?> storable) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      con.persistObject(storable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  
  public <T extends Storable<?>> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      return con.loadCollection(klass);
    } finally {
      finallyClose(con);
    }
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
  
  public static String getErrorCause(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

}
