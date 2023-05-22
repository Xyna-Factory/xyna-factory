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
package com.gip.xyna.utils.collections;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import com.gip.xyna.utils.collections.Graph.HasUniqueStringIdentifier;


//gerichteter graph
//achtung, knoten nicht �ndern, nachdem man darauf berechnungen durchgef�hrt hat
public class Graph<C extends HasUniqueStringIdentifier> {
  public interface HasUniqueStringIdentifier {

    public String getId();
  }

  public static class Node<C extends HasUniqueStringIdentifier> {
    /*
     * zkylische gerichtete graphen performant rekursiv die teilgraphen bestimmen lassen. 
     * etwa faktor 10 schneller als naives "f�r jeden knoten alle dependencies separat bestimmen"
     * 
     * idee: bei der rekursion bei allen besuchten knoten bereits die rekursiven dependencies mit speichern.
     *       dabei gibt es die komplikation bei zyklischen abh�ngigkeiten. diese wird behandelt, indem alle
     *       zu einem zyklus geh�renden knoten einen stellvertreter-knoten bestimmen (leader), und nur dieser
     *       die abh�ngigkeiten pflegt.
     */

    private final Map<String, Node<C>> dependencies = new HashMap<String, Node<C>>();
    private Map<String, Node<C>> dependenciesRecursively = new HashMap<String, Node<C>>(); //enth�lt this
    private Node<C> cycleLeader; //stellvertreter der im zyklus befindlichen nodes
    private boolean cycleComplete = false; //alle nodes des cycles haben alle ihre dependencies rekursiv bestimmt
    private Set<Node<C>> cycleMembers; //welche nodes geh�ren zum cycle
    private Map<String, Node<C>> parents = new HashMap<String, Node<C>>(); //wer hat den node als direkte dependency? bei cycles sind die parents nur beim leader gesetzt. sie enthalten dann nicht knoten aus dem cycle
    
    private final C content; //content


    public Node(C content) {
      this.content = content;
    }


    public Map<String, Node<C>> getDependenciesRecursively() {
      if (cycleLeader != null) {
        return cycleLeader.dependenciesRecursively;
      }
      if (dependenciesRecursively.size() > 0) {
        return dependenciesRecursively;
      }

      Stack<Node<C>> s = new Stack<Node<C>>();
      s.push(this);
      return getDependenciesRecursively(s);
    }


    private Map<String, Node<C>> getDependenciesRecursively(Stack<Node<C>> stack) {
      if (cycleLeader != null) {
        return cycleLeader.dependenciesRecursively;
      }

      if (dependenciesRecursively.size() == 0) {
        dependenciesRecursively.put(content.getId(), this);
        deps : for (Entry<String, Node<C>> e : dependencies.entrySet()) {
          Map<String, Node<C>> alreadyCollected = dependenciesRecursively;
          if (cycleLeader != null) {
            alreadyCollected = cycleLeader.dependenciesRecursively;
          }
          if (!alreadyCollected.containsKey(e.getKey())) {
            
            //cycle detection �ber stack
            for (int i = 0; i < stack.size(); i++) {
              if (stack.get(i).content.getId().equals(e.getKey())) {
                //cycle -> bei allen in zyklus den cycleleader eintragen und die bisher gesammelten dependencies im cycleleader aggregieren
                Node<C> localCycleLeader = stack.get(i);
                if (localCycleLeader.cycleLeader != null) {
                  //bestehendem cycle anschliessen
                  localCycleLeader = localCycleLeader.cycleLeader;
                }
                if (localCycleLeader.cycleMembers == null) {
                  localCycleLeader.cycleMembers = new HashSet<Node<C>>();
                }
                addStackElementsToCycle(localCycleLeader, stack, i + 1);
                continue deps;
              }
            }

            //bestehendem cycle anschliessen?!
            if (e.getValue().cycleLeader != null && !e.getValue().cycleLeader.cycleComplete) {
              //falls dependency teil von unfertigem cycle ist, muss man selbst dazu geh�ren (sonst w�r der cycle nicht unfertig, sondern fertig)
              Node<C> localCycleLeader;
              if (cycleLeader != null) {
                if (cycleLeader != e.getValue().cycleLeader) {
                  //cycles mergen
                  mergeCycleLeader(cycleLeader, e.getValue().cycleLeader);
                  localCycleLeader = e.getValue().cycleLeader;
                } else {
                  //gleicher cycle: ntbd 
                  localCycleLeader = null;
                }
              } else {
                if (cycleMembers == null) {
                  cycleLeader = e.getValue().cycleLeader;
                  cycleLeader.dependenciesRecursively.putAll(dependenciesRecursively);
                  dependenciesRecursively = null;
                  cycleLeader.cycleMembers.add(this);
                  localCycleLeader = cycleLeader;
                } else {
                  //man ist selbst leader
                  if (this != e.getValue().cycleLeader) {
                    mergeCycleLeader(this, e.getValue().cycleLeader);
                    localCycleLeader = e.getValue().cycleLeader;
                  } else {
                    //gleicher cylce: ntbd
                    localCycleLeader = null;
                  }
                }
              }
              if (localCycleLeader != null) {
                //ausserdem geh�ren dann alle stackelemente bis zum cycleleader dazu
                for (int i = 0; i < stack.size(); i++) {
                  if (stack.get(i) == localCycleLeader) {
                    addStackElementsToCycle(localCycleLeader, stack, i + 1);
                    break;
                  }
                }
              }
              continue;
            }

            //rekursion
            stack.push(e.getValue());
            Map<String, Node<C>> childDepsRecursively = e.getValue().getDependenciesRecursively(stack);
            stack.pop();

            //dependency hinzuf�gen
            if (childDepsRecursively != null) {
              if (cycleLeader != null) {
                cycleLeader.dependenciesRecursively.putAll(childDepsRecursively);
              } else {
                dependenciesRecursively.putAll(childDepsRecursively);
              }
            } //else: wurde bereits im kind dem gleichen leader hinzugef�gt
          }
        }

        if (cycleLeader != null) {
          return null;
        } else if (cycleMembers != null) {
          //cycleLeader hat alle members besucht und muss deshalb fertig sein
          cycleComplete = true;
        }
      }

      return dependenciesRecursively;
    }


    private void mergeCycleLeader(Node<C> cycleLeaderToMerge, Node<C> cycleLeaderToUse) {
      cycleLeaderToUse.cycleMembers.addAll(cycleLeaderToMerge.cycleMembers);
      for (Node<C> dep : cycleLeaderToMerge.cycleMembers) {
        dep.cycleLeader = cycleLeaderToUse;
      }
      cycleLeaderToUse.cycleMembers.add(cycleLeaderToMerge);
      cycleLeaderToMerge.cycleMembers = null;
      cycleLeaderToMerge.cycleLeader = cycleLeaderToUse;
      cycleLeaderToUse.dependenciesRecursively.putAll(cycleLeaderToMerge.dependenciesRecursively);
      cycleLeaderToMerge.dependenciesRecursively = null;
    }


    private void addStackElementsToCycle(Node<C> cycleLeader, Stack<Node<C>> stack, int startIdx) {
      for (int j = startIdx; j < stack.size(); j++) {
        Node<C> stackEl = stack.get(j);
        //falls der neue cyclemember vorher cycleleader war, dessen members umh�ngen
        if (stackEl.cycleMembers != null) {
          for (Node<C> cycleMember : stackEl.cycleMembers) {
            cycleMember.cycleLeader = cycleLeader;
          }
          cycleLeader.cycleMembers.addAll(stackEl.cycleMembers);
          stackEl.cycleMembers = null;
        }

        stackEl.cycleLeader = cycleLeader;
        cycleLeader.cycleMembers.add(stackEl);
        if (stackEl.dependenciesRecursively != null) {
          cycleLeader.dependenciesRecursively.putAll(stackEl.dependenciesRecursively);
          stackEl.dependenciesRecursively = null;
        }
      }
    }


    public void addDependency(Node<C> dep) {
      String depId = dep.content.getId();
      dependencies.put(depId, dep);
      String ownId = content.getId();
      if (!depId.equals(ownId)) {
        dep.parents.put(ownId, this);
      }
    }


    public Collection<Node<C>> getDependencies() {
      return dependencies.values();
    }


    public C getContent() {
      return content;
    }


    public String toString() {
      StringBuilder sb = new StringBuilder(content.getId());
      sb.append(" -> {");
      boolean first = true;
      for (String dep : dependencies.keySet()) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(dep);
      }
      sb.append(" }");
      return sb.toString();
    }

  }


  private final Collection<Node<C>> nodes;
  private final List<Node<C>> roots = new ArrayList<Node<C>>();


  public Graph(Collection<Node<C>> nodes) {
    this.nodes = nodes;
  }

  public Collection<Node<C>> getNodes() {
    return nodes;
  }

  /**
   * gibt die minimale menge an knoten zur�ck, von denen aus der gesamte graph erreichbar ist
   */
  public List<Node<C>> getRoots() {
    //achtung, genauso wie oben bei dependencies recursively ermittlung wird hier die datenstruktur ge�ndert, so dass eine nachtr�gliche �nderung
    //der daten probleme macht!
    if (roots.size() != 0) {
      return roots;
    }
    for (Node<C> n : nodes) {
      n.getDependenciesRecursively(); //zykleninformationen erstellen
    }
    for (Node<C> n : nodes) {
      if (n.parents != null && n.parents.size() == 0) {
        roots.add(n);
        continue;
      }
      if (n.cycleLeader != null) {
        continue; //cycleleader verwenden
      }
      if (n.cycleMembers != null) {
        //leader
        for (Node<C> cm : n.cycleMembers) {
          n.parents.putAll(cm.parents);
          cm.parents = null;
        }
        for (Node<C> cm : n.cycleMembers) {
          n.parents.remove(cm.content.getId());
        }
        n.parents.remove(n.content.getId());
        if (n.parents.size() == 0) {
          roots.add(n);
        }
        continue;
      }
    }
    return roots;
  }

}
