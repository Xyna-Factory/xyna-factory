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

package com.gip.xyna.xact.trigger.ssl;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;


public class WebSphereMQTriggerSSLConfigBuilder implements SSLConfigBuilder {

  public static class Constant {
    public static class XynaPropertyNameSuffix {
      public static final String TRUSTSTORE_PATH = "truststore.path";
      public static final String TRUSTSTORE_PASSWORD = "truststore.password";
      public static final String KEYSTORE_PATH = "keystore.path";
      public static final String KEYSTORE_PASSWORD = "keystore.password";
      public static final String CIPHER_SUITE = "cipher.suite";
    }
  }

  public static class XynaPropertyPrefixForSSLKeystoreConfig {
    private final String prefix;
    public XynaPropertyPrefixForSSLKeystoreConfig(String prefixIn) {
      this.prefix = prefixIn;
    }
    public String getPrefix() {
      return prefix;
    }
  }

  private static final Logger _logger = Logger.getLogger(WebSphereMQTriggerSSLConfigBuilder.class);
  private final XynaPropertyPrefixForSSLKeystoreConfig _prefix;


  public WebSphereMQTriggerSSLConfigBuilder(XynaPropertyPrefixForSSLKeystoreConfig prefixIn) {
    this._prefix = prefixIn;
  }


  public SSLConfig build() {
    SSLConfig ret = new SSLConfig();
    ret.setKeystorePassword(buildAndReadProperty(Constant.XynaPropertyNameSuffix.KEYSTORE_PASSWORD));
    ret.setKeystorePath(buildAndReadProperty(Constant.XynaPropertyNameSuffix.KEYSTORE_PATH));
    ret.setTruststorePassword(buildAndReadProperty(Constant.XynaPropertyNameSuffix.TRUSTSTORE_PASSWORD));
    ret.setTruststorePath(buildAndReadProperty(Constant.XynaPropertyNameSuffix.TRUSTSTORE_PATH));
    ret.setCipherSuiteName(buildAndReadProperty(Constant.XynaPropertyNameSuffix.CIPHER_SUITE));
    return ret;
  }


  private String buildAndReadProperty(String suffix) {
    String key = _prefix.getPrefix() + "." + suffix;
    _logger.debug("Reading xyna property '" + key + "'");
    String val = getXynaProperty(key);
    return val;
  }


  private static String getXynaProperty(String propname) {
    String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().
                    getConfiguration().getProperty(propname);
    if ((val == null) || (val.trim().length() < 1)) {
      throw new RuntimeException("Xyna Property " + propname + " is empty.");
    }
    return val;
  }

}
