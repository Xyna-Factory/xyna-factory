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
package com.gip.xyna.xprc.xfractwfe.python.jep;

import java.lang.reflect.Method;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.python.JepInterpreter;

public class JepThreadManagement {

  
  public static JepThread createJepThread(Method method, Object instance, Object[] inputs) {
    return new JepThread(method, instance, inputs);
  }
  
  public static JepKeywordsThread createJepKeywordThread(JepInterpreter jepInterpreter) {
    return new JepKeywordsThread(jepInterpreter);
  }
  
  public static class JepThread extends Thread {
    
    private final Method method;
    private final Object instance;
    private final Object[] inputs;
    private Object result;
    private Exception exception;
    private boolean success;
    
    private JepThread(Method method, Object instance, Object[] inputs) {
      this.method = method;
      this.instance = instance;
      this.inputs = inputs;
    }
    
    @Override
    public void run() {
      try {
        result = method.invoke(instance, inputs);
        success = true;
      } catch (Exception e) {
        exception = e;
        success = false;
      }
    }
    
    public Object getResult() {
      return result;
    }
    
    public Exception getException() {
      return exception;
    }
    
    public boolean wasSuccessful() {
      return success;
    }
  }

  public static class JepKeywordsThread extends Thread {

    private JepInterpreter jepInterpreter;
    
    private List<String> result;
    private Exception exception;
    private boolean success;

    public JepKeywordsThread(JepInterpreter jepInterpreter) {
      this.jepInterpreter = jepInterpreter;
    }

    @SuppressWarnings("unchecked")
    public void run() {
      try {
        jepInterpreter.exec("import keyword");
        result = (List<String>) jepInterpreter.get("keyword.kwlist");
        jepInterpreter.close();
      } catch (Exception e) {
        exception = e;
        success = false;
      }
    }

    public List<String> getResult() {
      return result;
    }

    public Exception getException() {
      return exception;
    }

    public boolean wasSuccessful() {
      return success;
    }
  }
}
