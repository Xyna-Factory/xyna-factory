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
package com.gip.xyna.utils.concurrent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;


public class AdministrativeParallelLockTest extends TestCase {

  public void testLockExclusive() {
    ParallelLockTestHelper.testLockExclusive( new AdministrativeParallelLock<Integer>() );
  }
  
  public void testLockParallel() {
    ParallelLockTestHelper.testLockParallel( new AdministrativeParallelLock<Integer>() );
  }
    
  
  public void testAdminLock_AdminLocksAreNotExclusive() {
    AdministrativeParallelLock<Integer> apl = new AdministrativeParallelLock<Integer>();
    
    List<Integer> notLocked = apl.administrativeLock( 1, 2, 3 );
    assertEquals( "[]", String.valueOf(notLocked) );
    assertEquals( new HashSet<Integer>(Arrays.asList(1,2,3)), apl.getAdministrativeLocks() );
    
    notLocked = apl.administrativeLock( 3, 4, 5 );
    assertEquals( "[]", String.valueOf(notLocked) );
    assertEquals( new HashSet<Integer>(Arrays.asList(1,2,3,4,5)), apl.getAdministrativeLocks() );
    
    List<Integer> notUnlocked = apl.administrativeUnlock( 2, 3, 4 );
    assertEquals( "[]", String.valueOf(notUnlocked) );
    assertEquals( new HashSet<Integer>(Arrays.asList(1,5)), apl.getAdministrativeLocks() );
 
    notUnlocked = apl.administrativeUnlock( 1, 3, 5 );
    assertEquals( "[]", String.valueOf(notUnlocked) );
    assertEquals( new HashSet<Integer>(), apl.getAdministrativeLocks() );
  }
  
  public void testAdminLock_AlreadyLocked() {
    
    AdministrativeParallelLock<Integer> apl = new AdministrativeParallelLock<Integer>();
     
    ParallelLockTestHelper.lockInOtherThread( apl, new Integer(1), 500 );
    
    ParallelLockTestHelper.sleep(100);
    List<Integer> notLocked = apl.administrativeLock(Arrays.asList(1, 2, 3) );
    assertEquals( "[1]", String.valueOf(notLocked) );  //ist bereits gelockt
    ParallelLockTestHelper. sleep(500);
    List<Integer> notLocked2 = apl.administrativeLock(Arrays.asList(1, 2, 3) );
    assertEquals( "[]", String.valueOf(notLocked2) );
    
  }

  public void testAdminLock_Parallel() {
    AdministrativeParallelLock<Integer> apl = new AdministrativeParallelLock<Integer>();
    
    List<Integer> notLocked = apl.administrativeLock(1, 2, 3);
    assertEquals( "[]", String.valueOf(notLocked) );
    
    long start = System.currentTimeMillis();
    
    AtomicLong lockTime4 = ParallelLockTestHelper.measureLockTimeInOtherThread( apl, new Integer(4), 400 );
    
    ParallelLockTestHelper.sleep(100);
    System.err.println( apl );
       
    ParallelLockTestHelper.assertSimilar( lockTime4.get(), start, 5 );  //Lock 4 sofort 
  }
  
  
  public void testAdminLock_Exclusive() {
    
    AdministrativeParallelLock<Integer> apl = new AdministrativeParallelLock<Integer>();
    
    List<Integer> notLocked = apl.administrativeLock(1, 2, 3);
    assertEquals( "[]", String.valueOf(notLocked) );
    
    AtomicLong lockTime1 = ParallelLockTestHelper.measureLockTimeInOtherThread( apl, new Integer(1), 400 );
    AtomicLong lockTime2 = ParallelLockTestHelper.measureLockTimeInOtherThread( apl, new Integer(2), 400 );
    
    ParallelLockTestHelper.sleep(10);
    System.err.println( apl );
    
    long unlocked = System.currentTimeMillis();
    List<Integer> notUnlocked = apl.administrativeUnlock(Arrays.asList(1, 3, 5) );
    
    ParallelLockTestHelper.sleep(100);
    ParallelLockTestHelper.assertSimilar( lockTime1.get(), unlocked, 5 ); //Lock 1 erst nach Unlock
    
    long unlocked2 = System.currentTimeMillis();
    List<Integer> notUnlocked2 = apl.administrativeUnlock(Arrays.asList(2,4) );
    
    ParallelLockTestHelper.sleep(10);
    ParallelLockTestHelper.assertSimilar( lockTime2.get(), unlocked2, 5 ); //Lock 2 erst nach Unlock2
    
    ParallelLockTestHelper.assertSimilar( lockTime2.get(), lockTime1.get() + 100, 5 ); //Locks wurden zu unterschiedlichen 
    //Zeiten freigegeben 
  }
  
  private static class A {
    private final int hashCode;
    private final int identity;
    public A(int hashCode, int identity) {
      this.hashCode = hashCode;
      this.identity = identity;
    }
    public int hashCode() {
      return hashCode;
    }
    public boolean equals(Object o) {
      return ((A)o).identity == identity;
    }
  }

  @Test
  public void testWaitForHashCollision() throws Exception {
    AdministrativeParallelLock<A> apl = new AdministrativeParallelLock<A>();
    AtomicLong lockTime1 = ParallelLockTestHelper.measureLockTimeInOtherThread(apl, new A(1, 1), 400);
    ParallelLockTestHelper.sleep(100);
    long start = System.currentTimeMillis();
    assertEquals(false, apl.tryLock(new A(1, 2))); //hashCollision! kommt sofort zurück
    ParallelLockTestHelper.assertSimilar(System.currentTimeMillis(), start, 5);

    assertEquals(false, apl.tryLock(new A(1, 1))); //kommt sofort zurück, weil identisches objekt bereits gelockt ist
    ParallelLockTestHelper.assertSimilar(System.currentTimeMillis(), start, 5);

    start = System.currentTimeMillis();    
    assertEquals(true, apl.tryLock(new A(1, 2), true)); //hashCollision! wartet, bis anderer thread lock freigibt
    ParallelLockTestHelper.assertSimilar(System.currentTimeMillis(), lockTime1.get() + 400, 5);
    
    ParallelLockTestHelper.sleep(100);
    start = System.currentTimeMillis();
    assertEquals(true, apl.tryLock(new A(1, 3))); //reentrancy. lock mit gleichem hash wurde erneut geholt
    ParallelLockTestHelper.assertSimilar(System.currentTimeMillis(), start, 5);
    
    start = System.currentTimeMillis();
    AtomicLong lockTime2 = ParallelLockTestHelper.measureLockTimeInOtherThread(apl, new A(1, 4), 100); //kann nicht gelockt werden, bis die unlocks passieren
    ParallelLockTestHelper.sleep(100);
        
    apl.unlock(new A(1, 2));
    
    ParallelLockTestHelper.sleep(100);
    assertEquals(0, lockTime2.get());
    
    apl.unlock(new A(1, 3));
    
    ParallelLockTestHelper.sleep(100);
    ParallelLockTestHelper.assertSimilar(lockTime2.get(), start + 200, 5); //nach den beiden unlocks wird das lock für den anderen thraed frei
        
    ParallelLockTestHelper.sleep(100); //lock wieder frei
    
    start = System.currentTimeMillis();
    assertEquals(true, apl.tryLock(new A(1, 3))); //lock wieder frei
    ParallelLockTestHelper.assertSimilar(System.currentTimeMillis(), start, 5);
  }
  
}
