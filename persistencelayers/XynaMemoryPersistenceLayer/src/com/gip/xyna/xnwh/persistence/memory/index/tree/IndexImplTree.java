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
package com.gip.xyna.xnwh.persistence.memory.index.tree;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdate;
import com.gip.xyna.xnwh.persistence.memory.index.AtomicBulkUpdateBase;
import com.gip.xyna.xnwh.persistence.memory.index.Condition;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.index.ResultHandler;
import com.gip.xyna.xnwh.persistence.memory.index.tree.TreeTraversal.ConditionResult;
import com.gip.xyna.xnwh.persistence.memory.index.tree.TreeTraversal.Handler;
import com.gip.xyna.xnwh.persistence.memory.index.tree.TreeTraversal.RecursionRequest;
import com.gip.xyna.xnwh.persistence.memory.index.tree.TreeTraversal.TraversalCondition;
import com.gip.xyna.xnwh.persistence.memory.index.tree.TreeTraversal.TraversalContinuation;



public class IndexImplTree<E extends Comparable<E>, F> implements Index<E, F> {
  
  public interface HasSize {
    
    public boolean isEmpty();
  }

  public final static class IndexNodeValue<E extends Comparable<E>, F> implements Comparable<IndexNodeValue<E, F>>, HasSize {

    private E key;
    private List<F> values;

    private IndexNodeValue() {
    }

    public int compareTo(IndexNodeValue<E, F> o) {
      if (key == null) {
        if (o.key == null) {
          return 0;
        }
        return -1;
      }
      if (o.key == null) {
        return 1;
      }
      return key.compareTo(o.key);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(key).append("=").append(values);
      return sb.toString();
    }

    public List<F> get() {
      return values;
    }


    public E getKey() {
      return key;
    }


    public boolean isEmpty() {
      return values.size() == 0;
    }

  }


  private static class IndexTraversalConditionSmaller<E extends Comparable<E>, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>> {

    private Condition<E> condition;


    public IndexTraversalConditionSmaller(Condition<E> condition) {
      this.condition = condition;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> currentNode, boolean reverseTraversal) {

      E lookupValue = condition.getLookupValue();
      int compared = lookupValue.compareTo(currentNode.getValue().key);
      if (reverseTraversal) {
        if (compared < 0) {
          //keines der kinder kann passen
          return new ConditionResult(false, RecursionRequest.NO_RECURSION_CHECK_NEXT);
        } else if (compared > 0) {
          //dieser knoten passt, alle folgenden geschwister m�ssen auch passen, bei den kindern k�nnen aber noch welche existieren, die nciht passen
          //FIXME hier k�nnte man f�r performance noch einen neuen recursionrequest (RECURSE_CHILD_NEXT_RECURSE_ALL) einf�hren, der weniger checks ab dem n�chsten geschwisterknoten durchf�hrt
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else {
          //in dem teilbaum findet sich nichts mehr
          //FIXME oben erw�hnter recursionrequest kann hier auch verwendet werden f�r die n�chsten geschwisterknoten
          return new ConditionResult(false, RecursionRequest.NO_RECURSION_CHECK_NEXT);
        }
      } else {
        //falls gesuchter wert > currentValue => rekursion
        //falls gesuchter wert = currentValue => stop
        //falls gesuchter wert < currentValue => nichts zu tun 
        if (compared < 0) {
          return new ConditionResult(false, RecursionRequest.NO_RECURSION);
        } else if (compared > 0) {
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else {
          return new ConditionResult(false, RecursionRequest.NO_RECURSION);
        }
      }
    }

  }


  private static class IndexTraversalConditionBigger<E extends Comparable<E>, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>> {

    private Condition<E> condition;


    public IndexTraversalConditionBigger(Condition<E> condition) {
      this.condition = condition;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> currentNode, boolean reverseTraversal) {
      E lookupValue = condition.getLookupValue();
      int compared = lookupValue.compareTo(currentNode.getValue().key);
      if (reverseTraversal) {
        if (compared < 0) {
          //ganzer teilbaum ist gesucht
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else if (compared > 0) {
          //es k�nnen gr��ere gr��ere knoten im teilbauem existieren, im n�chsten geschwister aber nicht mehr
          return new ConditionResult(false, RecursionRequest.RECURSE_CHILD_ONLY);
        } else {
          //es k�nnen gr��ere gr��ere knoten im teilbauem existieren, im n�chsten geschwister aber nicht mehr
          return new ConditionResult(false, RecursionRequest.RECURSE_CHILD_ONLY);
        }
      } else {
        //falls gesuchter wert < currentValue => weiter
        //falls gesuchter wert = currentValue => weiter
        //falls gesuchter wert > currentValue => n�chsten geschwisterknoten checken
        if (compared < 0) {
          return new ConditionResult(true, RecursionRequest.RECURSE_ALL);
        } else if (compared > 0) {
          return new ConditionResult(false, RecursionRequest.RECURSE_IF_NEXTSIBLING_ALL_RECURSION);
        } else {
          return new ConditionResult(false, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        }
      }
    }

  }

  private static class IndexTraversalConditionSmallerOrEqual<E extends Comparable<E>, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>> {

    private Condition<E> condition;


    public IndexTraversalConditionSmallerOrEqual(Condition<E> condition) {
      this.condition = condition;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> currentNode, boolean reverseTraversal) {
      E lookupValue = condition.getLookupValue();
      if (lookupValue == null) {
        throw new RuntimeException("1");
      }
      if (currentNode == null) {
        throw new RuntimeException("2");
      }
      if (currentNode.getValue() == null) {
        throw new RuntimeException("3");
      }
      int compared = lookupValue.compareTo(currentNode.getValue().key);
      if (reverseTraversal) {
        if (compared < 0) {
          //alle knoten in diesem teilbaum sind zu gro�
          return new ConditionResult(false, RecursionRequest.NO_RECURSION_CHECK_NEXT);
        } else if (compared > 0) {
          //in dem teilbaum k�nnen noch zu gro�e knoten sein
          //FIXME siehe oben: neuer recursionrequest f�r die n�chsten geschwister
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else {
          //alle kinder sind zu gro�, also n�chste geschwister testen
          //FIXME siehe oben: neuer recursionrequest f�r die n�chsten geschwister
          return new ConditionResult(true, RecursionRequest.NO_RECURSION_CHECK_NEXT);
        }
      } else {
        //falls gesuchter wert > currentValue => rekursion
        //falls gesuchter wert = currentValue => nur noch diesen knoten verarbeiten
        //falls gesuchter wert < currentValue => nichts zu tun
        if (compared < 0) {
          return new ConditionResult(false, RecursionRequest.NO_RECURSION);
        } else if (compared > 0) {
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else {
          return new ConditionResult(true, RecursionRequest.NO_RECURSION);
        }
      }
    }

  }


  private static class IndexTraversalConditionBiggerOrEqual<E extends Comparable<E>, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>> {

    private Condition<E> condition;


    public IndexTraversalConditionBiggerOrEqual(Condition<E> condition) {
      this.condition = condition;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> currentNode, boolean reverseTraversal) {
      //falls gesuchter wert < currentValue => weiter
      //falls gesuchter wert = currentValue => weiter
      //falls gesuchter wert > currentValue => n�chsten geschwisterknoten checken
      E lookupValue = condition.getLookupValue();
      int compared = lookupValue.compareTo(currentNode.getValue().key);
      if (reverseTraversal) {
        if (compared < 0) {
          //alle kinder geh�ren zum resultset
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        } else if (compared > 0) {
          //es k�nnen kinder in diesem teilbaum zum resultset geh�ren, aber folgende geschwisterknoten k�nnen ignoriert werden
          return new ConditionResult(false, RecursionRequest.RECURSE_CHILD_ONLY);
        } else {
        //es k�nnen kinder in diesem teilbaum zum resultset geh�ren, aber folgende geschwisterknoten k�nnen ignoriert werden
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_ONLY);
        }
      } else {
        if (compared < 0) {
          return new ConditionResult(true, RecursionRequest.RECURSE_ALL);
        } else if (compared > 0) {
          return new ConditionResult(false, RecursionRequest.RECURSE_IF_NEXTSIBLING_ALL_RECURSION);
        } else {
          return new ConditionResult(true, RecursionRequest.RECURSE_CHILD_CHECK_NEXT);
        }
      }
    }

  }

  private static class IndexTraversalConditionEquals<E extends Comparable<E>, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>> {

    private Condition<E> condition;


    public IndexTraversalConditionEquals(Condition<E> condition) {
      this.condition = condition;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> currentNode, boolean reverseTraversal) {
      E lookupValue = condition.getLookupValue();
      int compared = lookupValue.compareTo(currentNode.getValue().key);
      if (reverseTraversal) {
        if (compared < 0) {
          //alle kinder sind auch gr��er
          return new ConditionResult(false, RecursionRequest.NO_RECURSION_CHECK_NEXT);
        } else if (compared > 0) {
          //nur in diesem knoten rekursion, in sp�teren kann es nicht sein, in vorherigen auch nicht, sonst h�tten wir es bereits gefunden
          return new ConditionResult(false, RecursionRequest.RECURSE_CHILD_ONLY);
        } else {
          //fertig
          return new ConditionResult(true, RecursionRequest.NO_RECURSION);
        }
      } else {
        //falls gesuchter wert < currentValue => abbrechen
        //falls gesuchter wert = currentValue => fertig
        //falls gesuchter wert > currentValue => n�chsten geschwisterknoten checken
        if (compared < 0) {
          return new ConditionResult(false, RecursionRequest.NO_RECURSION, true);
        } else if (compared > 0) {
          //TODO performance: suboptimal, wenn der n�chste knoten == ist, weil dann wird trotzdem rekursiert, obwohl das nicht notwendig ist. allerdings nur ein level tief
          return new ConditionResult(false, RecursionRequest.RECURSE_IF_NEXTSIBLING_A);
        } else {
          return new ConditionResult(true, RecursionRequest.NO_RECURSION, true);
        }
      }
    }

  }

  private static class IndexHandler<E extends Comparable<E>, F> implements Handler<IndexNodeValue<E, F>> {

    private ResultHandler<F> resultHandler;


    public IndexHandler(ResultHandler<F> resultHandler) {
      this.resultHandler = resultHandler;
    }


    public TraversalContinuation handle(AbstractNode<IndexNodeValue<E, F>> node) {
      if (!resultHandler.handle(node.getValue().values)) {
        return TraversalContinuation.BREAK;
      }
      return TraversalContinuation.CONTINUE;
    }


    public boolean needsWriteLock() {
      return false;
    }

  }

  private enum AddElementCase {
    ALREADY_ADDED, LAST;
  }


  /**
   * f�gt element zu knoten hinzu, falls wert �bereinstimmt. erstellt neuen knoten, falls kein �bereinstimmender knoten
   * gefunden wurde und der letzte gr��er war.
   */
  private static class AddElementHandler<E extends Comparable<E>, F>
      implements
        Handler<IndexNodeValue<E, F>>,
        TraversalCondition<IndexNodeValue<E, F>> {

    private AddElementCase hasAdded = AddElementCase.LAST;
    private E key;
    private F value;
    private IndexImplTree<E, F> index;


    public AddElementHandler(E key, F value, IndexImplTree<E, F> index) {
      this.key = key;
      this.value = value;
      this.index = index;
    }


    public TraversalContinuation handle(AbstractNode<IndexNodeValue<E, F>> node) {
      index.addEntryToTreeAtPosition(node, key, value);
      hasAdded = AddElementCase.ALREADY_ADDED;
      return TraversalContinuation.BREAK;
    }


    public AddElementCase mustAddElement() {
      return hasAdded;
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> node, boolean reverseTraversal) {
      int compare = key.compareTo(node.getValue().key);
      if (compare == 0) {
        return new ConditionResult(true, RecursionRequest.NO_RECURSION, true);
      } else if (compare > 0) {
        return new ConditionResult(false, RecursionRequest.RECURSE_IF_NEXTSIBLING_A);
      } else {
        //erster knoten, wo der alte wert gr��er als der neue ist => einf�geposition parent.insertBefore 
        return new ConditionResult(true, RecursionRequest.NO_RECURSION, true);
      }
    }


    public boolean needsWriteLock() {
      return true;
    }

  }

  public interface NodeCreator<E extends Comparable<E>, F> extends NodeTypeTransformer<IndexNodeValue<E, F>> {

    public AbstractNode<IndexNodeValue<E, F>> createNode(int depth, AbstractNode<IndexNodeValue<E, F>> root,
                                                         IndexNodeValue<E, F> value, IndexNodeValue<E, F> parentValue);
  }


  private AbstractNode<IndexNodeValue<E, F>> index;
  private TreeTraversal<IndexNodeValue<E, F>> traversal = new TreeTraversal<IndexNodeValue<E, F>>();
  private Rebalancer<IndexNodeValue<E, F>> rebalancer = new Rebalancer<IndexNodeValue<E, F>>();
  private NodeCreator<E, F> nodeCreator;
  private final int rebalanceFrequencyInBulkUpdates;


  public IndexImplTree(NodeCreator<E, F> nodeCreator, int rebalanceFrequencyInBulkUpdates) {
    this.nodeCreator = nodeCreator;
    index = nodeCreator.createNode(0, null, null, null);
    index.setValue(createValue(null, null));
    this.rebalanceFrequencyInBulkUpdates = Math.max(2, rebalanceFrequencyInBulkUpdates);
  }

  public IndexImplTree(NodeCreator<E, F> nodeCreator) {
    this(nodeCreator, 10);
  }

  
  public void rebalance() {
    LockedOrderedNode<IndexNodeValue<E, F>> lockedRoot = null;
    if (index instanceof LockedOrderedNode) {
      lockedRoot = (LockedOrderedNode<IndexNodeValue<E, F>>) index;
    }
    if (lockedRoot != null) {
      //beim �ndern des baums (hinzuf�gen oder entfernen von knoten) wird u.a. rekursiv die gr��e der knoten geupdated. daf�r werden wiederum 
      //writelocks ben�tigt => gleich in der richtigen reihenfolge writelocks holen.
      //es gen�gt, das root writelock zu holen.
      lockedRoot.getLock().writeLock().lock();
    }
    try {
      rebalancer.rebalanceAll(nodeCreator, index);
    } finally {
      if (lockedRoot != null) {
        lockedRoot.getLock().writeLock().unlock();
      }
    }
  }


  public void readOnly(ResultHandler<F> resultHandler, Condition<E> condition, boolean reverse) {
    TraversalCondition<IndexNodeValue<E, F>> ttCondition;
    switch (condition.getType()) {
      case EQUALS :
        ttCondition = new IndexTraversalConditionEquals<E, F>(condition);
        break;
      case BIGGER :
        ttCondition = new IndexTraversalConditionBigger<E, F>(condition);
        break;
      case SMALLER :
        ttCondition = new IndexTraversalConditionSmaller<E, F>(condition);
        break;
      case SMALLER_OR_EQUAL :
        ttCondition = new IndexTraversalConditionSmallerOrEqual<E, F>(condition);
        break;
      case BIGGER_OR_EQUAL :
        ttCondition = new IndexTraversalConditionBiggerOrEqual<E, F>(condition);
        break;
      default :
        throw new RuntimeException("unhandled condition type: " + condition.getType());
    }
    Handler<IndexNodeValue<E, F>> handler = new IndexHandler<E, F>(resultHandler);
    traversal.traverseTree(index, ttCondition, handler, reverse);
  }


  private IndexNodeValue<E, F> createValue(E e, F f) {
    IndexNodeValue<E, F> value = new IndexNodeValue<E, F>();
    value.key = e;
    value.values = new ArrayList<F>();
    value.values.add(f);
    return value;
  }


  private AbstractNode<IndexNodeValue<E, F>> createNode(E e, F f, AbstractNode<IndexNodeValue<E, F>> parent) {
    AbstractNode<IndexNodeValue<E, F>> node =
        nodeCreator.createNode(parent.getDepth() + 1, index, createValue(e, f), parent.getValue());
    return node;
  }


  public void add(E e, F f) {
    AddElementHandler<E, F> handler = new AddElementHandler<E, F>(e, f, this);
    AbstractNode<IndexNodeValue<E, F>> root = index;
    traversal.traverseTree(root, handler, handler);
    switch (handler.mustAddElement()) {
      case ALREADY_ADDED :
        //ntbd
        break;
      case LAST :
        addEntryToRoot(e, f);
        break;
      default :
        throw new RuntimeException("unhandled case");
    }
  }


  private AbstractNode<IndexNodeValue<E, F>> addEntryToRoot(E e, F f) {
    AbstractNode<IndexNodeValue<E, F>> newChild = createNode(e, f, index);
    index.addChild(newChild);
    rebalancer.nodeToBeRebalanced(index);
    return newChild;
  }

  private static <T extends Comparable<T>> int compareWithNullCheck(T t0, T t1) {
    if (t0 == null) {
      if (t1 == null) {
        return 0;
      }
      return -1;
    }
    if (t1 == null) {
      return 1;
    }
    return t0.compareTo(t1);
  }
  

  /**
   * klasse mit der man es hinbekommt, atomar mehrere knoten zu bearbeiten: values hinzuf�gen oder entfernen oder
   * zuweisen eines neuen keys zu einem existierenden value.
   */
  public final class AtomicBulkUpdateImpl extends AtomicBulkUpdateBase<E, F>
      implements
        TraversalCondition<IndexNodeValue<E, F>>,
        Handler<IndexNodeValue<E, F>> {

    private AtomicBulkAction currentAction;
    private int cnt;

    public AtomicBulkUpdateImpl(ResultHandler<F> handler) {
      super(handler);
    }

    public void commit() {
      if (actions.size() == 0) {
        return;
      }
      currentAction = actions.poll();
      while (actions.size() > 0 || currentAction != null) {
        traversal.traverseTree(index, this, this);
        if (cnt > 0) {
          rebalance();
          cnt = 0;
        } else {
          //�berbleibende actions sind ausserhalb des baums.
          break;
        }
      }

      //nun sind nur noch adds �brig, die nicht innerhalb des baums eingef�gt werden konnten, und deshalb am ende angef�gt werden m�ssen.
      AbstractNode<IndexNodeValue<E, F>> lastAdded = null;
      while (currentAction != null) {
        //wert k�nnte gleich dem sein, der vorher schon geaddet wurde
        if (lastAdded != null && compareWithNullCheck(lastAdded.getValue().key, currentAction.getKey()) == 0) {
          handleInternally(lastAdded);
          continue;
        }

        //echt gr��er als letzter wert im index
        if (currentAction instanceof AtomicBulkUpdateBase.AtomicAddAction) {

          AtomicAddAction addAction = (AtomicAddAction) currentAction;
          lastAdded = addEntryToRoot(addAction.e, addAction.f);
          if (++cnt % rebalanceFrequencyInBulkUpdates == 0) {
            rebalance();
          }
        } else if (currentAction instanceof AtomicBulkUpdateBase.AtomicUpdateAction) {

          AtomicUpdateAction updateAction = (AtomicUpdateAction) currentAction;
          if (updateAction.nextKeyIsForAdd()) {
            lastAdded = addEntryToRoot(updateAction.currentKey, updateAction.value);
            if (++cnt % rebalanceFrequencyInBulkUpdates == 0) {
              rebalance();
            }
          } else {
            throw new RuntimeException("could not find value " + updateAction.value + " to update. "
                + updateAction.firstHandle);
          }
        }
        
        currentAction = actions.poll();
      }
    }


    public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> node, boolean reverseTraversal) {
      E currentlyCheckedKeyFromTree = node.getValue().key;
      E currentlyCheckedKeyFromBulk = getCurrentKey();
    /*  if (currentlyCheckedKeyFromBulk == null) {
        return new ConditionResult(false, RecursionRequest.NO_RECURSION);
      }*/
      int compare = compareWithNullCheck(currentlyCheckedKeyFromTree, currentlyCheckedKeyFromBulk);
      boolean isHit = false;
      boolean a = false;
      RecursionRequest recursionRequest;
      if (compare == 0) {
        isHit = true;
        recursionRequest = RecursionRequest.RECURSE_IF_NEXTSIBLING_A;
      } else if (compare < 0) {
        recursionRequest = RecursionRequest.RECURSE_IF_NEXTSIBLING_A;
      } else {
        //compare > 0
        //=> wie in AddElementHandler muss man hitten, wenn man beim n�chstgr��eren knoten ist, weil man offenbar bisher nicht getroffen hat.
        if (currentAction instanceof AtomicBulkUpdateBase.AtomicAddAction) {
          isHit = true;
        } else if (currentAction instanceof AtomicBulkUpdateBase.AtomicUpdateAction) {
          AtomicUpdateAction updateAction = (AtomicUpdateAction) currentAction;
          if (updateAction.nextKeyIsForAdd()) {
            isHit = true;
          }
        }
        recursionRequest = RecursionRequest.RECURSE_CHILD_CHECK_NEXT;
        a = true;
      }

      ConditionResult result = new ConditionResult(isHit, recursionRequest, a);
      return result;
    }


    private E getCurrentKey() {
      return currentAction.getKey();
    }


    public boolean needsWriteLock() {
      return true;
    }
    
    private void addEntryInternally(Map<E, AbstractNode<IndexNodeValue<E, F>>> additionallyCreatedNodes, AbstractNode<IndexNodeValue<E, F>> node, E key, F value) {
      AbstractNode<IndexNodeValue<E, F>> existingNode = additionallyCreatedNodes.get(key);
      if (existingNode != null) {
        addEntryToTreeAtPosition(existingNode, key, value);
      } else {
        AbstractNode<IndexNodeValue<E, F>> newNode =
            addEntryToTreeAtPosition(node, key, value);
        if (newNode != null) {
          additionallyCreatedNodes.put(key, newNode);
        }
      }
    }

    
    public TraversalContinuation handle(AbstractNode<IndexNodeValue<E, F>> node) {
      if (++cnt % rebalanceFrequencyInBulkUpdates == 0) {
        return TraversalContinuation.BREAK;
      } else {
        return handleInternally(node);
      }
    }
    
    
    public TraversalContinuation handleInternally(AbstractNode<IndexNodeValue<E, F>> node) {

      //es kann mehrere actions f�r diesen knoten geben => so lange loopen, bis man die alle behandelt hat
      //wenn ein add und ein remove einen gleichen key haben, gehen sie auf unterschiedliche (aufeinanderfolgende) knoten => muss also anders behandelt werden.
      //andersrum kann es sein, dass ein add und ein remove auf den gleichen knoten gehen, obwohl sie unterschiedliche keys haben.
      Map<E, AbstractNode<IndexNodeValue<E, F>>> additionallyCreatedNodes = new HashMap<E, AbstractNode<IndexNodeValue<E, F>>>();
      while (true) {
        AtomicBulkAction action = currentAction;
        Boolean comparisonIfChangedUpdate = null;
        if (action instanceof AtomicBulkUpdateBase.AtomicUpdateAction) {
          AtomicUpdateAction updateAction = (AtomicUpdateAction) action;

          if (updateAction.nextKeyIsForAdd()) {
            //add

            addEntryInternally(additionallyCreatedNodes, node, updateAction.newKey, updateAction.value);
          } else {
            //remove

            removeEntry(node, updateAction.oldKey, updateAction.value);
          }

          //spezialfall updateaction => muss zweimal currentAction sein => wieder zur liste hinzuf�gen
          if (updateAction.firstHandle) {
            comparisonIfChangedUpdate = currentAction.compareTo(action) != 0; //wird unten ben�tigt, aber dann ist die action bereits ge�ndert
            updateAction.firstHandle = false;
            //key tauschen.
            updateAction.currentKey =
                updateAction.nextKeyIsForAdd() ? updateAction.oldKey : updateAction.newKey;
            actions.add(updateAction);
          }
        } else if (action instanceof AtomicBulkUpdateBase.AtomicAddAction) {
          AtomicAddAction addAction = (AtomicAddAction) action;

          addEntryInternally(additionallyCreatedNodes, node, addAction.e, addAction.f);
        } else {
          //action instanceof Index.AtomicBulkUpdate.AtomicRemoveAction
          AtomicRemoveAction removeAction = (AtomicRemoveAction) action;

          removeEntry(node, removeAction.e, removeAction.f);
        }
        if (actions.size() > 0) {
          currentAction = actions.poll(); //n�chste action lesen
          //n�chste action muss entweder auch auf den gleichen knoten angewendet werden, oder auf einen sp�teren.
          //hier also entscheidung, ob die while-schleife nochmal durchlaufen werden muss, oder nicht.
          //sie muss genauo so lange weiter durchlaufen werden, wie der aktuelle key <= dem node-key ist.
          
          //currentaction == action f�hrt dazu, dass compare == 0 ist, obwohl immer != 0 gemeint ist (update hat sich ge�ndert von add auf remove oder andersrum)
          if (currentAction == action) {
            //update
            //=> (1) unterschiedlicher wert oder (2) gleicher wert mit unterschiedlichem add/remove.
            int compare = currentAction.getKey().compareTo(node.getValue().key);
            if (compare == 0) {
              //(2)
              if (!currentAction.isAddAction()) {
                //add => remove: 
                continue;
              } else {
                //remove => add: kann nicht sein (passt nicht zusammen mit dem compare)
                throw new RuntimeException("expected removeaction");
              }
            } else if (compare < 0) {
              if (currentAction.isAddAction()) {
                continue;
              }
            }
            break;
          } else if ((comparisonIfChangedUpdate != null && !comparisonIfChangedUpdate) 
                  || (comparisonIfChangedUpdate == null && currentAction.compareTo(action) != 0)) {
            int compare = currentAction.getKey().compareTo(node.getValue().key);
            if (compare > 0) {
              break;
            }
          }
          if (++cnt % rebalanceFrequencyInBulkUpdates == 0) {
            return TraversalContinuation.BREAK;
          }
          //else action ist gleich => entweder zweimal hintereinander add, oder zweimal hintereinander remove auf den gleichen wert.
          // => einfach weiter.
        } else {
          currentAction = null;
          return TraversalContinuation.BREAK;
        }
      }
      return TraversalContinuation.CONTINUE;
    }

  }


  public AtomicBulkUpdate<E, F> startBulkUpdate(ResultHandler<F> handler) {
    return new AtomicBulkUpdateImpl(handler);
  }


  public void removeEntry(AbstractNode<IndexNodeValue<E, F>> node, E key, F value) {
    if (compareWithNullCheck(node.getValue().key, key) == 0) {
      if (!node.getValue().values.remove(value)) {
        throw new RuntimeException("could not remove entry");
      }
    } else {
      //im parent beim n�chsten knoten schauen
      removeEntry(node.getParent().getChild(node.getParent().getChildIndex(node) + 1), key, value);
    }
    
    //rebalance entfernt "leere" knoten
    if (node.getValue().values.size() == 0) {
      rebalancer.nodeToBeRemoved(node);
    }
  }


  /**
   * f�gt neuen knoten als sibling vor dem �bergebenen knoten ein, oder modifiziert den knoten, falls der key gleich ist. 
   */
  public AbstractNode<IndexNodeValue<E, F>> addEntryToTreeAtPosition(AbstractNode<IndexNodeValue<E, F>> node, E e, F f) {
    IndexNodeValue<E, F> nodeValue = node.getValue();
    if (compareWithNullCheck(nodeValue.key, e) == 0) {
      nodeValue.values.add(f); //FIXME setValue, damit knoten zb im cluster geupdated wird?
      return null;
    } else {
      //erster knoten, wo der alte wert gr��er als der neue ist => einf�geposition parent.insertBefore

      AbstractNode<IndexNodeValue<E, F>> parent = node.getParent();
      AbstractNode<IndexNodeValue<E, F>> newNode = createNode(e, f, parent);
      parent.addChild(newNode);
      rebalancer.nodeToBeRebalanced(parent);
      return newNode;
    }
  }


  //f�r tests:
  public AbstractNode<IndexNodeValue<E, F>> getRoot() {
    return index;
  }
  

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("{");
    traversal.traverseTree(index, new TraversalCondition<IndexImplTree.IndexNodeValue<E, F>>() {

      @Override
      public ConditionResult check(AbstractNode<IndexNodeValue<E, F>> node, boolean reverseTraversal) {
        return new ConditionResult(true, RecursionRequest.RECURSE_ALL);
      }
    }, new Handler<IndexImplTree.IndexNodeValue<E, F>>() {

      @Override
      public TraversalContinuation handle(AbstractNode<IndexNodeValue<E, F>> node) {
        sb.append(node).append(", ");
        return TraversalContinuation.CONTINUE;
      }


      @Override
      public boolean needsWriteLock() {
        return false;
      }
    });
    sb.append("}");
    return sb.toString();
  }

}
