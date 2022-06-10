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
package com.gip.xyna.xnwh.utils;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;



public class ReflectionUtils {
  
  public static boolean ensureField(AtomicReference<Field> field, Class<?> clazz, String name, Logger logger) {
    if (field.get() == null) {
      Field f;
      try {
        f = clazz.getDeclaredField(name);
      } catch (SecurityException e) {
        logger.debug("Failed to ensure field", e);
        return false;
      } catch (NoSuchFieldException e) {
        logger.debug("Failed to ensure field", e);
        return false;
      }
      f.setAccessible(true);
      field.compareAndSet(null, f);
    }
    return true;
  }
  
  
  public static Object get(AtomicReference<Field> field, Object instance, Logger logger) {
    try {
      return field.get().get(instance);
    } catch (IllegalArgumentException e) {
      logger.debug("Failed to get field", e);
      return null;
    } catch (IllegalAccessException e) {
      logger.debug("Failed to get field", e);
      return null;
    }
  }

}
