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

package com.gip.xyna.update.outdatedclasses_7_0_2_13;

import java.util.List;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;


public class JavaDestinationAuditData extends XynaEngineSpecificAuditData {

  private static final long serialVersionUID = -5820045978705376839L;


  public JavaDestinationAuditData(AuditData parent) {
    super(parent);
  }


  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards)
      throws Ex_FileAccessException, XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    // TODO do something here?
    return "";
  }

}
