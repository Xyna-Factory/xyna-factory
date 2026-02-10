/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;



public class KryoSerializedSharedResourceDefinition<T> extends SharedResourceDefinition<T> {

  private final Kryo kryo;
  private final Class<T> clazz;
  private final int bufferSize;

  public KryoSerializedSharedResourceDefinition(String path, Class<T> clazz, Class<?>... additionalClasses) {
    this(path, clazz, 4096, additionalClasses);
  }
  
  public KryoSerializedSharedResourceDefinition(String path, Class<T> clazz, int bufferSize, Class<?>... additionalClasses) {
    super(path);
    this.clazz = clazz;
    this.bufferSize = bufferSize;
    kryo = new Kryo();
    kryo.register(clazz);
    for (Class<?> additioClass : additionalClasses) {
      kryo.register(additioClass);
    }
  }


  @Override
  public byte[] serialize(T value) {
    Output output = new ByteBufferOutput(bufferSize);
    kryo.writeObjectOrNull(output, value, clazz);
    output.close();
    return output.toBytes();
  }


  @Override
  public SharedResourceInstance<T> deserialize(byte[] value, String id) {
    Input input = new Input(value);
    T val = kryo.readObjectOrNull(input, clazz);
    input.close();
    return new SharedResourceInstance<T>(id, val);
  }

}
