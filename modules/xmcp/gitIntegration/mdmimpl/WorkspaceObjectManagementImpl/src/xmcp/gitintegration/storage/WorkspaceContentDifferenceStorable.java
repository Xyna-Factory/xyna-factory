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



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = WorkspaceContentDifferenceStorable.COL_LIST_ENTRY_INDEX, tableName = WorkspaceContentDifferenceStorable.TABLE_NAME)
public class WorkspaceContentDifferenceStorable extends Storable<WorkspaceContentDifferenceStorable> {

  private static final long serialVersionUID = -1L;
  private static PreparedQueryCache queryCache = new PreparedQueryCache();

  public static final String TABLE_NAME = "workspacecontentdifference";

  public static final String COL_LIST_ENTRY_INDEX = "listentryindex";
  public static final String COL_ENTRY_ID = "entryid";
  public static final String COL_LIST_ID = "listid";
  public static final String COL_CONTENT_TYPE = "contenttype";
  public static final String COL_DIFFERENCE_TYPE = "differencetype";
  public static final String COL_EXISTING_ITEM = "existingitem";
  public static final String COL_NEW_ITEM = "newitem";

  @Column(name = COL_LIST_ENTRY_INDEX)
  private String listentryindex;
  @Column(name = COL_ENTRY_ID)
  private long entryid;
  @Column(name = COL_LIST_ID)
  private long listid;
  @Column(name = COL_CONTENT_TYPE)
  private String contenttype;
  @Column(name = COL_DIFFERENCE_TYPE)
  private String differencetype;
  @Column(name = COL_EXISTING_ITEM, size = 5000)
  private String existingitem;
  @Column(name = COL_NEW_ITEM, size = 5000)
  private String newitem;


  public WorkspaceContentDifferenceStorable() {
    super();
  }


  public WorkspaceContentDifferenceStorable(long listid, long entryid) {
    this.listid = listid;
    this.entryid = entryid;
    this.listentryindex = createListentryindex(listid, entryid);
  }


  @Override
  public String getPrimaryKey() {
    return listentryindex;
  }


  private static WorkspaceContentDifferenceStorableReader reader = new WorkspaceContentDifferenceStorableReader();


  @Override
  public ResultSetReader<? extends WorkspaceContentDifferenceStorable> getReader() {
    return reader;
  }


  private static class WorkspaceContentDifferenceStorableReader implements ResultSetReader<WorkspaceContentDifferenceStorable> {

    public WorkspaceContentDifferenceStorable read(ResultSet rs) throws SQLException {
      WorkspaceContentDifferenceStorable result = new WorkspaceContentDifferenceStorable();
      result.listentryindex = rs.getString(COL_LIST_ENTRY_INDEX);
      result.listid = rs.getLong(COL_LIST_ID);
      result.entryid = rs.getLong(COL_ENTRY_ID);
      result.contenttype = rs.getString(COL_CONTENT_TYPE);
      result.differencetype = rs.getString(COL_DIFFERENCE_TYPE);
      result.existingitem = rs.getString(COL_EXISTING_ITEM);
      result.newitem = rs.getString(COL_NEW_ITEM);
      return result;
    }
  }


  @Override
  public <U extends WorkspaceContentDifferenceStorable> void setAllFieldsFromData(U data) {
    WorkspaceContentDifferenceStorable cast = data;
    listentryindex = cast.listentryindex;
    listid = cast.listid;
    entryid = cast.entryid;
    contenttype = cast.contenttype;
    differencetype = cast.differencetype;
    existingitem = cast.existingitem;
    newitem = cast.newitem;
  }


  public static List<WorkspaceContentDifferenceStorable> readAllEntriesForList(ODSConnection con, long listid)
      throws PersistenceLayerException {
    PreparedQuery<WorkspaceContentDifferenceStorable> query = queryCache.getQueryFromCache(QUERY_ENTRIES_FOR_LIST, con, reader);
    List<WorkspaceContentDifferenceStorable> result = con.query(query, new Parameter(listid), -1);
    result.sort(comparator);
    return result;
  }


  private static final String QUERY_ENTRIES_FOR_LIST =
      "select * from " + WorkspaceContentDifferenceStorable.TABLE_NAME + " where " + WorkspaceContentDifferenceStorable.COL_LIST_ID + "=?";


  public static String createListentryindex(long listId, long entryId) {
    return listId + "_" + entryId;
  }


  public String getListentryindex() {
    return listentryindex;
  }


  public long getEntryid() {
    return entryid;
  }


  public long getListid() {
    return listid;
  }


  public String getContenttype() {
    return contenttype;
  }


  public String getDifferencetype() {
    return differencetype;
  }


  public String getExistingitem() {
    return existingitem;
  }


  public String getNewitem() {
    return newitem;
  }


  public void setListentryindex(String listentryindex) {
    this.listentryindex = listentryindex;
  }


  public void setEntryid(long entryid) {
    this.entryid = entryid;
  }


  public void setListid(long listid) {
    this.listid = listid;
  }


  public void setContenttype(String contenttype) {
    this.contenttype = contenttype;
  }


  public void setDifferencetype(String differencetype) {
    this.differencetype = differencetype;
  }


  public void setExistingitem(String existingitem) {
    this.existingitem = existingitem;
  }


  public void setNewitem(String newitem) {
    this.newitem = newitem;
  }


  private static final EntryComparator comparator = new EntryComparator();


  private static class EntryComparator implements Comparator<WorkspaceContentDifferenceStorable> {

    @Override
    public int compare(WorkspaceContentDifferenceStorable o1, WorkspaceContentDifferenceStorable o2) {
      return (int) (o2.getEntryid() - o1.getEntryid());
    }

  }

}
