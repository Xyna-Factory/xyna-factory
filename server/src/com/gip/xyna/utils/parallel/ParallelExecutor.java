/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.utils.parallel;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.concurrent.IncrementableCountDownLatch;


/**
 * ParallelExecutor kann priorisierte Tasks in einer Queue sammeln und ausführen.
 * Eigenschaften:
 * <ul>
 * <li>{@link #addTask(ParallelTask)} nimmt auch während der Ausführung weitere {@link ParallelTask}s an</li>
 * <li>{@link #execute()} führt die Task aus und kehrt sofort zurück</li>
 * <li>{@link #await()} wartet auf Beendigung der Tasks</li>
 * <li>{@link #executeAndAwait()} führt die Task aus und wartet auf Beendigung, auch der aktuelle Thread wird benutzt.</li>
 * <li>{@link #size()} gibt die Anzahl der noch nicht ausgeführten Task an</li>
 * <li>{@link #currentlyExecutingTasks()} gibt die Anzahl der aktuell ausgeführten Tasks an</li>
 * <li>{@link #executedTasks()} gibt die Anzahl der bereits ausgeführten Task an</li>
 * <li>{@link #isExecuting()} gibt an, ob derzeit Threads mit der Task-Bearbeitung beschäftigt sind</li>
 * <li>{@link #drainTasksTo(Collection)} entfernt alle unbearbeiteten Task aus der Queue und kopiert diese
       in die übergebene Collection</li>
 * <li>Die ParallelTasks haben eine Priority, mit {@link #setPriorityThreshold(int)} kann ein Schwellenwert
 *     gesetzt werden. Tasks mit einer Priority unter diesem Wert werden nicht ausgeführt. Damit kann während 
 *     der Ausführung das Abarbeiten von weiteren Tasks unterbunden oder eingeschränkt werden.</li>
 * <li>Im Konstruktor kann ein ThreadLimit angegeben werden. Darüber wird die Anzahl der laufenden 
 *     Threads kontrolliert, auch eine nachträgliche Änderung der Threadanzahl ist über 
 *     {@link #setThreadLimit(int)} (und {@link #getThreadLimit()}) möglich.</li>
 * <li>Die Tasks werden in den Threads nicht direkt ausgeführt, sondern erst in den {@link TaskConsumer}n,
 *     welche in den Threads ausgeführt werden. Die TaskConsumer können über den 
 *     {@link TaskConsumerPreparator} konfiguriert werden, der TaskConsumerPreparator kann über 
 *     {@link #setTaskConsumerPreparator(TaskConsumerPreparator)} gesetzt werden.</li>
 * </ul>   
 * 
 * Funktioniert besonders gut mit Threadpools, die keine große Queue besitzen (z.b. SynchronousQueue).
 * TODO Queues besser unterstützen. Ziel: Main-Thread arbeitet alle Tasks ab, auch wenn der Threadpool voll ist, aber Platz in der Queue ist. 
 */
public class ParallelExecutor {

  protected static final Logger logger = Logger.getLogger(ParallelExecutor.class);

  protected final PriorityBlockingQueue<ParallelTask> allTasks; //alle Tasks 
  protected Executor threadPoolExecutor;              //ausführender ThreadPool
  protected AtomicBoolean executing;                            //Sperre, damit execute oder executeAndAwait nicht mehrfach gleichzeitig gestarten werden
  protected volatile int priorityThreshold;                     //Schwellwert, so dass nicht alle Tasks ausgeführt werden
  protected volatile ExecutionFinishedLatch executionFinished;  //Registriering laufender TaskConsumer/Threads
  protected volatile int threadLimit;                           //Einschränkung der Parallelität
  protected AtomicInteger executionCount;                       //Zählt, wieviel Tasks ausgeführt wurden
  protected TaskConsumerPreparator taskConsumerPreparator;      //Konfiguration der TaskConsumer
  protected boolean logTasks = false;                           //LogMeldungen, wenn neue Tasks hinzugefügt werden
  protected Runnable executionFinishedRunnable;                 //wird ausgeführt, wenn alle Task ausgeführt wurden
  private final AtomicInteger runningTaskConsumerCnt = new AtomicInteger(0);
  private final Object lock = new Object();
  
  /**
   * Unbeschränkte Verwendung des ThreadPools
   */
  public ParallelExecutor(Executor threadPoolExecutor) {
    this(threadPoolExecutor, Integer.MAX_VALUE );
  }
  
  /**
   * Auf Limit eingeschränkte Verwendung des ThreadPools
   * (bei executeAndAwait auf Limit-1 eingeschränkt, so dass maximal Limit Threads laufen)
   * @param threadPoolExecutor
   * @param threadLimit
   */
  public ParallelExecutor(Executor threadPoolExecutor, int threadLimit) {
    if (threadPoolExecutor == null) {
      throw new IllegalArgumentException("Threadpool executor may not be null.");
    }
    this.threadPoolExecutor = threadPoolExecutor;
    this.threadLimit = threadLimit;
    this.allTasks = new PriorityBlockingQueue<ParallelTask>(10, new TaskPrioComparator());
    this.executing = new AtomicBoolean(false);
    this.priorityThreshold = Integer.MIN_VALUE;
    this.executionCount = new AtomicInteger(0);
  }

  @Override
  public String toString() {
    if( executing.get() ) {
      int unworked = size();
      int current = currentlyExecutingTasks();
      return "ParallelExecutor(executing "+(unworked+current)+" tasks with "+current+" threads)";
    } else {
      return "ParallelExecutor("+size()+" tasks)";
    }
  }

  /**
   * ExecutionFinishedLatch:
   * <li>Latch, mit dem auf das Ende der aktuell ausgeführten Threads
   * gewartet werden kann</li>
   * <li>zählt die Anzahl der aktuell ausgeführten Threads</li>
   * <li>Setzt das <code>executing</code>-Flag auf <code>false</code> nach Beendigung der Threads</li>
   */
  private class ExecutionFinishedLatch extends IncrementableCountDownLatch {

    private volatile Runnable runnable;
    private final Object lock;
    /**
     * initialisiert latch mit 1
     * @param lock 
     */
    ExecutionFinishedLatch(Runnable runnable, Object lock) {
      super(1);
      this.lock = lock;
      this.runnable = runnable;
    }
    
    public void setRunnable(Runnable runnable) {
      this.runnable = runnable;
    }

    public void onRelease() {
      if (executing.compareAndSet(true, false)) {
        if (logger.isDebugEnabled()) {
          logger.debug("executing=false, executing tasks=" + currentlyExecutingTasks());
        }
      } else {
        logger.warn("latch was released, but executing already false!");
      }
      if( runnable != null ) {
        runnable.run();
      }
    }
    
    public boolean countDown() {
      synchronized (lock) {
        return super.countDown();
      }
    }
    
  }
  
  /**
   * Tasks mit hoher Priority sollen bevorzugt bearbeitet werden, dazu müssen die Tasks sortiert werden können.
   */
  private static class TaskPrioComparator implements Comparator<ParallelTask> {
    public int compare(ParallelTask pt1, ParallelTask pt2) {
      return pt2.getPriority() - pt1.getPriority(); //höchste zuerst
    }
  }  

  /**
   * Modifikation der Task-Ausführung: Vor und nach der Ausführung des Tasks 
   * werden diese Methoden ausgeführt.
   */
  public interface BeforeAndAfterExecution {

    /**
     * Wird vor jeder Task-Ausführung aufgerufen
     * @param task
     */
    void beforeExecution(ParallelTask task);

    /**
     * Wird nach jeder Task-Ausführung aufgerufen
     * @param task
     */
    void afterExecution(ParallelTask task);
    
  }
  
  /**
   * Modifikation des TaskConsumers: <br>
   * <ul>
   * <li>mit {@link #newTaskConsumer()} kann eine Subklasse des {@link TaskConsumer} erzeugt werden</li>
   * <li>mit {@link #prepareExecutorRunnable(TaskConsumer)} kann ein anderes Runnable als der 
   * TaskConsumer im ThreadPool ausgeführt werden. Dieses Runnable muss natürlich
   * auch TaskConsumer.run() ausführen</li>
   * </ul>
   * Damit kann beispielsweise im TaskConsumer über 
   * {@link TaskConsumer#setBeforeAndAfterExecution(BeforeAndAfterExecution)} eine Implementierung des 
   * Interfaces {@link BeforeAndAfterExecution} angeben werden, mit der Code vor und nach jeder Task-Ausführung
   * ausgeführt werden kann. 
   */
  public interface TaskConsumerPreparator {

    /**
     * Erzeugen einer neuen TaskConsumer-Instanz
     * @return
     */
    TaskConsumer newTaskConsumer();

    /**
     * Rückgabe des Runnable, welches dann im ThreadPool laufen soll
     * @param tc
     * @return
     */
    Runnable prepareExecutorRunnable(TaskConsumer tc);

    /**
     * TaskConsumerPreparator sollte seinen ParallelExecutor kennen, da die neuen 
     * TaskConsumer den ParallelExecutor kennen müssen
     * @param parallelExecutor
     */
    void setParallelExecutor(ParallelExecutor parallelExecutor);
    
  }  
  /**
   * TaskConsumer konsumiert die Tasks aus der Queue.
   * Eigenschaften:
   * <ul>
   * <li>Die Tasks werden in einer Schleife der Queue entnommen und ausgeführt -&gt;
   *   weniger Threads insgesamt werden verwendet, da ein Thread mehrere Tasks ausführen kann.</li>
   * <li>Zusätzlich wird in jedem Schleifendurchlauf probiert, einen weiteren TaskConsumer zu starten -&gt;
   *   ThreadPool kann voll ausgelastet werden, auch wenn zu Beginn nicht alle Threads zu Verfügung standen.</li>
   * </ul>
   */
  public static class TaskConsumer implements Runnable {

    protected ParallelExecutor parallelExecutor;
    protected boolean keepThreadRunningUntilTasksEmpty = false;
    protected BeforeAndAfterExecution beforeAndAfterExecution;    
   
    /**
     * Zählt die Anzahl der laufenden Tasks/Thread herauf
     */
    public TaskConsumer(ParallelExecutor parallelExecutor) {
      if (logger.isDebugEnabled()) {
        logger.debug(this + " created");
      }
      this.parallelExecutor = parallelExecutor;
      parallelExecutor.executionFinished.increment();
    }
    
    /**
     * Setzt BeforeAndAfterExecution, damit bestimmte Aktionen vor und nach Ausführung des Tasks durchgeführt werden können
     * @param beforeAndAfterExecution
     */
    public void setBeforeAndAfterExecution(BeforeAndAfterExecution beforeAndAfterExecution) {
      this.beforeAndAfterExecution = beforeAndAfterExecution;
    }
    
    /**
     * Zählt die Anzahl der laufenden Tasks/Thread herunter
     */
    public void finish() {
      parallelExecutor.executionFinished.countDown();
    }

    
    /**
     * Ausführung des TaskConsumers:<br>
     * Schleife, solange noch Tasks ausgeführt werden können (ThreadLimit):<br>
     * <ul>
     * <li>Holen des nächsten Tasks, Abbruch, wenn keiner mehr existiert</li>
     * <li>Versuch, neuen TaskConsumer zu starten, um ThreadPool so gut wie möglich auszunutzen</li>
     * <li>Eigentliche Ausführung des Tasks</li>
     * </ul>
     */
    public void run() {
      int runningTaskConsumers = parallelExecutor.runningTaskConsumerCnt.incrementAndGet();
      if (logger.isDebugEnabled()) {
        logger.debug("taskconsumer " + this + " running in pe=" + parallelExecutor + " (#=" + runningTaskConsumers + ")");
      }
      int cnt = 0;
      try {
        /*
         * achtung, hier können alle consumer-threads gleichzeitig entscheiden, dass sie sich beenden sollen, weil mehr consumer als tasks vorhanden sind.
         * es gibt aber immer einen consumer mit "keepThreadRunningUntilTasksEmpty=true", deshalb ist das nicht so schlimm.
         * vgl bug 25926. 
         */
        while (parallelExecutor.canNewTaskBeExcecuted(keepThreadRunningUntilTasksEmpty)) {
          ParallelTask task = parallelExecutor.getNextTask();
          if (task == null) {
            logger.debug("no task found");
            break;
          }
          try {
            parallelExecutor.tryToStartNewTaskConsumer();
          } catch (Throwable t) {
            //dann halt nicht, trotzdem mit dem aktuellen Thread weitermachen
            logger.warn("Could not start new task consumer", t);
          }
          executeTask(task);
          parallelExecutor.executionCount.getAndIncrement();
          cnt++;
        }
      } finally {
        finish();
        runningTaskConsumers = parallelExecutor.runningTaskConsumerCnt.decrementAndGet();
        if (logger.isDebugEnabled()) {
          logger.debug("taskconsumer " + this + " finished (started " + cnt + " tasks) (#=" + runningTaskConsumers + "), limit=" + parallelExecutor.threadLimit);
        }
      }
    }


    /**
     * Eigentliche Ausführung des Tasks
     */
    private void executeTask(ParallelTask task) {
      if( beforeAndAfterExecution == null ) {
        task.execute();
      } else {
        try {
          beforeAndAfterExecution.beforeExecution(task);
          task.execute();
        } finally {
          beforeAndAfterExecution.afterExecution(task);
        }
      }
    }

  }
  
  /**
   * Versuch, einen neuen Thread zu belegen
   */
  private void tryToStartNewTaskConsumer() {
    while (canNewThreadBeStarted()) {
      TaskConsumer tc = newTaskConsumer();
      int currentlyRunning = executionFinished.getCount();
      if (currentlyRunning > threadLimit) {
        //doch bereits threadlimit überschritten -> nochmal versuchen TODO das kann man bestimmt auch atomar sicher hinbekommen!
        //wenn man nicht erneut versuchen würde, gehen evtl alle taskconsumer zu (falls gleichzeitig erstellt)
        tc.finish();
        continue;
      }
      boolean success = false;
      try {
        execute(tc, false);
        success = true;
      } catch( RejectedExecutionException e ) {
        //dann halt keinen weiteren TaskConsumer starten
      } finally {
        if (!success) {
          tc.finish();
        }
      }
      break;
    }
  }
  
  /**
   * Hinzufügen eines neuen Tasks
   * @param task
   */
  public void addTask(ParallelTask task) {
    if( logTasks && logger.isDebugEnabled() ) {
      logger.debug("addTask "+task);
    }
    allTasks.add(task);
  }
  
  /**
   * Hinzufügen mehrerer neuer Tasks
   * @param tasks
   */
  public void addTasks(Collection<? extends ParallelTask> tasks) {
    if( logTasks && logger.isDebugEnabled() ) {
      logger.debug("addTasks "+tasks);
    }
    allTasks.addAll(tasks);
  }

  /**
   * Startet die Ausführung der Tasks, kehrt sofort zurück.
   * @throws RejectedExecutionException wenn kein Task gestartet werden konnte
   */
  public void execute() throws RejectedExecutionException {
    executeInternally(false);
  }
  
  /**
   * Startet die Ausführung der Tasks, und wartet, bis die gestarteten Tasks/Threads beendet sind.
   * Im aktuellen Thread werden ebenfalls Tasks gestartet, nicht nur im ThreadPool.
   * Achtung: Es kann sein, dass danach noch Tasks nicht ausgeführt worden sind,
   * wenn es eine ParallelismLimitation mit Limit 0 oder Task mit zu niedriger Priorität gibt
   * @throws InterruptedException 
   */
  public void executeAndAwait() throws InterruptedException {
    executeInternally(true); //kann keine rejectedexecutionexception werden
    await();
  }
  
  /**
   * Führt executeAndAwait() solange aus, bis kein Task mehr ausgeführt werden kann und schluckt
   * bis dahin alle InterruptedExceptions.
   * Achtung: Endlosschleife, wenn ParallelismLimitation mit Limit 0 konfiguriert ist.
   */
  public void executeAndAwaitUninterruptable() {
    while( hasExecutableTasks() ) {
      try {
        executeAndAwait();
      } catch (InterruptedException e) {
        logger.info("Continuing interrupted executeAndAwait", e);
      }
    }
  }
 
  /**
   * @param runInThisThread Soll auch ein TaskConsumer im aktuellen Thread gestartet werden?
   * @throws InterruptedException
   */
  private void executeInternally( boolean runInThisThread) throws RejectedExecutionException {
    if(allTasks.isEmpty() ) {      
      return; // keine weiteren Tasks zur Bearbeitung
    }
    
    boolean join = false;
    
    TaskConsumer tc;
    synchronized (lock) { //synchronized, damit nicht erst execution auf false gesetzt wird, und dann trotzdem noch taskconsumer gestartet werden
      if (executing.compareAndSet(false, true)) {
        //initialisiert latch mit count=1 - muss nach der taskconsumer erstellung wieder runtergezählt werden.
        //darf hier nicht mit 0 initialisiert werden, weil sonst ein zeitintervall existiert, in dem das latch von einem anderen thread releast werden kann.
        executionFinished = new ExecutionFinishedLatch(executionFinishedRunnable, lock);
      } else {
        join = true;
      }
      tc = newTaskConsumer();
    }
    if (!join) {
      //latch wurde mit 1 initialisiert und in newTaskConsumer nochmal um 1 erhöht
      executionFinished.countDown();
    }
    boolean success = false;
    try {
      if (logger.isDebugEnabled()) {
        int size = allTasks.size();
        String msg =
            "Attempting to " + (join ? "join execution of " : "execute ") + size + " tasks: " + (logTasks ? allTasks.toString() : "");
        logger.debug(msg);
      }

      //TaskConsumer wird intern weitere Threads starten, deswegen reicht hier ein einziger Start aus
      tc.keepThreadRunningUntilTasksEmpty = true;
      execute(tc, runInThisThread);
      success = true;
    } finally {
      if (!success) {
        tc.finish();
      }
    }
  }
  
  /**
   * Wartet, bis die derzeit gestarteten Tasks/Threads beendet sind.
   * Achtung: Es kann sein, dass danach noch Tasks nicht ausgeführt worden sind,
   * wenn es eine ParallelismLimitation mit Limit 0 oder Task mit zu niedriger Priorität gibt
   * @throws InterruptedException
   */
  public void await() throws InterruptedException {
    if( executionFinished != null ) {
      if( logger.isDebugEnabled() ) {
        logger.debug("await executionFinished");
      }
      executionFinished.await();
    }
  }


  /**
   * Liefert den nächsten ausführbaren Task. 
   * Wegen zu niedriger Priorität können evtl. manche Threads nicht ausgeführt werden
   * @return
   */
  private ParallelTask getNextTask() {
    ParallelTask task = allTasks.poll();
    if( task == null ) {
      return null;
    }
    if( task.getPriority() < priorityThreshold ) {
      addTask(task); //Task wieder hinzufügen
      logger.debug("task priority too low");
      return null;
    }
    return task;
  }


  /**
   * Erzeugt neuen TaskConsumer, evtl. über taskConsumerPreparator
   * @return
   */
  private TaskConsumer newTaskConsumer() {
    if( taskConsumerPreparator == null ) {
      return new TaskConsumer(this);
    } else {
      return taskConsumerPreparator.newTaskConsumer();
    }
  }
  
  /**
   * Startet den TaskConsumer im ThreadPool oder für MainThread direkt, evtl. über taskConsumerPreparator
   * @param tc
   * @throws RejectedExecutionException
   */
  private void execute(TaskConsumer tc, boolean inMainThread) throws RejectedExecutionException {
    Runnable r = taskConsumerPreparator == null ? tc : taskConsumerPreparator.prepareExecutorRunnable(tc);
    if( inMainThread ) {
      r.run();
    } else {
      try {
        threadPoolExecutor.execute(r);
      } catch (RejectedExecutionException e) {
        throw e;
      } catch (Throwable t) {
        throw new RejectedExecutionException(t);
      }
    }
  }

  /**
   * Prüfung, ob ein weiterer Task ausgeführt werden darf.
   * Der Grund dafür, einen Task nicht ausführen zu dürfen ist eine ParallelismLimitation, 
   * die ein kleineres Limit vorgibt als gerade an Threads aktiv ist.
   * @param runningInMainThread
   * @return
   */
  private synchronized boolean canNewTaskBeExcecuted(boolean keepThreadRunningUntilTasksEmpty) {
    //synchronized, weil: 
    //wenn 2 taskconsumer am laufen sind, können beide sehen, dass sie nichts mehr machen sollen und sich beenden.
    //dann wurden ggf zuviele beendet.
    //deshalb muss der check serialisiert werden.

    final boolean canNewTaskBeExcecuted;
    if( keepThreadRunningUntilTasksEmpty ) {
      //im MainThread darf immer gestartet werden, damit dieser am Laufen bleibt
      //Erst wenn kein Thread mehr laufen darf, muss auch MainThread beendet werden
      canNewTaskBeExcecuted = threadLimit > 0; 
    } else {
      int currentlyRunning = executionFinished.getCount();
      canNewTaskBeExcecuted = currentlyRunning <= threadLimit;
    }
    return canNewTaskBeExcecuted;
  }

  /**
   * Prüfung, ob ein weiterer Thread benutzt werden darf.
   * Der Grund dafür, keinen weiteren Thread benutzen zu dürfen ist eine ParallelismLimitation, 
   * deren Limit erreicht wurde.
   * @return
   */
  private boolean canNewThreadBeStarted() {
    int currentlyRunning = executionFinished.getCount();
    boolean canNewThreadBeStarted = currentlyRunning < threadLimit;
    return canNewThreadBeStarted;
  }

  /**
   * Anzahl der noch nicht bearbeiteten Tasks, kann bei laufender Ausführung kurzzeitig zu klein sein
   * @return
   */
  public int size() {
    return allTasks.size();
  }

  /**
   * ungefähre Anzahl der in Bearbeitung befindlichen Tasks/Threads
   * Ungefähr bedeutet, dass Threads noch einen kurzen Zeitraum nach Beendigung des Tasks gezählt werden
   * bzw. auch schon kurz vor Beginn der Ausführung. Außerdem werden kurzzeitig Tasks gezählt, die sofort 
   * vom ThreadPoolExecutor über RejectedExecutionException abgelehnt werden.
   * @return
   */
  public int currentlyExecutingTasks() {
    if( executionFinished == null ) {
      return 0;
    } else {
      return executionFinished.getCount();
    }
  }
  
  /**
   * Anzahl der bereits ausgeführten Tasks
   * @return
   */
  public int executedTasks() {
    return executionCount.get();
  }
  
  /**
   * Laufen derzeit Threads?
   * @return
   */
  public boolean isExecuting() {
    return executing.get();
  }

  /**
   * Entfernt alle unbearbeiteten Task aus der Queue und kopiert diese in die übergebene Collection
   */
  public void drainTasksTo(Collection<? super ParallelTask> c) {
    allTasks.drainTo(c);
  }

  /**
   * Setzen einer Prioritätsschwelle: Nur Task mit der gleichen oder höherer Priority dürfen bearbeitet werden
   * @param priorityThreshold
   */
  public void setPriorityThreshold(int priorityThreshold) {
    this.priorityThreshold = priorityThreshold;
  }
  
  /**
   * Setzen des ThreadLimits
   * @param threadLimit
   */
  public void setThreadLimit(int threadLimit) {
    int oldLimit = this.threadLimit;
    this.threadLimit = threadLimit;   
    if (oldLimit < threadLimit) {
      executeInternally(false);
    }
  }

  /**
   * @return threadLimit
   */
  public int getThreadLimit() {
    return threadLimit;
  }
  
  /**
   * Setzt den TaskConsumerPreparator, um die TaskConsumer zu konfigurieren und das Starten im ThreadPoolExecutor vorzubereiten.
   * @param taskConsumerPreparator
   */
  public void setTaskConsumerPreparator(TaskConsumerPreparator taskConsumerPreparator) {
    this.taskConsumerPreparator = taskConsumerPreparator;
    taskConsumerPreparator.setParallelExecutor(this);
  }

  public void setExecutionFinishedRunnable(Runnable executionFinishedRunnable) {
    this.executionFinishedRunnable = executionFinishedRunnable;
    if( executionFinished != null ) {
      executionFinished.setRunnable(executionFinishedRunnable);
    }
  }
  
  /**
   * unbearbeiteten Tasks aus der Liste streichen
   * @param task
   * @return true, wenn Task entfernt werden konnte
   */
  public boolean removeTask(ParallelTask task) {
    return allTasks.remove(task);
  }

  /**
   * Liegen noch unbearbeitete, ausführbare Tasks in der Liste?
   * Wegen priorityThreshold können manche Tasks nicht ausgeführt werden
   * @return true, wenn Tasks gestartet werden können
   */
  public boolean hasExecutableTasks() {
    ParallelTask pt = allTasks.peek();
    if( pt == null ) {
      return false;
    }
    return pt.getPriority() >= priorityThreshold;
  }

  
}
