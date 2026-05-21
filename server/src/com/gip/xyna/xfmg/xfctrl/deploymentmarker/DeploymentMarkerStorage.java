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
package com.gip.xyna.xfmg.xfctrl.deploymentmarker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.storables.DeploymentTagStorable;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.storables.DeploymentTaskStorable;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


public class DeploymentMarkerStorage {
  
  private static Logger logger = CentralFactoryLogging.getLogger(DeploymentMarkerStorage.class);

  private ODSImpl ods;
  private PreparedQueryCache queryCache;
  
  //Lock, damit z.B. der Index nicht gleichzeitg hochgezählt wird 
  //und beim Löschen aller DeploymentMarker eines DeploymentItems nicht gleichzeitig ein neuer Marker angelegt wird
  private ReentrantLock lock = new ReentrantLock();
  
  private static final String QUERY_GET_TASKS_FOR_DEPLOYMENT_ITEM =
     "select * from " + DeploymentTaskStorable.TABLENAME + " where " 
     + DeploymentTaskStorable.COL_DEPLOYMENT_ITEM_NAME + " = ? and " 
     + DeploymentTaskStorable.COL_DEPLOYMENT_ITEM_TYPE + " = ? and " 
     + DeploymentTaskStorable.COL_REVISION + " = ?";
  private static final String QUERY_GET_TASKS_FOR_REVISION =
     "select * from " + DeploymentTaskStorable.TABLENAME + " where " 
     + DeploymentTaskStorable.COL_REVISION + " = ?";
  private static final String QUERY_GET_TAGS_FOR_DEPLOYMENT_ITEM =
      "select * from " + DeploymentTagStorable.TABLENAME + " where " 
      + DeploymentTagStorable.COL_DEPLOYMENT_ITEM_NAME + " = ? and " 
      + DeploymentTagStorable.COL_DEPLOYMENT_ITEM_TYPE + " = ? and " 
      + DeploymentTagStorable.COL_REVISION + " = ?";
  private static final String QUERY_GET_TAGS_FOR_REVISION =
      "select * from " + DeploymentTagStorable.TABLENAME + " where " 
      + DeploymentTagStorable.COL_REVISION + " = ?";
  private static final String QUERY_COUNT_TASKS_FOR_DEPLOYMENT_ITEM = 
      "select count(*) from " + DeploymentTaskStorable.TABLENAME + " where " 
      + DeploymentTaskStorable.COL_DEPLOYMENT_ITEM_NAME + " = ? and " 
      + DeploymentTaskStorable.COL_DEPLOYMENT_ITEM_TYPE + " = ? and " 
      + DeploymentTaskStorable.COL_REVISION + " = ? and "
      + DeploymentTaskStorable.COL_DONE + " = ?";
  
  private static ResultSetReader<Integer> countReader = new ResultSetReader<Integer>() {
    public Integer read(ResultSet rs) throws SQLException {
      int count = rs.getInt(1);
      return count;
    }
  };
  
  public DeploymentMarkerStorage() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    ods.registerStorable(DeploymentTaskStorable.class);
    ods.registerStorable(DeploymentTagStorable.class);
    
    queryCache = new PreparedQueryCache();
  }
  

  public DeploymentMarker createDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Long revision = marker.getRevision();
    Storable<?> markerStorable;
    long id;
    
    lock.lock();
    try {
      DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      DeploymentItemState dis = dism.get(marker.getDeploymentItem().getName(), revision);
      
      if (dis == null || !dis.exists()) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(marker.getDeploymentItem().getName(), "deploymentItem");
      }
      
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        if (marker instanceof DeploymentTask) {
          DeploymentTaskStorable taskStorable = createDeploymentTaskStorable(con, (DeploymentTask)marker, revision);
          id = taskStorable.getId();
          markerStorable = taskStorable;
        } else if (marker instanceof DeploymentTag) {
          DeploymentTagStorable tagStorable = createDeploymentTagStorable(con, (DeploymentTag)marker, revision);
          id = tagStorable.getId();
          markerStorable = tagStorable;
        } else {
          throw new IllegalStateException("DeploymentMarker should be a DeploymentTag or DeploymentTask but " + marker.getClass().getName());
        }
        
        marker.setId(id);
        
        con.persistObject(markerStorable);
        con.commit();
      } finally {
        finallyClose(con);
      }
    } finally {
      lock.unlock();
    }
    
    return marker;
  }

  private DeploymentTaskStorable createDeploymentTaskStorable(ODSConnection con, DeploymentTask task, Long revision) throws PersistenceLayerException {
    int index = getNextTaskIndex(con, task.getDeploymentItem(), revision);
    return new DeploymentTaskStorable(task.getDeploymentItem().getName(),
                                      task.getDeploymentItem().getType(),
                                      revision,
                                      index,
                                      task.getDescription(),
                                      task.isDone(),
                                      task.getPriority());
  }
  
  private DeploymentTagStorable createDeploymentTagStorable(ODSConnection con, DeploymentTag tag, Long revision) throws PersistenceLayerException {
    int index = getNextTagIndex(con, tag.getDeploymentItem(), revision);
    return new DeploymentTagStorable(tag.getDeploymentItem().getName(),
                                     tag.getDeploymentItem().getType(),
                                     revision,
                                     index,
                                     tag.getLabel());
  }
  
  private int getNextTaskIndex(ODSConnection con, DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    List<DeploymentTaskStorable> tasks = searchTasksForDeploymentItem(con, deploymentItem, revision);
    if (tasks.size() == 0) {
      return 0;
    }
    
    DeploymentTaskStorable lastTask = tasks.get(tasks.size() - 1);
    return lastTask.getIndex() + 1;
  }

  private int getNextTagIndex(ODSConnection con, DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    List<DeploymentTagStorable> tags = searchTagsForDeploymentItem(con, deploymentItem, revision);
    if (tags.size() == 0) {
      return 0;
    }
    
    DeploymentTagStorable lastTag = tags.get(tags.size() - 1);
    return lastTag.getIndex() + 1;
  }
  
  public void deleteDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException {
    Storable<?> toDelete;
    if (marker instanceof DeploymentTask) {
      toDelete = new DeploymentTaskStorable(marker.getId());
    } else if (marker instanceof DeploymentTag) {
      toDelete = new DeploymentTagStorable(marker.getId());
    } else {
      throw new IllegalStateException("DeploymentMarker should be a DeploymentTag or DeploymentTask but " + marker.getClass().getName());
    }
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(toDelete);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  public void deleteDeploymentMarkerForDeploymentItem(DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    lock.lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        List<DeploymentTaskStorable> tasks = searchTasksForDeploymentItem(con, deploymentItem, revision);
        List<DeploymentTagStorable> tags = searchTagsForDeploymentItem(con, deploymentItem, revision);

        con.delete(tasks);
        con.delete(tags);
        con.commit();
      } finally {
        finallyClose(con);
      }
    } finally {
      lock.unlock();
    }
  }

  public void deleteDeploymentMarkerForRevision(Long revision) throws PersistenceLayerException {
    lock.lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        List<DeploymentTaskStorable> tasks = searchTasksForRevision(con, revision);
        List<DeploymentTagStorable> tags = searchTagsForRevision(con, revision);
        
        con.delete(tasks);
        con.delete(tags);
        con.commit();
      } finally {
        finallyClose(con);
      }
    } finally {
      lock.unlock();
    }
  }


  public void modifyDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    lock.lock();
    try {
      if (marker instanceof DeploymentTask) {
        modifyTask((DeploymentTask)marker);
      } else if (marker instanceof DeploymentTag) {
        modifyTag((DeploymentTag)marker);
      } else {
        throw new IllegalStateException("DeploymentMarker should be a DeploymentTag or DeploymentTask but " + marker.getClass().getName());
      }
    } finally {
      lock.unlock();
    }
  }
  
  
  private void modifyTask(DeploymentTask task) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DeploymentTaskStorable taskStorable = new DeploymentTaskStorable(task.getId());
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.queryOneRowForUpdate(taskStorable);
      taskStorable.setDescription(task.getDescription());
      taskStorable.setDone(task.isDone());
      taskStorable.setPriority(task.getPriority());
      con.persistObject(taskStorable);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  private void modifyTag(DeploymentTag tag) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DeploymentTagStorable tagStorable = new DeploymentTagStorable(tag.getId());
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.queryOneRowForUpdate(tagStorable);
      tagStorable.setLabel(tag.getLabel());
      con.persistObject(tagStorable);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  public void moveDeploymentMarker(DeploymentItemIdentifier oldDeploymentItem, DeploymentItemIdentifier newDeploymentItem, Long revision) throws PersistenceLayerException {
    lock.lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        List<DeploymentTaskStorable> oldTasks = searchTasksForDeploymentItem(con, oldDeploymentItem, revision);
        List<DeploymentTaskStorable> newTasks = CollectionUtils.transform(oldTasks, new MoveTask(newDeploymentItem));
        
        List<DeploymentTagStorable> oldTags = searchTagsForDeploymentItem(con, oldDeploymentItem, revision);
        List<DeploymentTagStorable> newTags = CollectionUtils.transform(oldTags, new MoveTag(newDeploymentItem));

        con.persistCollection(newTasks);
        con.persistCollection(newTags);
        con.commit();
      } finally {
        finallyClose(con);
      }
    } finally {
     lock.unlock();
    }
  }
  
  public List<DeploymentMarker> searchDeploymentTasks(Optional<? extends DeploymentItemIdentifier> deploymentItem, Long revision) throws PersistenceLayerException {
    List<DeploymentTaskStorable> taskStorables;
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      if (deploymentItem.isPresent()) {
        taskStorables = searchTasksForDeploymentItem(con, deploymentItem.get(), revision);
      } else {
        taskStorables = searchTasksForRevision(con, revision);
      }
    } finally {
      finallyClose(con);
    }
    
    return CollectionUtils.transform(taskStorables, new TransformTask());
  }

  @SuppressWarnings("unchecked")
  private List<DeploymentTaskStorable> searchTasksForDeploymentItem(ODSConnection con, DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    PreparedQuery<DeploymentTaskStorable> query = (PreparedQuery<DeploymentTaskStorable>) queryCache.getQueryFromCache(QUERY_GET_TASKS_FOR_DEPLOYMENT_ITEM, 
                                                                                                                       con, 
                                                                                                                       new DeploymentTaskStorable().getReader(),
                                                                                                                       DeploymentTaskStorable.TABLENAME);
    List<DeploymentTaskStorable> tasks = con.query(query, new Parameter(deploymentItem.getName(), deploymentItem.getType().toString(), revision), -1);
    Collections.sort(tasks, new DeploymentTaskComparator());
    return tasks;
  }

  @SuppressWarnings("unchecked")
  private List<DeploymentTaskStorable> searchTasksForRevision(ODSConnection con, Long revision) throws PersistenceLayerException {
    PreparedQuery<DeploymentTaskStorable> query = (PreparedQuery<DeploymentTaskStorable>) queryCache.getQueryFromCache(QUERY_GET_TASKS_FOR_REVISION, 
                                                                                                                       con, 
                                                                                                                       new DeploymentTaskStorable().getReader(),
                                                                                                                       DeploymentTaskStorable.TABLENAME);
    List<DeploymentTaskStorable> tasks = con.query(query, new Parameter(revision), -1);
    Collections.sort(tasks, new DeploymentTaskComparator());
    return tasks;
  }

  public int countOpenDeploymentTasks(DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<Integer> query = queryCache.getQueryFromCache(QUERY_COUNT_TASKS_FOR_DEPLOYMENT_ITEM, con, countReader, DeploymentTaskStorable.TABLENAME);
      return con.queryOneRow(query, new Parameter(deploymentItem.getName(), deploymentItem.getType().toString(), revision, false));
    } finally {
      finallyClose(con);
    }
  }

  
  public List<DeploymentMarker> searchDeploymentTags(Optional<? extends DeploymentItemIdentifier> deploymentItem, Long revision) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<DeploymentTagStorable> tagStorables;
      if (deploymentItem.isPresent()) {
        tagStorables = searchTagsForDeploymentItem(con, deploymentItem.get(), revision);
      } else {
        tagStorables = searchTagsForRevision(con, revision);
      }

      return CollectionUtils.transform(tagStorables, new TransformTag());
    } finally {
      finallyClose(con);
    }
  }

  @SuppressWarnings("unchecked")
  private List<DeploymentTagStorable> searchTagsForDeploymentItem(ODSConnection con, DeploymentItemIdentifier deploymentItem, Long revision) throws PersistenceLayerException {
    PreparedQuery<DeploymentTagStorable> query = (PreparedQuery<DeploymentTagStorable>) queryCache.getQueryFromCache(QUERY_GET_TAGS_FOR_DEPLOYMENT_ITEM, 
                                                                                                                     con, new DeploymentTagStorable().getReader(), 
                                                                                                                     DeploymentTagStorable.TABLENAME);
    List<DeploymentTagStorable> tags = con.query(query, new Parameter(deploymentItem.getName(), deploymentItem.getType().toString(), revision), -1);
    //aufsteigend nach index sortieren
    Collections.sort(tags, new DeploymentTagComparator());
    return tags;
  }
  
  @SuppressWarnings("unchecked")
  private List<DeploymentTagStorable> searchTagsForRevision(ODSConnection con, Long revision) throws PersistenceLayerException {
    PreparedQuery<DeploymentTagStorable> query = (PreparedQuery<DeploymentTagStorable>) queryCache.getQueryFromCache(QUERY_GET_TAGS_FOR_REVISION, 
                                                                                                                     con, 
                                                                                                                     new DeploymentTagStorable().getReader(),
                                                                                                                     DeploymentTagStorable.TABLENAME);
    List<DeploymentTagStorable> tags = con.query(query, new Parameter(revision), -1);
    //aufsteigend nach index sortieren
    Collections.sort(tags, new DeploymentTagComparator());
    return tags;
  }
  
  private static class DeploymentTaskComparator implements Comparator<DeploymentTaskStorable> {

    public int compare(DeploymentTaskStorable o1, DeploymentTaskStorable o2) {
      return o1.getIndex() > o2.getIndex() ? 1 : o1.getIndex() < o2.getIndex() ? -1 : 0;
    }
    
  }
  
  private static class DeploymentTagComparator implements Comparator<DeploymentTagStorable> {

    public int compare(DeploymentTagStorable o1, DeploymentTagStorable o2) {
      return o1.getIndex() > o2.getIndex() ? 1 : o1.getIndex() < o2.getIndex() ? -1 : 0;
    }
    
  }
  
  private static class TransformTask implements Transformation<DeploymentTaskStorable, DeploymentMarker> {
    public DeploymentMarker transform(DeploymentTaskStorable from) {
      if(from == null) {
        return null;
      }
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

      DeploymentTask to = new DeploymentTask();
      to.setId(from.getId());
      XMOMType type = XMOMType.valueOf(from.getDeploymentItemType());
      to.setDeploymentItem(new DeploymentItemIdentificationBase(type, from.getDeploymentItemName()));
      to.setDescription(from.getDescription());
      to.setDone(from.isDone());
      to.setPriority(from.getPriority());
      try {
        to.setRuntimeContext(revisionManagement.getRuntimeContext(from.getRevision()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("Revision " + from.getRevision() + " unkown", e);
      }
      
      return to;
    }
  }

  private static class TransformTag implements Transformation<DeploymentTagStorable, DeploymentMarker> {
    public DeploymentMarker transform(DeploymentTagStorable from) {
      if(from == null) {
        return null;
      }
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      
      DeploymentTag to = new DeploymentTag();
      to.setId(from.getId());
      XMOMType type = XMOMType.valueOf(from.getDeploymentItemType());
      to.setDeploymentItem(new DeploymentItemIdentificationBase(type, from.getDeploymentItemName()));
      to.setLabel(from.getLabel());
      try {
        to.setRuntimeContext(revisionManagement.getRuntimeContext(from.getRevision()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("Revision " + from.getRevision() + " unkown", e);
      }
      
      return to;
    }
  }

  private static class MoveTag implements Transformation<DeploymentTagStorable, DeploymentTagStorable> {
    
    private DeploymentItemIdentifier newDeploymentItem;
    
    public MoveTag(DeploymentItemIdentifier newDeploymentItem) {
      this.newDeploymentItem = newDeploymentItem;
    }

    public DeploymentTagStorable transform(DeploymentTagStorable from) {
      if(from == null) {
        return null;
      }
      
      from.setDeploymentItemName(newDeploymentItem.getName());
      from.setDeploymentItemType(newDeploymentItem.getType().toString());
      
      return from;
    }
  }

  private static class MoveTask implements Transformation<DeploymentTaskStorable, DeploymentTaskStorable> {
    
    private DeploymentItemIdentifier newDeploymentItem;
    
    public MoveTask(DeploymentItemIdentifier newDeploymentItem) {
      this.newDeploymentItem = newDeploymentItem;
    }
    
    public DeploymentTaskStorable transform(DeploymentTaskStorable from) {
      if(from == null) {
        return null;
      }
      
      from.setDeploymentItemName(newDeploymentItem.getName());
      from.setDeploymentItemType(newDeploymentItem.getType().toString());
      
      return from;
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
}
