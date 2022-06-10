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
package com.gip.xyna.xact.filter.actions.auth;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;



/**
 * http (reverse) proxy schickt client zertifikat als payload an xyna. z.b. könnte das der apache machen.
 * vgl https://tomcat.apache.org/tomcat-8.5-doc/api/org/apache/catalina/valves/SSLValve.html (beschreibung, wie das im tomcat terminiert)
 * 
 * beispiel request:
 * GET /FractalModeller/ HTTP/1.1
Host: 10.0.10.141:7443
User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,* /*;q=0.8
Accept-Language: de,en-US;q=0.7,en;q=0.3
Accept-Encoding: gzip, deflate, br
DNT: 1
Upgrade-Insecure-Requests: 1
If-Modified-Since: Mon, 06 Aug 2018 07:44:55 GMT
If-None-Match: W/"4416-1533541495000"
Cache-Control: max-age=0
SSL_CLIENT_CERT: -----BEGIN CERTIFICATE----- MIIDiTCCAnECCQDtSB9W0GaqOTANBgkqhkiG9w0BAQUFADCBgzELMAkGA1UEBhMC REUxGDAWBgNVBAgMD1JoZWlubGFuZCBQZmFsejEOMAwGA1UEBwwFTWFpbnoxDDAK BgNVBAoMA0dJUDEMMAoGA1UECwwDREVWMREwDwYDVQQDDAh2bWxpbjA1NzEbMBkG CSqGSIb3DQEJARYMYXhlbEBoaWVyLmRlMB4XDTE1MDYxMjEyMTQzNVoXDTI1MDQy MDEyMTQzNVowgYgxDzANBgNVBAMMBkxldmVsMTEOMAwGA1UECwwFVXNlcnMxETAP BgNVBAsMCEN1c3RvbWVyMRkwFwYKCZImiZPyLGQBGRYJbmdzc20tZ2lwMRIwEAYK CZImiZPyLGQBGRYCenoxIzAhBgkqhkiG9w0BCQEWFEdJUC5MZXZlbDFAZ2lwLmxv Y2FsMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyIFBkqZqSg4aylyI Uyv7lwklN60mLPwPP6Wksz+QE8Btfv8L6sFcoSzzHYwhDg5Tn5sD10p9WX9z0Mta mWg69oRtOiGbfJwswWZvCF1qD2U+bhiADsBlPdrFnjjYk8kaNGlamX+kLqv5mdK6 cStILuqj5nXlpZ8kidqJAuyh5EQoD8c26t3lSlFMo9Sa+Rz8HrVkM8xGCq24FS+G F6tvLFGxHegeWUR4Ty+luzFK9CHJhsXcB9CyPsPe1ibWp7i94CSazKv6YcLid3ZE 6k4S8XCJhh9s01zVAG0eP34V765XqCeZNnxVIA4xsUV6/P1PCqrCCPY2OJGMykF1 OClGgQIDAQABMA0GCSqGSIb3DQEBBQUAA4IBAQC2qk2h6UIAmLZQSrQkleiFRzIP EAMvBttVUpIksHoUEVeKiNajhJFjDsAN0DXnzzlCW2GiJyQXulbiI1rHCqMRK0b6 WSst7/HMV2xBsmAEYGrMNDtLpcjEkttwbEwRm8rtIquqQolp+XOPyRPEqXLe2wtz j2MfO9lCe5Tj89XInFW8sj9F4SwNWHCjOyoYBENy1I2dhfUG0UcG1sWQcVAggKQO fleYGSpS6DaENp9sttR0M0OxDVYuygqBG767jZaAHxdjykzgiEyO2tLCOzfTIUN3 0npArZ96b9p7pELz93RZJdQZCqopp9we5kITqxjSkN15QHuah6Vyq23hOUhb -----END CERTIFICATE----- 
SSL_CIPHER: ECDHE-RSA-AES128-GCM-SHA256
SSL_SESSION_ID: 90247e6aeb59ea2f424e656497dc29369a5d1b6b27b766b79f8561a6d2d118fb
SSL_CIPHER_USEKEYSIZE: 128
X-Forwarded-For: 
X-Forwarded-Host: 
X-Forwarded-Server: 
Connection: Keep-Alive
 * 
 *
 * Response:
 * {
 "username":"ME.Level1@myCorp.local",
 "userdisplayname":"ME.Level1",
 "externaldomains": ["MY_DOMAIN"]
  }
 * 
 * Response wenn kein Zert vorhanden:
 * {}
 * 
 * Response bei ungültigem Zert:
 * <Error-Response>
 * 
 */
public class ExternalUserLoginInformationAction implements FilterAction {


  private static final String USERNAME = "username";
  private static final String DISPLAY_NAME = "userdisplayname";
  private static final String EXTERNAL_DOMAINS = "externaldomains";


  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/auth/externalUserLoginInformation") && Method.GET == method;
  }


  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    Pair<Boolean, ExternalUserInfo> p = ExternalUserLoginAction.getExternalUserInfoOrFail(jfai, tc);
    if (p.getFirst()) {
      return jfai;
    }
    ExternalUserInfo eui = p.getSecond();
    if (eui == null) {
      jfai.sendJson(tc, "{}");
      return jfai;
    }

    JsonBuilder jb = new JsonBuilder();
    jb.startObject();
    jb.addStringAttribute(USERNAME, eui.externalUserName);
    jb.addStringAttribute(DISPLAY_NAME, eui.externalUserDisplayName);

    List<String> domains = new ArrayList<>();
    for (Domain d : XynaFactory.getInstance().getFactoryManagement().getDomains()) {
      if (d.getDomainTypeAsEnum() != DomainType.LOCAL) {
        domains.add(d.getName());
      }
    }
    jb.addStringListAttribute(EXTERNAL_DOMAINS, domains);
    jb.endObject();

    jfai.sendJson(tc, jb.toString());
    return jfai;
  }


  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Login Information";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }


}
