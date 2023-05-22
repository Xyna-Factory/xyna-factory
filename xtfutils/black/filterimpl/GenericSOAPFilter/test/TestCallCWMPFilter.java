import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

import com.gip.xtfutils.httptools.http.HttpPost;
import com.gip.xtfutils.httptools.http.HttpPostInput;
import com.gip.xtfutils.httptools.http.HttpResponse;
import com.gip.xtfutils.httptools.soap.SoapInput;
import com.gip.xtfutils.httptools.soap.SoapTools;
import com.gip.xtfutils.xmltools.nav.jdom.SoapBuilder;

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

public class TestCallCWMPFilter {

  public static Logger _logger = Logger.getLogger(TestCallCWMPFilter.class);

  public static class Sender implements Runnable {
    private String _xml;
    private int _index;
    public Sender(String xml, int index) {
      _xml = xml;
      _index = index;
    }
    public void run() {
      try {
        HttpPostInput inp = new HttpPostInput();
        inp.setPayload(_xml);
        inp.setUrl("http://10.0.10.45:8081/filter");
        _logger.info("Going to send in thread " + _index);
        HttpResponse resp = HttpPost.execute(inp);
        _logger.info("Response in thread " + _index + ":\n" + resp);
      }
      catch (Exception e) {
        _logger.error("Error in thread " + _index, e);
      }
    }
  }

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
    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    //String adapted = SoapBuilder.addSoapEnvelope(xml);
    //_logger.info("Adapted xml: \n " + adapted);

    HttpPostInput inp = new HttpPostInput();
    inp.setPayload(xml);
    inp.setUrl("http://10.0.10.45:8081/filter");
    HttpResponse resp = HttpPost.execute(inp);
    _logger.info(resp);
  }

  public static void test2() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    Thread t1 = new Thread(new Sender(xml, 0));
    Thread t2 = new Thread(new Sender(xml, 1));
    t1.start();

    Thread.sleep(500);
    t2.start();
  }

  public static void main(String[] args) {
    try {
      test2();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }


}
