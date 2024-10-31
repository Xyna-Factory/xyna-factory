/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xdev.yang.impl;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;



public class XmomDbInteraction {


  public List<XMOMDatabaseSearchResultEntry> searchYangDTs(String baseClassFqn, List<Long> revisions) {
    try {
      List<XMOMDatabaseSearchResultEntry> result = new ArrayList<>();
      XMOMDatabase xmomDb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      for (Long revision : revisions) {
        List<XMOMDatabaseSelect> selects = buildSelects(baseClassFqn); // new selects are required for every XMOM database search
        XMOMDatabaseSearchResult xmomDbSearchResult = xmomDb.searchXMOMDatabase(selects, -1, revision);
        List<XMOMDatabaseSearchResultEntry> xmomDbResults = xmomDbSearchResult.getResult();
        result.addAll(xmomDbResults);
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private List<XMOMDatabaseSelect> buildSelects(String baseClassFqn) {
    List<XMOMDatabaseSelect> selects = new ArrayList<>();
    XMOMDatabaseSelect select = new XMOMDatabaseSelect();
    select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.SERVICEGROUP, XMOMDatabaseType.DATATYPE));
    try {
      select.where(XMOMDatabaseEntryColumn.EXTENDS).isEqual(baseClassFqn);
    } catch (XNWH_WhereClauseBuildException e) {
      throw new RuntimeException(e);
    }
    selects.add(select);

    return selects;
  }
}
