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
package com.gip.xyna.utils.ldap;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import sun.misc.BASE64Decoder;



/**
 * CertUtils
 */
public class CertUtils {

  public static String certToString(X509Certificate cert) {
    String retVal= "Certificate-Infos";
    retVal+="\n  issuer DN                 : " + cert.getIssuerDN().getName();
    retVal+="\n  subject DN                : " + cert.getSubjectDN().getName();
    retVal+="\n  start date                : " + cert.getNotBefore();
    retVal+="\n  end date                  : " + cert.getNotAfter();
    retVal+="\n  serial number             : " + cert.getSerialNumber();
    retVal+="\n  signature algorigthm name : " + cert.getSigAlgName();
    retVal+="\n  version                   : " + cert.getVersion();
    retVal+="\n  type                      : " + cert.getType();
    retVal+="\n  OID                       : " + cert.getSigAlgOID();
    return retVal;
  }
  
  public static X509Certificate getDummyCRT1() {
    try {
      X509Certificate clientCRT=getX509Certificate(
        "MIICkDCCAXigAwIBAgICAjIwDQYJKoZIhvcNAQEEBQAwVTELMAkG"+
        "A1UEBhMCREUxDjAMBgNVBAoTBUFyY29yMRgwFgYDVQQLEw9UZXN0IENOIE1hbmFnZXIxHDAaBgNV"+
        "BAMTE0NlcnRpZmljYXRlIE1hbmFnZXIwHhcNMDMxMjA4MTYwNTI3WhcNMDQxMjA3MTYwNTI3WjBB"+
        "MQswCQYDVQQGEwJERTEOMAwGA1UEChMFQXJjb3IxDjAMBgNVBAsTBTE1OTgxMRIwEAYDVQQDEwlt"+
        "cGQuYWRtaW4wXDANBgkqhkiG9w0BAQEFAANLADBIAkEAyhMiOsk2mTCeg3pXrbvPndS+kKFRosZl"+
        "wEfLNtPrMrE6C3IILhCRKa4V4ypvJR3lHu3X4obqIRq/pGLVdop+ZQIDAQABo0YwRDARBglghkgB"+
        "hvhCAQEEBAMCBaAwDgYDVR0PAQH/BAQDAgXgMB8GA1UdIwQYMBaAFKtpxS6Efs5WBx4+/mhvJySY"+
        "kJrQMA0GCSqGSIb3DQEBBAUAA4IBAQBB0Ph1L+ZLSbrmDItQjE7i0VKjDTLexS9u4bTFaydcQ86c"+
        "WZBqWCmxqmWCFOCn9JfMWXVaXmFvKEknNm5y4ax6lryeKKxQtOu5TZDMMgGGfnym8kAGwcjlV/E9"+
        "7caozRK6lo2CvW63rl6TRmPlHNg5/EMPU2ZD+AOeWIy27ueRBcj+WgwmqdwC8xszxh1h92qvQiR6"+
        "hlFOzh3ziap6bfXct0tLdVTo4Ky1eH5ItsDIw2cq/g9+wEIuQRvNkH60n9RhqJkHX2tueN40jdqd"+
        "TCHTPPrNU6WAZN053jnuzlxAxTSWMI9Qoz5nqdOk7/qzWSE3yYV0EH2PM5cGBsMKlWL5");
      return clientCRT;    
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static X509Certificate getDummyCRT2() {
    try {
      X509Certificate clientCRT2=getX509Certificate(
        "MIICkDCCAXigAwIBAgICAjMwDQYJKoZIhvcNAQEEBQAwVTELMAkG"+
        "A1UEBhMCREUxDjAMBgNVBAoTBUFyY29yMRgwFgYDVQQLEw9UZXN0IENOIE1hbmFnZXIxHDAaBgNV"+
        "BAMTE0NlcnRpZmljYXRlIE1hbmFnZXIwHhcNMDMxMjA4MTYwNjMzWhcNMDQxMjA3MTYwNjMzWjBB"+
        "MQswCQYDVQQGEwJERTEOMAwGA1UEChMFQXJjb3IxDjAMBgNVBAsTBTE1OTgxMRIwEAYDVQQDEwlt"+
        "cGQuYWRtaW4wXDANBgkqhkiG9w0BAQEFAANLADBIAkEAyxgg+y+JhBCVrgg2ymbWHeqbzg0cDqi5"+
        "hu6PQHgjjzNRJ6q70DyANSpUCA0hEEMtNRLL60/MoVBiHAjO54bsvQIDAQABo0YwRDARBglghkgB"+
        "hvhCAQEEBAMCBaAwDgYDVR0PAQH/BAQDAgXgMB8GA1UdIwQYMBaAFKtpxS6Efs5WBx4+/mhvJySY"+
        "kJrQMA0GCSqGSIb3DQEBBAUAA4IBAQBOkYkdzme7bopUcNj+9FdmYGx4sGZOgrvBzWxxYRw7oEUf"+
        "RK/bl+5rsthK1EOdZjSJxlmemboWyiIkT5/rDjQQ1qNjfe9LiyNZxwwOXtUrvjSOAjElItyJ94ad"+
        "FzrI1dql5fXnXv6XnhjPMqTErivOFSEgidb2sJJjbpEgciJ2n+V9U+QuMq4iBg/r0VCxeWc6ePA2"+
        "b1JJ7bAzvNd1B2n3aZK0JEYtU2Ob2zfdgUo9Q2HEJQeR+G5eDZ6HCW4VP93CvMdpg0LdINqiIzML"+
        "pXlqPOAOT9CB13EaMdfNEqF4e9bB0ParPT6Cm6UxrVehsZwvWhlOxL07NIdj3RX5zaog");
      return clientCRT2;    
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  /**
   * Liefert das Java-x509-Zertifikat aus einen Base64-PEM-Format.
   * PEM-Format:
   * -----BEGIN CERTIFICATE-----
   * MIIChzCCAfACAQgwDQYJKoZIhvcNAQEEBQAwgZgxCzAJBgNVBAYTAkRFMRAwDgYDVQQIEwdHZXJtYW55MQ4
   * ...
   * xiHBgU2YNEol9e5YhvHOWmbkEmseVg6JNYUuKNvPhLupPcVZ+IKXPjE2p1AdZVvWxsf43LTCiACo9d40h9h
   * -----END CERTIFICATE-----
   * @param trimmedCertificate
   * @return Certificate
   * @throws Exception
   */
  public static X509Certificate getX509Certificate(String trimmedCertificate) throws Exception { 
    String beginCert = "-----BEGIN CERTIFICATE-----";
    String endCert   = "-----END CERTIFICATE-----";
  
    int start = trimmedCertificate.indexOf(beginCert);
    int end = trimmedCertificate.indexOf(endCert);
    
    String mainCertificate;
    // Header/Footer fehlen evtl.
    if (start < 0 && end < 0) {
      mainCertificate = trimmedCertificate;
    }
    else {
      mainCertificate = trimmedCertificate.substring(beginCert.length(), end);
    }
    
    byte data[];
    BASE64Decoder decoder = new BASE64Decoder();
    data = decoder.decodeBuffer(mainCertificate);
    CertificateFactory cF = CertificateFactory.getInstance("X509");
    ByteArrayInputStream bAIS = new ByteArrayInputStream(data);
    X509Certificate cert = (X509Certificate) cF.generateCertificate(bAIS);
    bAIS.close();
    return cert;
  }

}


