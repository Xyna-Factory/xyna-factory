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

package com.gip.www.juno.WS.CheckStatus;


import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;


import com.gip.juno.ws.enums.InstanceType;
import com.gip.juno.ws.exceptions.DPPWebserviceUnexpectedException;
import com.gip.juno.ws.handler.AuthenticationTools;
import com.gip.juno.ws.handler.StatusTools;
import com.gip.juno.ws.handler.StatusTools.InstanceInfo;
import com.gip.www.juno.Gui.WS.Messages.*;

public class CheckStatusBindingReal{

  private static Logger _logger = Logger.getLogger("CheckStatus");

  /**
   * web service operation
   */
  public StatusElement_ctype[] checkStatusForIp(CheckStatusForIpRequest_ctype checkStatusForIpRequest)
    throws java.rmi.RemoteException {
      //AuthenticationTools.authenticate(checkStatusForIpRequest.getInputHeader().getUsername(), checkStatusForIpRequest.getInputHeader().getPassword(), _logger);
      //AuthenticationTools.checkPermissions(checkStatusForIpRequest.getInputHeader().getUsername(), "checkstatus", "*", _logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(null);
    AuthenticationTools.authenticateAndAuthorize(checkStatusForIpRequest.getInputHeader().getUsername(),
                                                 checkStatusForIpRequest.getInputHeader().getPassword(),
                                                 "checkstatus", wsInvocationId, _logger);
      try {
        CheckStatusForIpInput_ctype input = checkStatusForIpRequest.getCheckStatusForIpInput();
        InstanceType instanceType = InstanceType.Dpp;
        if (input.getInstanceType().trim().equals("Dns")) {
          instanceType = InstanceType.Dns;
        }
        List<StatusTools.StatusElement> list = StatusTools.checkStatusForIp(input.getIp(), instanceType);
        return adaptStatusList(list, input.getInstanceType());
      } catch (RemoteException e) {
        _logger.error("Error in CheckStatus", e);
        throw e;
      } catch (Throwable e) {
        _logger.error(e);
        throw new DPPWebserviceUnexpectedException("Error in CheckStatus", e);
      }
  }


  /**
   * web service operation
   */
  public StatusElement_ctype[] checkStatusForIpv6(CheckStatusForIpRequest_ctype checkStatusForIpRequest)
    throws java.rmi.RemoteException {
      //AuthenticationTools.authenticate(checkStatusForIpRequest.getInputHeader().getUsername(), checkStatusForIpRequest.getInputHeader().getPassword(), _logger);
      //AuthenticationTools.checkPermissions(checkStatusForIpRequest.getInputHeader().getUsername(), "checkstatus", "*", _logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(null);
    AuthenticationTools.authenticateAndAuthorize(checkStatusForIpRequest.getInputHeader().getUsername(),
                                                 checkStatusForIpRequest.getInputHeader().getPassword(),
                                                 "checkstatus", wsInvocationId, _logger);
      try {
        CheckStatusForIpInput_ctype input = checkStatusForIpRequest.getCheckStatusForIpInput();
        InstanceType instanceType = InstanceType.Dpp;
        if (input.getInstanceType().trim().equals("Dns")) {
          instanceType = InstanceType.Dns;
        }
        List<StatusTools.StatusElement> list = StatusTools.checkStatusForIpv6(input.getIp(), instanceType);
        return adaptStatusList(list, input.getInstanceType());
      } catch (RemoteException e) {
        _logger.error("Error in CheckStatus", e);
        throw e;
      } catch (Throwable e) {
        _logger.error(e);
        throw new DPPWebserviceUnexpectedException("Error in CheckStatus", e);
      }
  }


  /**
   * web service operation
   */
  public InstanceInfo_ctype[] getInstanceInfoList(GetInstanceInfoListRequest_ctype getInstanceInfoListRequest)
        throws java.rmi.RemoteException {
    //AuthenticationTools.authenticate(getInstanceInfoListRequest.getInputHeader().getUsername(), getInstanceInfoListRequest.getInputHeader().getPassword(), _logger);
    //AuthenticationTools.checkPermissions(getInstanceInfoListRequest.getInputHeader().getUsername(), "checkstatus", "*", _logger);
    AuthenticationTools.WebServiceInvocationIdentifier wsInvocationId = new AuthenticationTools.WebServiceInvocationIdentifier(null);
    AuthenticationTools.authenticateAndAuthorize(getInstanceInfoListRequest.getInputHeader().getUsername(),
                                                 getInstanceInfoListRequest.getInputHeader().getPassword(),
                                                 "checkstatus", wsInvocationId, _logger);
    try {
      List<InstanceInfo> ret = StatusTools.getInstanceInfoList();
      return adaptInstanceList(ret);
    } catch (RemoteException e) {
      _logger.error("Error in CheckStatus", e);
      throw e;
    } catch (Throwable e) {
      _logger.error(e);
      throw new DPPWebserviceUnexpectedException("Error in CheckStatus", e);
    }
  }


  private StatusElement_ctype[] adaptStatusList(List<StatusTools.StatusElement> list, String type) {
    StatusElement_ctype[] ret = new StatusElement_ctype[list.size()];
    for (int i=0; i< list.size(); i++) {
      StatusElement_ctype retel = new StatusElement_ctype();
      StatusTools.StatusElement listel = list.get(i);
      retel.setIP(listel.ip);
      retel.setService(listel.service);
      retel.setStatus(listel.status);
      retel.setException(listel.exception);
      retel.setInstanceType(type);
      ret[i] = retel;
    }
    return ret;
  }


  private InstanceInfo_ctype[] adaptInstanceList(List<StatusTools.InstanceInfo> list) {
    InstanceInfo_ctype[] ret = new InstanceInfo_ctype[list.size()];
    for (int i=0; i< list.size(); i++) {
      InstanceInfo_ctype retel = new InstanceInfo_ctype();
      StatusTools.InstanceInfo listel = list.get(i);
      retel.setIP(listel.ip);
      InstanceType_type type = InstanceType_type.Dpp;
      if (listel.type == InstanceType.Dns) {
        //type = InstanceType_type.Dns;
        retel.setInstanceType("Dns");
      } else {
        retel.setInstanceType(listel.location);
      }
      ret[i] = retel;
    }
    return ret;
  }

}
