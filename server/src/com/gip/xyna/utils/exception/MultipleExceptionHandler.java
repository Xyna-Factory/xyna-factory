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
package com.gip.xyna.utils.exception;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Fange, ordne mehrere Exceptions und werfe sie dann weiter.
 * <br>
 * Beispiel:
 * <pre>
 *    MultipleExceptionHandler&lt;MyException1&gt; h = new MultipleExceptionHandler&lt;&gt;();
 *    MultipleExceptionHandler&lt;MyException2&gt; h2 = new MultipleExceptionHandler&lt;&gt;(ExceptionCauseOrdering.TIME);
 *    for (...) {
 *      try {
 *        something();
 *      } catch (MyException1 e) {
 *        h.addException(e);
 *      } catch (MyException2 e) {
 *        h2.addException(e);
 *      }
 *    }
 *    h.rethrow(h2); //wirft MyException1, MyException2, oder MultipleExceptions
 * </pre>
 * 
 * F�r jeden Exceptiontyp muss ein eigener Handler definiert werden.
 * F�r bis zu 5 Handler (und damit verschiedene Exceptiontypen) gibt es rethrow-Signaturen.
 * 
 */
public class MultipleExceptionHandler<E extends Throwable> {

  private final Map<Throwable, Long> exceptions = new HashMap<>(1);
  private final ExceptionCauseOrdering ordering;
  private static final AtomicLong ids = new AtomicLong(0);


  public enum ExceptionCauseOrdering {

    /**
     * Sortiert nach der Reihenfolge, in der "addException" aufgerufen wird. Die Reihenfolge wird �ber alle ExceptionHandler hinweg getrackt. 
     */
    TIME,
    /**
     * Sortiert nach den Typen in der Reihenfolge, wie sie beim Aufruf von rethrow angegeben werden.
     * MultipleExceptionHandler&lt;E1&gt;.rethrow(E2, E3, E4)
     *  ergibt Reihenfolge E1-&gt;E2-&gt;E3-&gt;E4
     */
    TYPE,
    /**
     * Default: Keine spezielle Reihenfolge
     */
    ARBITRARY;

  }


  public MultipleExceptionHandler() {
    this(ExceptionCauseOrdering.ARBITRARY);
  }


  public MultipleExceptionHandler(ExceptionCauseOrdering ordering) {
    this.ordering = ordering;
  }

  public void addRuntimeException(RuntimeException re) {
    exceptions.put(re, nextId());
  }
  
  public void addError(Error e) {
    exceptions.put(e, nextId());
  }

  public void addException(E e) {
    exceptions.put(e, nextId());
  }


  private long nextId() {
    return ids.getAndIncrement();
  }


  public void rethrow() throws E, MultipleExceptions {
    this.<RuntimeException> internally(this);
  }


  public <E1 extends Throwable> void rethrow(MultipleExceptionHandler<E1> h1) throws E, E1, MultipleExceptions {
    this.<RuntimeException> internally(this, h1);
  }


  public <E1 extends Throwable, E2 extends Throwable> void rethrow(MultipleExceptionHandler<E1> h1, MultipleExceptionHandler<E2> h2)
      throws E, E1, E2, MultipleExceptions {
    this.<RuntimeException> internally(this, h1, h2);
  }


  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable> void rethrow(MultipleExceptionHandler<E1> h1,
                                                                                         MultipleExceptionHandler<E2> h2,
                                                                                         MultipleExceptionHandler<E3> h3)
      throws E, E1, E2, E3, MultipleExceptions {
    this.<RuntimeException> internally(this, h1, h2, h3);
  }


  public <E1 extends Throwable, E2 extends Throwable, E3 extends Throwable, E4 extends Throwable> void rethrow(MultipleExceptionHandler<E1> h1,
                                                                                                               MultipleExceptionHandler<E2> h2,
                                                                                                               MultipleExceptionHandler<E3> h3,
                                                                                                               MultipleExceptionHandler<E4> h4)
      throws E, E1, E2, E3, E4, MultipleExceptions {
    this.<RuntimeException> internally(this, h1, h2, h3, h4);
  }


  private <T extends Throwable> void internally(MultipleExceptionHandler... multipleExceptionHandlers) throws T, MultipleExceptions {
    int s = 0;
    for (MultipleExceptionHandler h : multipleExceptionHandlers) {
      s += h.exceptions.size();
    }
    switch (s) {
      case 0 :
        return;
      case 1 :
        for (MultipleExceptionHandler<?> h : multipleExceptionHandlers) {
          if (h.exceptions.size() == 1) {
            throw (T) h.exceptions.keySet().iterator().next();
          }
        }
        break;
      default :
        List<Throwable> l = new ArrayList<>();
        switch (ordering) {
          case ARBITRARY :
          case TYPE :
            for (MultipleExceptionHandler h : multipleExceptionHandlers) {
              l.addAll(h.exceptions.keySet());
            }
            break;
          case TIME :
            List<Entry<Throwable, Long>> list = new ArrayList<>();
            for (MultipleExceptionHandler h : multipleExceptionHandlers) {
              list.addAll(h.exceptions.entrySet());
            }
            Collections.sort(list, new Comparator<Entry<Throwable, Long>>() {

              @Override
              public int compare(Entry<Throwable, Long> o1, Entry<Throwable, Long> o2) {
                return Long.compare(o1.getValue(), o2.getValue());
              }

            });
            for (Entry<Throwable, Long> e : list) {
              l.add(e.getKey());
            }
            break;
          default :
            throw new RuntimeException();
        }
        throw MultipleExceptions.create(l);
    }
  }

  /**
   * @return anzahl der exceptions
   */
  public int count() {
    return exceptions.size();
  }

}
