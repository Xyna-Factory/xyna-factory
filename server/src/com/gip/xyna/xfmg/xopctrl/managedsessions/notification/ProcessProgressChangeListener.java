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

package com.gip.xyna.xfmg.xopctrl.managedsessions.notification;




public class ProcessProgressChangeListener extends AChangeNotificationListener {

  private final Long orderId;
  private final ANotificationConnection connection;


  public ProcessProgressChangeListener(ANotificationConnection connection, Long orderId) {
    if (orderId == null) {
      throw new IllegalArgumentException("Cannot listen to progress changes on order id 'null'");
    }
    if (connection == null) {
      throw new IllegalArgumentException("Connection may not be null");
    }
    this.orderId = orderId;
    this.connection = connection;
  }


  @Override
  public void onChange(AChangeEvent event) {
    if (event instanceof ProcessProgressChangeEvent) {
      ProcessProgressChangeEvent progressEvent = (ProcessProgressChangeEvent) event;
      if (progressEvent.getID().equals(orderId)) {
        connection.reply(AChangeEvent.CHANGE_EVENT_PROCESS_PROGRESS);
        connection.reply(progressEvent.getID() + "");
      }
    }
  }


  @Override
  public final boolean matches(AChangeEvent event) {
    if (!(event instanceof ProcessProgressChangeEvent)) {
      return false;
    }
    return orderId.equals(((ProcessProgressChangeEvent) event).getID());
  }

}
