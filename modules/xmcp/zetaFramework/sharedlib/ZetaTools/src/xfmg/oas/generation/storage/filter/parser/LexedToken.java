

package xfmg.oas.generation.storage.filter.parser;

import java.util.Optional;

// erbt von filter elem?
public abstract class LexedToken {
  
  // attr. orig input string (für quote-blöcke)
  private final String originalInput;

  
  public LexedToken(String originalInput) {
    this.originalInput = originalInput;
  }


  // get orig input string
  public String getOriginalInput() {
    return originalInput;
  }


  // is finished: return false
  public boolean isFinished() {
    return false;
  }
  
}
