/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.HTTPStartParameter.ClientAuth;
import com.gip.xyna.xact.trigger.HTTPStartParameter.InterfaceProtocolPreference;
import com.gip.xyna.xact.trigger.HTTPStartParameter.KeyStoreParameter;



public class HTTPStartParameterTest {

  public Map<String, Object> paramMap = new HashMap<String, Object>() {

    {
      put("port", 123);
      put("address", "interfaceName123");
      put("protocol", InterfaceProtocolPreference.IPV4);
      put("https", KeyStoreParameter.KEY_MGMT);
      put("clientauth", ClientAuth.require);
      put("keystorepath", "/some/path");
      put("keystorepasswd", "keystorePassword");
      put("keystoretype", "type123");
      put("ssl", "TLSv1.2");
    }
  };


  @Test
  public void testStartParameters() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    HTTPStartParameter startParam = (HTTPStartParameter) new HTTPStartParameter().build(paramMap);
    assertEquals(123, startParam.getPort());
    assertEquals("interfaceName123", startParam.getAddress());
    assertEquals(InterfaceProtocolPreference.IPV4, startParam.getProtocolPreference());
    assertEquals(true, startParam.useHTTPs());
    assertEquals(ClientAuth.require, startParam.getClientAuth());
    assertEquals("/some/path", startParam.getKeyStorePath());
    assertEquals("keystorePassword", startParam.getKeyStorePassword());
    assertEquals("type123", startParam.getKeyStoreType());
    assertEquals("TLSv1.2", startParam.getSSLContextAlgorithm());
  }


  @Test
  public void test1StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123"));
    assertEquals(Arrays.asList("port=123"), startParam);
  }


  @Test
  public void test2StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123"), startParam);
  }


  @Test(expected = XACT_InvalidTriggerStartParameterValueException.class)
  public void test3StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv4"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV4"), startParam);

    startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv6"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV6"), startParam);

    startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv9"));
    fail("Invalid value 'ipv9' accepted as protocol preference");
  }


  @Test(expected = XACT_InvalidTriggerStartParameterValueException.class)
  public void test6StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "true", "none", "keystorePath", "keystorePassword"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "https=TRUE", "clientauth=none", "keystorepath=keystorePath",
                               "keystorepasswd=keystorePassword"),
                 startParam);

    startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv4", "none", "keystorePath", "keystorePassword"));
    fail("Unexpected value 'ipv4' accepted in inappropriate place");
  }


  @Test(expected = XACT_InvalidTriggerStartParameterValueException.class)
  public void test7StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv6", "true", "none", "keystorePath", "keystorePassword"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV6", "https=TRUE", "clientauth=none",
                               "keystorepath=keystorePath", "keystorepasswd=keystorePassword"),
                 startParam);

    startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "true", "none", "keystorePath", "keystorePassword", "type123"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "https=TRUE", "clientauth=none", "keystorepath=keystorePath",
                               "keystorepasswd=keystorePassword", "keystoretype=type123"),
                 startParam);

    startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "true", "none", "keystorePath", "keystorePassword", "tlsv1.2"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "https=TRUE", "clientauth=none", "keystorepath=keystorePath",
                               "keystorepasswd=keystorePassword", "ssl=TLSv1.2"),
                 startParam);

    startParam = new HTTPStartParameter()
        .convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv4", "none", "keystorePath", "keystorePassword", "JKS"));
    fail("Unexpected value 'ipv4' accepted in inappropriate place");
  }


  @Test(expected = XACT_InvalidTriggerStartParameterValueException.class)
  public void test8StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter().convertToNewParameters(Arrays
        .asList("123", "interfaceName123", "ipv6", "true", "none", "keystorePath", "keystorePassword", "type123"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV6", "https=TRUE", "clientauth=none",
                               "keystorepath=keystorePath", "keystorepasswd=keystorePassword", "keystoretype=type123"),
                 startParam);

    startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "true", "none", "keystorePath",
                                                                               "keystorePassword", "type123", "tlsv1"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "https=TRUE", "clientauth=none", "keystorepath=keystorePath",
                               "keystorepasswd=keystorePassword", "keystoretype=type123", "ssl=TLSv1"),
                 startParam);

    startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "IPV4", "true", "none",
                                                                               "keystorePath", "keystorePassword", "tls1"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV4", "https=TRUE", "clientauth=none",
                               "keystorepath=keystorePath", "keystorepasswd=keystorePassword", "ssl=TLS1"),
                 startParam);

    startParam = new HTTPStartParameter().convertToNewParameters(Arrays.asList("123", "interfaceName123", "ipv4", "none", "keystorePath",
                                                                               "keystorePassword", "TLSv1.3", "JKS"));
    fail("Unexpected value 'TLSv1.3' accepted in inappropriate place");
  }


  @Test
  public void test9StartParameter() throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    List<String> startParam = new HTTPStartParameter().convertToNewParameters(Arrays
        .asList("123", "interfaceName123", "ipv6", "true", "none", "keystorePath", "keystorePassword", "type123", "TLS"));
    assertEquals(Arrays.asList("port=123", "address=interfaceName123", "protocol=IPV6", "https=TRUE", "clientauth=none",
                               "keystorepath=keystorePath", "keystorepasswd=keystorePassword", "keystoretype=type123", "ssl=TLS"),
                 startParam);
  }
}
