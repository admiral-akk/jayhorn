package soottocfg.ast.Absyn; // Java Package generated by the BNF Converter.

public class SSnull extends SpecName {
  public SSnull() { }

  public <R,A> R accept(soottocfg.ast.Absyn.SpecName.Visitor<R,A> v, A arg) { return v.visit(this, arg); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof soottocfg.ast.Absyn.SSnull) {
      return true;
    }
    return false;
  }

  public int hashCode() {
    return 37;
  }


}