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
package com.gip.xyna.utils.jms;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import oracle.jms.AQjmsDestination;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;

import org.apache.log4j.Logger;

/**
 * ziel:<br>
 * jms wrapper klasse, die leicht zu bedienen ist und trotzdem flexibel gegenüber neuen anforderungen ist.<br>
 *
 * enqueue und dequeue methoden werden unterstützt.<br>
 * codebeispiele:<br>
 * <code>
 *    //mit datenbank-connection<br>
 *    jms = new JMSQueueUtils(dbCon, true);<br>
      jms.setQueueAQ("processing", "exitq");<br>
      jms.enqueue("124", xmlString);<br>
      jms.enqueue("125", xmlString);<br>
      jms.rollback();<br>
      jms.setDelay(20);<br>
      jms.enqueue("126", xmlString);<br>
      jms.commit();<br>
      jms.close();<br>
      <br>
      //über JNDI<br>
      JMSQueueUtils jms = new JMSQueueUtils(ctx, "jms/QueueConnectionFactory", true);<br>
      jms.enqueueJNDI("jms/demoQueue", "cl123", xmlString);<br>
      jms.rollback();<br>
 * </code>
 * <p>
 * datenbankverbindungen und connectionleaks/transaktionen:<br>
 * wenn man den konstruktor
 * <code>
 * public JMSQueueUtils(String jdbcUrl, String dbUser, String dbPassword,
    boolean transacted)
   </code>
   benutzt, sollte man darauf achten, dass man mit "close()" alle verbindungen schliesst. hierbei
   werden dann jms- und datenbank connections geschlossen.<br>
   datenbank-connections werden nicht geschlossen, wenn die connection im konstruktor übergeben wird,
   allerdings wirken sich rollback() und commit() nicht nur auf die jms-connection, sondern (natürlich)
   auch auf die db-connection aus.
 * <p>
 * wie funktioniert jms?<br>
 * 1. queue connection holen<br>
 *    das funktioniert meist über eine connectionfactory. implementierungen von connectionfactories
 *    sind zb standardmässig im applicationserver über JNDI zu finden. im hintergrund hängt im AQ
 *    fall an der queueconnection auch eine db connection.<br>
 *    bei transaktionen auf der queueconnection wird automatisch die db connection committed/rollbacked.<br>
 * 2. queue session eröffnen<br>
 *    über die queue connection<br>
 * 3. queue objekt bauen<br>
 *    siehe setQueue()<br>
 * 4. queue sender/receiver<br>
 *    die session stellt meist methoden zur verfügung um sender/receiver objekte zu erstellen. diese
 *    ermöglichen dann das eigentliche enqueue oder dequeue.
 * <p>
 * TODO: topics, falls benötigt. <br>
 *       usePLSQL für bessere abwärtskompatibilität mit älteren aqjms-libs/db-versionen implementieren.<br>
 *       exceptions mit xynaexceptionhandling
 */
/**
 * JMSQueueUtils
 */
public class JMSQueueUtils {

  private Logger logger = Logger.getLogger("xyna.utils.jms");

  private QueueConnection qCon = null;
  private QueueSession qSes = null;
  private Queue q = null;
  private boolean aq =
    false; //entweder (aq = true und ctx != null) oder (aq = false und ctx == null)
  private InitialContext ctx = null;
  private String correlationId = null;
  private int priority = -1;
  private int delay = -1;
  private boolean transacted = false;

  private String messageSelector = null;
  private long dequeueTimeout = -1;

  private boolean usePLSQL = false;

  /**
   * beim browsen werden Nachrichten für andere Consumer gelockt. Ähnlich einem Select for update.
   * @see oracle.jms.AQjmsSession#createBrowser(javax.jms.Queue, java.lang.String, boolean)
   */
  private boolean lockBrowsedMessages = false;

  //----------------------------------- diverse konstruktoren

  /**
   * baut QueueConnection und QueueSession auf. Transactions-Modus ist defaultmässig aus.
   * @param ctx
   * @param jndiQConnectionFactory wird über einen lookup im InitialContext versucht zu finden.
   * Dortiges Object muss Instanz einer Klasse sein, die javax.jms.QueueConnectionFactory implementiert.
   * @throws NamingException falls jndi-location nicht gefunden wird
   * @throws JMSException
   * @see com.gip.xyna.utils.jndi.ContextHelper
   */
  public JMSQueueUtils(InitialContext ctx,
    String jndiQConnectionFactory) throws NamingException, JMSException {
    this(ctx, jndiQConnectionFactory, false);
  }

  /**
   * baut QueueConnection und QueueSession auf.
   * @param ctx
   * @param jndiQConnectionFactory wird über einen lookup im InitialContext versucht zu finden.
   * Dortiges Object muss Instanz einer Klasse sein, die javax.jms.QueueConnectionFactory implementiert.
   * @param transacted true um transaktions-klammern zu erlauben
   * @throws NamingException falls jndi-location nicht gefunden wird
   * @throws JMSException
   * @see com.gip.xyna.utils.jndi.ContextHelper
   */
  public JMSQueueUtils(InitialContext ctx, String jndiQConnectionFactory,
    boolean transacted) throws NamingException, JMSException {
    this.transacted = transacted;
    this.ctx = ctx;
    QueueConnectionFactory factory = (QueueConnectionFactory)ctx.lookup(jndiQConnectionFactory);
    qCon = factory.createQueueConnection();
    qSes = qCon.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
  }

  /**
   * baut QueueConnection und QueueSession auf. (In der Implementierung AQjmsQueueConnection/AQjmsSession).<br>
   * Transaktions-Modus defaultmässig ausgeschaltet.<br>
   * Achtung: DBConnection wird automatisch committed!
   * @param dbCon
   * @throws JMSException
   */
  public JMSQueueUtils(Connection dbCon) throws JMSException {
    this(dbCon, false);
  }

  /**
   * baut QueueConnection und QueueSession auf. (In der Implementierung AQjmsQueueConnection/AQjmsSession).<br>
   * Achtung: bei transacted = false wird die dbConnection automatisch committed!
   * @param dbCon
   * @param transacted
   * @throws JMSException
   */
  public JMSQueueUtils(Connection dbCon, boolean transacted) throws JMSException {
    aq = true;
    this.transacted = transacted;
    qCon = AQjmsQueueConnectionFactory.createQueueConnection(dbCon);
    
    qSes = qCon.createQueueSession(transacted, Session.CLIENT_ACKNOWLEDGE);
  }

  /**
   * baut QueueConnection und QueueSession auf. (In der Implementierung AQjmsQueueConnection/AQjmsSession).<br>
   * Es wird eine eigene DBConnection erstellt, die erst beim jms.close() geschlossen wird.
   * @param jdbcUrl beispielsweise jdbc:oracle:thin:@10.0.10.11:1521:xynadb oder <br>
   * "jdbc:oracle:thin:@(DESCRIPTION = (ADDRESS = (PROTOCOL = TCP)(HOST = gipsun162-vip)(PORT = 1521))
   * (ADDRESS = (PROTOCOL = TCP)(HOST = gipsun163-vip)(PORT = 1521))(LOAD_BALANCE = yes) (CONNECT_DATA = (SERVER = DEDICATED)
   * (SERVICE_NAME = pallasha2) (FAILOVER_MODE = (TYPE = SELECT) (METHOD = BASIC) (RETRIES = 180) (DELAY =5)))"
   * @param dbUser benutzername für die datenbank verbindung
   * @param dbPassword password für die datenbank verbindung
   * @param transacted
   * @throws JMSException
   */
  public JMSQueueUtils(String jdbcUrl, String dbUser, String dbPassword,
    boolean transacted) throws JMSException {
    aq = true;
    this.transacted = transacted;
    QueueConnectionFactory qcf = AQjmsFactory.getQueueConnectionFactory(jdbcUrl, new Properties());
    qCon = qcf.createQueueConnection(dbUser, dbPassword);
    qSes = qCon.createQueueSession(transacted, Session.CLIENT_ACKNOWLEDGE);
  }

  /**
   * zentrale Enqueue Methode. Alle Anderen Enqueue Methoden rufen diese auf. <br>
   * CorrelationId wird die der
   * Message übernommen. Falls sie null ist, wird die in dieser Klasse gesetzte CorrelationId benutzt.<br>
   * Es wird mit Priority eingestellt, falls sie vorher gesetzt wurde.
   * <p>
   * Eine Message bekommt man typischerweise mit:<br>
   * <code>
   *  JMSQueueUtils jms = new JMSQueueUtils(...);<br>
      jms.setQueue(...);<br>
      Message m = jms.getSession().createTextMessage("&lt;bla /&gt;");<br>
      jms.enqueue(m);<br>
   * </code>
   * @param m
   * @throws JMSException
   */
  public void enqueue(Message m) throws JMSException {
    if (q == null) {
      throw new JMSException("Enqueue fehlgeschlagen, da keine Queue spezifiziert wurde.");
    }
    if (m == null) {
      throw new NullPointerException("Message ist null");
    }
    if (usePLSQL) {
      enqueuePLSQL(m, correlationId);
    } else {
      if (correlationId != null && isEmpty(m.getJMSCorrelationID())) {
        m.setJMSCorrelationID(correlationId);
      }
      if (delay > -1) {
        m.setIntProperty("JMS_OracleDelay", delay);
      }

      QueueSender sender = qSes.createSender(q);
      if (priority > -1) {
        sender.setPriority(priority);
      }
      qCon.start();
      sender.send(m);
    }
    debugSend(m);
  }
  
  private boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * zentrale Dequeue Methode. wird von allen anderen aufgerufen. Benutzt den MessageSelector und
   * Timeout, falls sie gesetzt wurden. Benutzt als MessageSelector die CorrelationId, falls kein
   * MessageSelector gesetzt wurde, aber die CorrelationId schon. Dabei wird aber das MessageSelector
   * Feld nicht gesetzt, man kann also zb mehrfach mit unterschiedlicher CorrelationId dequeuen, ohne
   * den MessageSelector benutzen zu müssen:<br>
   * <code>
   * JMSQueueUtils jms = new JMSQueueUtils(...);<br>
   * for (int i = 0; i&lt;100; i++) {<br>
   *   jms.setCorrelationId("corrId" + i);<br>
   *   Message m = jms.dequeue(); //findet Nachricht mit gesetzter CorrelationId.<br>
   *   tuwas(m);<br>
   * }<br>
   * jms.close();<br>
   * </code>
   * @return
   * @throws JMSException
   */
  public Message dequeue() throws JMSException {
    if (q == null) {
      throw new JMSException("Dequeue fehlgeschlagen, da keine Queue spezifiziert wurde.");
    }
    String messageSelectorLocal = messageSelector;
    if (messageSelector == null && correlationId != null) {
      messageSelectorLocal = buildMessageSelector(true, correlationId, null, null);
    }
    logger.debug("getting Message from " + q.getQueueName() + " with MessageSelector \"" +
        messageSelectorLocal + "\"...");
    Message m = null;
    if (usePLSQL) {
      m = dequeuePLSQL();
    } else {
      QueueReceiver receiver = qSes.createReceiver(q, messageSelectorLocal);
      qCon.start();
      m = dequeue(receiver);
    }
    debugReceive(m);
    return m;
  }

  private Message dequeue(QueueReceiver receiver) throws JMSException {
    Message m = null;
    if (dequeueTimeout == -1) {
      m = receiver.receiveNoWait();
    } else {
      m = receiver.receive(dequeueTimeout);
    }
    return m;
  }

  private void debugSend(Message m) throws JMSException {
    if (logger.isDebugEnabled()) {
      String prefix = "";
      if (m.getJMSCorrelationID() != null) {
        prefix = "[" + m.getJMSCorrelationID() + "] ";
      }
      if (m instanceof TextMessage) {
        logger.debug(prefix + "sent TextMessage to queue " + q.getQueueName() + ": " +
            ((TextMessage)m).getText());
      } else {
        logger.debug(prefix + "sent " + m.getClass().getName() + " " + m + " to queue " +
            q.getQueueName());
      }
    }
  }

  private void debugReceive(Message m) throws JMSException {
    if (logger.isDebugEnabled()) {
      if (m == null) {
        logger.debug((dequeueTimeout > -1 ? "waited for " + dequeueTimeout + "ms and " : "") +
            "got no Message from " + q.getQueueName() + ".");
      } else {
        String part1 =
          "Empfangene Nachricht hat CorrelationId=\"" + m.getJMSCorrelationID() + "\": ";
        if (m instanceof TextMessage) {
          logger.debug(part1 + ((TextMessage)m).getText());
        } else {
          logger.debug(part1 + m.getClass().getName() + " " + m);
        }
      }
    }
  }

  /**
   * gibt die verwendete QueueSession zurück. Mit dieser kann man zb Message oder Queue oder
   * Consumer/Producer -Objekte erstellen.
   * @return
   */
  public QueueSession getSession() {
    return qSes;
  }

  /**
   * gibt die verwendete QueueConnection zurück.
   * @return
   */
  public QueueConnection getConnection() {
    return qCon;
  }

  /**
   * in Millisekunden. Wirkt sich auf das Dequeue aus. 0 = warten, bis Nachricht gefunden wird. -1 =
   * nicht warten.
   * @param timeout
   */
  public void setTimeout(long timeout) {
    dequeueTimeout = timeout;
  }

  /**
   * Welche Nachrichten sollen dequeued werden? Format ist SQL-ähnlich:<br>
   * <code>
   * JMSCorrelationID = '123'<br>
   * </code> oder<br>
   * <code>
   * JMSCorrelationID LIKE '123%' AND myvalue > 0<br>
   * </code>
   * <p>
   * Hierbei gibt es zu beachten, dass die Validität/Syntax von MessageSelektoren abhängig von der
   * benutzten JMS Implementierung ist. So geht bei AQ nicht <code>JMSTimestamp &gt; 1234567890</code>, bei
   * in-Memory Queueing hingegen schon. Bei AQ muss man das dann z.B. 
   * <code>JMSTimestamp &gt; to_timestamp('2009-03-25 14:50:44', 'YYYY-MM-DD HH24:MI:SS')</code> 
   * schreiben.<br>
   * @param messageSelector
   */
  public void setMessageSelector(String messageSelector) {
    this.messageSelector = messageSelector;
  }

  /**
   * setzt Delay in Sekunden. Wirkt sich beim Enqueue in AQ-Queues.
   * Wartezeit, nach der die Nachricht erst aus der Queue ausgelesen werden darf.
   * @param delay
   */
  public void setDelay(int delay) {
    this.delay = delay;
  }

  /**
   * mit dieser Priority wird enqueued. Kleiner 0 für Default priority (implementierungsabhängig)
   * @param prio
   */
  public void setPriority(int prio) {
    this.priority = prio;
  }

  /**
   * setzt die CorrelationId. Wirkt sich aufs Enqueue und Dequeue aus. Beim Dequeue wird die CorrelationId
   * in einen passenden MessageSelector umgewandelt, falls kein anderer MessageSelector gesetzt wurde.
   * @param correlationId
   */
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  /**
   * generischer Queue-setter. Das Queue-Objekt bekommt man meistens über die Session. Meistens sollten
   * die Convenience Methoden setQueueAQ(String) oder setQueueJNDI(String, String) ausreichen.<br>
   * Beispiel um ein Queueobjekt zu bekommen:<br>
   * <code>
   *  Queue q = jms.getSession().createQueue("myqueue");<br>
      Queue q2 = (Queue)ctx.lookup("jms/myQueue");<br>
      Queue q3 = ((AQjmsSession)jms.getSession()).getQueue("processing", "exitq");<br>
   * </code>
   * @param q
   */
  public void setQueue(Queue q) {
    this.q = q;
  }

  /**
   * committed die QueueSession, falls im modus "transactional". Hierbei wird ggfs die basierende Datenbank
   * Connection committed!
   * @throws JMSException
   */
  public void commit() throws JMSException {
    if (qSes != null && transacted) {
      qSes.commit();
      logger.debug("QueueSession committed.");
    } else {
      throw new JMSException("QueueSession can't be commited. QueueSession ist entweder Null oder wurde nicht im Transaktionsmodus erzeugt.");
    }
  }

  /**
   * macht einen rollback auf die QueueSession, falls im modus "transactional". Hierbei wird ggfs die basierende
   * Datenbank Connection ge"rollbacked".
   * @throws JMSException
   */
  public void rollback() throws JMSException {
    if (qSes != null && transacted) {
      qSes.rollback();
      logger.debug("QueueSession rollbacked.");
    } else {
      throw new JMSException("QueueSession can't be rollbacked. QueueSession ist entweder Null oder wurde nicht im Transaktionsmodus erzeugt.");
    }
  }

  /**
   * schliesst die QueueConnection.
   * @throws JMSException
   */
  public void close() throws JMSException {
    if (qSes != null && transacted) {
      logger.debug("QueueSession committed.");
      qSes.commit();
    }
    if (qCon != null) {
      logger.debug("QueueConnection closed.");
      qCon.close(); //schliesst auch session und macht rollback auf db connection falls vorhanden
    } else {
      throw new JMSException("Connection can't be closed because it is not open.");
    }
  }

  /**
   * Beim Browsen die Nachrichten locken, die man liest. true = locked. default ist false.
   * @param b
   */
  public void setLockBrowsedMessages(boolean b) {
    lockBrowsedMessages = b;
  }

  /**
   * Gibt Nachrichten zurück, die sich in der Queue befinden, ohne sie zu dequeuen. Sie werden auf
   * Wunsch gelockt (nur bei AQ unterstützt), so dass sie niemand anderes auslesen kann, solange bis
   * die QueueSession beendet wird (unklar, ob auch schon bei commit/rollback das lock aufgegeben wird).<br>
   * Dieses Verhalten kann über {@link com.gip.xyna.utils.jms.JMSQueueUtils#setLockBrowsedMessages(boolean)}
   * gesteuert werden. Default ist <code>false</code>.<br>
   * Benutzt MessageSelector.<br>
   * Setzt voraus, dass Queue gesetzt ist.<br>
   * @see javax.jms.QueueBrowser
   * @param maxMessages Es werden maximal soviele Nachrichten ausgelesen und zurückgegeben.
   * @return Array von Messages
   * @throws JMSException
   */
  public Message[] browse(int maxMessages) throws JMSException {
    if (q == null) {
      throw new JMSException("Browse fehlgeschlagen, da keine Queue spezifiziert wurde.");
    }
    QueueBrowser browser = null;
    if (aq) {
      browser = ((AQjmsSession)qSes).createBrowser(q, messageSelector, lockBrowsedMessages);
    } else {
      browser = qSes.createBrowser(q, messageSelector);
    }
    qCon.start();
    Enumeration enuma = browser.getEnumeration();
    ArrayList<Message> messages = new ArrayList<Message>();
    int cnt = 0;
    while (enuma.hasMoreElements() && (cnt < maxMessages || maxMessages < 0)) {
      cnt++;
      messages.add((Message)enuma.nextElement());
    }
    return messages.toArray(new Message[] { });
  }

  /**
   * leert die queue durch wiederholtes dequeue mit nowait.<br>
   * benutzt den eingestellten MessageSelector, d.h. es werden nur Nachrichten gelöscht, die
   * diesem entsprechen. Ist kein MessageSelector gesetzt (null), werden alle Nachrichten
   * gelöscht.<br>
   * nachrichten, die nicht dequeued werden können, werden nicht gelöscht.
   * //FIXME methode ist recht langsam <<100 nachrichten/sekunde.
   * @param timeout methode bricht nach soviel millisekunden ab.
   * @return Anzahl der gelöschten Nachrichten
   * @throws JMSException
   */
  public int empty(long timeout) throws JMSException {
    if (q == null) {
      throw new JMSException("Empty fehlgeschlagen, da keine Queue spezifiziert wurde.");
    }
    long start = System.currentTimeMillis();

    QueueReceiver receiver = qSes.createReceiver(q, messageSelector);
    qCon.start();

    long oldTimeout = dequeueTimeout;
    setTimeout(-1);
    int cnt = 0;
    while (dequeue(receiver) != null && (System.currentTimeMillis() - start) < timeout) {
      cnt++;
    }
    setTimeout(oldTimeout);
    return cnt;
  }

  // ---------------------- convenience methoden:

  private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * baut aus CorrelationId und Dates einen MessageSelector. Falls einer der Parameter null ist, wird
   * er nicht berücksichtigt. Der generierte MessageSelector selektiert nach CorrelationId-Gleichheit
   * und selektiert nur Nachrichten, deren Einstell-Zeitpunkt (JMSTimestamp) nach minDate und vor maxDate liegt.
   * @param dateAsTimeStamp {@link com.gip.xyna.utils.jms.JMSQueueUtils#setMessageSelector(String)}
   * @param correlationId
   * @param minDate
   * @param maxDate
   * @return
   */
  public static synchronized String buildMessageSelector(boolean dateAsTimeStamp, String correlationId,
    Date minDate, Date maxDate) {
    //synchronized, weil statisches nicht threadsicheres simpledateformat benutzt wird
    StringBuffer sb = new StringBuffer();
    if (correlationId != null) {
      sb.append("JMSCorrelationID = '" + correlationId + "'");
    }
    if (minDate != null) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      if (dateAsTimeStamp) {
        sb.append("JMSTimestamp > to_timestamp('" + SDF.format(minDate) +
            "', 'YYYY-MM-DD HH24:MI:SS')");
      } else {
        sb.append("JMSTimestamp > " + minDate.getTime());
      }
    }
    if (maxDate != null) {
      if (sb.length() > 0) {
        sb.append(" AND ");
      }
      if (dateAsTimeStamp) {
        sb.append("JMSTimestamp < to_timestamp('" + SDF.format(maxDate) +
            "', 'YYYY-MM-DD HH24:MI:SS')");
      } else {
        sb.append("JMSTimestamp < " + maxDate.getTime());
      }
    }
    return sb.toString();
  }

  /**
   * enqueue Convenience Methode.<br>
   * Queue und CorrelationId (optional) müssen vorher gesetzt worden sein.<br>
   * enqueued eine TextMessage mit der übergebenen message als Inhalt.
   * @param message
   * @throws JMSException
   */
  public void enqueue(String message) throws JMSException {
    Message m = qSes.createTextMessage(message);
    enqueue(m);
  }

  /**
   * enqueue Convenience Methode.<br>
   * Queue muss vorher gesetzt worden sein.<br>
   * setzt die CorrelationId für zukünftige Operationen.<br>
   * enqueued eine TextMessage mit der übergebenen message als Inhalt.
   * @param correlationId
   * @param message
   * @throws JMSException
   */
  public void enqueue(String correlationId, String message) throws JMSException {
    setCorrelationId(correlationId);
    enqueue(message);
  }

  /**
   * enqueue Convenience Methode<br>
   * setzt auch die Queue für zukünftige Operationen.<br>
   * setzt auch die CorrelationId.<br>
   * AQ-spezifische Methode, d.h. funktioniert nur in Verbindung mit
   * AQ spezifischem Konstruktor
   * @param dbOwner schema, wo die queue liegt
   * @param qName queue name
   * @param correlationId correlatinoid, mit der eingestellt werden soll
   * @param message
   * @throws JMSException
   */
  public void enqueueAQ(String dbOwner, String qName, String correlationId,
    String message) throws JMSException {
    setQueueAQ(dbOwner, qName);
    enqueue(correlationId, message);
  }

  /**
   * enqueue Convenience Methode.<br>
   * setzt auch die Queue für zukünftige Operationen.<br>
   * setzt auch die CorrelationId.<br>
   * JNDI-spezifische Methode, d.h. funktioniert nur in Verbindung mit
   * JNDI spezifischem Konstruktor (die, die einen InitialContext als Parameter haben).<br>
   * @param jndiQueueDestination
   * @param correlationId
   * @param message
   * @throws JMSException
   * @throws NamingException
   */
  public void enqueueJNDI(String jndiQueueDestination, String correlationId,
    String message) throws JMSException, NamingException {
    setQueueJNDI(jndiQueueDestination);
    enqueue(correlationId, message);
  }

  /**
   * legt die Queue fest. JNDI-spezifische Methode, d.h. funktioniert nur in Verbindung mit
   * JNDI spezifischem Konstruktor (die, die einen InitialContext als Parameter haben).
   * @param jndiQueueDestination
   * @throws JMSException
   * @throws NamingException
   */
  public void setQueueJNDI(String jndiQueueDestination) throws JMSException, NamingException {
    if (aq) {
      throw new JMSException("Die Queue kann nur über JNDI bestimmt werden, wenn die QueueConnection auch über JNDI besteht.");
    }
    q = (Queue)ctx.lookup(jndiQueueDestination);
  }

  /**
   * legt die Queue fest. AQ spezifische Methode, d.h. funktioniert nur in Verbindung mit einem AQ-spezifischen
   * Konstruktor, ansonsten wird ein Fehler geworfen.
   * @param dbOwner besitzer-schema der queue
   * @param qName queuename
   * @throws JMSException
   */
  public void setQueueAQ(String dbOwner, String qName) throws JMSException {
    if (!aq) {
      throw new JMSException("Diese Methode kann nur benutzt werden, falls eine AQ JMS Verbindung besteht");
    }
    q = ((AQjmsSession)qSes).getQueue(dbOwner, qName);
  }

  /**
   * Gibt die erste Nachricht aus der gesetzten Queue zurück, die innerhalb des timeouts mit der correlationId
   * gefunden wird. Falls keine gefunden wird, wird <code>null</code> zurückgegeben.<br>
   * die Queue muss vorher mit setQueue(), setQueueAQ oder setAQJNDI gesetzt werden.<br>
   * Falls die Nachricht keine TextMessage ist, wird ein Fehler geworfen.
   * @param correlationId
   * @param timeout siehe setTimeout()
   * @return
   * @throws JMSException
   */
  public String dequeueTextMessageByCorrelationId(String correlationId,
    long timeout) throws JMSException {
    setTimeout(timeout);
    setMessageSelector(buildMessageSelector(true, correlationId, null, null));
    Message m = dequeue();
    if (m == null) {
      return null;
    }
    if (m instanceof TextMessage) {
      TextMessage tm = (TextMessage)m;
      return tm.getText();
    } else {
      throw new JMSException("Empfangene Nachricht war keine TextMessage sondern " +
          m.getClass().getName());
    }
  }


 /* public static void main(String[] args) {
    //junit tests bräuchten eine datenbank und einen application server... TODO mit 10.0.10.11 als junittest
    //1. AQ
    
    Logger logger = Logger.getLogger("213");
    logger.addAppender(new Appender(){
          public void addFilter(Filter filter) {
          }

          public Filter getFilter() {
            return null;
          }

          public void clearFilters() {
          }

          public void close() {
          }

          public void doAppend(LoggingEvent loggingEvent) {
            System.out.println(loggingEvent.getRenderedMessage());
          }

          public String getName() {
            return null;
          }

          public void setErrorHandler(ErrorHandler errorHandler) {
          }

          public ErrorHandler getErrorHandler() {
            return null;
          }

          public void setLayout(Layout layout) {
          }

          public Layout getLayout() {
            return null;
          }

          public void setName(String string) {
          }

          public boolean requiresLayout() {
            return false;
          }
        });        
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    ContextHelper ch = new ContextHelper();
    ch.setContextFactory(ContextHelper.ContextFactoryType.REMOTE_CLIENT);
    ch.setOC4JUserName("oc4jadmin");
    ch.setOC4JPassword("oracle10");
    ch.setProviderUrl("10.0.10.11", 6003, "oc4j_soa");
    
    Connection dbCon =
      ConnectionFactory.getConnection("processing/processing@10.0.10.11:1521:xynadb",
        "jmsqueueutils test");
    
    try {
      JMSQueueUtils jms =  new JMSQueueUtils(dbCon);
      jms.setLogger(logger);
      jms.setQueueAQ("processing", "exitq");
      Message m = jms.getSession().createTextMessage("inhalt");
      jms.enqueue("123", "12324");
      System.out.println(m.getJMSCorrelationID());
      jms.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }*/

  /**
   * überschreibt den default logger, der mit Logger.getLogger("xyna.utils.jms") definiert wurde.
   * @param _logger
   */
  public void setLogger(Logger _logger) {
    logger = _logger;
  }

  /**
   * in Verbindung mit AQ-spezifischem Konstruktor wird enqueue und dequeue mit PLSQL durchgeführt.
   * ist derzeit nicht implementiert!
   * @param usePLSQL
   * @throws JMSException
   */
  public void setUsePLSQL(boolean usePLSQL) throws JMSException {
    if (usePLSQL && !aq) {
      throw new JMSException("PLSQL kann nur in Verbindung mit AQ benutzt werden.");
    }
    this.usePLSQL = usePLSQL;
  }

  private void enqueuePLSQL(Message m, String correlationID) throws JMSException {
    Connection con = ((AQjmsSession) qSes).getDBConnection();

    if (null == con)
      throw new JMSException("PLSQLQueue nicht initialisiert, keine Connection");

    String plsql = getPLSQLEnqueueString(((AQjmsDestination)q).getCompleteName(), correlationID, null);
    CallableStatement stmt = null;
    try {
      stmt = con.prepareCall(plsql);
      oracle.sql.CLOB clob = oracle.sql.CLOB.createTemporary(con, true,
        oracle.sql.CLOB.DURATION_SESSION);
      String msgText = "";
      if (m instanceof TextMessage) {
        TextMessage tm = (TextMessage)m;
        msgText = tm.getText();
      } else {
        throw new JMSException("Empfangene Nachricht war keine TextMessage sondern " +
          m.getClass().getName());
      }
      clob.putString(1, msgText);
      stmt.setClob(1, clob);
      stmt.execute();
      con.commit();
      clob.freeTemporary();
    }
    catch (SQLException e) {
      JMSException exp = new JMSException(e.getSQLState());
      exp.setLinkedException(e);
      throw exp;
    }
    finally {
      try {
        stmt.close();
      }
      catch (SQLException e) {
        JMSException exp = new JMSException(e.getSQLState());
        exp.setLinkedException(e);
        throw exp;
      }
    }
  }

  /* 
   * @Deprecated
   */
  private Message dequeuePLSQL() throws JMSException {
    AQConnector aqcon = new AQConnector(((AQjmsSession) qSes).getDBConnection());
    AQjmsDestination dest = (AQjmsDestination)q;
    String owner = dest.getQueueOwner();
    String result = aqcon.dequeue(owner+"."+q.getQueueName(), correlationId, dequeueTimeout);
    Message m = qSes.createTextMessage(result);
    m.setJMSCorrelationID(correlationId);
    return m;
  }
  
  // neue Methoden zur Benutzung von PL/SQL
  private String getPLSQLEnqueueString(String queueName, String correlationID, Map properties) {
    String plsql = "DECLARE\n" + "agent              sys.aq$_agent   := sys.aq$_agent(' ', null, 0);\n" + 
      "message            sys.aq$_jms_text_message;\n" + "enqueue_options    dbms_aq.enqueue_options_t;\n" + "message_properties dbms_aq.message_properties_t;\n" + "msgid               raw(16);\n" + "BEGIN\n" + "message := sys.aq$_jms_text_message.construct;\n" + "message_properties.correlation := '" + correlationID + "';\n";

    // Weitere MessageProperties, falls vorhanden
    if (null != properties && properties.size() > 0) {
      // Hmm, AQ unterstuetzt keine beliebigen Properties
      if (properties.containsKey("delay") && (properties.get("delay") instanceof Integer)) {
        logger.debug("PLSQLQueue: Setze Delay fuer Message " + 
          correlationID + ": " + ((Integer) properties.get("delay")).intValue());
        plsql += ("message_properties.delay := " + ((Integer) properties.get("delay")).intValue() + ";\n");
      }
      if (properties.containsKey("expiration") && (properties.get("expiration") instanceof Integer)) {
        plsql += ("message_properties.expiration := " + ((Integer) properties.get("expiration"))
          .intValue() + ";\n");
      }
    }

    plsql += ("message.set_text(?);\n" + "dbms_aq.enqueue(queue_name => '" + queueName + "',\n" + "                enqueue_options => enqueue_options,\n" + "                message_properties => message_properties,\n" + "                payload => message,\n" + "                msgid => msgid);\n" + "END;");

    logger.debug(plsql);
    return plsql;
  }
}
