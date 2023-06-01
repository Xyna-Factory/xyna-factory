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
package xact.mail.account;

public class MailAccountStorageException extends Exception {

  
  private final Type type;
  private final String name;

  public MailAccountStorageException(Type type, String name) {
    super(type.getMessage(name));
    this.type = type;
    this.name = name;
  }

  private static final long serialVersionUID = 1L;

  public enum Type {
    ALREADY_REGISTERED {
      public String getMessage(String name) {
        return "Mail account \""+name+"\" is already registered";
      }
    },
    NOT_REGISTERED {
      public String getMessage(String name) {
        return "No mail account \""+name+"\" registered so far";
      }
    };

    public abstract String getMessage(String name);
    
  }
  
  public Type getType() {
    return type;
  }
  public String getName() {
    return name;
  }
  
  
}
