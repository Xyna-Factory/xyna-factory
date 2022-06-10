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

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

import com.gip.xtfutils.xmltools.nav.jdom.SoapBuilder;



public class TestSoapBuilder {

  public static Logger _logger = Logger.getLogger(TestSoapBuilder.class);

  public static String readFile(String filename) throws Exception {
    try {
      String line;
      StringBuilder builder = new StringBuilder("");
      BufferedReader f = new BufferedReader(new FileReader(filename));
      while ((line = f.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    } catch (Exception e) {
      throw e;
    }
  }

  public static void test1() throws Exception {
    String xml = readFile("test/data/return_isi.xml");
    _logger.debug("Read file: \n " + xml);
    String adapted = SoapBuilder.addSoapEnvelope(xml);
    _logger.info("Adapted xml: \n " + adapted);
  }

  public static void test2() throws Exception {
    String xml = readFile("test/data/return_isi.xml");
    _logger.debug("Read file: \n " + xml);
    String adapted = SoapBuilder.removeOptionalSoapEnvelope(xml);
    _logger.info("Adapted xml: \n " + adapted);
  }

  public static void test3() throws Exception {
    String xml = readFile("test/data/return_isi_soap.xml");
    _logger.debug("Read file: \n " + xml);
    String adapted = SoapBuilder.removeOptionalSoapEnvelope(xml);
    _logger.info("Adapted xml: \n " + adapted);
  }

  public static void test4() throws Exception {
    String xml = "<dummy/>";
    String adapted = SoapBuilder.addSoapEnvelopeWithWsSecData(xml, "myuser", "mypassword", "myid1");
    _logger.info("Adapted xml: \n " + adapted);
  }

  public static void main(String[] args) {
    try {
      test4();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
