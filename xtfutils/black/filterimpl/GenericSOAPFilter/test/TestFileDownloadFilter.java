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

import org.apache.log4j.Logger;

import com.gip.xtfutils.httptools.http.HttpPost;
import com.gip.xtfutils.httptools.http.HttpPostInput;
import com.gip.xtfutils.httptools.http.HttpResponse;
import com.gip.xtfutils.httptools.soap.SoapTools;



public class TestFileDownloadFilter {

  public static Logger _logger = Logger.getLogger(TestFileDownloadFilter.class);

  public static void test1() throws Exception {
    HttpPostInput input = new HttpPostInput();
    input.setUrl("http://10.0.10.45:8082/DummyDownload");
    input.setPayload("123");
    HttpResponse resp = HttpPost.execute(input);
    _logger.info(resp);
  }

  public static void main(String[] args) {
    try {
      test1();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
