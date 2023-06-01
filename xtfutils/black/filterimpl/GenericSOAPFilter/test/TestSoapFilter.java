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

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

import com.gip.xtfutils.httptools.http.*;
import com.gip.xtfutils.httptools.soap.SoapInput;
import com.gip.xtfutils.httptools.soap.SoapTools;
import com.gip.xtfutils.xmltools.nav.jdom.SoapBuilder;


public class TestSoapFilter {

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

    SoapInput inp = new SoapInput();
    inp.setPayload(adapted);
    inp.setUrl("http://10.0.10.137:5250/DummyWS");
    HttpResponse resp = SoapTools.invokeWebservice(inp);
    _logger.info(resp);
  }

  public static void test2() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    _logger.debug("Read file: \n " + xml);
    //String adapted = SoapBuilder.addSoapEnvelope(xml);
    //_logger.info("Adapted xml: \n " + adapted);

    SoapInput inp = new SoapInput();
    inp.setPayload(xml);
    inp.setUrl("http://10.0.10.45:8081/filter");
    HttpResponse resp = SoapTools.invokeWebservice(inp);
    _logger.info(resp);
  }

  public static void test3() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    //String adapted = SoapBuilder.addSoapEnvelope(xml);
    //_logger.info("Adapted xml: \n " + adapted);

    HttpPostInput inp = new HttpPostInput();
    inp.setPayload(xml);
    inp.setUrl("http://10.0.10.20:1243/filter");
    HttpResponse resp = HttpPost.execute(inp);
    _logger.info(resp);
  }

  public static void main(String[] args) {
    try {
      test3();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
