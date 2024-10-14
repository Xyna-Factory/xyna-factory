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
package xmcp.gitintegration.storage;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.w3c.dom.Node;

import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;

import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.impl.processing.WorkspaceContentProcessingPortal;



/***
 *
 * 'Interface' between the outside world and the persistence classes.
 * Users of this class no not need to be aware of how the data backing
 * WorkspaceContentDifferences work.
 *
 */
public class WorkspaceDifferenceListStorage {


  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();

    ods.registerStorable(WorkspaceContentDifferencesStorable.class);
    ods.registerStorable(WorkspaceContentDifferenceStorable.class);
  }


  public WorkspaceContentDifferences loadDifferences(long id) {

    WorkspaceContentDifferences result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(WorkspaceContentDifferencesStorable.class, WorkspaceContentDifferenceStorable.class))
          .execute(new LoadDifferencesList(id));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    return result;
  }


  private List<WorkspaceContentDifference> convertEntryListToDatatype(Collection<WorkspaceContentDifferenceStorable> entries) {
    List<WorkspaceContentDifference> result = new ArrayList<WorkspaceContentDifference>();

    for (WorkspaceContentDifferenceStorable entry : entries) {
      WorkspaceContentDifference converted = convertEntryToDatatype(entry);
      result.add(converted);
    }
    return result;
  }


  private WorkspaceContentDifference convertEntryToDatatype(WorkspaceContentDifferenceStorable entry) {
    WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();

    WorkspaceContentDifference.Builder builder = new WorkspaceContentDifference.Builder();
    builder.contentType(entry.getContenttype());
    builder.differenceType(WorkspaceContentProcessingPortal.differenceTypes.get(entry.getDifferencetype()));
    builder.existingItem(portal.parseWorkspaceContentItem(convertToXMLNode(entry.getExistingitem())));
    builder.id(entry.getEntryid());
    builder.newItem(portal.parseWorkspaceContentItem(convertToXMLNode(entry.getNewitem())));
    return builder.instance();
  }


  private Node convertToXMLNode(String s) {
    if (s == null || s.isEmpty()) {
      return null;
    }
    try {
      return XMLUtils.parseString(s).getChildNodes().item(0);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
  }


  private WorkspaceContentDifferenceStorable convertEntryToStorable(WorkspaceContentDifference entry, long listId,
                                                                    WorkspaceContentProcessingPortal portal) {
    WorkspaceContentDifferenceStorable result = new WorkspaceContentDifferenceStorable();
    result.setContenttype(entry.getContentType());
    result.setDifferencetype(entry.getDifferenceType().getClass().getSimpleName());
    result.setEntryid(entry.getId());
    result.setListid(listId);
    result.setListentryindex(WorkspaceContentDifferenceStorable.createListentryindex(listId, entry.getId()));

    XmlBuilder builder = new XmlBuilder();
    portal.writeItem(builder, entry.getExistingItem());
    result.setExistingitem(builder.toString());
    builder = new XmlBuilder();
    portal.writeItem(builder, entry.getNewItem());
    result.setNewitem(builder.toString());


    return result;
  }


  public List<? extends WorkspaceContentDifferences> loadAllDifferencesLists() {
    List<WorkspaceContentDifferences> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(WorkspaceContentDifferencesStorable.class, WorkspaceContentDifferenceStorable.class))
          .execute(new LoadAllDifferencesLists());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    return result;
  }


  /**
   * If differences.listId is -1, a new differences list is created and the new listId is set.
   * Otherwise, the entry for the given listId is updated
   * @param differences
   * data to persist
   */
  public void persist(WorkspaceContentDifferences differences) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(WorkspaceContentDifferencesStorable.class, WorkspaceContentDifferenceStorable.class))
          .execute(new PersistDifferencesList(differences));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteWorkspaceDifferenceList(long listId) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(WorkspaceContentDifferencesStorable.class, WorkspaceContentDifferenceStorable.class))
          .execute(new DeleteDifferencesList(listId));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteDifferenceFromList(long listId, long entryId) {

  }


  private class LoadDifferencesList implements WarehouseRetryExecutableNoException<WorkspaceContentDifferences> {

    private long id;


    public LoadDifferencesList(long id) {
      this.id = id;
    }


    @Override
    public WorkspaceContentDifferences executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      WorkspaceContentDifferences.Builder result = new WorkspaceContentDifferences.Builder();
      WorkspaceContentDifferencesStorable storable = new WorkspaceContentDifferencesStorable(id);
      List<WorkspaceContentDifferenceStorable> entries;
      try {
        con.queryOneRow(storable);
        entries = WorkspaceContentDifferenceStorable.readAllEntriesForList(con, id);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("No list with id '" + id + "' found.");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      result.listId(storable.getListid());
      result.workspaceName(storable.getWorkspacename());
      result.differences(convertEntryListToDatatype(entries));

      return result.instance();
    }
  }
  
  public List<WorkspaceContentDifferences> loadDifferencesLists(String workspace, boolean loadContent) {
    List<WorkspaceContentDifferences> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(WorkspaceContentDifferencesStorable.class, WorkspaceContentDifferenceStorable.class))
          .execute(new LoadDifferencesLists(workspace, loadContent));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    return result;
  }
  
  private class LoadDifferencesLists implements WarehouseRetryExecutableNoException<List<WorkspaceContentDifferences>> {

    private String workspace;
    private boolean loadContent;
    
    public LoadDifferencesLists(String workspace, boolean loadContent) {
      this.workspace = workspace;
      this.loadContent = loadContent;
    }
    
    @Override
    public List<WorkspaceContentDifferences> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<WorkspaceContentDifferences> result = new ArrayList<WorkspaceContentDifferences>();
      Collection<WorkspaceContentDifferencesStorable> collection = con.loadCollection(WorkspaceContentDifferencesStorable.class);
      Collection<WorkspaceContentDifferenceStorable> entries = loadContent ? con.loadCollection(WorkspaceContentDifferenceStorable.class) : new ArrayList<>();

      for(WorkspaceContentDifferencesStorable storable : collection) {
        if(!Objects.equals(workspace, storable.getWorkspacename())) {
          continue;
        }
        
        xmcp.gitintegration.WorkspaceContentDifferences.Builder differences = new WorkspaceContentDifferences.Builder();
        differences.listId(storable.getListid());
        
        if(loadContent) {
          Collection<WorkspaceContentDifferenceStorable> filteredEntries = filterEntries(entries, storable.getListid());
          differences.differences(convertEntryListToDatatype(filteredEntries));
        }
        
        result.add(differences.instance());
      }
      
      return result;
    }
    
  }

  private class LoadAllDifferencesLists implements WarehouseRetryExecutableNoException<List<WorkspaceContentDifferences>> {

    @Override
    public List<WorkspaceContentDifferences> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<WorkspaceContentDifferences> result = new ArrayList<WorkspaceContentDifferences>();

      Collection<WorkspaceContentDifferencesStorable> collection = con.loadCollection(WorkspaceContentDifferencesStorable.class);
      Collection<WorkspaceContentDifferenceStorable> entries = con.loadCollection(WorkspaceContentDifferenceStorable.class);

      for (WorkspaceContentDifferencesStorable singleList : collection) {
        xmcp.gitintegration.WorkspaceContentDifferences.Builder differences = new WorkspaceContentDifferences.Builder();
        Collection<WorkspaceContentDifferenceStorable> filteredEntries = filterEntries(entries, singleList.getListid());
        differences.listId(singleList.getListid());
        differences.workspaceName(singleList.getWorkspacename());
        differences.differences(convertEntryListToDatatype(filteredEntries));

        result.add(differences.instance());
      }

      return result;
    }
  }
  
  private Collection<WorkspaceContentDifferenceStorable> filterEntries(Collection<WorkspaceContentDifferenceStorable> entries, long listId) {
    return entries.stream().filter(y -> y.getListid() == listId).collect(Collectors.toList());
  }
  
  private class DeleteDifferencesList implements WarehouseRetryExecutableNoResult {

    private long id;


    public DeleteDifferencesList(long id) {
      this.id = id;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<WorkspaceContentDifferenceStorable> entries = WorkspaceContentDifferenceStorable.readAllEntriesForList(con, id);
      con.delete(entries);
      con.deleteOneRow(new WorkspaceContentDifferencesStorable(id));
    }

  }

  private class PersistDifferencesList implements WarehouseRetryExecutableNoResult {

    private WorkspaceContentDifferences content;


    public PersistDifferencesList(WorkspaceContentDifferences content) {
      this.content = content;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      WorkspaceContentProcessingPortal portal = new WorkspaceContentProcessingPortal();
      WorkspaceContentDifferencesStorable storable = new WorkspaceContentDifferencesStorable();
      boolean isUpdate = true;
      storable.setListid(content.getListId());
      if (content.getListId() == -1) {
        long id = System.currentTimeMillis();
        storable.setListid(id);
        content.unversionedSetListId(storable.getListid());
        storable.setListid(content.getListId());
        isUpdate = false;
      }
      storable.setWorkspacename(content.getWorkspaceName());

      List<WorkspaceContentDifferenceStorable> entries = new ArrayList<WorkspaceContentDifferenceStorable>();
      List<? extends WorkspaceContentDifference> differences = content.getDifferences();
      for (WorkspaceContentDifference diff : differences) {
        WorkspaceContentDifferenceStorable s = convertEntryToStorable(diff, content.getListId(), portal);
        entries.add(s);
      }

      con.persistObject(storable);

      //TODO: determine difference first and only apply that instead of deleting everything and adding most of it back
      if (isUpdate) {
        //remove all existing entries for this list
        List<WorkspaceContentDifferenceStorable> oldEntries =
            WorkspaceContentDifferenceStorable.readAllEntriesForList(con, content.getListId());
        con.delete(oldEntries);
      }

      con.persistCollection(entries);
    }

  }
}
