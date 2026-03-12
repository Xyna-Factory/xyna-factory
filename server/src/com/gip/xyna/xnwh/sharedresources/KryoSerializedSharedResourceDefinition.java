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
import com.esotericsoftware.kryo.util.Pool;



public class KryoSerializedSharedResourceDefinition<T> extends SharedResourceDefinition<T> {

  private final Pool<Kryo> kryoPool;
  private final Class<T> clazz;
  private final int bufferSize;


  public KryoSerializedSharedResourceDefinition(String path, Class<T> clazz, Class<?>... additionalClasses) {
    this(path, clazz, 4096, additionalClasses);
  }


  public KryoSerializedSharedResourceDefinition(String path, Class<T> clazz, int bufferSize, Class<?>... additionalClasses) {
    super(path);
    this.clazz = clazz;
    this.bufferSize = bufferSize;
    kryoPool = new Pool<Kryo>(true, false, 8) {

      protected Kryo create() {
        Kryo result = new Kryo();
        result.register(clazz);
        for (Class<?> additioClass : additionalClasses) {
          result.register(additioClass);
        }
        return result;
      }
    };

  }


  @Override
  public byte[] serialize(T value) {
    Output output = new ByteBufferOutput(bufferSize);
    Kryo kryo = kryoPool.obtain();
    try {
      kryo.writeObjectOrNull(output, value, clazz);
      output.close();
    } finally {
      kryoPool.free(kryo);
    }
    return output.toBytes();
  }


  @Override
  public SharedResourceInstance<T> deserialize(byte[] value, String id, Long created) {
    Input input = new Input(value);
    T val;
    Kryo kryo = kryoPool.obtain();
    try {
      val = kryo.readObjectOrNull(input, clazz);
      input.close();
    } finally {
      kryoPool.free(kryo);
    }
    return new SharedResourceInstance<T>(id, created, val);
  }

}
