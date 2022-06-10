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

package com.gip.xyna.xact.filter.actions;



import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.session.XMOMGuiReply;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;

import xmcp.processmodeller.datatypes.GetRemoteDestinationResponse;
import xmcp.processmodeller.datatypes.RemoteDestination;



public class RemoteDestinationsAction extends H5xFilterAction {

  private static final String BASE_PATH = "/" + PathElements.REMOTEDESTINATIONS;
  private static final RemoteDestinationManagement rdMgmt =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();


  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {

    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    String[] rights = {GuiRight.PROCESS_MODELLER.getKey(), Rights.READ_MDM.toString()};

    if (!checkLoginAndRights(tc, jfai, rights)) {
      return jfai;
    }


    XMOMGuiReply reply = new XMOMGuiReply();
    GetRemoteDestinationResponse remoteDestinationResponse = new GetRemoteDestinationResponse();

    Collection<RemoteDestinationInstanceInformation> allRemoteDestinationInstances = rdMgmt.listRemoteDestinationInstances();
    List<RemoteDestination> remoteDestinationsResultList = new LinkedList<RemoteDestination>();
    for (RemoteDestinationInstanceInformation remoteDestinationInstanceInformation : allRemoteDestinationInstances) {
      RemoteDestination rd = new RemoteDestination();
      rd.setName(remoteDestinationInstanceInformation.getName());
      rd.setDescription(remoteDestinationInstanceInformation.getDescription());
      remoteDestinationsResultList.add(rd);
    }
    remoteDestinationResponse.setRemoteDestinations(remoteDestinationsResultList);

    reply.setXynaObject(remoteDestinationResponse);
    jfai.sendJson(tc, HTTPTriggerConnection.HTTP_OK, reply.getJson());
    return jfai;
  }


  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH + "/h5.data/Simple/open", "h5.data.Simple");
  }


  @Override
  public String getTitle() {
    return "RemoteDestinations";
  }


  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }


  @Override
  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) && url.getPathLength() == 1 && method == Method.GET;
  }

}
