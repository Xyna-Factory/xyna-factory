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
package com.gip.xyna.utils.mail;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * DummyTrustManager: accepts everything without checking idendity, integrity or
 * anything. Please replace by a more sophisticated version. Similar applies to
 * DummySSLSocketFactory. Both accept/do their job without checking the input or
 * goal of their invocation - that is why they are called "Dummy".
 * 
 * 
 */
public class DummyTrustManager implements X509TrustManager {

   /*
    * public boolean isClientTrusted(X509Certificate[] cert) { return true; }
    * 
    * public boolean isServerTrusted(X509Certificate[] cert) { return true; }
    * 
    * public X509Certificate[] getAcceptedIssuers() { return new
    * X509Certificate[0]; }
    */

   // FIXME: behavior could have been changed
   /*
    * (non-Javadoc)
    * 
    * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[],
    *      java.lang.String)
    */
   public void checkClientTrusted(X509Certificate[] chain, String authType) {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[],
    *      java.lang.String)
    */
   public void checkServerTrusted(X509Certificate[] chain, String authType) {
      // TODO Auto-generated method stub

   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
    */
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }
}
