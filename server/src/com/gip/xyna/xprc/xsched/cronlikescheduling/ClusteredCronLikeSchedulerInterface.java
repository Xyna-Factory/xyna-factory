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
package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Calendar;

import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;



public interface ClusteredCronLikeSchedulerInterface extends Remote {

  public void removeCronLikeOrderRemotely(Long id) throws RemoteException;


  @Deprecated
  public void modifyCronLikeOrderRemotely(Long id, String label, String ordertype, String payloadXML, Long revision,
                                          Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws RemoteException, XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException;


  public void modifyCronLikeOrderRemotely(Long id, String label, DestinationKey destination, String payloadXML, Long revision,
                                          Long firstStartupTime, String timeZoneID, Long interval, String calendarDefinition, Boolean useDST,
                                          Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                          String cloCustom2, String cloCustom3) throws RemoteException,
      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException;


  public void modifyCronLikeOrderRemotely(Long id, String label, DestinationKey destination, String payloadXML, Long revision,
                                          Calendar firstStartupTimeWithTimeZone, Long interval, String calendarDefinition, Boolean useDST,
                                          Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                          String cloCustom2, String cloCustom3) throws RemoteException,
      XPRC_CronLikeOrderStorageException, XPRC_InvalidCronLikeOrderParametersException;


}
