package soottocfg.ast.Absyn; // Java Package generated by the BNF Converter.

public abstract class IterStm implements java.io.Serializable {
  public abstract <R,A> R accept(IterStm.Visitor<R,A> v, A arg);
  public interface Visitor <R,A> {
    public R visit(soottocfg.ast.Absyn.While p, A arg);
    public R visit(soottocfg.ast.Absyn.Do p, A arg);

  }

}