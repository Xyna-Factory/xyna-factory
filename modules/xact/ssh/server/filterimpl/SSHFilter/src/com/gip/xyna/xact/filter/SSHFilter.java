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
package com.gip.xyna.xact.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.SSHConnectionParameter;
import com.gip.xyna.xact.trigger.SSHCustomizationParameter;
import com.gip.xyna.xact.trigger.SSHCustomizationParameter.NewLine;
import com.gip.xyna.xact.trigger.SSHStartParameter.ErrorHandling;
import com.gip.xyna.xact.trigger.SSHTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.connection.Command;
import xact.connection.Response;
import xact.ssh.server.SSHSession;
import xact.ssh.server.SSHSessionCustomization;
import xact.ssh.server.enums.Newline;
import xact.ssh.server.enums.NewlineAuto;
import xact.ssh.server.enums.NewlineCRLF;
import xact.ssh.server.enums.NewlineLF;

public class SSHFilter extends ConnectionFilter<SSHTriggerConnection> {

  private static final long serialVersionUID = 1L;
  
  private static Logger logger = CentralFactoryLogging.getLogger(SSHFilter.class);

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
  public FilterResponse createXynaOrder(SSHTriggerConnection tc) throws XynaException {
    
    DestinationKey dk = null;
    String orderType = tc.getOrderType();
    if( orderType != null ) {
      dk = new DestinationKey(orderType);
    }
    logger.info("RequestType " + tc.getRequestType()+" -> DestinationKey " + dk);
    switch( tc.getRequestType() ) {
    case Init:
      if( dk != null ) {
        XynaOrder order = new XynaOrder(dk, createSSHSession(tc) );
        return FilterResponse.responsible(order);
      } else {
        //TODO MOTD?
        return FilterResponse.responsibleWithoutXynaorder();
      }
    case Exec:
      try {
        Command command = new Command(tc.readLine());
        if( tc.isConnectionClosed() ) {
          //Ende
          if( command.getContent().length() > 0 ) {
            logger.info("SSH-Connection closed, request" + command.getContent() +" maybe inconmplete?");
          } else {
            logger.info("SSH-Connection closed, empty request");
          }
          return FilterResponse.responsibleWithoutXynaorder(); //TODO doch nochmal XynaOrder starten?
        } else {
          logger.info("Request " + command.getContent() );
          if( dk != null ) {
            XynaOrder order = new XynaOrder(dk, createSSHSession(tc), command );
            return FilterResponse.responsible(order);
          } else {
            return FilterResponse.responsibleWithoutXynaorder();
          }
        }
        
      } catch( IOException e ) {
        tc.sendLineQuietly(tc.getCustomization().getErrorPrefix()+"IOException "+ e.getMessage() );
        return FilterResponse.responsibleWithoutXynaorder();
      }
      
    case Close:
      if( dk != null ) {
        XynaOrder order = new XynaOrder(dk, createSSHSession(tc) );
        return FilterResponse.responsible(order);
      } else {
        return FilterResponse.responsibleWithoutXynaorder();
      }
    default:
      tc.sendLineQuietly(tc.getCustomization().getErrorPrefix()+"Unexpected RequestType "+ tc.getRequestType() );
      tc.close();
      return FilterResponse.responsibleWithoutXynaorder();
    }
  }

  private SSHSession createSSHSession(SSHTriggerConnection tc) {
    SSHConnectionParameter cp = tc.getConnectionParameter();
    
    return new SSHSession.Builder().
      localHost(cp.getLocalHost()).
      localPort(cp.getLocalPort()).
      remoteHost(cp.getRemoteHost()).
      remotePort(cp.getRemotePort()).
      user(cp.getUser()).
      uniqueId(cp.getUniqueId()).
      customIdentifier(tc.getCustomization().getIdentifier()).
      instance();
  }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject gxo, SSHTriggerConnection tc) {
    try {
      switch( tc.getRequestType() ) {
      case Init:
        Container c = (Container)gxo;
        tc.customize( customize((SSHSessionCustomization)c.get(1)) );
        if( sendResponse( tc, (Response)c.get(0) ) ) {
          tc.nextRequest();
        }
        break;
      case Exec:
        if( sendResponse( tc, (Response)gxo ) ) {
          tc.nextRequest();
        }
        break;
      case Close:
        return; //nichts mehr zu tun: SSH-Verbindung bereits geschlossen, gxo muss nicht ausgewertet werden
      default:
        tc.sendLine(tc.getCustomization().getErrorPrefix()+"Unexpected RequestType "+ tc.getRequestType() );
        break;
      }
    } catch( Exception e ) {
      //ClassCastException, ArrayIndexOutOfBound, etc
      logger.info( "onResponse for "+tc.getRequestType(), e);
      tc.sendLineQuietly(tc.getCustomization().getErrorPrefix()+"Unexpected Response ");
    }
  }

  private SSHCustomizationParameter customize(SSHSessionCustomization sshSessionCustomization) {
    NewLine newLine = NewLine.None;
    Newline nl = sshSessionCustomization.getNewline();
    if( nl instanceof NewlineAuto ) {
      newLine = NewLine.CRLF; //FIXME oder NewLine.LF: aus SessionParametern PTY ermitteln!
    } else if( nl instanceof NewlineCRLF ) {
      newLine = NewLine.CRLF;
    } else if( nl instanceof NewlineLF ) {
      newLine = NewLine.LF;
    } else {
      newLine = NewLine.None;
    }
    
    
    return SSHCustomizationParameter.build(
        sshSessionCustomization.getIdentifier(), 
        sshSessionCustomization.getErrorPrefix(), 
        newLine );
  }

  private boolean sendResponse(SSHTriggerConnection tc, Response response) {
    String responseString = ((Response)response).getContent();
    if( responseString != null ) {
      tc.sendQuietly(responseString);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param xes
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] xes, SSHTriggerConnection tc) {
    tc.sendLineQuietly( errorsToString(xes, tc.getErrorHandling(), tc.getCustomization().getErrorPrefix() ) );
  }
  
  private String errorsToString(XynaException[] xes, ErrorHandling errorHandling, String errorPrefix) { 
    StringBuilder sb = new StringBuilder();
    sb.append(errorPrefix);
    switch( xes.length ) {
    case 0:
      sb.append( "Workflow failed without exception.");
      return sb.toString();
    case 1:
      sb.append( "Workflow failed");
      break;
    default:
      sb.append( "Workflow failed with ").append( xes.length);
    }
    
    if( errorHandling == ErrorHandling.infoOnly ) {
      sb.append(".");
      return sb.toString();
    }
    
    if( xes.length == 1 ) {
      appendException(sb, xes[0], errorHandling );
    } else {
      for( XynaException xe : xes ) {
        appendException(sb, xe, errorHandling );
      }
    }
    return sb.toString();
  }

  private void appendException(StringBuilder sb, XynaException xe, ErrorHandling errorHandling) {
    sb.append(": ").append(xe.getClass().getSimpleName()).append(":").append(xe.getMessage());
    if( errorHandling == ErrorHandling.stacktrace ) {
      StringWriter sw = new StringWriter();
      xe.printStackTrace(new PrintWriter(sw));
      sb.append(sw.toString());
    }
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "SSHFilter configured via SSHTrigger";
  }

  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @SuppressWarnings("rawtypes")
  public void onDeployment(EventListener triggerInstance) {
    super.onDeployment(triggerInstance);
  }

  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  @SuppressWarnings("rawtypes")
  public void onUndeployment(EventListener triggerInstance) {
    super.onUndeployment(triggerInstance);
  }

}
