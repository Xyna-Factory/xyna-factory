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
package com.gip.xyna.xact.filter;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.MailTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.mail.Mail;
import xact.mail.enums.Keep;
import xact.mail.enums.ReceivedMailTreatment;

public class MailFilter extends ConnectionFilter<MailTriggerConnection> {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(MailFilter.class);

  private MailConfigurationParameter config;

  /**
   * Called to create a configuration template to parse configuration and show configuration options.
   * @return POP3ConfigurationParameter template
   */
  @Override
  public FilterConfigurationParameter createFilterConfigurationTemplate() {
    return new MailConfigurationParameter();
  }

  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  @Override
  public FilterResponse createXynaOrder(MailTriggerConnection tc, FilterConfigurationParameter baseConfig ) throws XynaException {
     this.config = (MailConfigurationParameter)baseConfig;
     Mail mail = MailConverter.convertToXmom(tc.getMail());
     XynaOrder order = new XynaOrder(new DestinationKey(config.getOrderType()), mail);
     return FilterResponse.responsible(order);
  }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  @Override
  public void onResponse(GeneralXynaObject response, MailTriggerConnection tc) {
    Mail reply = null;
    ReceivedMailTreatment treatment = null;
    if( response instanceof Mail) {
      reply = (Mail)response;
    } else if( response instanceof ReceivedMailTreatment ) {
      treatment = (ReceivedMailTreatment)response;
    } else if( response instanceof Container ) {
      Container c = (Container)response;
      for( int i=0; i<c.size(); ++i ) {
        GeneralXynaObject el = c.get(i);
        if( el instanceof Mail && reply == null ) {
          reply = (Mail)el;
        } else if( el instanceof ReceivedMailTreatment && treatment == null ) {
          treatment = (ReceivedMailTreatment)el;
        } 
      }
    }
    onResponse(reply, treatment, tc);
  }

  private void onResponse(Mail reply, ReceivedMailTreatment treatment, MailTriggerConnection tc) {
    boolean delete = true;
    if( treatment instanceof Keep ) {
      delete = false;
    }
    if( delete ) {
      delete(tc);
    }
    if( reply != null ) {
      //optionale Reply kann leer sein, da sie nicht als null zurückgegeben werden kann
      if( reply.getBody() != null || reply.getAttachments() != null ) {
        //wichtige Felder sind gefüllt, daher Antwort erzeugen
        tc.sendReply(MailConverter.convertFromXmom(reply));
      }
    }
  }

  private void delete(MailTriggerConnection tc) {
    try {
      tc.delete();
    } catch (MessagingException e) {
      logger.warn("Failed to delete "+tc.getMessageId(), e); //TODO Retries?
    }
  }

  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, MailTriggerConnection tc) {
    //Exceptions werden ignoriert, sie sind in den Auditdaten zu finden.
    boolean retry = tc.retry(this.config.getRetriesOnError());
    if( ! retry ) {
      delete(tc);
    }
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "MailFilter";
  }

}
