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

package com.gip.queue.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MsgToSend {

  public enum MsgType {
    TEXT_MSG, BYTES_MSG
  }

  public static class JmsProperty {
    private String key;
    private String value;
    public JmsProperty(String k, String v) {
      key = k;
      value = v;
    }
    public String getKey() {
      return key;
    }
    public String getValue() {
      return value;
    }
  }

  private String _message;
  private String _corrId;
  private MsgType _messageType;
  private List<JmsProperty> _jmsPropertyList = new ArrayList<JmsProperty>();
  private String _jmsReplyToQueueName;


  public void addJmsProperty(JmsProperty prop) {
    _jmsPropertyList.add(prop);
  }

  public void addJmsProperty(String key, String value) {
    JmsProperty prop = new JmsProperty(key, value);
    _jmsPropertyList.add(prop);
  }

  public List<JmsProperty> getJmsPropertyList() {
    //return Collections.unmodifiableList(_jmsPropertyList);
    return _jmsPropertyList;
  }

  public String getMessage() {
    return _message;
  }

  /*
  public int getTimeout() {
    return _timeout;
  }
  */

  public String getCorrId() {
    return _corrId;
  }

  public MsgType getMessageType() {
    return _messageType;
  }

  public String getJmsReplyToQueueName() {
    return _jmsReplyToQueueName;
  }


  /*
  public MsgToSend timeout(int val) {
    _timeout = val;
    return this;
  }
  */

  public MsgToSend message(String msg) {
    _message = msg;
    return this;
  }

  public MsgToSend corrId(String id) {
    _corrId = id;
    return this;
  }

  public MsgToSend messageType(MsgType type) {
    _messageType = type;
    return this;
  }

  public MsgToSend jmsReplyToQueueName(String _jmsReplyToQueueName) {
    this._jmsReplyToQueueName = _jmsReplyToQueueName;
    return this;
  }

}
