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

package com.gip.xyna.xfmg.xopctrl.managedsessions.notification;

import java.util.List;



public class MdmModificationChangeListener extends AChangeNotificationListener {

  private final ANotificationConnection connection;


  public MdmModificationChangeListener(ANotificationConnection connection) {
    this.connection = connection;
  }


  @Override
  public void onChange(AChangeEvent event) {
    if (event instanceof MdmModificationChangeEvent) {
      MdmModificationChangeEvent mdmEvent = (MdmModificationChangeEvent) event;
      List<String> modifiedClasses = mdmEvent.getModifiedFullyQualifiedNames();
      connection.replyList(2);
      connection.reply(AChangeEvent.CHANGE_EVENT_MDM_MODIFICATION);
      connection.replyList(modifiedClasses.size());
      for (String s : modifiedClasses) {
        connection.reply(s);
      }
      connection.finished();
    }
  }


  @Override
  public boolean matches(AChangeEvent event) {
    if (event instanceof MdmModificationChangeEvent) {
      return true;
    } else {
      return false;
    }
  }

}
