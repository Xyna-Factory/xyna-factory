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
package com.gip.xyna.xprc;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.Redirection.Answers;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement.ManualInteractionResponse;


public class MIAbstractionLayer extends JavaDestination implements WorkflowAbstractionLayer<RedirectionBean, RedirectionAnswer> {


  private final static long serialVersionUID = -175952363685899859L;
  public final static String ORDERTYPE = "ManualInteractionRedirection";

  public MIAbstractionLayer() {
    super(ORDERTYPE);
  }


  public GeneralXynaObject createInputData(RedirectionBean in) {
    return in;
  }


  public RedirectionAnswer createOutputData(GeneralXynaObject obj) {
    //FIXME das ist sehr unschön. siehe bugz 8866
    if (obj.getClass().getName().contains("Retry")) {
      return new RedirectionAnswer(Answers.RETRY);
    } else if (obj.getClass().getName().contains("Continue")) {
      return new RedirectionAnswer(Answers.IGNORE);
    } else {
      return new RedirectionAnswer(Answers.CANCEL);
    }
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RedirectionBean in = (RedirectionBean)(input);
    XynaOrderServerExtension redirectedOrder = in.getRedirectedOrder();
    XynaOrderServerExtension redirectionOrder = in.getRedirectionOrder();

    String reason = in.getReason();
    String type = "Automatic ManualInteraction";
    String userGroup = null;
    String todo = "Continue => no retry of hanging step, Retry => retry of hanging step";
    GeneralXynaObject payload = null;
        
    return XynaFactory.getInstance().getXynaMultiChannelPortal().waitForMI(redirectionOrder, reason, type, userGroup, todo, payload);    
  }


  public XynaOrderServerExtension createOrder(RedirectionBean in, XynaOrderServerExtension redirectedOrder) {
    List<ManualInteractionResponse> miResponse = new ArrayList<ManualInteractionResponse>();
    miResponse.add(ManualInteractionResponse.CONTINUE);
    miResponse.add(ManualInteractionResponse.RETRY);
    miResponse.add(ManualInteractionResponse.ABORT);
    RedirectionXynaOrder xo = new RedirectionXynaOrder( XynaDispatcher.DESTINATION_KEY_REDIRECTION, createInputData(in), redirectedOrder, miResponse);
    in.setRedirectionOrder(xo);
    return xo;
  }
  

}
