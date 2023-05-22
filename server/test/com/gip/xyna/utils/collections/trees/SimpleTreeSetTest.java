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
package com.gip.xyna.utils.collections.trees;

import junit.framework.TestCase;

import com.gip.xyna.utils.collections.trees.SimpleTreeSet;
import com.gip.xyna.utils.collections.trees.SimpleTreeSet.TreeElement;


/**
 *
 */
public class SimpleTreeSetTest extends TestCase {

  
 
  public void testAdd() {
    SimpleTreeSet<StringTreeElement> tree = new SimpleTreeSet<StringTreeElement>();
    
    tree.add( element( "hallo" ) );
    tree.add( element( "halb" ) );
    tree.add( element( "hammer" ) );
    
    assertEquals( 3, tree.size() );
    assertEquals( "[hallo, halb, hammer]", tree.toUnmodifiableList().toString() );
    tree.sort();
    assertEquals( "[halb, hallo, hammer]", tree.toUnmodifiableList().toString() );
    
    tree.add( element( "ha" ) );
    tree.add( element( "ham" ) );
    assertEquals( 5, tree.size() );
    tree.sort();
    assertEquals( "[ha, halb, hallo, ham, hammer]", tree.toUnmodifiableList().toString() );
   
    boolean added = tree.add( element( "ham" ) );
    assertEquals( false, added );
    assertEquals( 5, tree.size() );
    assertEquals( "[ha, halb, hallo, ham, hammer]", tree.toUnmodifiableList().toString() );
   
  }
  
  
  public void testRemove() {
    SimpleTreeSet<StringTreeElement> tree = new SimpleTreeSet<StringTreeElement>();
    
    tree.add( element( "hallo" ) );
    tree.add( element( "halb" ) );
    tree.add( element( "hammer" ) );
    tree.add( element( "ha" ) );
    tree.add( element( "ham" ) );
    tree.sort();
    assertEquals( 5, tree.size() );
    assertEquals( "[ha, halb, hallo, ham, hammer]", tree.toUnmodifiableList().toString() );
    
    tree.remove(element( "ham" ) );
    assertEquals( "[ha, halb, hallo, hammer]", tree.toUnmodifiableList().toString() );
    tree.remove(element( "ha" ) );
    assertEquals( "[halb, hallo, hammer]", tree.toUnmodifiableList().toString() );
    assertEquals( 3, tree.size() );
    
    boolean removed = tree.remove(element( "ham" ) );
    assertEquals( false, removed );
    assertEquals( "[halb, hallo, hammer]", tree.toUnmodifiableList().toString() );
    assertEquals( 3, tree.size() );
    
  }
  
  public void testParentChildren() {
    SimpleTreeSet<StringTreeElement> tree = new SimpleTreeSet<StringTreeElement>();
    
    tree.add( element( "hallo" ) );
    tree.add( element( "halb" ) );
    tree.add( element( "hammer" ) );
    tree.add( element( "ha" ) );
    tree.add( element( "ham" ) );
    tree.add( element( "halbe" ) );
    tree.sort();
    assertEquals( 6, tree.size() );

    assertEquals( "halb", tree.getParent( element( "halbe" ) ).toString() );
    assertEquals( "ha", tree.getParent( element( "hal" ) ).toString() );
    assertNull( tree.getParent( element( "h" ) ) );

    assertEquals( "[halb, halbe, hallo]", tree.getChildren( element( "hal" ), true ).toString() );
    assertEquals( "[halb, hallo]", tree.getChildren( element( "hal" ), false ).toString() );
    
    assertEquals( "[hammer]", tree.getChildren( element( "ham" ), true ).toString() );
    assertEquals( "[hammer]", tree.getChildren( element( "ham" ), false ).toString() );
     
    assertEquals( "[halbe]", tree.getChildren( element( "halb" ), true ).toString() );
    assertEquals( "[halbe]", tree.getChildren( element( "halb" ), false ).toString() );
    assertEquals( "[]", tree.getChildren( element( "halbe" ), true ).toString() );
    
    assertEquals( "[halb, halbe, hallo, ham, hammer]", tree.getChildren( element( "ha" ), true ).toString() );
    assertEquals( "[halb, hallo, ham]", tree.getChildren( element( "ha" ), false ).toString() );
  }
  
  
  private static StringTreeElement element(String string) {
    return new StringTreeElement(string);
  }

  private static class StringTreeElement implements TreeElement<StringTreeElement>, Comparable<StringTreeElement> {

    private String string;

    public StringTreeElement(String string) {
      this.string = string;
    }

    public boolean hasChild(StringTreeElement possibleChild) {
      return possibleChild.string.startsWith(string);
    }
    public int compareTo(StringTreeElement o) {
      return string.compareTo(o.string);
    }

    @Override
    public String toString() {
      return string;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((string == null) ? 0 : string.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      StringTreeElement other = (StringTreeElement) obj;
      if (string == null) {
        if (other.string != null)
          return false;
      } else if (!string.equals(other.string))
        return false;
      return true;
    }
    
    
    
  }

  
}
