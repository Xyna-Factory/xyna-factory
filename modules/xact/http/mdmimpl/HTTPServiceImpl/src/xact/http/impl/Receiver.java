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
package xact.http.impl;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

import xact.http.exceptions.HttpException;
import xact.templates.Document;
import xact.templates.PlainText;


public class Receiver {

  
  public String getString(HttpConnectionImpl hc) throws HttpException {
    HttpEntity entity = hc.receive();
    
    return readString(entity);
  }

  private String readString(HttpEntity entity) throws HttpException {
    if (entity == null) {
      return "";
    }
    
    try {
      return EntityUtils.toString(entity);
    } catch (ParseException e) {
      throw new HttpException(e);
    } catch (IOException e) {
      throw new HttpException(e);
    }
  }

  public Document getDocument(HttpConnectionImpl hc) throws HttpException {
    HttpEntity entity = hc.receive();
    String string = readString(entity);
    //String mime = null;
    //if( entity.getContentType() != null ) {
    //  mime = entity.getContentType().getValue();
    //}
    Document doc = new Document( new PlainText(), string);
    return doc;
  }

  public InputStream getStream(HttpConnectionImpl hc) throws HttpException {
    HttpEntity entity = hc.receive();
    if (entity == null) {
      return InputStream.nullInputStream();
    }
    InputStream is;
    try {
      is = entity.getContent();
    } catch (UnsupportedOperationException | IOException e) {
      throw new HttpException(e);
    }
    return is;
  }
}
