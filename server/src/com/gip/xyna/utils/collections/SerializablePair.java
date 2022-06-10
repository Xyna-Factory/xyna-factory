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
package com.gip.xyna.utils.collections;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;


public class SerializablePair<F extends Serializable,S extends Serializable> extends Pair<F, S> implements Externalizable {

  private static final long serialVersionUID = 1247265824973082291L;
  
  public SerializablePair() {
    super(null, null);
  }
  
  public SerializablePair(F first, S second) {
    super(first, second);
  }


  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(first);
    out.writeObject(second);
  }


  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    first = (F) in.readObject();
    second = (S) in.readObject();
  }

  
  public static <F extends Serializable,S extends Serializable> SerializablePair<F,S> of(F first, S second) {
    return new SerializablePair<F,S>(first,second);
  }
  
}
