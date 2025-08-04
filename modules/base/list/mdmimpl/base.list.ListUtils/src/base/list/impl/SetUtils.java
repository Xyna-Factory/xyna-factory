/* 
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package base.list.impl;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;



public class SetUtils {

  private SetUtils() {
  }


  public static List<GeneralXynaObject> difference(List<GeneralXynaObject> minuend, List<GeneralXynaObject> subtrahend,
                                                   Comparator<? super GeneralXynaObject> comparator) {

    //leider O(N*M)! 
    //TODO bessere Implementierung möglich, wenn Comparator nur ein Feld anschaut
    List<GeneralXynaObject> difference = new ArrayList<>();

    for (GeneralXynaObject candidate : minuend) {
      boolean remove = false;
      for (GeneralXynaObject gxo : subtrahend) {
        if (comparator.compare(candidate, gxo) == 0) {
          remove = true;
          break;
        }
      }
      if (!remove) {
        difference.add(candidate);
      }
    }
    return difference;
  }


  public static List<GeneralXynaObject> intersection(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB,
                                                     Comparator<? super GeneralXynaObject> comparator) {

    //leider O(N*M)! 
    //TODO bessere Implementierung möglich, wenn Comparator nur ein Feld anschaut
    List<GeneralXynaObject> intersection = new ArrayList<>();

    for (GeneralXynaObject candidate : listA) {
      boolean add = false;
      for (GeneralXynaObject gxo : listB) {
        if (comparator.compare(candidate, gxo) == 0) {
          add = true;
          break;
        }
      }
      if (add) {
        intersection.add(candidate);
      }
    }
    return intersection;
  }


  public static List<GeneralXynaObject> remove(List<GeneralXynaObject> list, GeneralXynaObject element,
                                               Comparator<? super GeneralXynaObject> comparator) {
    List<GeneralXynaObject> ret = new ArrayList<>();
    for (GeneralXynaObject gxo : list) {
      if (comparator.compare(element, gxo) == 0) {
        //remove
      } else {
        ret.add(gxo);
      }
    }
    return ret;
  }


  public static List<GeneralXynaObject> replace(List<GeneralXynaObject> list, GeneralXynaObject element,
                                                Comparator<? super GeneralXynaObject> comparator, boolean add) {
    List<GeneralXynaObject> ret = new ArrayList<>();
    boolean replaced = false;
    for (GeneralXynaObject gxo : list) {
      if (comparator.compare(element, gxo) == 0) {
        //replace
        ret.add(element);
        replaced = true;
      } else {
        ret.add(gxo);
      }
    }
    if (!replaced && add) {
      ret.add(element);
    }
    return ret;
  }


  public static List<GeneralXynaObject> removeAndAddOrReplace(List<GeneralXynaObject> existing, List<GeneralXynaObject> remove,
                                                              List<GeneralXynaObject> addOrReplace,
                                                              Comparator<? super GeneralXynaObject> comparator) {
    List<GeneralXynaObject> aor = addOrReplace;
    List<GeneralXynaObject> ret = new ArrayList<>();
    for (GeneralXynaObject gxo : existing) {
      if (listContains(remove, gxo, comparator)) {
        //remove
      } else {
        Pair<List<GeneralXynaObject>, GeneralXynaObject> p = removeToPair(aor, gxo, comparator);
        if (p.getSecond() != null) {
          ret.add(p.getSecond()); //replace
          aor = p.getFirst();
        } else {
          ret.add(gxo); //retain
        }
      }
    }
    ret.addAll(aor); //add
    return ret;
  }


  private static Pair<List<GeneralXynaObject>, GeneralXynaObject> removeToPair(List<GeneralXynaObject> list, GeneralXynaObject element,
                                                                               Comparator<? super GeneralXynaObject> comparator) {
    GeneralXynaObject found = null;
    List<GeneralXynaObject> ret = new ArrayList<>();
    for (GeneralXynaObject gxo : list) {
      if (comparator.compare(element, gxo) == 0) {
        //remove
        found = gxo;
      } else {
        ret.add(gxo);
      }
    }
    return Pair.of(ret, found);
  }


  private static boolean listContains(List<GeneralXynaObject> list, GeneralXynaObject element,
                                      Comparator<? super GeneralXynaObject> comparator) {
    for (GeneralXynaObject gxo : list) {
      if (comparator.compare(element, gxo) == 0) {
        return true;
      }
    }
    return false;
  }

}
