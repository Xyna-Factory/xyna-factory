

package xfmg.oas.generation.storage.filter.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;


public class Lexer {

  // attribut: liste lexed-token
  //private List<LexedToken> _tokens = new ArrayList<>();
  
  // constr. (string)
  // -> call tokenize
  
  // tokenize
  // -> call java tokenizer : liste strings
  // pro string: get (optional) op-type (enum ), sonst literal (oder whitespace?)
  // output (erbt von lexedtoken): literal, whitespace, op-token
  public List<LexedToken> execute(String input) {
    List<LexedToken> ret = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(input, " &!|()'\"\t\n", true);
    while (st.hasMoreTokens()) {
      Optional<LexedToken> opt = buildToken(st.nextToken());
      if (opt.isPresent()) {
        ret.add(opt.get());
      }
    }
    return ret;
  }
  
  
  private Optional<LexedToken> buildToken(String input) {
    if (input == null) { return Optional.empty(); }
    Optional<LexedToken> opt = Whitespace.buildIfMatches(input);
    if (opt.isPresent()) { return opt; }
    opt = OperatorToken.buildIfMatches(input);
    if (opt.isPresent()) { return opt; }
    return Optional.of(new Literal(input));
  }
  
}
