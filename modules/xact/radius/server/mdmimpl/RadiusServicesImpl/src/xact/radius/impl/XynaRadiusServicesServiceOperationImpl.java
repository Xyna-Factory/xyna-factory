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
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

import base.Text;
import xact.radius.Code;
import xact.radius.Node;
import xact.radius.RadiusUser;
import xact.radius.RequestAuthenticator;
import xact.radius.TypeWithValueNode;
import xact.radius.impl.util.ByteUtil;
import xint.crypto.AESCrypto;
import xint.crypto.exceptions.AESCryptoException;
import xint.crypto.parameter.AESCryptoParameter;



public class XynaRadiusServicesServiceOperationImpl implements ExtendedDeploymentTask {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaRadiusServicesServiceOperationImpl.class);

  private static final String REJECT = "3";
  private static final String ACCEPT = "2";

  private static final String TIMEOUTPROPERTY = "xact.radius.passwordExpirationTime";
  private static final String SHAREDSECRETPROPERTY = "xact.radius.sharedSecret";
  private static final String AESKEYNAMEPROPERTY = "xact.radius.aes.keyName";
  private static final String AESKEYSIZEPROPERTY = "xact.radius.aes.keySize";

  private static final XynaPropertyDuration timeoutXynaProp = new XynaPropertyDuration(TIMEOUTPROPERTY, "900 s")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Maximale Gültigkeitsdauer für Benutzer mit Einmalpasswort")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Maximum validity period for users with a one-time password");
  private final static XynaPropertyString sharedSecretXynaProp = new XynaPropertyString(SHAREDSECRETPROPERTY, "sharedSecret", false)
      .setDefaultDocumentation(DocumentationLanguage.DE,
                               "Standardwert für den RADIUS Server. Wird verwendet, wenn kein benutzerspezifisches shared secret existiert.")
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Default value for the RADIUS server. Used when no user-specific shared secret exists.");

  private final static XynaPropertyString aesKeyNameXynaProp = new XynaPropertyString(AESKEYNAMEPROPERTY, null, true)
      .setDefaultDocumentation(DocumentationLanguage.DE,
                               "Name (identifier) des AES-Schlüssels zum Ver- und Entschlüsseln der RADIUS-Benutzerdaten.")
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Name (identifier) of the AES key for encryption and decryption of RADIUS user data");
  private final static XynaPropertyInt aesKeySizeXynaProp = new XynaPropertyInt(AESKEYSIZEPROPERTY, 256)
      .setDefaultDocumentation(DocumentationLanguage.DE, "AES Schlüssellänge (bits): 128, 192, oder 256 (default)")
      .setDefaultDocumentation(DocumentationLanguage.EN, "AES key size (bits): 128, 192, or 256 (default)");


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


  public xact.radius.PrivilegeLevel getPrivilegeLevel(xact.radius.FunctionalRole role, xact.radius.Vendor vendor) {
    String vendorName = vendor != null ? vendor.getName() : null;

    if (vendorName == null)
      return new xact.radius.PrivilegeLevel("0");

    return new xact.radius.PrivilegeLevel("0");
  }


  public Code validateCredentials(RadiusUser inputUser, RadiusUser databaseUser, RequestAuthenticator requestAuthenticator) {
    AESCryptoParameter aesParam = new AESCryptoParameter(aesKeyNameXynaProp.get(), aesKeySizeXynaProp.get());
    String databaseUserPassword = databaseUser.getPassword();
    String sharedSecret = databaseUser.getSharedSecret();

    try {
      databaseUserPassword = AESCrypto.aESDecrypt(new Text(databaseUserPassword), aesParam).getText();
      if (sharedSecret != null && sharedSecret.length() > 0) {
        sharedSecret = AESCrypto.aESDecrypt(new Text(sharedSecret), aesParam).getText();
      } else {
        sharedSecret = sharedSecretXynaProp.get(); // use default shared secret from property
      }
    } catch (AESCryptoException e) {
      throw new RuntimeException("Error during decryption of user data", e);
    }

    String passwordindatabase = encode(sharedSecret, requestAuthenticator.getValue(), databaseUserPassword);

    if (!passwordindatabase.equals(inputUser.getPassword())) {
      if (logger.isDebugEnabled()) {
        logger.debug("RADIUS Authentication failed, because password does not match. Sending Reject!");
      }
      return new Code(REJECT);
    }

    return new Code(ACCEPT);
  }


  public RadiusUser getBasicUserInfoFromNodes(List<? extends Node> inputNodes) {
    RadiusUser.Builder radUserBuilder = new RadiusUser.Builder();

    for (Node n : inputNodes) {
      if (n.getTypeName().equalsIgnoreCase("USER-NAME"))
        radUserBuilder.username(((TypeWithValueNode) n).getValue().replaceAll("\"", ""));
      if (n.getTypeName().equalsIgnoreCase("USER-PASSWORD"))
        radUserBuilder.password(((TypeWithValueNode) n).getValue());
      if (n.getTypeName().equalsIgnoreCase("NAS-Identifier"))
        radUserBuilder.iPAddress(((TypeWithValueNode) n).getValue());
    }

    return radUserBuilder.instance();
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
