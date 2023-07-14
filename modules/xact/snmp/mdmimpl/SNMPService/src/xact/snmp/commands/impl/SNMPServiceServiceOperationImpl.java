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
package xact.snmp.commands.impl;



import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.snmp4j.security.UsmTimeTable;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.SnmpAccessData.SADBuilder;
import com.gip.xyna.utils.snmp.exception.SnmpManagerException;
import com.gip.xyna.utils.snmp.exception.SnmpResponseException;
import com.gip.xyna.utils.snmp.manager.SnmpContext;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;
import com.gip.xyna.utils.snmp.varbind.ByteArrayVarBind;
import com.gip.xyna.utils.snmp.varbind.Counter32VarBind;
import com.gip.xyna.utils.snmp.varbind.Counter64VarBind;
import com.gip.xyna.utils.snmp.varbind.Gauge32VarBind;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.IpAddressVarBind;
import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.OIDVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.TimeTicksVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;

import xact.snmp.OID;
import xact.snmp.OIDs;
import xact.snmp.RetryModel;
import xact.snmp.SNMPConnectionData;
import xact.snmp.SNMPConnectionDataV2c;
import xact.snmp.SNMPConnectionDataV3;
import xact.snmp.SimpleRetryModel;
import xact.snmp.VarBinding;
import xact.snmp.VarBindings;
import xact.snmp.VariableContent;
import xact.snmp.commands.EngineIdGeneration;
import xact.snmp.commands.SNMPServiceServiceOperation;
import xact.snmp.exception.SNMPConnectionException;
import xact.snmp.exception.SNMPResponseException;
import xact.snmp.types.SNMPCounter32;
import xact.snmp.types.SNMPCounter64;
import xact.snmp.types.SNMPGauge32;
import xact.snmp.types.SNMPInteger;
import xact.snmp.types.SNMPIpAddress;
import xact.snmp.types.SNMPNull;
import xact.snmp.types.SNMPOctet;
import xact.snmp.types.SNMPString;
import xact.snmp.types.SNMPTimeTicks;
import xact.snmp.types.SNMPUnsignedInteger;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;


public class SNMPServiceServiceOperationImpl implements ExtendedDeploymentTask, SNMPServiceServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPServiceServiceOperationImpl.class);
  private static final String XYNAPROPERTY_SOCKETTIMEOUT = "xact.snmp.sockettimeout";
  private static final int SOCKETTIMEOUT_DEFAULT = 5000;
  private static final String XYNAPROPERTY_RETRIES = "xact.snmp.timeout.retries";
  private static final int RETRIES_DEFAULT = 2;
  private static final String XYNAPROPERTY_RETRYINTERVAL = "xact.snmp.timeout.retryinterval";
  private static int RETRYINTERVAL_DEFAULT = 1000;
  private static final String SECURE_STORAGE_KEY_AUTHPASSWORD = "xact.snmp.v3.authpassword";
  private static final String SECURE_STORAGE_KEY_PRIVPASSWORD = "xact.snmp.v3.privpassword";

  private static int socketTimeout = -1;
  private static EngineIdGeneration engineIdGeneration;
  private static int maxRetriesAfterTimeout = -1;
  private static int retryinterval = -1;


  public SNMPServiceServiceOperationImpl() {
  }


  public void onDeployment() throws XynaException {
    engineIdGeneration = new EngineIdGeneration(getRevision(), "SNMPService");
    SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthMD5());
    SecurityProtocols.getInstance().addAuthenticationProtocol(new AuthSHA());
  }


  public void onUndeployment() {
    engineIdGeneration.unregisterProperties();
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  private static long getRevision() {
    if (SNMPServiceServiceOperationImpl.class.getClassLoader() instanceof ClassLoaderBase) {
      return ((ClassLoaderBase) SNMPServiceServiceOperationImpl.class.getClassLoader()).getRevision();
    }

    return VersionManagement.REVISION_WORKINGSET;
  }


  private static boolean getNextFinished(VarBindList last, VarBindList next, String oidScope) {
    if (next.size() == 0) {
      return true;
    }
    if (next.equals(last)) {
      return true;
    }

    for (int i = 0; i < next.size(); i++) {
      VarBind vb = next.get(i);
      if ((vb.getObjectIdentifier() == null)) {
        return true;
      } else if (!vb.getObjectIdentifier().startsWith(oidScope)) {
        return true;
      } else if (vb instanceof NullVarBind && ((NullVarBind) vb).getSyntax() >= org.snmp4j.smi.SMIConstants.EXCEPTION_NO_SUCH_OBJECT) { //siehe klasse org.snmp4j.smi.SMIConstants
        return true;
      }
    }

    /* TODO
     *  (vb.getOid().size() < rootOID.size()) ||
      (rootOID.leftMostCompare(rootOID.size(), vb.getOid()) != 0)
      
            else if (!ignoreLexicographicOrder &&
             (vb.getOid().compareTo(lastOID) <= 0)) {
      finished = true;
    }*/
    return false;
  }


  private static synchronized int getSocketTimeout() {
    if (socketTimeout == -1) {
      try {
        socketTimeout =
            Integer.valueOf(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                .getProperty(XYNAPROPERTY_SOCKETTIMEOUT));
      } catch (NumberFormatException e) {
        logger.warn("XynaProperty " + XYNAPROPERTY_SOCKETTIMEOUT + " not set correctly or not set at all. setting to defaultvalue "
            + SOCKETTIMEOUT_DEFAULT);
        socketTimeout = SOCKETTIMEOUT_DEFAULT;
      }
      logger.debug("socketTimeout = " + socketTimeout);
    }
    return socketTimeout;
  }


  private static synchronized int getRetries() {
    if (maxRetriesAfterTimeout == -1) {
      try {
        maxRetriesAfterTimeout =
            Integer.valueOf(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                .getProperty(XYNAPROPERTY_RETRIES));
      } catch (NumberFormatException e) {
        logger.warn("XynaProperty " + XYNAPROPERTY_RETRIES + " not set correctly or not set at all. setting to defaultvalue "
            + RETRIES_DEFAULT);
        maxRetriesAfterTimeout = RETRIES_DEFAULT;
      }
      logger.debug("retries = " + maxRetriesAfterTimeout);
    }
    return maxRetriesAfterTimeout;
  }


  private static synchronized int getRetryInterval() {
    if (retryinterval == -1) {
      try {
        retryinterval =
            Integer.valueOf(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                .getProperty(XYNAPROPERTY_RETRYINTERVAL));
      } catch (NumberFormatException e) {
        logger.warn("XynaProperty " + XYNAPROPERTY_RETRYINTERVAL + " not set correctly or not set at all. setting to defaultvalue "
            + RETRYINTERVAL_DEFAULT);
        retryinterval = RETRYINTERVAL_DEFAULT;
      }
      logger.debug("retryinterval = " + retryinterval);
    }
    return retryinterval;
  }


  private static SnmpContext getSnmpContext(SNMPConnectionData connectionData) throws SNMPConnectionException {
    //TODO caching
    //TODO authentifizierungs protokolle konfigurierbar?
    
    String sourceIpSlashHost="0.0.0.0/0";//default
    if(connectionData.getSourceHost()!=null && connectionData.getSourcePort()!=null){
      sourceIpSlashHost=connectionData.getSourceHost().getHostname()+"/"+connectionData.getSourcePort().getValue();
    }

    if (connectionData instanceof SNMPConnectionDataV3) {
      SNMPConnectionDataV3 connectionDataV3 = (SNMPConnectionDataV3) connectionData;
      SADBuilder builder =
          SnmpAccessData.newSNMPv3().host(connectionData.getHost().getHostname()).port(connectionData.getPort().getValue())
              .username(connectionDataV3.getUserName()).timeoutModel("simple", getRetries(), getRetryInterval());
      //timeoutmodels findet man in der klasse TimeoutModels.java
      String authPassword = retrievePassword(connectionDataV3, SECURE_STORAGE_KEY_AUTHPASSWORD);
      String privPassword = retrievePassword(connectionDataV3, SECURE_STORAGE_KEY_PRIVPASSWORD);

      if (authPassword != null) {
        builder.authenticationProtocol(SnmpAccessData.MD5).authenticationPassword(authPassword);
      }
      if (privPassword != null) {
        builder.privacyProtocol(SnmpAccessData.DES56).privacyPassword(privPassword);
      }
      SnmpAccessData accessData = builder.build();
      try {
        SnmpContextImplApache scia = new SnmpContextImplApache(accessData, getSocketTimeout(),sourceIpSlashHost);
        scia.getSnmp().setLocalEngine(engineIdGeneration.getEngineId(), XynaFactory.getInstance().getBootCount(), (int) (System.currentTimeMillis() - XynaFactory.STARTTIME) / 1000);
        setEngineTimePerReflection(scia.getSnmp().getUSM().getTimeTable(), XynaFactory.STARTTIME);
        return scia;
      } catch (IOException e) {
        throw new SNMPConnectionException(e);
      }
    } else if (connectionData instanceof SNMPConnectionDataV2c) {
      SADBuilder builder =
          SnmpAccessData.newSNMPv2c().host(connectionData.getHost().getHostname()).port(connectionData.getPort().getValue())
              .community(((SNMPConnectionDataV2c) connectionData).getCommunity()).timeoutModel("simple", getRetries(), getRetryInterval());

      SnmpAccessData accessData = builder.build();
      try {
        return new SnmpContextImplApache(accessData, getSocketTimeout(),  sourceIpSlashHost);
      } catch (IOException e) {
        throw new SNMPConnectionException(e);
      }
    } else {
      throw new RuntimeException("unsupported snmp version: " + connectionData);
    }
  }

  private static String retrievePassword(SNMPConnectionDataV3 connectionDataV3, String destination) {
    String key = connectionDataV3.getHost().getHostname() + ":" + connectionDataV3.getPort().getValue() + "/" + connectionDataV3.getUserName();
    String password = (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve(destination, key);
    
    if (password == null) {
      if (logger.isDebugEnabled()) {
        logger.debug((destination.equals(SECURE_STORAGE_KEY_AUTHPASSWORD) ? "authentication" : "privacy") + " password not set for " + key);
      }
      
      //falls der Key nicht vorhanden ist, den globalen Key "*:<Port>/<UserName>" verwenden
      key = "*:" + connectionDataV3.getPort().getValue() + "/" + connectionDataV3.getUserName();
      password = (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve(destination, key);
      if (password == null) {
        if (logger.isDebugEnabled()) {
          logger.debug((destination.equals(SECURE_STORAGE_KEY_AUTHPASSWORD) ? "authentication" : "privacy") + " password not set for " + key);
        }
      }
    }
    
    return password;
  }
  
  private static Field lastLocalTimeChangeField;


  /**
   * böser hack: SNMP4j speichert die enginetime auf eine merkwürdige art und weise in UsmTimeTable.
   * beim verschicken eines requests mit authorativeEngineTime wird diese auf die vergangene zeit seit dem
   * letzten mal, dass jemand "setEngineTime" gesagt hat, gesetzt. anstatt auf die vergangene zeit seit der
   * derzeit gesetzten enginestarttime.
   * 
   * unklar, wieso das so ist.... aber so funktionierts.
   * gleicher hack auch im SNMPTrigger (FIXME duplicate code!!)
   */
  private static void setEngineTimePerReflection(UsmTimeTable usmTimeTable, long factoryStarttime) {
    if (lastLocalTimeChangeField == null) {
      Field f;
      try {
        f = UsmTimeTable.class.getDeclaredField("lastLocalTimeChange");
      } catch (SecurityException e) {
        throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
      }
      f.setAccessible(true);
      lastLocalTimeChangeField = f;
    }
    try {
      lastLocalTimeChangeField.set(usmTimeTable, factoryStarttime);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
    }
  }


  private static VarBindList getMDMVarBindingToSNMPUtilsVarBinding(List<? extends VarBinding> varBindings, boolean typeMustBeKnownBeforehand) {
    VarBindList vbl = new VarBindList();
    if (varBindings != null) {
      for (VarBinding vb : varBindings) {
        if (vb.getType() instanceof SNMPString) {
          vbl.add(new StringVarBind(vb.getOID().getOID(), vb.getValue().getContent()));
        } else if (vb.getType() instanceof SNMPInteger) {
          try {
            int v = Integer.valueOf(vb.getValue().getContent());
            vbl.add(new IntegerVarBind(vb.getOID().getOID(), v));
          } catch (NumberFormatException e) {
            throw new RuntimeException("invalid variable content. expected integer, got \"" + vb.getValue().getContent() + "\"", e);
          }
        } else if (vb.getType() instanceof SNMPOctet) {
          byte[] b = new byte[vb.getValue().getContent().length() / 2];
          for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(vb.getValue().getContent().substring(2 * i, 2 * i + 2), 16);
          }
          vbl.add(new ByteArrayVarBind(vb.getOID().getOID(), b));
        } else if (vb.getType() instanceof SNMPUnsignedInteger) {
          try {
            long l = Long.valueOf(vb.getValue().getContent());
            vbl.add(new UnsIntegerVarBind(vb.getOID().getOID(), l));
          } catch (NumberFormatException e) {
            throw new RuntimeException("invalid variable content. expected unsigned integer, got \"" + vb.getValue().getContent() + "\"", e);
          }
        } else if (vb.getType() instanceof SNMPIpAddress) {
          try {
            vbl.add(new IpAddressVarBind(vb.getOID().getOID(), vb.getValue().getContent()));
          } catch (NumberFormatException e) {
            throw new RuntimeException("invalid variable content. expected ipAddress, got \"" + vb.getValue().getContent() + "\"", e);
          }
        } else if (vb.getType() instanceof SNMPNull) {
          vbl.add(new NullVarBind(vb.getOID().getOID()));
        } else if (vb.getType() instanceof SNMPCounter64) {
          String val = vb.getValue().getContent();
          try {
            vbl.add(new Counter64VarBind(vb.getOID().getOID(), Long.valueOf(val)));
          } catch (NumberFormatException e) {
            try {
              vbl.add(new Counter64VarBind(vb.getOID().getOID(), new BigInteger(val).longValue()));
            } catch (NumberFormatException e1) {
              throw new RuntimeException("invalid variable content. expected long value for counter64, got \"" + vb.getValue().getContent()
                + "\"", e);
            }
          }
        } else {
          if (typeMustBeKnownBeforehand) {
            throw new RuntimeException("unsupported snmp variable type: " + vb.getType());
          } else {
            vbl.add(new NullVarBind(vb.getOID().getOID()));
          }
        }
      }
    }
    return vbl;
  }


  private static <U extends XynaException> void waitForRetry(SimpleRetryModel srm, U ex, int retries) throws U {
    if (retries >= srm.getRetries()) {
      throw ex;
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("error during snmp set. will retry for the " + (retries + 1) + ". time in " + srm.getRetryIntervalMilliseconds0()
            + "ms.", ex);
      }
      try {
        Thread.sleep(srm.getRetryIntervalMilliseconds0());
      } catch (InterruptedException e1) {
        throw new RuntimeException("Thread was interrupted while waiting for retry interval to be finished.", e1);
      }
    }

  }
  
  private static final BigInteger twoexp64 = BigInteger.ONE.shiftLeft(64);


  private static List<VarBinding> getSNMPUtilsVarBindingToMDMVarBinding(VarBindList vbl) {
    List<VarBinding> list = new ArrayList<VarBinding>();
    for (int i = 0; i < vbl.size(); i++) {
      VarBind v = vbl.get(i);
      VarBinding vb = new VarBinding();
      vb.setOID(new OID(v.getObjectIdentifier()));
      //type
      if (v instanceof StringVarBind) {
        vb.setType(new SNMPString());
      } else if (v instanceof IntegerVarBind) {
        vb.setType(new SNMPInteger());
      } else if (v instanceof UnsIntegerVarBind) {
        vb.setType(new SNMPUnsignedInteger());
      } else if (v instanceof Counter32VarBind) {
        vb.setType(new SNMPCounter32());
      } else if (v instanceof Gauge32VarBind) {
        vb.setType(new SNMPGauge32());
      } else if (v instanceof TimeTicksVarBind) {
        vb.setType(new SNMPTimeTicks());
      } else if (v instanceof OIDVarBind) {
        vb.setType(new SNMPString()); // TODO: here should be an OID type
      } else if (v instanceof NullVarBind) {
        vb.setType(new SNMPNull());
      } else if (v instanceof Counter64VarBind) {
        vb.setType(new SNMPCounter64());

        long val = ((Counter64VarBind)v).longValue();
        if (val < 0) {
          BigInteger valBI = BigInteger.valueOf(val).add(twoexp64);
          vb.setValue(new VariableContent(valBI.toString()));
          list.add(vb);
          continue;
        } 
      } else {
        throw new RuntimeException("got unsupported varBindType: " + v.getClass().getName() + " = " + v);
      }
      //value (evtl anders fuer andere typen??)
      vb.setValue(new VariableContent(String.valueOf(v.getValue())));
      list.add(vb);
    }
    return list;
  }


  public List<? extends VarBinding> getNext(List<? extends VarBinding> varBindings, SNMPConnectionData sNMPConnectionData)
      throws SNMPConnectionException, SNMPResponseException {
    SnmpContext ctx = getSnmpContext(sNMPConnectionData);
    try {
      VarBindList vblOld = getMDMVarBindingToSNMPUtilsVarBinding(varBindings, false);
      VarBindList vbl = ctx.getNext(vblOld, "xyna.snmpservice");
      if (getNextFinished(vblOld, vbl, "")) {
        throw new SNMPResponseException(0, -1); //FIXME andere zahl besser geeignet???
      }
      List<VarBinding> vb = getSNMPUtilsVarBindingToMDMVarBinding(vbl);
      return vb;
    } catch (SnmpManagerException e) {
      throw new SNMPConnectionException(e);
    } catch (SnmpResponseException e) {
      throw new SNMPResponseException(e.getErrorIndex(), e.getErrorStatus(), e); //FIXME andere parameter
    } finally {
      ctx.close(); //FIXME bei caching nicht tun? (s.o.)
    }
  }


  public VarBindings get(OIDs oIDs, SNMPConnectionData sNMPConnectionData, RetryModel retryModel) throws SNMPConnectionException,
      SNMPResponseException {
    if (retryModel instanceof SimpleRetryModel) {
      SimpleRetryModel srm = (SimpleRetryModel) retryModel;
      int retries = -1;
      while (true) {
        retries++;

        SnmpContext ctx = getSnmpContext(sNMPConnectionData);
        try {
          VarBindList vbl = convert(oIDs);
          vbl = ctx.get(vbl, "xyna.snmpservice");
          List<VarBinding> vb = getSNMPUtilsVarBindingToMDMVarBinding(vbl);
          return convert(vb);
        } catch (SnmpManagerException e) {
          waitForRetry(srm, new SNMPConnectionException(e), retries);
        } catch (SnmpResponseException e) {
          waitForRetry(srm, new SNMPResponseException(e.getErrorIndex(), e.getErrorStatus(), e), retries); //FIXME andere parameter
        } finally {
          ctx.close(); //FIXME bei caching nicht tun? (s.o.)
        }
      }
    } else {
      throw new RuntimeException("unsupported retry model: " + retryModel);
    }
  }


  private VarBindings convert(List<VarBinding> varBindingList) {
    VarBindings varBindings = new VarBindings();
    for (VarBinding vb : varBindingList) {
      try {
        String oid = vb.getOID().getOID();
        if (oid != null && oid.startsWith(".")) {
          oid = oid.substring(1);
        }
        VarBinding newVb = varBindings.setContentInMap(oid, null, vb.getValue().getContent());
        newVb.setType(vb.getType());
      } catch (XynaException e) {
        throw new RuntimeException(e);
      }
    }
    return varBindings;
  }


  private VarBindList convert(OIDs oids) {
    VarBindList vbl = new VarBindList();
    for (OID oid : oids.getOID()) {
      vbl.add(new NullVarBind(oid.getOID()));
    }
    return vbl;
  }


  public void set(VarBindings varBindings, SNMPConnectionData sNMPConnectionData, RetryModel retryModel) throws SNMPConnectionException,
      SNMPResponseException {
    if (retryModel instanceof SimpleRetryModel) {
      SimpleRetryModel srm = (SimpleRetryModel) retryModel;
      boolean success = false;
      int retries = -1;
      while (!success) {
        retries++;
        try {
          set(varBindings.getVarBinding(), sNMPConnectionData);
          success = true;
        } catch (SNMPConnectionException e) {
          waitForRetry(srm, e, retries);
        } catch (SNMPResponseException e) {
          waitForRetry(srm, e, retries);
        }
      }
    } else {
      throw new RuntimeException("unsupported retry model: " + retryModel);
    }
  }


  private static void set(List<? extends VarBinding> varBindings, SNMPConnectionData connectionData) throws SNMPConnectionException,
      SNMPResponseException {
    SnmpContext ctx = getSnmpContext(connectionData);
    try {
      VarBindList vbl = getMDMVarBindingToSNMPUtilsVarBinding(varBindings, true);
      ctx.set(vbl, "xyna.snmpservice");
    } catch (SnmpManagerException e) {
      throw new SNMPConnectionException(e);
    } catch (SnmpResponseException e) {
      throw new SNMPResponseException(e.getErrorIndex(), e.getErrorStatus(), e); //FIXME andere parameter
    } finally {
      ctx.close(); //FIXME bei caching nicht tun? (s.o.)
    }
  }


  public void trap(OID trapOID, VarBindings varBindings, SNMPConnectionData sNMPConnectionData, RetryModel retryModel)
      throws SNMPConnectionException, SNMPResponseException {
    SnmpContext ctx = getSnmpContext(sNMPConnectionData);
    try {
      VarBindList vbl = getMDMVarBindingToSNMPUtilsVarBinding(varBindings.getVarBinding(), true);
      ctx.trap(trapOID.getOID(), System.currentTimeMillis() - XynaFactory.STARTTIME, vbl, "xyna.snmpservice");
    } catch (SnmpManagerException e) {
      throw new SNMPConnectionException(e);
    } catch (SnmpResponseException e) {
      throw new SNMPResponseException(e.getErrorIndex(), e.getErrorStatus(), e); //FIXME andere parameter
    } finally {
      ctx.close(); //FIXME bei caching nicht tun? (s.o.)
    }
  }


  public VarBindings walk(SNMPConnectionData sNMPConnectionData, OID oID) throws SNMPResponseException, SNMPConnectionException {
    SnmpContext ctx = getSnmpContext(sNMPConnectionData);
    try {
      VarBindList vbl = new VarBindList();
      vbl.add(new NullVarBind(oID.getOID()));
      VarBindList traversedOids = new VarBindList();

      String oidScope = "." + oID.getOID();

      while (true) {
        VarBindList vblNext = ctx.getNext(vbl, "xyna.snmpservice");

        if (getNextFinished(vbl, vblNext, oidScope)) {
          break;
        }

        vbl = vblNext;
        traversedOids.add(vbl.get(0)); //es wird nur ein varbinding reingegeben, also auch nur ein varbinding im result erwartet
      }

      List<VarBinding> vb = getSNMPUtilsVarBindingToMDMVarBinding(traversedOids);
      return convert(vb);
    } catch (SnmpManagerException e) {
      throw new SNMPConnectionException(e);
    } catch (SnmpResponseException e) {
      throw new SNMPResponseException(e.getErrorIndex(), e.getErrorStatus(), e); //FIXME andere parameter
    } finally {
      ctx.close(); //FIXME bei caching nicht tun? (s.o.)
    }
  }

  
  public OIDs findOIDByValue(VarBindings varBindings, VariableContent variableContent) {
    OIDs oids = new OIDs();
    
    for (VarBinding vb : varBindings.getVarBinding()) {
      if (StringUtils.isEqual(vb.getValue().getContent(), variableContent.getContent())) {
        oids.addToOID(vb.getOID());
      }
    }
    
    return oids;
  }
}
