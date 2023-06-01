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
package com.gip.xyna.demon;

public enum DemonSignal {
  EXIT(9),
  TERM(15),
  LOG(0),
  START(1);

  private int signal;

  private DemonSignal( int signal ) {
    this.signal = signal;
  }

  public static DemonSignal fromInt( int s ) {
    for( DemonSignal sig : values() ) {
      if( sig.signal == s ) {
        return sig;
      }
    }
    return null;
  }

  public int toInt() {
    return signal;
  }
  
}