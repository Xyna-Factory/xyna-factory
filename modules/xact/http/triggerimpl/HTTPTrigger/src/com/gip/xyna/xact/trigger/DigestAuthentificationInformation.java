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

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public final class DigestAuthentificationInformation implements Serializable {

  private static final long serialVersionUID = 1L;
  private static volatile ConcurrentHashMap<String, Pattern> authorizationParsingPatterns = new ConcurrentHashMap<String, Pattern>();


  private volatile boolean authenticated = false;

  private final String username, realm, nonce, qop, uri, cnonce, nc, receivedResponse, algorithm, method;


  DigestAuthentificationInformation(String auth, String method) {

    // TODO performance: it should increase performance if the comma separated list was parsed only once
    String algorithm = parseStringValueOutOfCommaSeparatedKeyValueList(auth, "algorithm");
    if (algorithm == null) {
      this.algorithm = HTTPTriggerConnection.MD5_ALGORITHM;
    } else {
      this.algorithm = algorithm;
    }

    this.username = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_USERNAME);
    this.realm = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_REALM);
    this.nonce = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_NONCE);
    this.uri = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_URI);
    this.qop = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_QOP);
    this.nc = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_NONCE_COUNT);
    this.cnonce = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_CNONCE);

    this.receivedResponse = parseStringValueOutOfCommaSeparatedKeyValueList(auth, DigestAuthentificationUtilities.DIGEST_AUTH_RESPONSEFIELD_RESPONSE);

    this.method = method;

  }


  public synchronized void reevaluate(String password) {
    this.authenticated = DigestAuthentificationUtilities.checkAuthentificationHeader(this, password);
  }


  public boolean isAuthenticated() {
    return authenticated;
  }


  public String getUsername() {
    return username;
  }


  public String getRealm() {
    return realm;
  }


  public String getNonce() {
    return nonce;
  }


  public String getQop() {
    return qop;
  }


  public String getUri() {
    return uri;
  }


  public String getCnonce() {
    return cnonce;
  }


  public String getNc() {
    return nc;
  }


  public String getReceivedResponse() {
    return receivedResponse;
  }


  public String getAlgorithm() {
    return algorithm;
  }


  public String getMethod() {
    return method;
  }


  /**
   * Parses values out of a list of provided parameters gibt null zur�ck, falls kein key value paar vorhanden ist
   */
  static String parseStringValueOutOfCommaSeparatedKeyValueList(String keyValueList, String key) {

    // find out if the key is contained at all, and if so, find out whether we can chop everything before
    // its first occurence
    int firstIndex = keyValueList.indexOf(key);
    String toBeUsed;
    boolean keyIsLeading = false;
    if (firstIndex < 0) {
      return null;
    } else {
      if (firstIndex == 0) {
        toBeUsed = keyValueList;
        keyIsLeading = true;
      } else {
        String tmp;
        while (keyValueList.charAt(firstIndex - 1) != ',' && keyValueList.charAt(firstIndex - 1) != ' ') {
          tmp = keyValueList.substring(firstIndex + 1);
          int index = tmp.indexOf(key);
          if (index < 0) {
            return null;
          }
          firstIndex = firstIndex + 1 + index;
        }
        toBeUsed = keyValueList.substring(firstIndex);
        keyIsLeading = true;
      }
    }

    if (keyIsLeading) {
      // chop everything that definitely does not belong to the expression after the value
      // we might as well do even more if the target value is not surrounded by \"...\" but the performance gain
      // does not seem to be worth it; however, if performance is an issue, this might lead to slight improvements
      int definitelyLastIndex1 = toBeUsed.indexOf("\"");
      if (definitelyLastIndex1 > 0) {
        String tmp = toBeUsed.substring(definitelyLastIndex1 + 1);
        int definitelyLastIndex = tmp.indexOf("\"") + 1;
        if (definitelyLastIndex > definitelyLastIndex1) {
          int length = definitelyLastIndex + definitelyLastIndex1 + 1;
          toBeUsed = toBeUsed.substring(0, length > toBeUsed.length() ? toBeUsed.length() : length);
        }
      }
    }

    Pattern p = authorizationParsingPatterns.get(key);
    if (p == null) {
      synchronized (authorizationParsingPatterns) {
        p = authorizationParsingPatterns.get(key);
        if (p == null) {
          // the patternToMatch is what we want but the String to match to may have a suffix that we did
          // not get rid of above (if the target value is not surrounded by \"...\")
          String patternToMatch = "\\s*" + Pattern.quote(key) + "\\s*=\\s*(?:\"([^\"]*)\"|([^,\" ]*))\\s*";
          String eventualSuffix = "(?:,\\s*.*\\s*=\\s*(?:\"[^\"]*\"|[^,\" ]*)\\s*)*";
          p = Pattern.compile(patternToMatch + eventualSuffix);
          authorizationParsingPatterns.put(key, p);
        }
      }
    }

    Matcher m;
    synchronized (p) {
      m = p.matcher(toBeUsed);
    }
    
    if (m.matches()) {
      String result = m.group(1);
      if (result != null) {
        return result;
      }
      result = m.group(2);
      return result;
    } else {
      return null;
    }

  }

}
