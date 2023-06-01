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


public class ServiceDestinationAuditData extends XynaEngineSpecificAuditData {

  private static final long serialVersionUID = 120549527090914245L;


  public ServiceDestinationAuditData(AuditData parent) {
    super(parent);
  }


  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards)
      throws Ex_FileAccessException, XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    return createXmlForServiceDestination(id, parentId, revision, removeAuditDataAfterwards);
  }


  private String createXmlForServiceDestination(long id, long parentId, Long revision, boolean removeAuditDataAfterwards) {

    XMLExtension xmlExtension = new XMLExtension(this, revision, removeAuditDataAfterwards);
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(Integer.MAX_VALUE);
    if (container != null) {
      xmlExtension.createXmlForServiceDestination(id, parentId, container);
    }
    return xmlExtension.getString();
  }

}
