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
package com.gip.xyna.xnwh.persistence.memory.index.tree;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class TreeTraversal<E extends Comparable<E>> {

  private static final Logger logger = CentralFactoryLogging.getLogger(TreeTraversal.class);
  
  public enum RecursionRequest {
    RECURSE_CHILD_ONLY, NO_RECURSION, RECURSE_ALL, RECURSE_CHILD_CHECK_NEXT, NO_RECURSION_CHECK_NEXT, 
    RECURSE_IF_NEXTSIBLING_A, RECURSE_IF_NEXTSIBLING_ALL_RECURSION;
  }

  public enum TraversalContinuation {
    CONTINUE, BREAK;
  }

  public interface TraversalCondition<E> {

    /**
     * was soll mit diesem knoten passieren =&gt; rekursion abbrechen, weiter, etc 
     */
    public ConditionResult check(AbstractNode<E> node, boolean reverseTraversal);

  }

  public interface Handler<E> {

    public TraversalContinuation handle(AbstractNode<E> node);

    public boolean needsWriteLock();
  }
  
  public static class ConditionResult {

    final private boolean isHit;
    final private RecursionRequest recursionRequest;
    final private boolean a; //FIXME umbenennen

    public ConditionResult(boolean isHit, RecursionRequest recursionRequest) {
      this(isHit, recursionRequest, false);
    }


    public ConditionResult(boolean isHit, RecursionRequest recursionRequest, boolean a) {
      this.isHit = isHit;
      this.recursionRequest = recursionRequest;
      this.a = a;
    }


    public boolean isHit() {
      return isHit;
    }


    public RecursionRequest recursionRequest() {
      return recursionRequest;
    }
    
    public boolean isA() {
      return a;
    }

  }


  public void traverseTree(AbstractNode<E> root, TraversalCondition<E> condition, Handler<E> handler) {
    traverseTree(root, condition, handler, null);
  }
  
  
  public void traverseTree(AbstractNode<E> root, TraversalCondition<E> condition, Handler<E> handler, boolean reverse) {
    if (reverse) {
      traverseTreeReverse(root, condition, handler, null);
    } else {
      traverseTree(root, condition, handler, null);
    }
  }


  private TraversalContinuation traverseTree(AbstractNode<E> node, TraversalCondition<E> condition, Handler<E> handler,
                                             ConditionResult result) {
    boolean checkCondition = result == null;
    TraversalContinuation continuation = null;
    LockedOrderedNode<?> lockedNode = null;
    if (node instanceof LockedOrderedNode) {
      lockedNode = (LockedOrderedNode<?>)node;
    }
    if (lockedNode != null) {
      if (handler.needsWriteLock()) {
        //FIXME writelock erst bei hits holen, s.u.
        lockedNode.getLock().writeLock().lock();
      } else {
        lockedNode.readLock().lock();
      }
    }
    try {
      loop : for (int i = 0; i < node.getNumberOfChildren(); i++) {
        AbstractNode<E> child = node.getChild(i);
        if (checkCondition) {
          //muss ggfs nicht mehrfach ausgeführt werden (vgl RECURSE_ALL)
          result = condition.check(child, false);
        }
        if (result.isHit()) {
      /*    if (lockedNode != null) {
            if (handler.needsWriteLock()) {
              //lock upgrade nicht möglich
              lockedNode.getLock().readLock().unlock();
              lockedNode.getLock().writeLock().lock();
              //TODO:
              //checken, dass die kinder dieses knotens nicht geändert wurden
              //das kann passieren, wenn zb ein rebalance thread das writelock zuerst bekommt
              //dann ist man nun evtl an einer ganz anderen stelle im baum (sowohl child als auch parent können im baum ihre position geändert haben)
              //idee 1: 
              //=> solange zum parent zurück, bis parent sich nicht geändert hat.
              //von dort aus dann traverseTree erneut aufrufen
              //frage: was passiert mit bereits behandelten knoten (handler ausgeführt), die dann erneut gecheckt werden?
              //antwort: needsWriteLock UseCase => entfernen und/oder hinzufügen von knoten im baum. dann darf man handler nicht erneut ausführen
              
              //idee 2:
              //=> bei dem knoten weitermachen, auch wenn der knoten nun an einem anderen parent hängt, weil die position im baum
              //nicht so wichtig ist, wie die position in der sortierreihenfolge (diese ist aber gleich geblieben, weil der wert
              //des knotens sich nicht ändern kann).
              //dann muss man bei der rekursion an den parent-aufruf den neuen parentknoten zurückgeben, weil sich dieser geändert haben kann.
              //ausserdem muss die stacktiefe nicht mehr mit der tiefe des aktuellen knotens im baum übereinstimmen. => kompliziert.
              
              //es kann ausserdem sein, dass der parent derweil ein anderes readlock besitzt, und dieses im handler versucht wird, auf 
              //ein writelock zu upgraden (=>deadlock).
            }
          }*/
          try {
            continuation = handler.handle(child);
          } finally {
            /*if (lockedNode != null) {
              if (handler.needsWriteLock()) {
                //lock downgrade
                lockedNode.getLock().readLock().lock();
                lockedNode.getLock().writeLock().unlock();
              }
            }*/
          }
          //FIXME beim einfügen oder delete anderen returnwert, weil hier die liste der kinder evtl geändert wird? (removenode oder addnode)
          if (continuation == TraversalContinuation.BREAK) {
            //z.b. maxrows erreicht, die gefunden werden sollen
            return continuation;
          }
        }
        switch (result.recursionRequest()) {
          case RECURSE_IF_NEXTSIBLING_ALL_RECURSION :
            boolean recurse = false;
            if (i < node.getNumberOfChildren() - 1) {
              //FIXME wie ist das mit locking-reihenfolge? => ok, wenn das lock hier wieder freigegeben wird.
              AbstractNode<E> nextChild = node.getChild(i + 1); //FIXME performance: kann in nächstem schleifendurchlauf wiederverwendet werden
              ConditionResult nextResult = condition.check(nextChild, false);
              if (nextResult.recursionRequest == RecursionRequest.RECURSE_ALL) {
                recurse = true;
              }
            } else {
              recurse = true;
            }
            if (recurse) {
              continuation = traverseTree(child, condition, handler, null);
              if (continuation == TraversalContinuation.BREAK) {
                //z.b. maxrows wurde in rekursion erreicht
                return continuation;
              }
            }
            break;
          case RECURSE_IF_NEXTSIBLING_A :
            recurse = false;
            if (i < node.getNumberOfChildren() - 1) {
              AbstractNode<E> nextChild = node.getChild(i + 1); //FIXME performance: kann in nächstem schleifendurchlauf wiederverwendet werden
              ConditionResult nextResult = condition.check(nextChild, false);
              if (nextResult.a) {
                recurse = true;
              }
            } else {
              recurse = true;
            }
            if (recurse) {
              continuation = traverseTree(child, condition, handler, null);
              if (continuation == TraversalContinuation.BREAK) {
                //z.b. maxrows wurde in rekursion erreicht
                return continuation;
              }
            }
            break;
          case RECURSE_CHILD_CHECK_NEXT :
            //rekursion für dieses kind, für das nächste erneut checken: geoderte bedingungen
            continuation = traverseTree(child, condition, handler, null);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            break;
          case NO_RECURSION_CHECK_NEXT :
            //keine rekursion für dieses kind, für das nächste erneut checken: geoderte bedingungen
            break;
          case RECURSE_ALL :
            //für dieses und alle folgenden kinder rekursion (entspricht suchen nach > bedingung, wenn für alle folgenden kinder klar ist, was passieren muss)
            //zb bei >: result.isHit = true, result.recursionRequest = RECURSE_ALL
            checkCondition = false;
            continuation = traverseTree(child, null, handler, result);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            break;
          case RECURSE_CHILD_ONLY :
            //nur für das kind rekursion, und nicht für folgende (entspricht suchen nach knoten mit gleichheits-bedingung)    
            continuation = traverseTree(child, condition, handler, null);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            //fall through
          case NO_RECURSION :
            // zb bei < bedingung, wenn alle kinder gefunden wurden 
            break loop;
          default :
            throw new RuntimeException("unhandled result: " + result);
        }
      }
    } finally {
      if (lockedNode != null) {
        if (handler.needsWriteLock()) {
          lockedNode.getLock().writeLock().unlock();
        } else {
          lockedNode.readLock().unlock();
        }
      }
    }
   
    return TraversalContinuation.CONTINUE;
  }


  // doing the same but instead of using pre-order traversal (self, left, right), we use the reverse (right, left, self)
  private TraversalContinuation traverseTreeReverse(AbstractNode<E> node, TraversalCondition<E> condition,
                                                    Handler<E> handler, ConditionResult result) {
    boolean checkCondition = result == null;
    TraversalContinuation continuation = null;
    LockedOrderedNode<?> lockedNode = null;
    if (node instanceof LockedOrderedNode) {
      lockedNode = (LockedOrderedNode<?>) node;
    }
    if (lockedNode != null) {
      if (handler.needsWriteLock()) {
        //FIXME writelock erst bei hits holen, s.u.
        lockedNode.getLock().writeLock().lock();
      } else {
        lockedNode.readLock().lock();
      }
    }
    try {
      for (int i = node.getNumberOfChildren() - 1; i >= 0; i--) {
        boolean breakloop = false;
        AbstractNode<E> child = node.getChild(i);
        if (checkCondition) {
          //muss ggfs nicht mehrfach ausgeführt werden (vgl RECURSE_ALL)
          result = condition.check(child, true);
        }
        switch (result.recursionRequest()) {
          case RECURSE_IF_NEXTSIBLING_ALL_RECURSION :
            boolean recurse = false;
            if (i > 0) {
              //FIXME wie ist das mit locking-reihenfolge? => ok, wenn das lock hier wieder freigegeben wird.
              AbstractNode<E> nextChild = node.getChild(i - 1); //FIXME performance: kann in nächstem schleifendurchlauf wiederverwendet werden
              ConditionResult nextResult = condition.check(nextChild, true);
              if (nextResult.recursionRequest == RecursionRequest.RECURSE_ALL) {
                recurse = true;
              }
            } else {
              recurse = true;
            }
            if (recurse) {
              continuation = traverseTreeReverse(child, condition, handler, null);
              if (continuation == TraversalContinuation.BREAK) {
                //z.b. maxrows wurde in rekursion erreicht
                return continuation;
              }
            }
            break;
          case RECURSE_IF_NEXTSIBLING_A :
            recurse = false;
            if (i > 0) {
              AbstractNode<E> nextChild = node.getChild(i - 1); //FIXME performance: kann in nächstem schleifendurchlauf wiederverwendet werden
              ConditionResult nextResult = condition.check(nextChild, true);
              if (nextResult.a) {
                recurse = true;
              }
            } else {
              recurse = true;
            }
            if (recurse) {
              continuation = traverseTreeReverse(child, condition, handler, null);
              if (continuation == TraversalContinuation.BREAK) {
                //z.b. maxrows wurde in rekursion erreicht
                return continuation;
              }
            }
            break;
          case RECURSE_CHILD_CHECK_NEXT :
            //rekursion für dieses kind, für das nächste erneut checken: geoderte bedingungen
            continuation = traverseTreeReverse(child, condition, handler, null);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            break;
          case NO_RECURSION_CHECK_NEXT :
            //keine rekursion für dieses kind, für das nächste erneut checken: geoderte bedingungen
            break;
          case RECURSE_ALL :
            //für dieses und alle folgenden kinder rekursion (entspricht suchen nach > bedingung, wenn für alle folgenden kinder klar ist, was passieren muss)
            //zb bei >: result.isHit = true, result.recursionRequest = RECURSE_ALL
            checkCondition = false;
            continuation = traverseTreeReverse(child, null, handler, result);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            break;
          case RECURSE_CHILD_ONLY :
            //nur für das kind rekursion, und nicht für folgende (entspricht suchen nach knoten mit gleichheits-bedingung)    
            continuation = traverseTreeReverse(child, condition, handler, null);
            if (continuation == TraversalContinuation.BREAK) {
              //z.b. maxrows wurde in rekursion erreicht
              return continuation;
            }
            //fall through
          case NO_RECURSION :
            // zb bei < bedingung, wenn alle kinder gefunden wurden 
            breakloop = true;
            break;
          default :
            throw new RuntimeException("unhandled result: " + result);
        }

        if (result.isHit()) {
          /*    if (lockedNode != null) {
                if (handler.needsWriteLock()) {
                  //lock upgrade nicht möglich
                  lockedNode.getLock().readLock().unlock();
                  lockedNode.getLock().writeLock().lock();
                  //TODO:
                  //checken, dass die kinder dieses knotens nicht geändert wurden
                  //das kann passieren, wenn zb ein rebalance thread das writelock zuerst bekommt
                  //dann ist man nun evtl an einer ganz anderen stelle im baum (sowohl child als auch parent können im baum ihre position geändert haben)
                  //idee 1: 
                  //=> solange zum parent zurück, bis parent sich nicht geändert hat.
                  //von dort aus dann traverseTree erneut aufrufen
                  //frage: was passiert mit bereits behandelten knoten (handler ausgeführt), die dann erneut gecheckt werden?
                  //antwort: needsWriteLock UseCase => entfernen und/oder hinzufügen von knoten im baum. dann darf man handler nicht erneut ausführen
                  
                  //idee 2:
                  //=> bei dem knoten weitermachen, auch wenn der knoten nun an einem anderen parent hängt, weil die position im baum
                  //nicht so wichtig ist, wie die position in der sortierreihenfolge (diese ist aber gleich geblieben, weil der wert
                  //des knotens sich nicht ändern kann).
                  //dann muss man bei der rekursion an den parent-aufruf den neuen parentknoten zurückgeben, weil sich dieser geändert haben kann.
                  //ausserdem muss die stacktiefe nicht mehr mit der tiefe des aktuellen knotens im baum übereinstimmen. => kompliziert.
                  
                  //es kann ausserdem sein, dass der parent derweil ein anderes readlock besitzt, und dieses im handler versucht wird, auf 
                  //ein writelock zu upgraden (=>deadlock).
                }
              }*/
          try {
            continuation = handler.handle(child);
          } finally {
            /*if (lockedNode != null) {
              if (handler.needsWriteLock()) {
                //lock downgrade
                lockedNode.getLock().readLock().lock();
                lockedNode.getLock().writeLock().unlock();
              }
            }*/
          }
          //FIXME beim einfügen oder delete anderen returnwert, weil hier die liste der kinder evtl geändert wird? (removenode oder addnode)
          if (continuation == TraversalContinuation.BREAK) {
            //z.b. maxrows erreicht, die gefunden werden sollen
            return continuation;
          }
        }
        if (breakloop) {
          break;
        }
      }
    } finally {
      if (lockedNode != null) {
        if (handler.needsWriteLock()) {
          lockedNode.getLock().writeLock().unlock();
        } else {
          lockedNode.readLock().unlock();
        }
      }
    }

    return TraversalContinuation.CONTINUE;
  }

}
