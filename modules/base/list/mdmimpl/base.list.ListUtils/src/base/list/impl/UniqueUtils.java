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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

import base.list.pair.Pair;



public class UniqueUtils {

  private UniqueUtils() {
  }


  public static List<GeneralXynaObject> unique(List<GeneralXynaObject> list, Comparator<? super GeneralXynaObject> comparator) {
    //leider O(N^2)! 
    //TODO bessere Implementierung möglich, wenn Comparator nur ein Feld anschaut


    List<GeneralXynaObject> unique = new ArrayList<>();
    for (GeneralXynaObject candidate : list) {
      boolean add = true;
      for (GeneralXynaObject gxo : unique) {
        if (comparator.compare(candidate, gxo) == 0) {
          add = false;
          break;
        }
      }
      if (add) {
        unique.add(candidate);
      }
    }
    return unique;
  }


  public static List<GeneralXynaObject> notUnique(List<GeneralXynaObject> list, Comparator<? super GeneralXynaObject> comparator) {
    //leider O(N^2)! 
    //TODO bessere Implementierung möglich, wenn Comparator nur ein Feld anschaut

    List<GeneralXynaObject> unknown = new ArrayList<>();
    List<GeneralXynaObject> notUnique = new ArrayList<>();
    for (GeneralXynaObject candidate : list) {
      boolean add = false;
      for (GeneralXynaObject gxo : unknown) {
        if (comparator.compare(candidate, gxo) == 0) {
          add = true;
          break;
        }
      }
      if (add) {
        notUnique.add(candidate);
      } else {
        unknown.add(candidate);
      }
    }
    for (GeneralXynaObject candidate : unknown) {
      boolean add = false;
      for (GeneralXynaObject gxo : notUnique) {
        if (comparator.compare(candidate, gxo) == 0) {
          add = true;
          break;
        }
      }
      if (add) {
        notUnique.add(candidate);
      }
    }
    return notUnique;
  }


  public static List<Pair> merge(List<GeneralXynaObject> listA, List<GeneralXynaObject> listB,
                                 Comparator<? super GeneralXynaObject> comparator) {
    int sizeA = listA.size();
    int sizeB = listB.size();
    Set<GeneralXynaObject> lookup;
    List<GeneralXynaObject> driving;
    boolean drivingFirst;
    if (sizeA < sizeB) {
      lookup = new LinkedHashSet<>(listA);
      driving = listB;
      drivingFirst = false;
    } else {
      lookup = new LinkedHashSet<>(listB);
      driving = listA;
      drivingFirst = true;
    }
    return merge(driving, lookup, drivingFirst, comparator);
  }


  private static List<Pair> merge(List<GeneralXynaObject> driving, Set<GeneralXynaObject> lookup, boolean drivingFirst,
                                  Comparator<? super GeneralXynaObject> comparator) {
    List<Pair> result = new ArrayList<>();
    for (GeneralXynaObject gxoD : driving) {
      Iterator<GeneralXynaObject> lIter = lookup.iterator();
      boolean found = false;
      while (lIter.hasNext()) {
        GeneralXynaObject gxoL = lIter.next();
        if (comparator.compare(gxoD, gxoL) == 0) {
          result.add(createPair(drivingFirst, gxoD, gxoL));
          found = true;
          lIter.remove();
          break;
        }
      }
      if (!found) {
        result.add(createPair(drivingFirst, gxoD, null));
      }
    }
    for (GeneralXynaObject gxoL : lookup) {
      result.add(createPair(drivingFirst, null, gxoL));
    }
    return result;
  }


  private static Pair createPair(boolean drivingFirst, GeneralXynaObject gxoD, GeneralXynaObject gxoL) {
    Pair pair = new Pair();
    if (drivingFirst) {
      pair.setFirst(gxoD);
      pair.setSecond(gxoL);
    } else {
      pair.setFirst(gxoL);
      pair.setSecond(gxoD);
    }
    return pair;
  }

}
