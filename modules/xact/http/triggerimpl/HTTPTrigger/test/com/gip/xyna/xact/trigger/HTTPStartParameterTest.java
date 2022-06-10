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

package com.gip.xyna.xact.trigger;

import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.HTTPStartParameter.InterfaceProtocolPreference;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_HTTP_INVALID_STARTPARAMETERS;

import junit.framework.TestCase;



public class HTTPStartParameterTest extends TestCase {

  public void test1StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123"});
    assertEquals(param.getPort(), 123);

  }


  public void test2StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");

  }


  public void test3StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "ipv4"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV4);

    param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "ipv6"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV6);

    try {
      param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123", "interfaceName123", "ipv9"});
      fail("Invalid value 'ipv9' accepted as protocol preference");
    } catch (HTTPTRIGGER_HTTP_INVALID_STARTPARAMETERS e) {
      // expected
    }

  }


  public void test6StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "true", "none", "keystorePath", "keystorePassword"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV4);
    assertEquals(param.useHTTPs(), true);
    assertEquals(param.getClientAuth(), "none");
    assertEquals(param.getKeyStorePath(), "keystorePath");
    assertEquals(param.getKeyStorePassword(), "keystorePassword");

    try {
      param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123", "interfaceName123", "ipv4",
                      "none", "keystorePath", "keystorePassword"});
      fail("Unexpected value 'ipv4' accepted in inappropriate place");
    } catch (HTTPTRIGGER_HTTP_INVALID_STARTPARAMETERS e) {
      // expected
    }

  }


  public void test7StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "ipv6", "true", "none", "keystorePath", "keystorePassword"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV6);
    assertEquals(param.useHTTPs(), true);
    assertEquals(param.getClientAuth(), "none");
    assertEquals(param.getKeyStorePath(), "keystorePath");
    assertEquals(param.getKeyStorePassword(), "keystorePassword");

    param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "false", "none", "keystorePath", "keystorePassword", "type123"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV4);
    assertEquals(param.useHTTPs(), false);
    assertEquals(param.getClientAuth(), "none");
    assertEquals(param.getKeyStorePath(), "keystorePath");
    assertEquals(param.getKeyStorePassword(), "keystorePassword");
    assertEquals(param.getKeyStoreType(), "type123");

    try {
      param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123", "interfaceName123", "ipv4",
                      "none", "keystorePath", "keystorePassword", "JKS"});
      fail("Unexpected value 'ipv4' accepted in inappropriate place");
    } catch (HTTPTRIGGER_HTTP_INVALID_STARTPARAMETERS e) {
      // expected
    }

  }


  public void test8StartParameter() throws XACT_InvalidStartParameterCountException,
                  XACT_InvalidTriggerStartParameterValueException {

    HTTPStartParameter param = (HTTPStartParameter) new HTTPStartParameter().build(new String[] {"123",
                    "interfaceName123", "ipv6", "true", "none", "keystorePath", "keystorePassword", "type123"});
    assertEquals(param.getPort(), 123);
    assertEquals(param.getAddress(), "interfaceName123");
    assertEquals(param.getProtocolPreference(), InterfaceProtocolPreference.IPV6);
    assertEquals(param.useHTTPs(), true);
    assertEquals(param.getClientAuth(), "none");
    assertEquals(param.getKeyStorePath(), "keystorePath");
    assertEquals(param.getKeyStorePassword(), "keystorePassword");
    assertEquals(param.getKeyStoreType(), "type123");

  }
}
