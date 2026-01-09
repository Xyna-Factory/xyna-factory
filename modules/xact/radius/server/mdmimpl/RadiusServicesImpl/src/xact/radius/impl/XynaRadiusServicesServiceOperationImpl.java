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
package xact.radius.impl;



import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;

import xact.radius.Code;
import xact.radius.Node;
import xact.radius.TypeOnlyNode;
import xact.radius.TypeWithValueNode;
import xact.radius.impl.database.RadiusUserStorable;
import xact.radius.impl.util.ByteUtil;



public class XynaRadiusServicesServiceOperationImpl implements ExtendedDeploymentTask {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusServicesServiceOperationImpl.class);

  private static String REJECT = "3";
  private static String ACCEPT = "2";

  private static String sharedSecretProp = "xact.radius.sharedSecret";
  private static String timeoutProp = "xact.radius.passwordExpirationTime";

  private static String sqlGetUserByName =
      "select * from " + RadiusUserStorable.TABLENAME + " where " + RadiusUserStorable.COL_USERNAME + " =  ?";

  private static String sqlDeleteUsersByTimeout = "delete from " + RadiusUserStorable.TABLENAME + " where "
      + RadiusUserStorable.COL_TIMESTAMP + " > 0 and " + RadiusUserStorable.COL_TIMESTAMP + "  < ?";

  private final static XynaPropertyDuration timeoutXynaProp = new XynaPropertyDuration(timeoutProp, "900 s");

  private final static XynaPropertyString sharedSecretXynaProp = new XynaPropertyString(sharedSecretProp, "sharedSecret", false);

  private final static XynaPropertyInt ciscoROprivlvl = new XynaPropertyInt("xact.radius.shell-privilege-level.cisco.read-only", 1);
  private final static XynaPropertyInt ciscoRWprivlvl = new XynaPropertyInt("xact.radius.shell-privilege-level.cisco.read-write", 7);
  private final static XynaPropertyInt ciscoSUprivlvl = new XynaPropertyInt("xact.radius.shell-privilege-level.cisco.super-user", 15);

  public XynaRadiusServicesServiceOperationImpl() {
  }


  public void onDeployment() throws XynaException {

    ODS ods = ODSImpl.getInstance(true);
    try {
      ods.registerStorable(RadiusUserStorable.class);
    } catch (Exception e) {
      logger.error("storable registration", e);
    }

  }


  public void onUndeployment() throws XynaException {
    ODS ods = ODSImpl.getInstance(true);
    try {
      ods.unregisterStorable(RadiusUserStorable.class);
    } catch (Exception e) {
      logger.error("storable unregistration", e);
    }
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty
    // xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if
    // this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted,
    // while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued
    // in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be
    // continued after calling Thread.stop on the thread.;
    // executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public xact.radius.XynaPropertyStringValue getXynaProperty(xact.radius.XynaPropertyKey xynaPropertyKey) {
    String result = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getProperty(xynaPropertyKey.getXynaPropertyKey());
    return new xact.radius.XynaPropertyStringValue(result);
  }


  public xact.radius.PrivilegeLevel getPrivilegeLevel(xact.radius.FunctionalRole role, xact.radius.Vendor vendor) {
    String vendorName = vendor != null ? vendor.getName() : null;

    if (vendorName == null)
      return new xact.radius.PrivilegeLevel("0");

    if ("cisco".equals(vendorName.toLowerCase()))
      return new xact.radius.PrivilegeLevel(String.valueOf(getPrivilegeLevelForCisco(role)));

    return new xact.radius.PrivilegeLevel("0");
  }


  private static int getPrivilegeLevelForCisco(xact.radius.FunctionalRole role) {

    if (role instanceof xact.radius.SuperUser)
      return ciscoSUprivlvl.get();

    if (role instanceof xact.radius.ReadOnlyUser)
      return ciscoROprivlvl.get();

    if (role instanceof xact.radius.ReadWriteUser)
      return ciscoRWprivlvl.get();

    return 0;
  }


  private static RadiusUserStorable queryUserByName(String username) {
    ODSConnection ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().openConnection(ODSConnectionType.HISTORY);

    try {
      PreparedQuery<RadiusUserStorable> pq =
          ods.prepareQuery(new Query<RadiusUserStorable>(sqlGetUserByName, (new RadiusUserStorable()).getReader()), false);
      return ods.queryOneRow(pq, new Parameter(username));
    } catch (PersistenceLayerException e) {
      logger.error(null, e);
    } finally {
      try {
        ods.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }

    return null;
  }


  private static void storeRadiusUserEntry(RadiusUserStorable entry) throws PersistenceLayerException {

    ODSConnection ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().openConnection(ODSConnectionType.HISTORY);
    try {
      ods.persistObject(entry);
      ods.commit();

    } catch (Exception e) {
      logger.error("Problems saving single Radius User: ", e);
    } finally {
      ods.closeConnection();
    }
  }


  private static void deleteRadiusUserEntry(RadiusUserStorable entry) {
    ODSConnection ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().openConnection(ODSConnectionType.HISTORY);
    try {
      if (logger.isDebugEnabled())
        logger.debug("Removing User/Password entry");
      ods.deleteOneRow(entry);
      ods.commit();
      if (logger.isDebugEnabled())
        logger.debug("User/Password removed!");
    } catch (PersistenceLayerException e) {
      logger.error(null, e);
    } finally {
      try {
        ods.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }

  }


  public xact.radius.RadiusAddUserResult addTemporaryRadiusUser(IPv4 inputip, xact.radius.RadiusUserData userdata) {
    if (logger.isDebugEnabled())
      logger.debug("addTemporaryRadiusUser called for " + userdata.getUsername() + "@" + inputip.getValue());

    long timestamp = System.currentTimeMillis();

    String username = userdata.getUsername();
    String password = userdata.getPassword();
    String role = userdata.getRole();
    String ip = inputip.getValue();
    String sharedsecret = "";
    String servicetype = "";

    if (sharedsecret == null || sharedsecret.length() == 0) {
      if (logger.isDebugEnabled())
        logger.debug("Reading sharedSecret from property " + sharedSecretProp);
      sharedsecret = sharedSecretXynaProp.get();
    }
    if (sharedsecret == null || sharedsecret.length() == 0) {
      logger.error("Property " + sharedSecretProp + " not set. Can't create new radius user. Aborting ...");

      return new xact.radius.RadiusAddUserResult("Property " + sharedSecretProp + " not set. Can't create new radius user. Aborting ...");
    }

    if (ip == null || ip.length() == 0) {
      logger.error("Could not determine IP! Aborting...");
      return new xact.radius.RadiusAddUserResult("Could not determine IP! Aborting...");
    }

    RadiusUserStorable founduser = queryUserByName(username);

    try {
      RadiusUserStorable newuser =
          new RadiusUserStorable(founduser != null ? founduser.getId() : com.gip.xyna.idgeneration.IDGenerator.getInstance().getUniqueId(),
                                 username, password, sharedsecret, servicetype, timestamp, role, ip);
      if (logger.isDebugEnabled())
        logger.debug("Creating User with: id: " + newuser.getId() + " username: " + username + " sharedsecret: " + sharedsecret
            + " servicetype: " + servicetype + " timestamp: " + timestamp + " role: " + role + " ip: " + ip);

      storeRadiusUserEntry(newuser);
    } catch (PersistenceLayerException e) {
      logger.error("Problems saving new temporary RadiusUser: ", e);
      return new xact.radius.RadiusAddUserResult("Problems saving new temporary RadiusUser!");
    } catch (XynaException e) {
      logger.error("Problem creating id for new temporary RadiusUser: ", e);
      return new xact.radius.RadiusAddUserResult("Problem creating id for new temporary RadiusUser!");
    }

    return new xact.radius.RadiusAddUserResult("Successful");
  }


  @Deprecated
  public List<? extends Node> addPrivilegeLevel(xact.radius.PrivilegeLevel prvlvl, List<? extends Node> inputnodes) {
    if (logger.isDebugEnabled())
      logger.debug("addPriviledgeLevel called ...");
    List<Node> resultlist = new ArrayList<Node>();

    String prvlvlstring = prvlvl.getLevel();
    if (prvlvlstring == null || prvlvlstring.length() == 0) {
      logger.error("No Priviledge Level given, can not add it to reply. Aborting ...");
      return resultlist;
    }

    String vendor = "";
    for (Node n : inputnodes) {
      if (n.getTypeName().equalsIgnoreCase("NAS-Identifier")) {
        vendor = ((TypeWithValueNode) n).getValue();
        if (logger.isDebugEnabled())
          logger.debug("NAS Identifier found: " + vendor);
      }
    }

    if (vendor.contains("Cisco") || vendor.contains("CISCO") || vendor.contains("cisco")) {
      if (logger.isDebugEnabled())
        logger.debug("Found Cisco Device, adding priviledge Level ...");
      resultlist.add(new TypeWithValueNode("Service-Type", "7"));

      TypeWithValueNode ciscooption = new TypeWithValueNode("ciscooption", "shell:priv-lvl=" + prvlvlstring);
      List<Node> subnodes = new ArrayList<Node>();
      subnodes.add(ciscooption);
      TypeOnlyNode vendorspecific = new TypeOnlyNode("Vendor-Specific9", subnodes);
      resultlist.add(vendorspecific);
    } else {
      if (logger.isDebugEnabled())
        logger.debug("Could not determine Devicevendor, adding Cisco priviledge Level ...");

      resultlist.add(new TypeWithValueNode("Service-Type", "7"));

      TypeWithValueNode ciscooption = new TypeWithValueNode("ciscooption", "shell:priv-lvl=" + prvlvlstring);
      List<Node> subnodes = new ArrayList<Node>();
      subnodes.add(ciscooption);
      TypeOnlyNode vendorspecific = new TypeOnlyNode("Vendor-Specific9", subnodes);
      resultlist.add(vendorspecific);
    }

    return resultlist;
  }


  private static long getTimeout() {
    if (logger.isDebugEnabled())
      logger.debug("Reading Password Expiration Time from property ...");
    String expirationtime = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(timeoutProp);
    long timeout = 15 * 60000; // 15 Minuten defaultwert
    if (expirationtime == null || expirationtime.length() == 0) {
      timeout = timeoutXynaProp.getMillis();
    } else {
      try {
        timeout = Long.parseLong(expirationtime);
      } catch (Exception e) {
        logger.warn("Property " + timeoutProp + " not set correctly. Using 15 minutes!");
      }
    }

    return timeout;
  }


  private static xact.radius.Code authenticateRadiusUserAndRemoveExpired(RadiusUserStorable founduser, String radiusUserpassword,
                                                                         xact.radius.SourceIP sourceip, String requestauthenticator) {

    long timeout = getTimeout();

    if (logger.isDebugEnabled())
      logger.debug("Starting RADIUS authentication ...");

    if (founduser == null) {
      if (logger.isDebugEnabled())
        logger.debug("RADIUS user not found! Authentication failed. Sending Reject!");
      return new xact.radius.Code(REJECT);
    }

    if (!(sourceip.getValue().equals(founduser.getIp()))) {
      if (logger.isDebugEnabled())
        logger.debug("RADIUS Authentication failed, because ip does not match. (src: " + sourceip.getValue() + " expected: "
            + founduser.getIp() + ") Sending Reject");
      return new xact.radius.Code(REJECT);
    }

    String passwordindatabase = encode(founduser.getSharedSecret(), requestauthenticator, founduser.getUserPassword());
    if (logger.isTraceEnabled()) {
      logger.trace("### Userpassword in database: " + founduser.getUserPassword());
      logger.trace("### Encoded Userpassword in database: " + passwordindatabase);
      logger.trace("### Encoded Userpassword in message: " + radiusUserpassword);
    }

    if (!passwordindatabase.equals(radiusUserpassword)) {
      if (logger.isDebugEnabled())
        logger.debug("RADIUS Authentication failed, because password does not match. Sending Reject!");
      return new xact.radius.Code(REJECT);
    }

    if (founduser.getTimestamp() == 0 || (System.currentTimeMillis() - founduser.getTimestamp()) <= timeout) {
      if (logger.isDebugEnabled())
        logger.debug("RADIUS Authentication successful. Sending Accept!");

      if (founduser.getTimestamp() > 0) // Einmalpasswort mit Ablaufdatum
      {
        if (logger.isDebugEnabled())
          logger.debug("One time password used.");
        deleteRadiusUserEntry(founduser);
      }

      return new xact.radius.Code(ACCEPT);
    } else {
      if (logger.isDebugEnabled())
        logger.debug("RADIUS Authentication failed, because password used is expired. Sending Reject!");
    }

    return new xact.radius.Code(REJECT);
  }


  public Container authenticateRadiusUser(List<? extends Node> inputnodes, xact.radius.Code xmomcode,
                                          xact.radius.RequestAuthenticator xmomauthenticator, xact.radius.SourceIP sourceip) {
    String code = xmomcode.getValue();
    String requestauthenticator = xmomauthenticator.getValue();
    String radiusUsername = "";
    String radiusUserpassword = "";
    String nasidentifier = "";

    for (Node n : inputnodes) {
      if (n.getTypeName().equalsIgnoreCase("USER-NAME"))
        radiusUsername = ((TypeWithValueNode) n).getValue().replaceAll("\"", "");
      if (n.getTypeName().equalsIgnoreCase("USER-PASSWORD"))
        radiusUserpassword = ((TypeWithValueNode) n).getValue();
      if (n.getTypeName().equalsIgnoreCase("NAS-Identifier"))
        nasidentifier = ((TypeWithValueNode) n).getValue();

    }

    if (logger.isDebugEnabled())
      logger.debug("got Code: " + code + " Authenticator: " + requestauthenticator + " Username: " + radiusUsername);
    if (logger.isDebugEnabled())
      logger.debug("got NAS-Identifier: " + nasidentifier);

    RadiusUserStorable founduser = queryUserByName(radiusUsername);
    Code resCode = authenticateRadiusUserAndRemoveExpired(founduser, radiusUserpassword, sourceip, requestauthenticator);

    xact.radius.NASIdentifier nasidentifiernode = new xact.radius.NASIdentifier(nasidentifier);
    xact.radius.UserRole userrolenode = new xact.radius.UserRole(founduser != null ? founduser.getRole() : "");

    return new Container(resCode, userrolenode, nasidentifiernode);

  }


  @Deprecated
  public Container processAccessRequest(List<? extends Node> inputnodes, xact.radius.Code xmomcode, xact.radius.Identifier xmomidentifier,
                                        xact.radius.RequestAuthenticator xmomauthenticator, xact.radius.SourceIP sourceip) {
    if (logger.isDebugEnabled())
      logger.debug("Processing received RequestAccess Message ...");

    String code = xmomcode.getValue();
    String identifier = xmomidentifier.getValue();
    String requestauthenticator = xmomauthenticator.getValue();
    String radiusUsername = "";
    String radiusUserpassword = "";

    for (Node n : inputnodes) {
      if (n.getTypeName().equalsIgnoreCase("USER-NAME"))
        radiusUsername = ((TypeWithValueNode) n).getValue().replaceAll("\"", "");
      if (n.getTypeName().equalsIgnoreCase("USER-PASSWORD"))
        radiusUserpassword = ((TypeWithValueNode) n).getValue();
    }

    if (logger.isDebugEnabled())
      logger.debug("got Code: " + code + " Identifier: " + identifier + " Authenticator: " + requestauthenticator + " Username: "
          + radiusUsername);

    RadiusUserStorable founduser = queryUserByName(radiusUsername);
    Code resCode = authenticateRadiusUserAndRemoveExpired(founduser, radiusUserpassword, sourceip, requestauthenticator);

    xact.radius.SharedSecret xmomsharedsecret = new xact.radius.SharedSecret(founduser != null ? founduser.getSharedSecret() : "");

    xact.radius.PrivilegeLevel privlvl = new xact.radius.PrivilegeLevel();

    privlvl.setLevel("0"); // default 0

    if (founduser != null) {
      if (logger.isDebugEnabled())
        logger.debug("Trying to determine priviledge level ...");
      if (founduser.getRole() != null && founduser.getRole().length() > 0) {
        String getLevelFromProperty = "NetFactoryRD.radius.rightmanagment." + founduser.getRole();
        try {
          String fachlStufe = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(getLevelFromProperty);
          if (fachlStufe != null && fachlStufe.length() > 0) {
            if (fachlStufe.equals("read-only"))
              privlvl.setLevel("1");
            if (fachlStufe.equals("read-write"))
              privlvl.setLevel("7");
            if (fachlStufe.equals("super-user"))
              privlvl.setLevel("15");
          } else {
            logger.warn("NetFactoryRD.radius.rightmanagment." + founduser.getRole()
                + " not set correctly. Should be read-only, read-write or super-user!");
          }

        } catch (Exception e) {
          logger.warn("Problem reading Property " + getLevelFromProperty);
        }
      } else {
        logger.warn("No Role for radius user given. Can not determine priviledge level!");
      }
    }
    if (logger.isDebugEnabled())
      logger.debug("Privilege Level found as: " + privlvl.getLevel());

    return new Container(resCode, xmomsharedsecret, privlvl);
  }


  private static byte[] md5(byte[] input) {

    byte[] md5 = null;

    if (input == null)
      return null;

    try {
      // Create MessageDigest object for MD5
      MessageDigest digest = MessageDigest.getInstance("MD5");

      // Update input string in message digest
      digest.update(input, 0, input.length);
      md5 = digest.digest();
    } catch (Exception e) {
      logger.warn("RADIUS: Problems building md5sum: " + e);
    }

    return md5;
  }


  private static String encode(String sharedSecret, String requestAuthenticator, String password) {
    String result = "";

    if (password.length() == 0)
      return result; // leeres Passwort

    byte[] pwinput = password.getBytes();

    int blocksOfSixteen; // Anzahl 16er Blocks
    if (pwinput.length % 16 == 0) {
      blocksOfSixteen = pwinput.length / 16;
    } else {
      blocksOfSixteen = (pwinput.length / 16) + 1;
    }

    byte[] pw = new byte[blocksOfSixteen * 16];

    // Padding
    for (int i = 0; i < pw.length; i++) {
      if (i < pwinput.length) {
        pw[i] = pwinput[i];
      } else {
        pw[i] = 0;
      }
    }

    byte[] sS = sharedSecret.getBytes();
    byte[] au = xact.radius.impl.util.ByteUtil.toByteArray(requestAuthenticator);

    byte[] sSau = new byte[sS.length + au.length]; // aneinanderhaengen von SharedSecret und Authenticator
    System.arraycopy(sS, 0, sSau, 0, sS.length);
    System.arraycopy(au, 0, sSau, sS.length, au.length);

    byte[] xorop = md5(sSau); // MD5 ueber aneinandergehaengtes Array

    byte[] resultarray = new byte[16 * blocksOfSixteen];

    for (int i = 0; i < 16; i++) {
      resultarray[i] = (byte) (xorop[i] ^ pw[i]); // XOR Operation
    }

    int counter = 1; // ersten 16 Stellen bereits befuellt

    while (counter < blocksOfSixteen) {
      byte[] sSxorop = new byte[sS.length + xorop.length];
      System.arraycopy(sS, 0, sSxorop, 0, sS.length);
      System.arraycopy(xorop, 0, sSxorop, sS.length, xorop.length);

      xorop = md5(sSxorop);

      int offset = counter * 16;

      for (int i = 0; i < 16; i++) {
        resultarray[i + offset] = (byte) (xorop[i] ^ pw[i + offset]); // XOR Operation
      }

      counter++;
    }

    result = ByteUtil.toHexValue(resultarray);

    return result;
  }


  public void cleanUsers() {
    long timeout = getTimeout();

    ODSConnection ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS().openConnection(ODSConnectionType.HISTORY);

    try {
      Command sqlCmd = new Command(sqlDeleteUsersByTimeout);
      Parameter sqlparameter = new Parameter(System.currentTimeMillis() - timeout);
      ods.executeDML(ods.prepareCommand(sqlCmd), sqlparameter);
      ods.commit();
    } catch (PersistenceLayerException e) {
      logger.error(null, e);
    } finally {
      try {
        ods.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error(null, e);
      }
    }
  }

}
