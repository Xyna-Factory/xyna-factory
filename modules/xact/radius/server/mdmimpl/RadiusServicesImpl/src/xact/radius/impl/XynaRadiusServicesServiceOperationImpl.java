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
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

import base.Text;
import xact.radius.Code;
import xact.radius.Node;
import xact.radius.RadiusUser;
import xact.radius.RequestAuthenticator;
import xact.radius.TypeWithValueNode;
import xact.radius.impl.util.ByteUtil;



public class XynaRadiusServicesServiceOperationImpl implements ExtendedDeploymentTask {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusServicesServiceOperationImpl.class);

  private static final String REJECT = "3";
  private static final String ACCEPT = "2";

  private static final String TIMEOUTPROPERTY = "xact.radius.passwordExpirationTime";
  private static final String SHAREDSECRETPROPERTY = "xact.radius.sharedSecret";

  private static final XynaPropertyDuration timeoutXynaProp = new XynaPropertyDuration("xact.radius.passwordExpirationTime", "900 s")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Maximale Gültigkeitsdauer für Benutzer mit Einmalpasswort")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Maximum validity period for users with a one-time password");
  private final static XynaPropertyString sharedSecretXynaProp = new XynaPropertyString(SHAREDSECRETPROPERTY, "sharedSecret", false)
      .setDefaultDocumentation(DocumentationLanguage.DE,
                               "Standardwert für den Radius Server. Wird verwendet, wenn kein benutzerspezifisches shared secret existiert.")
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Default value for the radius server. Used when no user-specific shared secret exists.");


  public XynaRadiusServicesServiceOperationImpl() {
  }


  public void onDeployment() throws XynaException {
  }


  public void onUndeployment() throws XynaException {
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
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

    return new xact.radius.PrivilegeLevel("0");
  }


  public Text checkCredentialsExpired(RadiusUser radiusUser) {
    Long timeout = getTimeout();

    // permanent user or one-time password not expired
    if (radiusUser.getTimestamp() == 0L || (System.currentTimeMillis() - radiusUser.getTimestamp()) <= timeout) {
      return new Text("false");
    }
    return new Text("true"); // password expired
  }


  public Code validateCredentials(RadiusUser radiusUser, RequestAuthenticator requestAuthenticator) {
    String radiusUserpassword = radiusUser.getPassword();
    String sharedsecret = radiusUser.getSharedSecret();
    if (sharedsecret == null || sharedsecret.length() == 0) {
      sharedsecret = sharedSecretXynaProp.get();
    }

    String passwordindatabase = encode(sharedsecret, requestAuthenticator.getValue(), radiusUserpassword);

    if (!passwordindatabase.equals(radiusUserpassword)) {
      if (logger.isDebugEnabled()) {
        logger.debug("RADIUS Authentication failed, because password does not match. Sending Reject!");
      }
      return new Code(REJECT);
    }

    return new Code(ACCEPT);
  }


  public RadiusUser getBasicUserInfoFromNodes(List<? extends Node> inputNodes) {
    RadiusUser radUser = new RadiusUser();

    for (Node n : inputNodes) {
      if (n.getTypeName().equalsIgnoreCase("USER-NAME"))
        radUser.setUsername(((TypeWithValueNode) n).getValue().replaceAll("\"", ""));
      if (n.getTypeName().equalsIgnoreCase("USER-PASSWORD"))
        radUser.setPassword(((TypeWithValueNode) n).getValue());
      if (n.getTypeName().equalsIgnoreCase("NAS-Identifier"))
        radUser.setIPAddress(((TypeWithValueNode) n).getValue());
    }

    return radUser;
  }


  private static long getTimeout() {
    String expirationtime = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty(TIMEOUTPROPERTY);
    long timeout = 15 * 60000; // 15 minutes default value
    if (expirationtime == null || expirationtime.length() == 0) {
      timeout = timeoutXynaProp.getMillis();
    } else {
      try {
        timeout = Long.parseLong(expirationtime);
      } catch (Exception e) {
        logger.warn("Property " + TIMEOUTPROPERTY + " not set correctly. Using 15 minutes!");
      }
    }

    return timeout;
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

}
