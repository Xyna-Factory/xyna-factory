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
package com.gip.xyna.xact.trigger;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.SSHTriggerConnection.RequestType;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;

import xact.ssh.server.SSHServerParameter;

public class SSHStartParameter extends EnhancedStartParameter implements SSHServerParameter {

  
  public enum ErrorHandling {
    infoOnly, exception, stacktrace;
  }
  
  public static final StringParameter<Integer> PORT = 
      StringParameter.typeInteger("port").
      documentation( Documentation.de("Port").en("Port").build() ).
      defaultValue(22).build();
  public static final StringParameter<String> ADDRESS = 
      StringParameter.typeString("address").
      documentation( Documentation.
                     de("Name der IP im NetworkConfigurationManagement oder Network-Interface-name (Default:= akzeptiert Connections von allen Interfaces)").
                     en("Name of ip in NetworkConfigurationManagement or network interface name (Default=accept connections from all interfaces)").build() ).
      build();
  public static final StringParameter<String> HOST_KEY = 
      StringParameter.typeString("hostKey").
      documentation( Documentation.
                     de("Name der Datei, in der der SSH-HostKey serialisiert gespeichert wird").
                     en("Name of file in which serialized ssh host key will be stored").build() ).
      defaultValue("hostKey.ser").
      build();

 
  public static final StringParameter<Auth> AUTH = 
      StringParameter.typeEnum(Auth.class, "auth").
      documentation( Documentation.de("Authentication").en("Authentication").build() ).
      defaultValue(Auth.needless).
      build();
  public static final StringParameter<ErrorHandling> ERROR_HANDLING = 
      StringParameter.typeEnum(ErrorHandling.class, "errorHandling").
      documentation( Documentation.de("Fehlerbehandlung").en("Error handling").build() ).
      defaultValue(ErrorHandling.exception).
      build();
  
  public static final StringParameter<Duration> IDLE_TIMEOUT = 
      StringParameter.typeDuration("idle_timeout").
      documentation( Documentation.
                     de("Idle-Timeout: nach dem Timeout wird die Verbindung vom Server getrennt").
                     en("Idle timeout: after timeout the connection will be disconnected by server").build() ).
      defaultValue( Duration.valueOf("10 min")).
      build();

  public static final StringParameter<String> ORDERTYPE_INIT = 
      StringParameter.typeString("ordertype_init").
      documentation( Documentation.
                     de("Name des OrderTypes, der bei Verbindungsaufbau gestartet wird. Signatur: (xact.ssh.server.SSHSession) -> (xact.connection.Response, xact.ssh.server.SSHSessionCustomization)").
                     en("Name of ordertype started on connection initialisation. signature (xact.ssh.server.SSHSession) -> (xact.connection.Response, xact.ssh.server.SSHSessionCustomization)").build() ).
      build();

  public static final StringParameter<String> ORDERTYPE_EXEC = 
      StringParameter.typeString("ordertype_exec").
      documentation( Documentation.
                     de("Name des OrderTypes, der bei jedem Command gestartet wird. Signatur: (xact.ssh.server.SSHSession, xact.connection.Command) -> (xact.connection.Response)").
                     en("Name of ordertype started on connection initialisation. signature (xact.ssh.server.SSHSession, xact.connection.Command) -> (xact.connection.Response)").build() ).
      build();

  public static final StringParameter<String> ORDERTYPE_CLOSE = 
      StringParameter.typeString("ordertype_close").
      documentation( Documentation.
                     de("Name des OrderTypes, der nach Verbindungsabbau gestartet wird. Signatur: (xact.ssh.server.SSHSession) -> ()").
                     en("Name of ordertype started after connection finalisation. signature (xact.ssh.server.SSHSession) -> ()").build() ).
      build();

 
  
  public static final List<StringParameter<?>> ALL_PARAMS = Arrays.<StringParameter<?>>asList(
      PORT,ADDRESS,AUTH,HOST_KEY,
      ERROR_HANDLING, IDLE_TIMEOUT,
      ORDERTYPE_INIT,ORDERTYPE_EXEC,ORDERTYPE_CLOSE);

  private int port;
  private String address;
  private Map<RequestType, String> orderTypes;
  private Auth auth;
  private ErrorHandling errorHandling;
  private String hostKeyFileName;
  private String privateKeyFile;
  private Duration idleTimeout;
  
  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public SSHStartParameter() {
  }

  @Override
  public StartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    SSHStartParameter param = new SSHStartParameter();
    param.port = PORT.getFromMap(paramMap);
    param.address = ADDRESS.getFromMap(paramMap);
    param.auth = AUTH.getFromMap(paramMap);
    param.errorHandling = ERROR_HANDLING.getFromMap(paramMap);
    param.orderTypes = new EnumMap<RequestType,String>(RequestType.class);
    param.orderTypes.put( RequestType.Init, ORDERTYPE_INIT.getFromMap(paramMap) );
    param.orderTypes.put( RequestType.Exec, ORDERTYPE_EXEC.getFromMap(paramMap) );
    param.orderTypes.put( RequestType.Close, ORDERTYPE_CLOSE.getFromMap(paramMap) );
    param.hostKeyFileName = HOST_KEY.getFromMap(paramMap);
    param.idleTimeout = IDLE_TIMEOUT.getFromMap(paramMap);
    return param;
  }

  @Override
  public List<String> convertToNewParameters(List<String> list)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    //gibt keine alten Parameter
    return list;
  }

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMS;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getHost() {
    return address;
  }

  @Override
  public Auth getAuth() {
    return auth;
  }

  @Override
  public String getPrivateKey() {
    return "";
  }

  @Override
  public String getPrivateKeyFile() {
    return "";
  }

  @Override
  public String getPublicKey() {
    return "";
  }

  @Override
  public String getKnownHostFile() {
    return "";
  }


  @Override
  public String getPassPhrase() {
    return "";
  }
  
  /*
  public boolean isUsePublickey() {
    return auth == Auth.publickey || auth == Auth.both;
  }

  public boolean isUsePassword() {
    return auth == Auth.password || auth == Auth.both;
  }

  public boolean isAuthNeedless() {
    return auth == Auth.needless;
  }*/
  
  public ErrorHandling getErrorHandling() {
    return errorHandling;
  }

  public Map<RequestType, String> getOrderTypes() {
    return orderTypes;
  }

  @Override
  public String getHostKeyFilename() {
    return hostKeyFileName;
  }

  @Override
  public String getAlgorithm() {
    return "RSA"; //FIXME
  }
  
  public Duration getIdleTimeout() {
    return idleTimeout;
  }
  
}
