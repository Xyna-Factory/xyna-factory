/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.openapi.codegen.templating.mustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class StatusCodeLambda implements Mustache.Lambda {

  public static final HashMap<String, String> httpStatusCodes;
  static {
    httpStatusCodes = new HashMap<>();
    httpStatusCodes.put("100", "Continue");
    httpStatusCodes.put("101", "Switching Protocols");
    httpStatusCodes.put("102", "Processing");
    httpStatusCodes.put("103", "Early Hints"); // (RFC 8297)
    httpStatusCodes.put("200", "OK");
    httpStatusCodes.put("201", "Created");
    httpStatusCodes.put("202", "Accepted");
    httpStatusCodes.put("203", "Non-Authoritative Information");
    httpStatusCodes.put("204", "No Content");
    httpStatusCodes.put("205", "Reset Content");
    httpStatusCodes.put("206", "Partial Content");
    httpStatusCodes.put("207", "Multi-Status"); // (WebDAV; RFC 4918)
    httpStatusCodes.put("208", "Already Reported"); // (WebDAV; RFC 5842)
    httpStatusCodes.put("226", "IM Used"); // (RFC 3229)
    httpStatusCodes.put("300", "Multiple Choices");
    httpStatusCodes.put("301", "Moved Permanently");
    httpStatusCodes.put("302", "Found"); 
    httpStatusCodes.put("303", "See Other");
    httpStatusCodes.put("304", "Not Modified");
    httpStatusCodes.put("305", "Use Proxy");
    httpStatusCodes.put("306", "Switch Proxy");
    httpStatusCodes.put("307", "Temporary Redirect");
    httpStatusCodes.put("308", "Permanent Redirect");
    httpStatusCodes.put("400", "Bad Request");
    httpStatusCodes.put("401", "Unauthorized");
    httpStatusCodes.put("402", "Payment Required");
    httpStatusCodes.put("403", "Forbidden");
    httpStatusCodes.put("404", "Not Found");
    httpStatusCodes.put("405", "Method Not Allowed");
    httpStatusCodes.put("406", "Not Acceptable");
    httpStatusCodes.put("407", "Proxy Authentication Required");
    httpStatusCodes.put("408", "Request Timeout");
    httpStatusCodes.put("409", "Conflict");
    httpStatusCodes.put("410", "Gone");
    httpStatusCodes.put("411", "Length Required");
    httpStatusCodes.put("412", "Precondition Failed");
    httpStatusCodes.put("413", "Payload Too Large");
    httpStatusCodes.put("414", "URI Too Long");
    httpStatusCodes.put("415", "Unsupported Media Type");
    httpStatusCodes.put("416", "Range Not Satisfiable");
    httpStatusCodes.put("417", "Expectation Failed");
    httpStatusCodes.put("418", "I am a teapot"); // (RFC 2324, RFC 7168)
    httpStatusCodes.put("421", "Misdirected Request");
    httpStatusCodes.put("422", "Unprocessable Entity");
    httpStatusCodes.put("423", "Locked"); // (WebDAV; RFC 4918)
    httpStatusCodes.put("424", "Failed Dependency"); // (WebDAV; RFC 4918)
    httpStatusCodes.put("425", "Too Early"); // (RFC 8470)
    httpStatusCodes.put("426", "Upgrade Required");
    httpStatusCodes.put("428", "Precondition Required"); // (RFC 6585)
    httpStatusCodes.put("429", "Too Many Requests"); // (RFC 6585)
    httpStatusCodes.put("431", "Request Header Fields Too Large"); // (RFC 6585)
    httpStatusCodes.put("451", "Unavailable For Legal Reasons"); // (RFC 7725)
    httpStatusCodes.put("500", "Internal Server Error");
    httpStatusCodes.put("501", "Not Implemented");
    httpStatusCodes.put("502", "Bad Gateway");
    httpStatusCodes.put("503", "Service Unavailable");
    httpStatusCodes.put("504", "Gateway Timeout");
    httpStatusCodes.put("505", "HTTP Version Not Supported");
    httpStatusCodes.put("506", "Variant Also Negotiates"); // (RFC 2295)
    httpStatusCodes.put("507", "Insufficient Storage"); // (WebDAV; RFC 4918)
    httpStatusCodes.put("508", "Loop Detected"); // (WebDAV; RFC 5842)
    httpStatusCodes.put("510", "Not Extended"); // (RFC 2774)
    httpStatusCodes.put("511", "Network Authentication Required"); // (RFC 6585)
  }

  public StatusCodeLambda() {

  }

  /**
   * add message to status code
   * use: {{#lambda.statuscode}}{{code}}{{/lambda.statuscode}}
   */
  @Override
  public void execute(Template.Fragment fragment, Writer writer) throws IOException {
    String text = fragment.execute();
    if (httpStatusCodes.containsKey(text)) {
      writer.write(text + " " + httpStatusCodes.get(text));
    }
  }

}
