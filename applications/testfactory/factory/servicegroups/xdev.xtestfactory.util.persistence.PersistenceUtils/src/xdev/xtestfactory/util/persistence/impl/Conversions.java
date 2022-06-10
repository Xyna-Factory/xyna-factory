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
package xdev.xtestfactory.util.persistence.impl;

import java.util.List;

import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;

import xnwh.persistence.Alternative;
import xnwh.persistence.Default;
import xnwh.persistence.ExtendedParameter;
import xnwh.persistence.History;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;

public class Conversions {

  static com.gip.xyna.xnwh.persistence.xmom.QueryParameter convertQueryParameter(QueryParameter queryParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.QueryParameter(queryParameter.getMaxObjects(),
                                                                 queryParameter.getQueryHistory(),
                                                                 convertSortCriterions(queryParameter
                                                                     .getSortCriterion()));
  }
  
  static SortCriterion[] convertSortCriterions(List<? extends xnwh.persistence.SortCriterion> sortCriterion) {
    if (sortCriterion == null || sortCriterion.size() == 0) {
      return null;
    }
    SortCriterion[] ret = new SortCriterion[sortCriterion.size()];
    for (int i = 0; i<ret.length; i++) {
      ret[i] = convertSortCriterion(sortCriterion.get(i));
    }
    return ret;
  }
  
  static SortCriterion convertSortCriterion(xnwh.persistence.SortCriterion sortCriterion) {    
    return new SortCriterion(sortCriterion.getCriterion(), sortCriterion.getReverse());
  }

  static com.gip.xyna.xnwh.persistence.xmom.SelectionMask convertSelectionMask(SelectionMask mask) {
    return new com.gip.xyna.xnwh.persistence.xmom.SelectionMask(mask.getRootType(), mask.getColumns());
  }
  
  static com.gip.xyna.xnwh.persistence.xmom.ExtendedParameter convertExtendedParameter(ExtendedParameter extendedParameter) {
    ODSConnectionType conType;
    if (extendedParameter == null) {
      conType = ODSConnectionType.DEFAULT;
    } else if (extendedParameter.getConnectionType() instanceof Default) {
      conType = ODSConnectionType.DEFAULT;
    } else if (extendedParameter.getConnectionType() instanceof History) {
      conType = ODSConnectionType.HISTORY;
    } else if (extendedParameter.getConnectionType() instanceof Alternative) {
      conType = ODSConnectionType.ALTERNATIVE;
    } else {
      conType = ODSConnectionType.DEFAULT;
    }
    return new com.gip.xyna.xnwh.persistence.xmom.ExtendedParameter(conType);
  }
}
