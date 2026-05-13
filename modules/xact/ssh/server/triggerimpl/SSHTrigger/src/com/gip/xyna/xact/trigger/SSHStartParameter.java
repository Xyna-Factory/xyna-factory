/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.SSHShellTriggerConnection.RequestType;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;

import xact.ssh.server.SSHServerParameter;
import xact.ssh.sftp.SFTPSubsystemParameter;

import org.apache.log4j.Logger;
import org.apache.sshd.common.cipher.CipherFactory;
import org.apache.sshd.common.mac.MacFactory;
import org.apache.sshd.common.kex.DHFactory;
import org.apache.sshd.common.signature.SignatureFactory;

import org.apache.sshd.common.cipher.BuiltinCiphers;
import org.apache.sshd.common.mac.BuiltinMacs;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.signature.BuiltinSignatures;

public class SSHStartParameter extends EnhancedStartParameter implements SSHServerParameter, SFTPSubsystemParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(SSHStartParameter.class);

  public enum ErrorHandling {
    infoOnly, exception, stacktrace;
  }

  public static final StringParameter<Integer> PORT = StringParameter.typeInteger("port")
      .documentation(Documentation.de("Port").en("Port").build()).defaultValue(22).build();
  public static final StringParameter<String> ADDRESS = StringParameter.typeString("address")
      .documentation(Documentation.de(
          "Name der IP im NetworkConfigurationManagement oder Network-Interface-name (Default:= akzeptiert Connections von allen Interfaces)")
          .en("Name of ip in NetworkConfigurationManagement or network interface name (Default=accept connections from all interfaces)")
          .build())
      .build();
  public static final StringParameter<String> HOST_KEY = StringParameter.typeString("hostKey")
      .documentation(Documentation.de("Name der Datei, in der der SSH-HostKey serialisiert gespeichert wird")
          .en("Name of file in which serialized ssh host key will be stored").build())
      .defaultValue("hostKey.ser").build();

  public static final StringParameter<Auth> AUTH = StringParameter.typeEnum(Auth.class, "auth")
      .documentation(Documentation.de("Authentication").en("Authentication").build()).defaultValue(Auth.needless)
      .build();
  public static final StringParameter<ErrorHandling> ERROR_HANDLING = StringParameter
      .typeEnum(ErrorHandling.class, "errorHandling")
      .documentation(Documentation.de("Fehlerbehandlung").en("Error handling").build())
      .defaultValue(ErrorHandling.exception).build();

  public static final StringParameter<Duration> IDLE_TIMEOUT = StringParameter.typeDuration("idle_timeout")
      .documentation(Documentation.de("Idle-Timeout: nach dem Timeout wird die Verbindung vom Server getrennt")
          .en("Idle timeout: after timeout the connection will be disconnected by server").build())
      .defaultValue(Duration.valueOf("10 min")).build();

  public static final StringParameter<String> ORDERTYPE_INIT = StringParameter.typeString("ordertype_init")
      .documentation(Documentation.de(
          "Name des OrderTypes, der bei Verbindungsaufbau gestartet wird. Signatur: (xact.ssh.server.SSHSession) -> (xact.connection.Response, xact.ssh.server.SSHSessionCustomization)")
          .en("Name of ordertype started on connection initialisation. signature (xact.ssh.server.SSHSession) -> (xact.connection.Response, xact.ssh.server.SSHSessionCustomization)")
          .build())
      .build();

  public static final StringParameter<String> ORDERTYPE_EXEC = StringParameter.typeString("ordertype_exec")
      .documentation(Documentation.de(
          "Name des OrderTypes, der bei jedem Command gestartet wird. Signatur: (xact.ssh.server.SSHSession, xact.connection.Command) -> (xact.connection.Response)")
          .en("Name of ordertype started on connection initialisation. signature (xact.ssh.server.SSHSession, xact.connection.Command) -> (xact.connection.Response)")
          .build())
      .build();

  public static final StringParameter<String> ORDERTYPE_CLOSE = StringParameter.typeString("ordertype_close")
      .documentation(Documentation.de(
          "Name des OrderTypes, der nach Verbindungsabbau gestartet wird. Signatur: (xact.ssh.server.SSHSession) -> ()")
          .en("Name of ordertype started after connection finalisation. signature (xact.ssh.server.SSHSession) -> ()")
          .build())
      .build();

  public static final StringParameter<Boolean> SHELL = StringParameter.typeBoolean("shell")
      .documentation(Documentation.de("Erlaube Shell").en("Enable Shell").build())
      .defaultValue(true)
      .build();

  public static final StringParameter<Boolean> OTC = StringParameter.typeBoolean("otc")
      .documentation(Documentation.de("Benutze einmal Passwörter").en("One Time Credentials").build())
      .defaultValue(false)
      .build();

  public static final StringParameter<Boolean> SCP = StringParameter.typeBoolean("scp")
      .documentation(Documentation.de("Erlaube SCP").en("Enable SCP").build())
      .defaultValue(false)
      .build();

  public static final StringParameter<Boolean> SFTP = StringParameter.typeBoolean("sftp")
      .documentation(Documentation.de("Erlaube SFTP").en("Enable SFTP").build())
      .defaultValue(false)
      .build();

  public static final StringParameter<Boolean> NATIVE_FILE_ACCESS = StringParameter.typeBoolean("fileAccess")
      .documentation(Documentation.de("Erlaube Zugriff auf das Dateisystem").en("Enable filesystem access").build())
      .defaultValue(false)
      .build();

  public static final StringParameter<String> NATIVE_FILE_ROOT = StringParameter.typeString("fileRoot")
      .documentation(
          Documentation.de("Basis Verzeichnis für Dateizugriff").en("Root directory for filesystem access").build())
      .optional()
      .build();

  public static final StringParameter<String> NATIVE_FILE_ROOT_PROP = StringParameter.typeString("fileRootProperty")
      .documentation(
          Documentation.de("Property für Basis Verzeichnis für Dateizugriff. Falls fileRoot nicht gesetzt.")
              .en("Property for root directory for filesystem access. Used if fileRoot not set.").build())
      .defaultValue("xact.sftp.localSftpRoot")
      .build();

  public static final StringParameter<String> NATIVE_FILE_PREFIX = StringParameter.typeString("filePrefix")
      .documentation(Documentation.de("Präfix für Zugriff auf Dateisystem").en("Prefix for filesystem access").build())
      .defaultValue("/StaticXfc/")
      .build();

  public static final StringParameter<Duration> SFTP_TIMEOUT = StringParameter.typeDuration("requestTimeout")
      .documentation(Documentation.de("Die maximale Wartezeit einer SFTP- oder SCP-Anfrage")
          .en("Timeout for SFTP or SCP requests").build())
      .defaultValue(Duration.valueOf("15 min", null)).build();

  private static final List<org.apache.sshd.common.kex.BuiltinDHFactories> defaultKEX = org.apache.sshd.common.BaseBuilder.DEFAULT_KEX_PREFERENCE;
  public static final StringParameter<List<String>> KEX = StringParameter
      .typeList(String.class, "kex", com.gip.xyna.utils.misc.StringParameter.ListSeparator.COMMA_WHITESPACE)
      .documentation(Documentation
          .de("KEX Algorithmen, Werte:\n         ("
              + String.join(",",
                  BuiltinDHFactories.VALUES.stream().map(BuiltinDHFactories::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .en("KEX algorithms, values:\n         ("
              + String.join(",",
                  BuiltinDHFactories.VALUES.stream().map(BuiltinDHFactories::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .build())
      .defaultValue(defaultKEX.stream().map(BuiltinDHFactories::getName).collect(Collectors.toList()))
      .build();

  private static final List<org.apache.sshd.common.mac.BuiltinMacs> defaultMAC = org.apache.sshd.common.BaseBuilder.DEFAULT_MAC_PREFERENCE;

  public static final StringParameter<List<String>> MAC = StringParameter
      .typeList(String.class, "mac", com.gip.xyna.utils.misc.StringParameter.ListSeparator.COMMA_WHITESPACE)
      .documentation(Documentation
          .de("MAC Algorithmen, Werte:\n         ("
              + String.join(",",
                  BuiltinMacs.VALUES.stream().map(BuiltinMacs::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .en("MAC algorithms, values:\n         ("
              + String.join(",",
                  BuiltinMacs.VALUES.stream().map(BuiltinMacs::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .build())
      .defaultValue(defaultMAC.stream().map(BuiltinMacs::getName).collect(Collectors.toList()))
      .build();

  private static final List<org.apache.sshd.common.signature.BuiltinSignatures> defaultAuth = org.apache.sshd.common.BaseBuilder.DEFAULT_SIGNATURE_PREFERENCE;
  public static final StringParameter<List<String>> AUTH_ALGO = StringParameter
      .typeList(String.class, "authAlgo", com.gip.xyna.utils.misc.StringParameter.ListSeparator.COMMA_WHITESPACE)
      .documentation(Documentation
          .de("Authentifizierungs Algorithmen, Werte:\n         ("
              + String.join(",",
                  BuiltinSignatures.VALUES.stream().map(BuiltinSignatures::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .en("Authentification algorithms, values:\n         (" + String.join(",",
              BuiltinSignatures.VALUES.stream().map(BuiltinSignatures::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .build())
      .defaultValue(defaultAuth.stream().map(BuiltinSignatures::getName).collect(Collectors.toList()))
      .build();

  private static final List<org.apache.sshd.common.cipher.BuiltinCiphers> defaultCipher = org.apache.sshd.common.BaseBuilder.DEFAULT_CIPHERS_PREFERENCE;

  public static final StringParameter<List<String>> CIPHER = StringParameter
      .typeList(String.class, "cipher",
          com.gip.xyna.utils.misc.StringParameter.ListSeparator.COMMA_WHITESPACE)
      .documentation(Documentation
          .de("Verschlüsselungs Algorithmen, Werte:\n         (" + String.join(",",
              BuiltinCiphers.VALUES.stream().map(BuiltinCiphers::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .en("Cipher algorithms, values:\n         (" + String.join(",",
              BuiltinCiphers.VALUES.stream().map(BuiltinCiphers::getName).collect(Collectors.toSet()))
              + ")\n         ")
          .build())
      .defaultValue(defaultCipher.stream().map(BuiltinCiphers::getName).collect(Collectors.toList()))
      .build();

  public static final List<StringParameter<?>> ALL_PARAMS = Arrays.<StringParameter<?>>asList(
      PORT, ADDRESS, AUTH, HOST_KEY,
      ERROR_HANDLING, IDLE_TIMEOUT,
      SHELL,
      ORDERTYPE_INIT, ORDERTYPE_EXEC, ORDERTYPE_CLOSE,
      OTC, SCP, SFTP, NATIVE_FILE_ACCESS, NATIVE_FILE_PREFIX, NATIVE_FILE_ROOT,
      NATIVE_FILE_ROOT_PROP, AUTH_ALGO, CIPHER, KEX, MAC, SFTP_TIMEOUT);

  private int port = PORT.getDefaultValue();
  private String address;
  private Map<RequestType, String> orderTypes;
  private ErrorHandling errorHandling = ERROR_HANDLING.getDefaultValue();
  private Duration idleTimeout = IDLE_TIMEOUT.getDefaultValue();

  private boolean useOTC = OTC.getDefaultValue();

  private String hostKeyFileName = HOST_KEY.getDefaultValue();

  private boolean enableShell = SHELL.getDefaultValue();

  private boolean enableSCP = SCP.getDefaultValue();

  private boolean enableSFTP = SFTP.getDefaultValue();

  private boolean fileAccess = NATIVE_FILE_ACCESS.getDefaultValue();

  private String fileRoot = NATIVE_FILE_ROOT.getDefaultValue();

  private String filePrefix = NATIVE_FILE_PREFIX.getDefaultValue();

  private Duration sftp_timeout = SFTP_TIMEOUT.getDefaultValue();

  private List<BuiltinSignatures> authAlgoFactories = defaultAuth;

  private List<BuiltinDHFactories> kexFactories = defaultKEX;

  private List<BuiltinMacs> macFactories = defaultMAC;

  private List<BuiltinCiphers> cipherFactories = defaultCipher;

  private boolean passwordauth = AUTH.getDefaultValue() != null
      ? AUTH.getDefaultValue().equals(Auth.both) || AUTH.getDefaultValue().equals(Auth.password)
      : false;
  private boolean publickeyauth = AUTH.getDefaultValue() != null
      ? AUTH.getDefaultValue().equals(Auth.both) || AUTH.getDefaultValue().equals(Auth.publickey)
      : false;;

  private boolean alwaysauth = AUTH.getDefaultValue() != null
      ? AUTH.getDefaultValue().equals(Auth.needless)
      : false;

  // the empty constructor may not be removed or throw exceptions! additional ones
  // are possible, though.
  public SSHStartParameter() {
  }

  @Override
  public StartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    SSHStartParameter param = new SSHStartParameter();
    param.port = PORT.getFromMap(paramMap);
    param.address = ADDRESS.getFromMap(paramMap);
    param.errorHandling = ERROR_HANDLING.getFromMap(paramMap);
    param.orderTypes = new EnumMap<RequestType, String>(RequestType.class);
    param.orderTypes.put(RequestType.Init, ORDERTYPE_INIT.getFromMap(paramMap));
    param.orderTypes.put(RequestType.Exec, ORDERTYPE_EXEC.getFromMap(paramMap));
    param.orderTypes.put(RequestType.Close, ORDERTYPE_CLOSE.getFromMap(paramMap));
    param.hostKeyFileName = HOST_KEY.getFromMap(paramMap);
    param.idleTimeout = IDLE_TIMEOUT.getFromMap(paramMap);

    Auth auth = AUTH.getFromMap(paramMap);

    switch (auth) {
      case password:
        param.passwordauth = true;
        param.publickeyauth = false;
        param.alwaysauth = false;
        break;
      case publickey:
        param.publickeyauth = true;
        param.passwordauth = false;
        param.alwaysauth = false;
        break;
      case both:
        param.passwordauth = true;
        param.publickeyauth = true;
        param.alwaysauth = false;
        break;
      case needless:
      default:
        param.passwordauth = false;
        param.publickeyauth = false;
        param.alwaysauth = true;
        break;
    }

    param.useOTC = OTC.getFromMap(paramMap);
    param.enableShell = SHELL.getFromMap(paramMap);
    param.enableSCP = SCP.getFromMap(paramMap);
    param.enableSFTP = SFTP.getFromMap(paramMap);

    param.fileAccess = NATIVE_FILE_ACCESS.getFromMap(paramMap);
    param.fileRoot = NATIVE_FILE_ROOT.getFromMap(paramMap);
    if (param.fileRoot == null) {
      var prop = NATIVE_FILE_ROOT_PROP.getFromMap(paramMap);
      if (prop != null)
        param.fileRoot = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getProperty(prop);
    }
    param.filePrefix = NATIVE_FILE_PREFIX.getFromMap(paramMap);

    param.sftp_timeout = SFTP_TIMEOUT.getFromMap(paramMap);

    var kex = KEX.getFromMap(paramMap);
    if (kex != null) {
      param.kexFactories = kex.stream().map(BuiltinDHFactories::fromFactoryName).collect(Collectors.toList());
      if (logger.isDebugEnabled()) {
        logger.debug("KEXFactories: " + kexFactories);
      }
    }

    var mac = MAC.getFromMap(paramMap);
    if (mac != null) {
      param.macFactories = mac.stream().map(BuiltinMacs::fromFactoryName).collect(Collectors.toList());
      if (logger.isDebugEnabled()) {
        logger.debug("MACFactories: " + macFactories);
      }
    }

    var authAlgo = AUTH_ALGO.getFromMap(paramMap);
    if (authAlgo != null) {
      param.authAlgoFactories = authAlgo.stream().map(BuiltinSignatures::fromFactoryName).collect(Collectors.toList());
      if (logger.isDebugEnabled()) {
        logger.debug("AuthFactories: " + authAlgoFactories);
      }
    }

    var cipher = CIPHER.getFromMap(paramMap);
    if (cipher != null) {
      param.cipherFactories = cipher.stream().map(BuiltinCiphers::fromFactoryName).collect(Collectors.toList());
      if (logger.isDebugEnabled()) {
        logger.debug("CipherFactories: " + cipherFactories);
      }
    }

    if (!(param.enableShell || param.enableSCP || param.enableSFTP)) {
      throw new IllegalStateException("At least one of Shell, SCP or SFTP needs to be enabled.");
    }

    if (param.fileAccess) {
      if (param.fileRoot == null || param.fileRoot.isEmpty() || param.fileRoot.isBlank()) {
        throw new IllegalStateException("fileRoot or fileRootProp needs to be specified when fileAccess is used.");
      }

      File root = new File(param.fileRoot);
      if (!root.exists()) {
        throw new IllegalArgumentException("fileRoot '" + param.fileRoot + "' needs to exist.");
      }
      if (!root.isDirectory()) {
        throw new IllegalArgumentException("fileRoot '" + param.fileRoot + "' needs to be a directory.");
      }
      if (!root.canRead()) {
        throw new IllegalArgumentException("fileRoot '" + param.fileRoot + "' needs to be readable.");
      }
      if (!root.canExecute()) {
        throw new IllegalArgumentException("fileRoot '" + param.fileRoot + "' needs to be executable.");
      }
    }

    return param;
  }

  @Override
  public List<String> convertToNewParameters(List<String> list)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    // gibt keine alten Parameter
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
  public String getHostkeyAlgorithm() {
    return "RSA"; // FIXME
  }

  @Override
  public int getHostkeySize() {
    return 4096; // FIXME
  }

  public Duration getIdleTimeout() {
    return idleTimeout;
  }

  public Duration getSftpTimeout() {
    return sftp_timeout;
  }

  public List<BuiltinSignatures> getAuthAlgoFactories() {
    return authAlgoFactories;
  }

  public List<BuiltinDHFactories> getKexFactories() {
    return kexFactories;
  }

  public List<BuiltinMacs> getMacFactories() {
    return macFactories;
  }

  public List<BuiltinCiphers> getCipherFactories() {
    return cipherFactories;
  }

  public boolean isEnableShell() {
    return enableShell;
  }

  public boolean isEnableSCP() {
    return enableSCP;
  }

  public boolean isEnableSFTP() {
    return enableSFTP;
  }

  public boolean isFileAccess() {
    return fileAccess;
  }

  public String getFileRoot() {
    return fileRoot;
  }

  public String getFilePrefix() {
    return filePrefix;
  }

  public boolean getPasswordAuth() {
    return passwordauth;
  }

  public boolean getPublicKeyAuth() {
    return publickeyauth;
  }

  public boolean getAlwaysAuth() {
    return alwaysauth;
  }

  public boolean getOTCAuth() {
    return useOTC;
  }

}
