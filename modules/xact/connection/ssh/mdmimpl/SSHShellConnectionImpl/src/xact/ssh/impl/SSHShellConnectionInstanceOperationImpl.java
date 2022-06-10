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
package xact.ssh.impl;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import xact.connection.Command;
import xact.connection.ConnectionTypeSpecificExtension;
import xact.connection.DeviceType;
import xact.connection.ReadTimeout;
import xact.connection.Response;
import xact.connection.SendParameter;
import xact.ssh.SSHConnectionParameter;
import xact.ssh.SSHMessagePayload;
import xact.ssh.SSHSendParameter;
import xact.ssh.SSHShellConnection;
import xact.ssh.SSHShellConnectionInstanceOperation;
import xact.ssh.SSHShellConnectionSuperProxy;
import xact.ssh.SSHShellPromptExtractor;
import xact.ssh.SSHShellResponse;
import xact.ssh.SSHSpecificExtension;
import xact.templates.CommandLineInterface;
import xact.templates.DocumentType;
import xfmg.xfmon.protocolmsg.ProtocolMessage;

import com.gip.xyna.xfmg.Constants;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;


public class SSHShellConnectionInstanceOperationImpl extends SSHShellConnectionSuperProxy implements SSHShellConnectionInstanceOperation {

  private static final long serialVersionUID = 1L;

  public SSHShellConnectionInstanceOperationImpl(SSHShellConnection instanceVar) {
    super(instanceVar);
  }
  
  
  private String loginResult;


  protected void initChannelAndStreams(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType, Command cmd) {
    try {
      ChannelShell shellChannel = (ChannelShell) getSession().openChannel("shell");
      shellChannel.setPty(true);
      String terminalType = "gogrid";
      if (instanceVar.getConnectionParameter() instanceof SSHConnectionParameter) {
        SSHConnectionParameter sshConParams = (SSHConnectionParameter)instanceVar.getConnectionParameter();
        if (sshConParams.getTerminalType() != null &&
            !sshConParams.getTerminalType().isEmpty()) {
          terminalType = sshConParams.getTerminalType().trim();
        }
      }
      shellChannel.setPtyType(terminalType, 5000, 5000, 5000, 5000);

      SSHSendParameter sp;
      if (sendParameter == null) {
        sp = new SSHSendParameter.Builder().connectionTimeoutInMilliseconds(5000).readTimeoutInMilliseconds(2000).reconnectAfterRestart(true).throwExceptionOnReadTimeout(true).instance();
      } else {
        sp = (SSHSendParameter) sendParameter;  
      }
      
      setChannelAndStreams(shellChannel);
      shellChannel.connect((int) Math.min(sp.getConnectionTimeoutInMilliseconds(), Integer.MAX_VALUE));

      loginResult =
          readFromInputStream(getInputStream(), shellChannel, documentType, deviceType, sp.getReadTimeoutInMilliseconds(), cmd,
                              getThrowReadTimeoutException(sp.getThrowExceptionOnReadTimeout()));
    } catch (JSchException t) {
      throw new RuntimeException(t);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ReadTimeout e) {
      throw new RuntimeException(e);
    }
  }
  
  
  @Override
  protected Response generateResponse(String response, DeviceType deviceType) {
    String ls = getLineSeperator(deviceType);
    if (ls.length() == 0) {
      //spezialfall: leerer string.
      ls = "[\r\n]+";
    }
    response = response.replaceAll(ls, Constants.LINE_SEPARATOR);
    List<? extends ConnectionTypeSpecificExtension> extensions = deviceType.getConnectionTypeSpecificExtension();
    if (extensions != null) {
      for (ConnectionTypeSpecificExtension conTypeSpecificExtension : extensions) {
        if (conTypeSpecificExtension instanceof SSHShellPromptExtractor) {
          SSHShellPromptExtractor promptExtractor = (SSHShellPromptExtractor) conTypeSpecificExtension;
          return new SSHShellResponse(response, promptExtractor.extractPrompt(response));
        }
      }
    }
    return super.generateResponse(response, deviceType);
  }
  

  @Override
  protected byte[] transformCommandForSend(String commandString, DeviceType deviceType)
                  throws UnsupportedEncodingException {
    String lineSeperator = getLineSeperator(deviceType);
    if (commandString.endsWith(Constants.LINE_SEPARATOR)) {
      commandString = commandString.substring(0, commandString.length() - Constants.LINE_SEPARATOR.length());
    }
    if (!commandString.endsWith(lineSeperator)) {
      commandString += lineSeperator;
    }
    return super.transformCommandForSend(commandString, deviceType);
  }
  
  
  private String getLineSeperator(DeviceType deviceType) {
    String lineSeperator = Constants.LINE_SEPARATOR;
    List<? extends ConnectionTypeSpecificExtension> list = deviceType.getConnectionTypeSpecificExtension();
    if (list != null) {
      for (ConnectionTypeSpecificExtension connectionTypeSpecificExtension : list) {
        if (connectionTypeSpecificExtension instanceof SSHSpecificExtension) {
          lineSeperator = ((SSHSpecificExtension)connectionTypeSpecificExtension).getLineSeperator();
          if (lineSeperator.equals("\\n")) {
            lineSeperator = "\n";
          } else if (lineSeperator.equals("\\r")) {
            lineSeperator = "\r";
          } else if (lineSeperator.equals("\\r\\n")) {
            lineSeperator = "\r\n";
          } else if (lineSeperator == null || lineSeperator.length() == 0) {
            lineSeperator = ""; //dafür verwendet, dass man z.b. ctrl-c drückt. das wird nicht mit enter bestätigt.
          }
        }
      }
    }
    return lineSeperator;
  }
  
  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


  public Response getLoginMessage(DeviceType deviceType) {
    if (!prepared) {
      reconnectIfNecessary();
      initChannelAndStreams(getInstanceVar().getConnectionParameter().getDefaultSendParameter(), new CommandLineInterface(), deviceType, new Command(""));
      prepared = true;
    }
    return new Response(loginResult);
  }


  protected ProtocolMessage createPartialProtocolMessage(String content) {
    ProtocolMessage msg = new ProtocolMessage();
    msg.setPayload(new SSHMessagePayload(content));
    msg.setProtocolAdapterName("SSHShellConnection");
    msg.setProtocolName("SSH");
    return msg;
  }
  
  
}
