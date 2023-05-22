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
package com.gip.xyna;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;



/**
 * Klasse zur Abarbeitung von Aufgaben, die voneinander abh�ngig sind und deshalb in einer bestimmten Reihenfolge
 * ausgef�hrt werden m�ssen.
 * <p>
 * Idee: Man registriert in einer beliebigen Reihenfolge Aufgaben in der Form von {@link FutureExecutionTask}s. Diese
 * definieren ihre Abh�ngigkeiten derart, dass sie bestimmen, welche anderen Tasks vor ihnen und welche nach ihnen
 * ausgef�hrt werden m�ssen.
 * <p>
 * Normalerweise darf man Aufgaben nie sofort starten, auch wenn alle angegebenen Abh�ngigkeiten erf�llt sind, weil es
 * sein k�nnte, dass zu einem sp�teren Zeitpunkt noch eine Aufgabe registriert wird, die vor der aktuellen Aufgabe
 * ausgef�hrt werden muss.<br>
 * Deshalb schliesst man die Registrierungsphase manuell mittels {@link #finishedRegistrationProcess()} ab.<br>
 * Falls man das warten auf {@link #finishedRegistrationProcess()} umgehen m�chte, kann man die fr�hstm�gliche
 * Ausf�hrung einer Aufgabe erzwingen, indem man {@link FutureExecutionTask#waitForOtherTasksToRegister()} �berschreibt
 * und <code>false</code> zur�ckgibt.<p>
 * Nach dem Aufruf von {@link #finishedRegistrationProcess()} k�nnen weitere Aufgaben hinzugef�gt werden. Diese werden
 * genauso behandelt wie vorher, d.h. erst ausgef�hrt, wenn erneut {@link #finishedRegistrationProcess()} aufgerufen wird mit oben erw�hnten Ausnahmen..
 * Bereits ausgef�hrte Aufgaben gelten weiterhin als bereits ausgef�hrt (f�r zuk�nftige Aufgaben, die davon abh�ngig sind).
 */
public class FutureExecution {

  public static final Logger logger = CentralFactoryLogging.getLogger(FutureExecution.class);

  private AtomicInteger cnt = new AtomicInteger();

  private Map<Integer, List<FutureExecutionTask>> tasks;
  private List<TaskId> alreadyExecutedTasks;
  private List<TaskId> currentlyExecutingTasks;
  private HashSet<TaskId> canceledTasks;
  private String name;
  private Documentation documentation;
  
  public static class TaskId {

    private String name;
    private Object id;

    public TaskId(int id) {
      this(Integer.valueOf(id));
    }
    
    public TaskId(Object id) {
      if( id == null ) {
        throw new IllegalArgumentException(" id is null");
      }
      if( id instanceof Class<?> ) {
        this.name = ((Class<?>)id).getSimpleName();
      } else {
        this.name = id.toString();
      }
      this.id = id;
    } 

    @Override
    public String toString() {
      return name;
    }
    
    @Override
    public boolean equals(Object obj) {
      if( obj instanceof TaskId ) {
        return id.equals( ((TaskId)obj).id );
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      return id.hashCode();
    }

    public Type getType() {
      if( id instanceof String ) {
        return Type.String;
      }
      if( id instanceof Integer ) {
        return Type.Integer;
      }
      return Type.Class;
    }
    
    enum Type { Class, String, Integer }; 
  }

  /**
   * @param name Name der FutureExecution zur Identifikation der Log-Meldungen 
   */
  public FutureExecution(String name) {
    tasks = new HashMap<Integer, List<FutureExecutionTask>>();
    alreadyExecutedTasks = new ArrayList<TaskId>();
    currentlyExecutingTasks = new ArrayList<TaskId>();
    canceledTasks = new HashSet<TaskId>();
    this.name = name;
  }
  
  public FutureExecution(String name, boolean writeDocumentation ) {
    this(name);
    if( writeDocumentation ) {
      this.documentation = new Documentation(name);
    }
  }


  public int nextId() {
    return cnt.getAndIncrement();
  }


  /**
   * F�hrt das {@link FutureExecutionTask} auf jeden Fall vor allen in {@link FutureExecutionTask#before()} angegebenen
   * {@link FutureExecutionTask}s aus. <br>
   * Ausserdem erst dann, wenn alle in {@link FutureExecutionTask#after()} angegebenen {@link FutureExecutionTask}s
   * bereits ausgef�hrt worden sind und zus�tzlich entweder 
   * <ul><li>
   * {@link #finishedRegistrationProcess()} aufgerufen wird</li></ul>
   * oder<br>
   * <ul><li>{@link FutureExecutionTask#waitForOtherTasksToRegister()} gibt false zur�ck.</li></ul> <br>
   * Der Aufruf wartet nicht auf die Ausf�hrung, wenn die Ausf�hrung nicht direkt stattfinden kann.
   */
  public void execAsync(FutureExecutionTask task) {
    if (task.waitForOtherTasksToRegister()) {
      addTask(task);
    } else {
      if (isBlockedByOtherTasks(task)) {
        addTask(task);
      } else {
        executeTask(task);
      }
    }
  }


  private void executeTask(FutureExecutionTask task) {
    if( logger.isDebugEnabled() ) {
      logger.debug("FutureExecution "+name+" executing task "+task);
    }
    synchronized (currentlyExecutingTasks) {
      currentlyExecutingTasks.add(task.getId());
    }
    long start = System.currentTimeMillis();
    boolean success = false;
    try {
      task.execute();
      success = true;
      if( documentation != null ) {
        documentation.addTask(task);
      }
    } finally {
      synchronized (currentlyExecutingTasks) {
        currentlyExecutingTasks.remove(task.getId());
        currentlyExecutingTasks.notifyAll();
      }
      synchronized (tasks) {
        alreadyExecutedTasks.add(task.getId());
      }
      long end = System.currentTimeMillis();
      if( logger.isDebugEnabled() ) {
        logger.debug("FutureExecution "+name+" executed task "+task+" in "+(end-start)+" ms" + (success?"":" unsuccessfully") );
      }
    }
  }


  private boolean isBlockedByOtherTasks(FutureExecutionTask task) {
    synchronized (tasks) {
      //task muss "after" otherTask durchgef�hrt werden? => sicherstellen, dass diese bereits durchgef�hrt wurden
      for (TaskId afterId : task.afterTasks()) {
        boolean foundAfterTaskHasBeenExecuted = false;
        for (TaskId executedTaskId : alreadyExecutedTasks) {
          if (afterId.equals(executedTaskId)) {
            foundAfterTaskHasBeenExecuted = true;
            break;
          }
        }
        if (!foundAfterTaskHasBeenExecuted) {
          return true;
        }
      }

      // otherTask muss "before" task durchgef�hrt werden? => sicherstellen, dass die otherTasks bereits durchgef�hrt
      // wurden
      for (List<FutureExecutionTask> taskList : tasks.values()) {
        for (FutureExecutionTask otherTask : taskList) {
          if (otherTask == task) {
            continue;
          }

          for (TaskId beforeId : otherTask.beforeTasks()) {
            if (beforeId.equals(task.getId())) {
              // d.h. otherTask muss zuerst durchgef�hrt werden.
              return true;
            }
          }
        }
      }
    }
    return false;
  }


  private void addTask(FutureExecutionTask task) {
    if( logger.isDebugEnabled() ) {
      logger.debug("FutureExecution "+name+" add task "+task);
    }
    synchronized (tasks) {
      Integer priority = task.getPriority();
      List<FutureExecutionTask> taskList = tasks.get(priority);
      if(taskList == null) {
        taskList = new ArrayList<FutureExecutionTask>();
        tasks.put(priority, taskList);
      }
      taskList.add(task);
    }
  }


  /**
   * Wie {@link #execAsync(FutureExecutionTask)}, nur dass auf die Ausf�hrung gewartet wird.
   * @param task
   */
  public void execSync(FutureExecutionTask task) {
    throw new RuntimeException("unsupported");
  }
  
  public static class FutureExecutionTaskDependencyException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public FutureExecutionTaskDependencyException(Map<Integer, List<FutureExecutionTask>> tasks) {
      super("found cyclic dependency or dependent task is missing. remainingTasks: " + createStringForRemainingTasks(tasks));
    }

  }

  //kann auch in die exception
  private static String createStringForRemainingTasks(Map<Integer, List<FutureExecutionTask>> tasks) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for(Integer prio : tasks.keySet()) {
      for (FutureExecutionTask task : tasks.get(prio)) {
        if (!first) {
          sb.append(", ");
        }
        sb.append("[").append(task.toString()).append(" id=").append(task.getId());
        if (task.afterTasks().length > 0) {
          sb.append(" after {");          
          boolean ifirst = true;
          for (TaskId t : task.afterTasks()) {
            if (!ifirst) {
              sb.append(", ");
            }
            sb.append(t);
            ifirst = false;
          }
          sb.append("}");
        }
        if (task.beforeTasks().length > 0) {
          sb.append(" before {");
          boolean ifirst = true;
          for (TaskId t : task.beforeTasks()) {
            if (!ifirst) {
              sb.append(", ");
            }
            sb.append(t);
            ifirst = false;
          }
          sb.append("}");
        }
        sb.append("]");
        first = false;
      }
    }
    return sb.toString();
  }
  
  
 
  /**
   * Beginnt die Abarbeitung aller bisher noch nicht gestarteten {@link FutureExecutionTask}s unter Ber�cksichtigung
   * ihrer Abh�ngigkeiten. Durch die Ausf�hrung der Tasks k�nnen noch weitere Tasks dazukommen.
   */
  public void finishedRegistrationProcess() {
    if( logger.isDebugEnabled() ) {
      logger.debug("FutureExecution "+name+" finishedRegistrationProcess");
      logger.debug("all Tasks: " + createStringForRemainingTasks(tasks) );
    }
    
    //printFutureExecutions();
    
    
    // FutureExecution order in debug monitoren f�r Fehlerfall, aber per default nur auf Trace ausgeben
    StringBuilder sb = new StringBuilder();
    if(logger.isDebugEnabled()) {
      sb.append("Execution order of the FutureExecution ").append(name).append(":\n");
    }
    
    while(true) {
      // mit gr��ter Prio anfangen
      ArrayList<Integer> sortedprios = new ArrayList<Integer>(tasks.keySet());
      if(sortedprios.size() == 0) {
        // fertig
        if( logger.isDebugEnabled() ) {
          logger.debug("FutureExecution "+name+" finished");
        }
        if(logger.isTraceEnabled()) {
          logger.trace(sb.toString());
        }
        
        if( documentation != null ) {
          documentation.finish();
          XynaExtendedStatusManagement.addFurtherInformationAtStartup("FutureExecution_"+name,documentation.getDocString());
        }
        return;
      }
      Collections.sort(sortedprios, Collections.reverseOrder());
      boolean executedAtLeastOneTask = false;
      for(Integer prio : sortedprios) {
        List<FutureExecutionTask> copyOfTasks = new ArrayList<FutureExecutionTask>();
        copyOfTasks.addAll(tasks.get(prio));
        if(executeTaskList(copyOfTasks, prio, sb)) {
          executedAtLeastOneTask = true;
          // Abbruch der for-Schleife, damit h�herpriorisierte Task zu erst weiter abgearbeitet werden. 
          break;
        }
      }
      
      if (!executedAtLeastOneTask &&
          !waitedForExecutingTask()) {
        if(logger.isTraceEnabled()) {
          logger.trace(sb.toString());
        }
        logger.warn("FutureExecution "+name+" executed nothing");
        synchronized (tasks) {
          if(logger.isDebugEnabled()) {
            logger.debug(sb.toString());
          }
          throw new FutureExecutionTaskDependencyException(tasks); //TODO in fehlermeldung integrieren, welche tasks das betrifft.)
        }
      }      
    }
  }
  
  
  private static class Documentation {
    private StringBuilder sb;
    private int count =0;

    public Documentation(String name) {
      sb = new StringBuilder();
      sb.append("digraph FutureExecution {\n");
      sb.append("  node [fontsize=12,width=\".2\", height=\".2\", margin=0, color=lightblue2, style=filled];\n");
      sb.append("graph[fontsize=12];\n");
      sb.append("rankdir=LR;\n");
      sb.append("#       size=\"6,200\";\n");
      sb.append("overlap=false\n");
      sb.append("label=\"Future Execution Tasks Dependencies for ").append(name).append("\"\n");
      sb.append("fontsize=12;\n");
      sb.append("nodesep=0.5;\n");
      sb.append("ranksep=1;\n");
      sb.append("ratio=0.625;\n");

      sb.append("\n\n");
    }

    public String getDocString() {
      return sb.toString();
    }

    public void finish() {
      sb.append("}\n");
    }

    private void addTask(FutureExecutionTask task) {
      ++count;
      TaskId id = task.getId();
      
      sb.append(toId(id)).append(" [label = \"").append(toLabel(task))
        .append("\\nTask ").append(count).append("\"");
      if( task.isMeta() ) {
        sb.append(" shape=\"box\" margin=0.3");
      }
      if( task.isDeprecated() ) {
        sb.append(" color=red");
      } else {
        switch( id.getType() ) {
        case Class:
          break;
        case Integer:
          sb.append(" color=dodgerblue");
          break;
        case String:
          sb.append(" color=deepskyblue");
          break;
        }
      }
      sb.append("];\n");
      for( TaskId ti : task.afterTasks() ) {
        appendDir( sb, ti, id, true );
      }
      for( TaskId ti : task.beforeTasks() ) {
        appendDir( sb, id, ti, false );
      }
      sb.append("\n");
    }

    private String toId(TaskId id) {
      return id.toString().
          replaceAll("\n","_").
          replaceAll("\\.","_").
          replaceAll(" ","_").
          replaceAll("-","_");
    }
    
    private String toLabel(FutureExecutionTask task) {
      String str = task.toString();
      if( str.contains("@")) {
        int pos2 = str.lastIndexOf('@');
        int pos1 = str.lastIndexOf('.', pos2);

        str = str.substring(pos1+1,pos2);
      }
      str = str.replaceAll("\n",Matcher.quoteReplacement("\\n"));
      str = str.replaceAll("\\$",Matcher.quoteReplacement("\\n"));
      str = str.replaceAll("\\.",Matcher.quoteReplacement("\\n"));
      if( task.getId().id instanceof Integer ) {
        return task.getId().toString()+"\\n"+str;
      } else {
        return str;
      }
    }
    
    private void appendDir(StringBuilder sb, TaskId from, TaskId to, boolean after ) {
      if( after ) {
        sb.append(toId(from)).append(" -> ").append(toId(to)).append(";\n");
      } else {
        sb.append(toId(from)).append(" -> ").append(toId(to)).append(" [ dir=back arrowtail=inv ];\n");
      }
    }
  }


  private boolean waitedForExecutingTask() {
    synchronized (currentlyExecutingTasks) {
      if (currentlyExecutingTasks.size() == 0) {
        return false;
      }
      while (currentlyExecutingTasks.size() > 0) {
        try {
          currentlyExecutingTasks.wait();
        } catch (InterruptedException e) {
          logger.warn("FutureExcution processing was interrupted while waiting for currentlyExecuting task");
        }
      }
    }
    return true;
  }

  
  private boolean executeTaskList(List<FutureExecutionTask> taskList, int priority, StringBuilder loggerStringBuilder) {
    boolean executedAtLeastOneTaskLastRound = true;
    boolean executedAtLeastOneTaskAtAll = false;
    middle: while (executedAtLeastOneTaskLastRound) {
      executedAtLeastOneTaskLastRound = false;
      Iterator<FutureExecutionTask> it = taskList.iterator();
      while (it.hasNext()) {
        FutureExecutionTask task = it.next();
        if (!isBlockedByOtherTasks(task)) {
          it.remove();
          executeTask(task);
          if(task.isCanceled()) {
            canceledTasks.add(task.getId());
          }
          synchronized (tasks) {
            List<FutureExecutionTask> origTaskList = tasks.get(priority);
            origTaskList.remove(task);
            if(origTaskList.size() == 0) {
              tasks.remove(priority);
            }
          }
          executedAtLeastOneTaskLastRound = true;
          executedAtLeastOneTaskAtAll = true;
          if(logger.isDebugEnabled()) {
            loggerStringBuilder.append(task.getId()).append("(").append(task.toString()).append(")");
            if(task.beforeTasks().length > 0) {
              loggerStringBuilder.append(" before: ");
              for(TaskId b : task.beforeTasks()) {
                loggerStringBuilder.append(b).append(" ");
              }
            }
            if(task.afterTasks().length > 0) {
              loggerStringBuilder.append(" after: ");
              for(TaskId a : task.afterTasks()) {
                loggerStringBuilder.append(a).append(" ");
              }
            }
            loggerStringBuilder.append("\n");
          }
        }
      }
      if (taskList.size() == 0) {
        break middle;
      }
    }
    return executedAtLeastOneTaskAtAll;
  }
  
  
  @Override
  public String toString() {
    synchronized (tasks) {
      return "FutureExecution("+name+","+tasks+")";
    }
    
  }   
  
  public boolean isCanceled(TaskId id) {
    return canceledTasks.contains(id);
  }


  public FutureExecutionTaskBuilder addTask( Object id, String name ) {
    return new FutureExecutionTaskBuilder( this, id, name);
  }
  public FutureExecutionTaskBuilder addMetaTask( Object id, String name ) {
    return new FutureExecutionTaskBuilder( this, id, name).meta();
  }
  
  public static class FutureExecutionTaskBuilder {

    private FutureExecution futureExecution;
    private Object id;
    private String name;
    private List<TaskId> afterTaskIds = new ArrayList<TaskId>();
    private List<TaskId> beforeTaskIds = new ArrayList<TaskId>();
    private boolean meta;
    private boolean deprecated;

    public FutureExecutionTaskBuilder(FutureExecution futureExecution, Object id, String name) {
      this.futureExecution = futureExecution;
      this.id = id;
      this.name = name;
    }
    
    public FutureExecutionTaskBuilder meta() {
      this.meta = true;
      return this;
    }
    
    public FutureExecutionTaskBuilder deprecated() {
      this.deprecated = true;
      return this;
    }

    public FutureExecutionTaskBuilder after(Class<?> ... afterClasses) {
      for( Class<?> c : afterClasses ) {
        afterTaskIds.add( new TaskId(c));
      }
      return this;
    }

    public FutureExecutionTaskBuilder after(String ... afterStrings) {
      for( String s : afterStrings ) {
        afterTaskIds.add( new TaskId(s));
      }
      return this;
    }
    
    public FutureExecutionTaskBuilder after(int ... afterIds) {
      for( int i : afterIds ) {
        afterTaskIds.add( new TaskId(i));
      }
      return this;
    }
   
    public FutureExecutionTaskBuilder before(Class<?> ... beforeClasses) {
      for( Class<?> c : beforeClasses ) {
        beforeTaskIds.add( new TaskId(c));
      }
      return this;
    }

    public FutureExecutionTaskBuilder before(String ... beforeStrings) {
      for( String s : beforeStrings ) {
        beforeTaskIds.add( new TaskId(s));
      }
      return this;
    }
    
    public FutureExecutionTaskBuilder before(int ... beforeIds) {
      for( int i : beforeIds ) {
        beforeTaskIds.add( new TaskId(i));
      }
      return this;
    }
   
   
    
    public void execAsync() {
      execAsync( new Runnable() { public void run() {} });
    }
    
    public void execAsync(final Runnable runnable) {
      final TaskId[] afterTasks = this.afterTaskIds.toArray( new TaskId[this.afterTaskIds.size()] );
      final TaskId[] beforeTasks = this.beforeTaskIds.toArray( new TaskId[this.beforeTaskIds.size()] );
      
      futureExecution.execAsync(
      new FutureExecutionTask(id) {
        @Override
        public void execute() {
          runnable.run();
        }
        @Override
        public TaskId[] afterTasks() {
          return afterTasks;
        }
        @Override
        public TaskId[] beforeTasks() {
          return beforeTasks;
        }
        @Override
        public String toString() {
          return name;
        }
        @Override
        public boolean isMeta() {
          return meta;
        }
        @Override
        public boolean isDeprecated() {
          return deprecated;
        }
      });
    }
    
    /**
     * Task auch ausf�hren, wenn normale Task-Ausf�hrung bereits abgeschlossen ist
     * @param runnable
     */
    public void execNowOrAsync(final Runnable runnable) {
      final TaskId[] afterTasks = this.afterTaskIds.toArray( new TaskId[this.afterTaskIds.size()] );
      final TaskId[] beforeTasks = this.beforeTaskIds.toArray( new TaskId[this.beforeTaskIds.size()] );
      
      futureExecution.execAsync(
      new FutureExecutionTask(id) {
        @Override
        public boolean waitForOtherTasksToRegister() {
          return false;
        }
        @Override
        public void execute() {
          runnable.run();
        }
        @Override
        public TaskId[] afterTasks() {
          return afterTasks;
        }
        @Override
        public TaskId[] beforeTasks() {
          return beforeTasks;
        }
        @Override
        public String toString() {
          return name;
        }
        @Override
        public boolean isMeta() {
          return meta;
        }
        
      });
    }

  }
   
}
