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
import java.util.stream.Collectors;

import org.w3c.dom.Node;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentDifferences;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;

public class FactoryDifferenceListStorage {
  public static void init() throws PersistenceLayerException {
    ODSImpl ods = ODSImpl.getInstance();

    ods.registerStorable(FactoryContentDifferencesStorable.class);
    ods.registerStorable(FactoryContentDifferenceStorable.class);
  }
  
  public FactoryContentDifferences loadDifferences(long id) {

    FactoryContentDifferences result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(FactoryContentDifferencesStorable.class, FactoryContentDifferenceStorable.class))
          .execute(new LoadDifferencesList(id));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    return result;
  }



  private List<FactoryContentDifference> convertEntryListToDatatype(Collection<FactoryContentDifferenceStorable> entries) {
    List<FactoryContentDifference> result = new ArrayList<FactoryContentDifference>();

    for (FactoryContentDifferenceStorable entry : entries) {
      FactoryContentDifference converted = convertEntryToDatatype(entry);
      result.add(converted);
    }
    return result;
  }


  private FactoryContentDifference convertEntryToDatatype(FactoryContentDifferenceStorable entry) {
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();

    FactoryContentDifference.Builder builder = new FactoryContentDifference.Builder();
    builder.contentType(entry.getContenttype());
    builder.differenceType(FactoryContentProcessingPortal.differenceTypes.get(entry.getDifferencetype()));
    builder.existingItem(portal.parseWorkspaceContentItem(convertToXMLNode(entry.getExistingitem())));
    builder.entryId(entry.getEntryid());
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


  private FactoryContentDifferenceStorable convertEntryToStorable(FactoryContentDifference entry, long listId,
                                                                    FactoryContentProcessingPortal portal) {
    FactoryContentDifferenceStorable result = new FactoryContentDifferenceStorable();
    result.setContenttype(entry.getContentType());
    result.setDifferencetype(entry.getDifferenceType().getClass().getSimpleName());
    result.setEntryid(entry.getEntryId());
    result.setListid(listId);
    result.setListentryindex(FactoryContentDifferenceStorable.createListentryindex(listId, entry.getEntryId()));

    XmlBuilder builder = new XmlBuilder();
    portal.writeItem(builder, entry.getExistingItem());
    result.setExistingitem(builder.toString());
    builder = new XmlBuilder();
    portal.writeItem(builder, entry.getNewItem());
    result.setNewitem(builder.toString());


    return result;
  }


  public List<? extends FactoryContentDifferences> loadAllDifferencesLists() {
    List<FactoryContentDifferences> result;
    try {
      result = WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(FactoryContentDifferencesStorable.class, FactoryContentDifferenceStorable.class))
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
  public void persist(FactoryContentDifferences differences) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(FactoryContentDifferencesStorable.class, FactoryContentDifferenceStorable.class))
          .execute(new PersistDifferencesList(differences));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteWorkspaceDifferenceList(long listId) {
    try {
      WarehouseRetryExecutor.buildMinorExecutor().connection(ODSConnectionType.HISTORY)
          .storables(new StorableClassList(FactoryContentDifferencesStorable.class, FactoryContentDifferenceStorable.class))
          .execute(new DeleteDifferencesList(listId));
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  public void deleteDifferenceFromList(long listId, long entryId) {

  }


  private class LoadDifferencesList implements WarehouseRetryExecutableNoException<FactoryContentDifferences> {

    private long id;


    public LoadDifferencesList(long id) {
      this.id = id;
    }


    @Override
    public FactoryContentDifferences executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      FactoryContentDifferences.Builder result = new FactoryContentDifferences.Builder();
      FactoryContentDifferencesStorable storable = new FactoryContentDifferencesStorable(id);
      List<FactoryContentDifferenceStorable> entries;
      try {
        con.queryOneRow(storable);
        entries = FactoryContentDifferenceStorable.readAllEntriesForList(con, id);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException("No list with id '" + id + "' found.");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      result.listId(storable.getListid());
      result.differences(convertEntryListToDatatype(entries));

      return result.instance();
    }
  }

  private class LoadAllDifferencesLists implements WarehouseRetryExecutableNoException<List<FactoryContentDifferences>> {

    @Override
    public List<FactoryContentDifferences> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<FactoryContentDifferences> result = new ArrayList<FactoryContentDifferences>();

      Collection<FactoryContentDifferencesStorable> collection = con.loadCollection(FactoryContentDifferencesStorable.class);
      Collection<FactoryContentDifferenceStorable> entries = con.loadCollection(FactoryContentDifferenceStorable.class);

      for (FactoryContentDifferencesStorable singleList : collection) {
        FactoryContentDifferences.Builder differences = new FactoryContentDifferences.Builder();
        Collection<FactoryContentDifferenceStorable> filteredEntries =
            entries.stream().filter(y -> y.getListid() == singleList.getListid()).collect(Collectors.toList());
        differences.listId(singleList.getListid());
        differences.differences(convertEntryListToDatatype(filteredEntries));

        result.add(differences.instance());
      }

      return result;
    }

  }

  private class DeleteDifferencesList implements WarehouseRetryExecutableNoResult {

    private long id;


    public DeleteDifferencesList(long id) {
      this.id = id;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      List<FactoryContentDifferenceStorable> entries = FactoryContentDifferenceStorable.readAllEntriesForList(con, id);
      con.delete(entries);
      con.deleteOneRow(new FactoryContentDifferencesStorable(id));
    }

  }

  private class PersistDifferencesList implements WarehouseRetryExecutableNoResult {

    private FactoryContentDifferences content;


    public PersistDifferencesList(FactoryContentDifferences content) {
      this.content = content;
    }


    @Override
    public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
      FactoryContentDifferencesStorable storable = new FactoryContentDifferencesStorable();
      boolean isUpdate = true;
      storable.setListid(content.getListId());
      if (content.getListId() == -1) {
        long id = System.currentTimeMillis();
        storable.setListid(id);
        content.unversionedSetListId(storable.getListid());
        storable.setListid(content.getListId());
        isUpdate = false;
      }

      List<FactoryContentDifferenceStorable> entries = new ArrayList<FactoryContentDifferenceStorable>();
      List<? extends FactoryContentDifference> differences = content.getDifferences();
      for (FactoryContentDifference diff : differences) {
        FactoryContentDifferenceStorable s = convertEntryToStorable(diff, content.getListId(), portal);
        entries.add(s);
      }

      con.persistObject(storable);

      //TODO: determine difference first and only apply that instead of deleting everything and adding most of it back
      if (isUpdate) {
        //remove all existing entries for this list
        List<FactoryContentDifferenceStorable> oldEntries =
            FactoryContentDifferenceStorable.readAllEntriesForList(con, content.getListId());
        con.delete(oldEntries);
      }

      con.persistCollection(entries);
    }

  }
}
