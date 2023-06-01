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

package com.gip.xtfutils.httptools.http;


public class HttpPostInput extends HttpInput {

  private String _payload;


  public HttpPostInput() {
  }


  public String getPayload() {
    return _payload;
  }


  public void setPayload(String payload) {
    this._payload = payload;
  }

  public void verify() {
    super.verify();
    if (_payload == null) {
      throw new IllegalArgumentException("Payload is null.");
    }
  }

}
