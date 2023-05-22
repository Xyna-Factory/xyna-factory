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

package com.gip.xyna.xact.trigger;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;




public final class DigestAuthentificationUtilities {

  public static final String DIGEST_AUTH_HEADERFIELD_DIGEST = "Digest";
  public static final String DIGEST_AUTH_HEADERFIELD_REALM = "realm";
  public static final String DIGEST_AUTH_HEADERFIELD_QOP = "qop";
  public static final String DIGEST_AUTH_HEADERFIELD_NONCE = "nonce";
  public static final String DIGEST_AUTH_HEADERFIELD_STALE = "stale";
  public static final String DIGEST_AUTH_HEADERFIELD_ALGORITHM = "algorithm";
  public static final String DIGEST_AUTH_HEADERFIELD_OPAQUE = "opaque";
  public static final String QOP_VALUE_AUTH = "auth";


  public static final String DIGEST_AUTH_RESPONSEFIELD_RESPONSE = "response";
  public static final String DIGEST_AUTH_RESPONSEFIELD_USERNAME = "username";
  public static final String DIGEST_AUTH_RESPONSEFIELD_URI = "uri";
  public static final String DIGEST_AUTH_RESPONSEFIELD_QOP = "qop";
  public static final String DIGEST_AUTH_RESPONSEFIELD_CNONCE = "cnonce";
  public static final String DIGEST_AUTH_RESPONSEFIELD_NONCE = "nonce";
  public static final String DIGEST_AUTH_RESPONSEFIELD_NONCE_COUNT = "nc";
  public static final String DIGEST_AUTH_RESPONSEFIELD_REALM = "realm";


  private static final String DIGEST_AUTH_CONSTANT_OPAQUE_FIELD = "alsidufganlsjk34589zm345mv3mz459";
  private static final String NONCE_GENERATION_PRIVATE_KEY = "jUo98HUIzIHz7";


  private DigestAuthentificationUtilities() {
  }


  static boolean checkAuthentificationHeader(DigestAuthentificationInformation info, String password) {

    if (password != null) {
      String expectedResponse;
      expectedResponse = calculateExpectedResponse(info.getUsername(), info.getRealm(), password,
                                                   info.getMethod(), info.getUri(), info.getNonce(), info.getQop(),
                                                   info.getNc(), info.getCnonce(), info.getAlgorithm());
      return expectedResponse.equals(info.getReceivedResponse());
    } else {
      return false;
    }

  }


  /**
   * Calculates the expected response string according to http://tools.ietf.org/html/rfc2617
   * @throws IllegalArgumentException if one of the passed strings is null
   */
  private static String calculateExpectedResponse(String username, String realmName, String password, String method,
                                                  String URI, String nonce, String qop, String ncValue, String cnonce,
                                                  String algorithm) {

    if (username == null) {
      throw new IllegalArgumentException("Field 'username' is missing for Digest Access Authentication.");
    }
    if (realmName == null) {
      throw new IllegalArgumentException("Field 'realmName' is missing for Digest Access Authentication.");
    }
    if (password == null) {
      throw new IllegalArgumentException("Field 'password' is missing for Digest Access Authentication.");
    }
    if (method == null) {
      throw new IllegalArgumentException("Field 'method' is missing for Digest Access Authentication.");
    }
    if (URI == null) {
      throw new IllegalArgumentException("Field 'URI' is missing for Digest Access Authentication.");
    }
    if (nonce == null) {
      throw new IllegalArgumentException("Field 'nonce' is missing for Digest Access Authentication.");
    }
    // see RFC 2617: 'qop' SHOULD be provided but does not have to. If present, it has to be one of
    // the previously sent values. Since we always send 'auth', just compare to that string.
    if (qop != null) {
      if (!QOP_VALUE_AUTH.equals(qop)) {
        throw new IllegalArgumentException("Field 'qop' contains unsupported value for Digest Access Authentication.");
      }
    }
    if (ncValue == null) {
      if (qop != null) { // this is only required if "qop" is specified
        throw new IllegalArgumentException("Field 'nc' is missing for Digest Access Authentication.");
      }
    }
    if (cnonce == null) {
      if (qop != null) { // this is only required if "qop" is specified
        throw new IllegalArgumentException("Field 'cnonce' is missing for Digest Access Authentication.");
      }
    }
    if (algorithm == null) {
      throw new IllegalArgumentException("Field 'algorithm' is missing for Digest Access Authentication.");
    }

    MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
    }

    // see 3.2.2.2 in RFC 2617
    String step1 = username + ":" + realmName + ":" + password;
    byte[] step1Bytes = step1.getBytes(); // FIXME encoding? ASCII
    String step1Digested;
    synchronized (md5) {
      md5.reset();
      byte[] step1DigestedBytes = md5.digest(step1Bytes);
      step1Digested = digest2HexString(step1DigestedBytes);
    }

    // see 3.2.2.3 in RFC 2617; if something other than "auth" or "<null>" was supported for 'qop', this
    // had to be extended
    String step2 = method + ":" + URI;
    byte[] step2Bytes = step2.getBytes(); // FIXME encoding?
    String step2Digested;
    synchronized (md5) {
      md5.reset();
      byte[] step2DigestedBytes = md5.digest(step2Bytes);
      step2Digested = digest2HexString(step2DigestedBytes);
    }

    // see 3.2.2.1 in RFC 2617
    StringBuilder step3 = new StringBuilder();
    // "auth-int" would be allowed here to but that is not supported at the moment
    if (QOP_VALUE_AUTH.equals(qop)) {
      step3.append(step1Digested);
      step3.append(":");
      step3.append(nonce);
      step3.append(":");
      step3.append(ncValue);
      step3.append(":");
      step3.append(cnonce);
      step3.append(":");
      step3.append(qop);
      step3.append(":");
      step3.append(step2Digested);
    } else {
      step3.append(step1Digested);
      step3.append(":");
      step3.append(nonce);
      step3.append(":");
      step3.append(step2Digested);
    }
    byte[] step3Bytes = step3.toString().getBytes(); // FIXME encoding?
    String step3Digested;
    synchronized (md5) {
      md5.reset();
      byte[] step3DigestedBytes = md5.digest(step3Bytes);
      step3Digested = digest2HexString(step3DigestedBytes);
    }

    return step3Digested;

  }


  /**
   * Extract of RFC 2617: <br>
   * <br>
   * ...<br>
   * <br>
   * nonce<br>
   * A server-specified data string which should be uniquely generated each time a 401 response is made. It is
   * recommended that this string be base64 or hexadecimal data. Specifically, since the string is passed in the header
   * lines as a quoted string, the double-quote character is not allowed.<br>
   * <br>
   * The contents of the nonce are implementation dependent. The quality of the implementation depends on a good choice.
   * A nonce might, for example, be constructed as the base 64 encoding of<br>
   * <br>
   * <code>time-stamp H(time-stamp ":" ETag ":" private-key)</code><br>
   * <br>
   * ...<br>
   * <br>
   * We use a variation (using <code>H=MD5</code>):<br>
   * <br>
   * <code>MD5("&lt;client address&gt;":"&lt;current system time since 1970&gt;":"&lt;private key&gt;")</code>
   */
  static String generateNonce(String algorithm, String clientAddress) {

    if (algorithm == null) {
      algorithm = HTTPTriggerConnection.MD5_ALGORITHM;
    }

    MessageDigest md5 = null;
    try {
      md5 = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Unknown algorithm: '" + algorithm + "'");
    }

    long currentTime = System.currentTimeMillis();
    String nOnceValue = new StringBuilder().append(clientAddress).append(":").append(currentTime)
                    .append(":" + NONCE_GENERATION_PRIVATE_KEY).toString();

    synchronized (md5) {
      md5.reset();
      byte buffer[] = md5.digest(nOnceValue.getBytes());
      return digest2HexString(buffer);
    }

  }


  private static String digest2HexString(byte[] digest) {
    StringBuilder digestStringBuilder = new StringBuilder();
    int low, hi;

    for (int i = 0; i < digest.length; i++) {
      low = (digest[i] & 0x0f);
      hi = ((digest[i] & 0xf0) >> 4);
      digestStringBuilder.append(Integer.toHexString(hi));
      digestStringBuilder.append(Integer.toHexString(low));
    }

    return digestStringBuilder.toString();
  }


/**
   * Creates a header object according to the following rules:
   * 
   * <li>challenge        =  "Digest" digest-challenge
   *
   * <li>digest-challenge  = 1#( realm | [ domain ] | nonce | [ opaque ] | [ stale ] | [ algorithm ] | [ qop-options ] | [auth-param] )
   *
   * <li>domain            = "domain" "=" <"> URI ( 1*SP URI ) <">
   * <li>URI               = absoluteURI | abs_path
   * <li>nonce             = "nonce" "=" nonce-value
   * <li>nonce-value       = quoted-string
   * <li>opaque            = "opaque" "=" quoted-string
   * <li>stale             = "stale" "=" ( "true" | "false" )
   * <li>algorithm         = "algorithm" "=" ( "MD5" | "MD5-sess" | token )
   * <li>qop-options       = "qop" "=" <"> 1#qop-value <">
   * <li>qop-value         = "auth" | "auth-int" | token</li>
   *
   * For more details see 3.2.1 in RFC 2617
   */
  static Properties createDigestAuthenticationHeader(String algorithm, String realm, InetAddress address) {

    String nonce = DigestAuthentificationUtilities.generateNonce(algorithm, address.toString());
    Properties header = new Properties();

    // build the header according to the rfc, separating the fields by commas
    StringBuilder challengeBuilder = new StringBuilder();
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_DIGEST).append(" ");
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_REALM).append("=\"").append(realm).append("\"");
    challengeBuilder.append(",");
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_NONCE + "=\"").append(nonce).append("\"");
    challengeBuilder.append(",");
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_OPAQUE).append("=\"" + DIGEST_AUTH_CONSTANT_OPAQUE_FIELD + "\"");
    challengeBuilder.append(",");

    // this ok with the RFC for now: if user and password match but the response is not valid any longer,
    // this shoudl be "true". However, according to the current implementation, responses are always valid
    // if user and password match the delivered response.
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_STALE + "=\"false\"");

    challengeBuilder.append(",");
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_ALGORITHM).append("=\"").append(algorithm).append("\"");
    challengeBuilder.append(",");
    challengeBuilder.append(DIGEST_AUTH_HEADERFIELD_QOP + "=\"" + QOP_VALUE_AUTH + "\"");

    header.put(HTTPTriggerConnection.PROP_KEY_WWW_AUTHENTICATE, challengeBuilder.toString());
    header.put(HTTPTriggerConnection.PROP_KEY_CONNECTION, "close");
    return header;

  }

}
