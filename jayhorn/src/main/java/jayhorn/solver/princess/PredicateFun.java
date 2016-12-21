package jayhorn.solver.princess;

import ap.parser.IAtom;
import ap.parser.ITerm;
import ap.terfor.preds.Predicate;
import jayhorn.solver.ProverExpr;
import jayhorn.solver.ProverFun;
import jayhorn.solver.ProverTupleExpr;
import scala.collection.mutable.ArrayBuffer;

class PredicateFun implements ProverFun {

	private final Predicate pred;

	PredicateFun(Predicate pred) {
		this.pred = pred;
	}

	public ProverExpr mkExpr(ProverExpr[] args) {
            ProverExpr[] flatArgs = ProverTupleExpr.flatten(args);
            
            final ArrayBuffer<ITerm> argsBuf = new ArrayBuffer<ITerm>();
            for (int i = 0; i < flatArgs.length; ++i) {
                argsBuf.$plus$eq(((PrincessProverExpr)flatArgs[i]).toTerm());
            }

            return new FormulaExpr(new IAtom(pred, argsBuf.toSeq()));
	}

    public String toString() {
        return pred.toString();
    }

    public String toSMTLIBDeclaration() {
        StringBuffer res = new StringBuffer();
        res.append("(declare-fun " + pred.name() + "(");
        String sep = "";
        for (int i = 0; i < pred.arity(); ++i) {
            res.append(sep);
            res.append("Int");
            sep = " ";
        }
        res.append(") Bool)");
        return res.toString();
    }

    public int hashCode() {
        return pred.hashCode() + 13;
    }
    
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PredicateFun other = (PredicateFun) obj;
        if (pred == null) {
            if (other.pred != null)
                return false;
        } else if (!pred.equals(other.pred))
            return false;
        return true;
    }

}
