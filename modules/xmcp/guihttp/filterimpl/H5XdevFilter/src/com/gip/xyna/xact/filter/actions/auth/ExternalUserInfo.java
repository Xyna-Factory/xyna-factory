/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.actions.auth;



import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.gip.xyna.utils.ByteUtils;



public class ExternalUserInfo {

  private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
  private static final String END_CERT = "-----END CERTIFICATE-----";

  public final String externalUserName;
  public final String externalUserDisplayName;
  public final String externalUserPassword;

  
  private ExternalUserInfo(String externalUserName, String externalUserDisplayName, String externalUserPassword) {
    this.externalUserName = externalUserName;
    this.externalUserDisplayName = externalUserDisplayName;
    this.externalUserPassword = externalUserPassword;
  }


  /* 
  SSL_CLIENT_CERT: -----BEGIN CERTIFICATE----- MIIDiTCCAnECCQDtSB9W0GaqOTANBgkqhkiG9w0BAQUFADCBgzELMAkGA1UEBhMC REUxGDAWBgNVBAgMD1JoZWlubGFuZCBQZmFsejEOMAwGA1UEBwwFTWFpbnoxDDAK BgNVBAoMA0dJUDEMMAoGA1UECwwDREVWMREwDwYDVQQDDAh2bWxpbjA1NzEbMBkG CSqGSIb3DQEJARYMYXhlbEBoaWVyLmRlMB4XDTE1MDYxMjEyMTQzNVoXDTI1MDQy MDEyMTQzNVowgYgxDzANBgNVBAMMBkxldmVsMTEOMAwGA1UECwwFVXNlcnMxETAP BgNVBAsMCEN1c3RvbWVyMRkwFwYKCZImiZPyLGQBGRYJbmdzc20tZ2lwMRIwEAYK CZImiZPyLGQBGRYCenoxIzAhBgkqhkiG9w0BCQEWFEdJUC5MZXZlbDFAZ2lwLmxv Y2FsMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyIFBkqZqSg4aylyI Uyv7lwklN60mLPwPP6Wksz+QE8Btfv8L6sFcoSzzHYwhDg5Tn5sD10p9WX9z0Mta mWg69oRtOiGbfJwswWZvCF1qD2U+bhiADsBlPdrFnjjYk8kaNGlamX+kLqv5mdK6 cStILuqj5nXlpZ8kidqJAuyh5EQoD8c26t3lSlFMo9Sa+Rz8HrVkM8xGCq24FS+G F6tvLFGxHegeWUR4Ty+luzFK9CHJhsXcB9CyPsPe1ibWp7i94CSazKv6YcLid3ZE 6k4S8XCJhh9s01zVAG0eP34V765XqCeZNnxVIA4xsUV6/P1PCqrCCPY2OJGMykF1 OClGgQIDAQABMA0GCSqGSIb3DQEBBQUAA4IBAQC2qk2h6UIAmLZQSrQkleiFRzIP EAMvBttVUpIksHoUEVeKiNajhJFjDsAN0DXnzzlCW2GiJyQXulbiI1rHCqMRK0b6 WSst7/HMV2xBsmAEYGrMNDtLpcjEkttwbEwRm8rtIquqQolp+XOPyRPEqXLe2wtz j2MfO9lCe5Tj89XInFW8sj9F4SwNWHCjOyoYBENy1I2dhfUG0UcG1sWQcVAggKQO fleYGSpS6DaENp9sttR0M0OxDVYuygqBG767jZaAHxdjykzgiEyO2tLCOzfTIUN3 0npArZ96b9p7pELz93RZJdQZCqopp9we5kITqxjSkN15QHuah6Vyq23hOUhb -----END CERTIFICATE----- 
  SSL_CIPHER: ECDHE-RSA-AES128-GCM-SHA256
  SSL_SESSION_ID: 90247e6aeb59ea2f424e656497dc29369a5d1b6b27b766b79f8561a6d2d118fb
  SSL_CIPHER_USEKEYSIZE: 128
  */

  public static ExternalUserInfo createFromClientCertificate(String client_cert) throws CertificateException {
    if (client_cert == null) {
      return null;
    }
    client_cert = client_cert.trim();
    if (client_cert.length() < BEGIN_CERT.length() + 1) {
      return null;
    }
    client_cert = BEGIN_CERT + "\n"
        + client_cert.substring(BEGIN_CERT.length() + 1, client_cert.length() - END_CERT.length() - 1).replace(' ', '\n') + "\n" + END_CERT;

    ByteArrayInputStream bais;
    try {
      bais = new ByteArrayInputStream(client_cert.getBytes("ISO-8859-1"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    CertificateFactory cf;
    try {
      cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(bais);

      //TODO will man das wie früher im webservice konfigurierbar machen, welche daten aus dem zertifikat ausgelesen werden?
      BigInteger serialnumber = cert.getSerialNumber();
      String dnName = cert.getSubjectDN().getName();
      LdapName ldapname;
      try {
        ldapname = new LdapName(dnName);
      } catch (InvalidNameException e) {
        throw new RuntimeException(e);
      }
      String email = getFromLdapName(ldapname, "emailaddress");
      if (email == null) {
        throw new CertificateException("Email address not found in certificate subjectdn");
      }
      String user_displayname = email.substring(0, email.indexOf('@'));
      String pw = ByteUtils.toHexString(serialnumber.toByteArray(), false, ":");
      return new ExternalUserInfo(email, user_displayname, pw);
    } catch (CertificateException e) {
      throw e;
    }
  }


  private static String getFromLdapName(LdapName ldapname, String key) {
    for (Rdn rdn : ldapname.getRdns()) {
      if (rdn.getType().equalsIgnoreCase(key)) {
        return rdn.getValue().toString();
      }
    }
    return null;
  }

}
