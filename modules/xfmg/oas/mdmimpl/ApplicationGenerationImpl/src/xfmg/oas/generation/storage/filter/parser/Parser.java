

package xfmg.oas.generation.storage.filter.parser;


public class Parser {

  // keine attribute, nur tool-klasse, wird von expression-parse-methoden durchgereicht?
  
  // parse
  // input: liste lexed-token 
  // -> root-container erzeugen
  // -> handle quotes
   
  // merge operators: && || als doppelte ops identifizieren und ersetzen?
  // schon hier check ob mehr als zwei hintereinander, dann fehler?
  
  // ? replace container
     
     
  // handle quotes (input liste lexed-token)
  // find first quote (single / double), find next (selber typ), dann replace-quotes
  
  // replace-quotes (liste lex-token, int startpos, int endpos)
  // output: neue liste lex-token (sublisten vorher u. nachher, quote-bereich durch neues literal ersetzt)
  // mit generisch replace() von container?
}
