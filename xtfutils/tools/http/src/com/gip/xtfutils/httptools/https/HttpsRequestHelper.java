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

package com.gip.xtfutils.httptools.https;


public class HttpsRequestHelper {

  public static void setSystemPropertyTruststorePath(String path) {
    System.setProperty("javax.net.ssl.trustStore", path);
  }

  public static void setSystemPropertyTruststorePassword(String password) {
    System.setProperty("javax.net.ssl.trustStorePassword", password);
  }

  public static void setSystemPropertyKeystorePath(String path) {
    System.setProperty("javax.net.ssl.keyStore", path);
  }

  public static void setSystemPropertyKeystorePassword(String password) {
    System.setProperty("javax.net.ssl.keyStorePassword", password);
  }


}
