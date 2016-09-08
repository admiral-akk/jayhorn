/**
 * 
 */
package jayhorn.solver.spacer;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.InterpolationContext;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;

import jayhorn.Options;
import jayhorn.solver.BoolType;
import jayhorn.solver.IntType;
import jayhorn.solver.Prover;
import jayhorn.solver.ProverExpr;
import jayhorn.solver.ProverFun;
import jayhorn.solver.ProverHornClause;
import jayhorn.solver.ProverListener;
import jayhorn.solver.ProverResult;
import jayhorn.solver.ProverType;

/**
 * @author schaef
 *
 */
public class SpacerProver implements Prover {

	private Context ctx;
	private Solver solver;
	private boolean useHornLogic = false;

	private Set<FuncDecl> userDeclaredFunctions = new HashSet<FuncDecl>(); // todo
																			// hack.

	private HashMap<String, String> cfg = new HashMap<String, String>();

	static class Z3SolverThread implements Runnable {
		private final Solver solver;
		private Status status;

		public Z3SolverThread(Solver s) {
			this.solver = s;
		}

		@Override
		public void run() {
			this.status = this.solver.check();
		}

		public Status getStatus() {
			return this.status;
		}
	}

	public SpacerProver() {
		com.microsoft.z3.Global.ToggleWarningMessages(true);
		this.cfg.put("model", "true");
		this.ctx = new Context(this.cfg);
		createSolver(useHornLogic);
	}

	private void createSolver(boolean useHorn) {
		Params params = this.ctx.mkParams();
		if (useHorn) {
			this.solver = this.ctx.mkSolver();
			 params.add(":engine", "spacer");
			 params.add(":xform.slice", false);
			 params.add(":use_heavy_mev", true);
			 params.add(":reset_obligation_queue", true);
			 params.add(":pdr.flexible_trace", false);
			 params.add(":xform.inline-linear", false);
			 params.add(":xform.inline-eager", false);
			 params.add(":pdr.utvpi", false);
	
			if (Options.v().getTimeout() > 0) {
				int timeoutInMsec = (int)TimeUnit.SECONDS.toMillis(Options.v().getTimeout());
				params.add("timeout", timeoutInMsec);
				this.solver.setParameters(params);
			}

		} else {
			this.solver = this.ctx.mkSolver();
		}
	}

	protected Expr unpack(ProverExpr exp) {
		if (exp instanceof SpacerTermExpr) {
			return ((SpacerTermExpr) exp).getExpr();
		} else if (exp instanceof SpacerBoolExpr) {
			return ((SpacerBoolExpr) exp).getExpr();
		}
		throw new RuntimeException("not implemented " + exp.getType().toString());
	}

	protected Expr[] unpack(ProverExpr[] exp) {
		Expr[] res = new Expr[exp.length];
		for (int i = 0; i < exp.length; i++) {
			res[i] = unpack(exp[i]);
		}
		return res;
	}

	protected ProverExpr pack(Expr e) {
		if (e instanceof BoolExpr) {
			return new SpacerBoolExpr((BoolExpr) e);
		} else {
			return new SpacerTermExpr(e, pack(e.getSort()));
		}
	}

	protected ProverExpr[] pack(Expr[] e) {
		ProverExpr[] res = new ProverExpr[e.length];
		for (int i = 0; i < e.length; i++) {
			res[i] = pack(e[i]);
		}
		return res;
	}

	protected Sort unpack(ProverType type) {
		if (type instanceof IntType) {
			return ctx.getIntSort();
		} else if (type instanceof BoolType) {
			return ctx.getBoolSort();
		} else if (type instanceof SpacerArrayType) {
			return ((SpacerArrayType) type).getSort();
		}
		throw new RuntimeException("not implemented");
	}

	protected ProverType pack(Sort type) {
		if (type.equals(ctx.getIntSort())) {
			return this.getIntType();
		} else if (type.equals(ctx.getBoolSort())) {
			return this.getBooleanType();
		} else if (type.equals(ctx.getRealSort())) {
			throw new RuntimeException("not implemented");
		} else if (type instanceof ArraySort) {
			ArraySort as = (ArraySort) type;
			return new SpacerArrayType(as, pack(as.getRange()), pack(as.getDomain()));
		} else {
			throw new RuntimeException("not implemented");
		}
	}

	@Override
	public ProverType getBooleanType() {
		return BoolType.INSTANCE;
	}

	@Override
	public ProverType getIntType() {
		return IntType.INSTANCE;
	}

	@Override
	public ProverType getArrayType(ProverType[] argTypes, ProverType resType) {
		if (argTypes.length == 0) {
			throw new RuntimeException("Array should have at one dimension");
		}
		ProverType t = resType;
		Sort sort = unpack(resType);
		// fold everything but the last iteration.
		for (int i = argTypes.length - 1; i > 0; i--) {
			sort = lookupArraySort(unpack(argTypes[i]), sort);
		}
		return new SpacerArrayType(lookupArraySort(unpack(argTypes[0]), sort), argTypes[0], t);
	}

	private ArraySort lookupArraySort(Sort idx, Sort val) {
		return this.ctx.mkArraySort(idx, val);
	}

	@Override
	public ProverExpr mkBoundVariable(int deBruijnIndex, ProverType type) {
		return new SpacerTermExpr(ctx.mkBound(deBruijnIndex, unpack(type)), type);
	}

	@Override
	public ProverExpr mkVariable(String name, ProverType type) {
		Expr exp = null;
		if (type instanceof IntType) {
			exp = ctx.mkIntConst(name);
		} else if (type instanceof BoolType) {
			exp = ctx.mkBoolConst(name);
		} else if (type instanceof SpacerArrayType) {
			exp = ctx.mkArrayConst(name, unpack(((SpacerArrayType) type).getIndexType()),
					unpack(((SpacerArrayType) type).getValueType()));
		} else {
			throw new RuntimeException("not implemented");
		}
		return new SpacerTermExpr(exp, type);
	}

	@Override
	public ProverFun mkUnintFunction(String name, ProverType[] argTypes, ProverType resType) {
		Sort[] argSorts = new Sort[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			argSorts[i] = unpack(argTypes[i]);
		}
		FuncDecl fd = ctx.mkFuncDecl(name, argSorts, unpack(resType));
		userDeclaredFunctions.add(fd);
		return new SpacerFun(fd, ctx, resType);
	}

	/**
	 * TODO: Define a new interpreted function. The body is supposed to contain
	 * bound variables with indexes <code>0, 1, ..., (n-1)</code> representing
	 * the arguments of the function.
	 */
	@Override
	public ProverFun mkDefinedFunction(String name, ProverType[] argTypes, ProverExpr body) {
		final ProverExpr b = body;
		return new ProverFun() {
			public ProverExpr mkExpr(ProverExpr[] args) {
				final Expr[] z3args = new Expr[args.length];
				for (int i = 0; i < args.length; i++) {
					if (args[i] instanceof SpacerTermExpr) {
						z3args[i] = ((SpacerTermExpr) args[i]).getExpr();
					} else if (args[i] instanceof SpacerBoolExpr) {
						z3args[i] = ctx.mkITE(((SpacerBoolExpr) args[i]).getExpr(), ctx.mkInt(0), ctx.mkInt(1));
					}
				}
				if (b instanceof SpacerTermExpr) {
					return new SpacerTermExpr(unpack(b).substituteVars(z3args), b.getType());
				} else {
					return new SpacerBoolExpr((BoolExpr) unpack(b).substituteVars(z3args));
				}
			}
		};

	}

	@Override
	public ProverExpr mkAll(ProverExpr body, ProverType type) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkEx(ProverExpr body, ProverType type) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkTrigger(ProverExpr body, ProverExpr[] triggers) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkEq(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkEq(unpack(left), unpack(right)));
	}

	@Override
	public ProverExpr mkLiteral(boolean value) {
		return new SpacerBoolExpr(ctx.mkBool(value));
	}

	@Override
	public ProverExpr mkNot(ProverExpr body) {
		return new SpacerBoolExpr(ctx.mkNot((BoolExpr) unpack(body)));
	}

	@Override
	public ProverExpr mkAnd(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkAnd((BoolExpr) unpack(left), (BoolExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkAnd(ProverExpr[] args) {

		BoolExpr[] bargs = new BoolExpr[args.length];
		for (int i = 0; i < args.length; i++) {
			bargs[i] = (BoolExpr) unpack(args[i]);
		}

		return new SpacerBoolExpr(ctx.mkAnd(bargs));
	}

	@Override
	public ProverExpr mkOr(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkOr((BoolExpr) unpack(left), (BoolExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkOr(ProverExpr[] args) {
		BoolExpr[] bargs = new BoolExpr[args.length];
		for (int i = 0; i < args.length; i++) {
			bargs[i] = (BoolExpr) unpack(args[i]);
		}
		return new SpacerBoolExpr(ctx.mkOr(bargs));
	}

	@Override
	public ProverExpr mkImplies(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkImplies((BoolExpr) unpack(left), (BoolExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkIte(ProverExpr cond, ProverExpr thenExpr, ProverExpr elseExpr) {
		return new SpacerTermExpr(ctx.mkITE((BoolExpr) unpack(cond), unpack(thenExpr), unpack(elseExpr)),
				thenExpr.getType());
	}

	@Override
	public ProverExpr mkLiteral(int value) {
		return new SpacerTermExpr(ctx.mkInt(value), this.getIntType());
	}

	@Override
	public ProverExpr mkLiteral(BigInteger value) {
		return new SpacerTermExpr(ctx.mkInt(value.longValue()), this.getIntType());
	}

	@Override
	public ProverExpr mkPlus(ProverExpr left, ProverExpr right) {
		return new SpacerTermExpr(ctx.mkAdd((ArithExpr) unpack(left), (ArithExpr) unpack(right)), left.getType());
	}

	@Override
	public ProverExpr mkPlus(ProverExpr[] args) {
		ArithExpr[] aargs = new ArithExpr[args.length];
		for (int i = 0; i < args.length; i++) {
			aargs[i] = (ArithExpr) unpack(args[i]);
		}
		return new SpacerTermExpr(ctx.mkAdd(aargs), this.getBooleanType());
	}

	@Override
	public ProverExpr mkMinus(ProverExpr left, ProverExpr right) {
		return new SpacerTermExpr(ctx.mkSub((ArithExpr) unpack(left), (ArithExpr) unpack(right)), left.getType());
	}

	@Override
	public ProverExpr mkNeg(ProverExpr arg) {
		return new SpacerTermExpr(ctx.mkUnaryMinus((ArithExpr) unpack(arg)), arg.getType());
	}

	@Override
	public ProverExpr mkEDiv(ProverExpr num, ProverExpr denom) {
		return new SpacerTermExpr(ctx.mkDiv((ArithExpr) unpack(num), (ArithExpr) unpack(num)), this.getIntType());
	}

	@Override
	public ProverExpr mkEMod(ProverExpr num, ProverExpr denom) {
		return new SpacerTermExpr(ctx.mkMod((IntExpr) unpack(num), (IntExpr) unpack(num)), this.getIntType());
	}

	@Override
	public ProverExpr mkTDiv(ProverExpr num, ProverExpr denom) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkTMod(ProverExpr num, ProverExpr denom) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkMult(ProverExpr left, ProverExpr right) {
		return new SpacerTermExpr(ctx.mkMul((ArithExpr) unpack(left), (ArithExpr) unpack(right)), left.getType());
	}

	@Override
	public ProverExpr mkGeq(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkGe((ArithExpr) unpack(left), (ArithExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkGt(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkGt((ArithExpr) unpack(left), (ArithExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkLeq(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkLe((ArithExpr) unpack(left), (ArithExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkLt(ProverExpr left, ProverExpr right) {
		return new SpacerBoolExpr(ctx.mkLt((ArithExpr) unpack(left), (ArithExpr) unpack(right)));
	}

	@Override
	public ProverExpr mkSelect(ProverExpr ar, ProverExpr[] indexes) {
		if (indexes.length == 1) {
			return new SpacerTermExpr(ctx.mkSelect((ArrayExpr) unpack(ar), unpack(indexes[0])), ar.getType());
		}
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr mkStore(ProverExpr ar, ProverExpr[] indexes, ProverExpr value) {
		if (indexes.length == 1) {
			return new SpacerTermExpr(ctx.mkStore((ArrayExpr) unpack(ar), unpack(indexes[0]), unpack(value)), ar.getType());
		}
		throw new RuntimeException("not implemented");
	}

	@Override
	public void push() {
		this.solver.push();
	}

	@Override
	public void pop() {
		this.solver.pop();
		interpolationPattern.clear(); // TODO make this a bit smarter.
		interpolationPartition = -1;
	}

	private SortedMap<Integer, List<BoolExpr>> interpolationPattern = new TreeMap<Integer, List<BoolExpr>>();
	private int interpolationPartition = -1;

	@Override
	public void addAssertion(ProverExpr assertion) {
		if (assertion instanceof SapcerHornExpr) {

			SapcerHornExpr hc = (SapcerHornExpr) assertion;
			BoolExpr head = (BoolExpr) unpack(hc.getHead());
			BoolExpr body = (BoolExpr) unpack(hc.getConstraint());

			Set<Expr> freeVars = new HashSet<Expr>();
			freeVars.addAll(freeVariables(head));

			if (hc.getBody().length > 0) {
				BoolExpr[] conj = new BoolExpr[hc.getBody().length];
				int i = 0;
				for (Expr e : unpack(hc.getBody())) {
					freeVars.addAll(freeVariables(e));
					conj[i] = (BoolExpr) e;
					i++;
				}
				BoolExpr b = (conj.length == 1) ? conj[0] : ctx.mkAnd(conj);
				if (body.equals(ctx.mkTrue())) {
					body = b;
				} else {
					body = ctx.mkAnd(b, body);
				}
			}
			// from Nikolajs example
			BoolExpr asrt;
			if (freeVars.size() > 0) {
				asrt = ctx.mkForall(freeVars.toArray(new Expr[freeVars.size()]), ctx.mkImplies(body, head), 1, null,
						null, null, null);
			} else {
				asrt = ctx.mkImplies(body, head);
			}
			this.solver.add(asrt);
		} else if (assertion instanceof SpacerBoolExpr || assertion instanceof SpacerTermExpr) {
			BoolExpr asrt = (BoolExpr) unpack(assertion);
			this.solver.add(asrt);

			if (interpolationPartition >= 0 && ctx instanceof InterpolationContext) {
				if (!this.interpolationPattern.containsKey(this.interpolationPartition)) {
					this.interpolationPattern.put(this.interpolationPartition, new LinkedList<BoolExpr>());
				}
				this.interpolationPattern.get(this.interpolationPartition).add(asrt);
			}
		} else {
			throw new RuntimeException("Cannot add " + assertion + " of type " + assertion.getClass());
		}
	}

	private ExecutorService executor = null;
	private Future<?> future = null;
	private Z3SolverThread thread = null;

	@Override
	public ProverResult checkSat(boolean block) {
		if (block) {
			return translateResult(this.solver.check());
		} else {
			if (future != null && !future.isDone()) {
				throw new RuntimeException("Another check is still running.");
			}
			this.executor = Executors.newSingleThreadExecutor();
			this.thread = new Z3SolverThread(solver);
			this.future = executor.submit(this.thread);
			return ProverResult.Running;
		}
	}

	private ProverResult translateResult(Status status) {
		if (status == Status.SATISFIABLE) {
			return ProverResult.Sat;
		} else if (status == Status.UNSATISFIABLE) {
			return ProverResult.Unsat;
		} else {
			return ProverResult.Unknown;
		}
	}

	private void killThread() {
		if (this.future != null && !this.future.isDone()) {
			this.future.cancel(true);
		}
		if (this.executor != null) {
			this.executor.shutdown();
		}
		this.executor = null;
		this.future = null;
		this.thread = null;
	}

	@Override
	public ProverResult nextModel(boolean block) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverResult getResult(boolean block) {
		ProverResult result;
		if (future != null) {
			if (block) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException("solver failed");
				}
				result = translateResult(this.thread.getStatus());
				killThread();
			} else {
				if (future.isDone()) {
					result = translateResult(this.thread.getStatus());
					killThread();
				} else {
					result = ProverResult.Running;
				}
			}
		} else {
			// throw new RuntimeException("Start query with check sat first.");
			result = ProverResult.Unknown;
		}
		return result;
	}

	@Override
	public ProverResult getResult(long timeoutInSeconds) {
		ProverResult result;
		if (future != null) {
			try {
				future.get(timeoutInSeconds, TimeUnit.SECONDS);
				result = translateResult(this.thread.getStatus());
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("solver failed");
			} catch (TimeoutException e) {
				result = ProverResult.Unknown;
			}
			killThread();
		} else {
			throw new RuntimeException("Start query with check sat first.");
		}
		return result;
	}

	@Override
	public ProverResult stop() {
		// TODO: what is this used for?
		ProverResult res = getResult(false);
		killThread();
		if (res == ProverResult.Running)
			return ProverResult.Unknown;
		return res;
	}

	@Override
	public void setConstructProofs(boolean b) {
		killThread();
		if (b) {
			cfg.put("proof", "true");
			this.ctx = new InterpolationContext(this.cfg);
		} else {
			cfg.put("proof", "false");
			this.ctx = new Context(this.cfg);

		}
		createSolver(useHornLogic);
	}

	@Override
	public void setPartitionNumber(int num) {
		if (!(ctx instanceof InterpolationContext)) {
			throw new RuntimeException("call setConstructProofs(true) first");
		}
		if (num < 0) {
			throw new RuntimeException("only positive partition numbers please");
		}
		this.interpolationPartition = num;
	}

	@Override
	public ProverExpr[] interpolate(int[][] partitionSeq) {
		if (!(ctx instanceof InterpolationContext)) {
			throw new RuntimeException("call setConstructProofs(true) first");
		}
		InterpolationContext ictx = (InterpolationContext) ctx;

		List<BoolExpr> patternList = new LinkedList<BoolExpr>();
		for (Entry<Integer, List<BoolExpr>> entry : this.interpolationPattern.entrySet()) {
			BoolExpr conj = ictx.mkAnd(entry.getValue().toArray(new BoolExpr[entry.getValue().size()]));
			patternList.add(ictx.MkInterpolant(conj));
		}
		patternList.add(ictx.mkTrue());

		BoolExpr pat = ictx.mkAnd(patternList.toArray(new BoolExpr[patternList.size()]));
		Params params = ictx.mkParams();
		Expr proof = this.solver.getProof();
		Expr[] interps = ictx.GetInterpolant(proof, pat, params);

		List<SpacerBoolExpr> result = new LinkedList<SpacerBoolExpr>();
		// TODO check if the bounds makes sense.
		// for (int i = 0; i < interps.length; i++) {
		for (int i = 0; i < partitionSeq.length - 1; i++) {
			result.add(new SpacerBoolExpr((BoolExpr) interps[i]));
		}
		return result.toArray(new ProverExpr[result.size()]);
	}

	@Override
	public void addListener(ProverListener listener) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ProverExpr evaluate(ProverExpr expr) {
		Model m = this.solver.getModel();
		if (m == null) {
			throw new RuntimeException("no model :(");
		}
		Expr e = unpack(expr);

		if (e.isConst()) {
			return new SpacerTermExpr(m.getConstInterp(e), expr.getType());
		}

		return new SpacerTermExpr(m.evaluate(e, false), expr.getType());
	}

	@Override
	public ProverExpr[] freeVariables(ProverExpr expr) {
		List<Expr> freeVars = freeVariables(unpack(expr));
		List<ProverExpr> freeProverVars = new LinkedList<ProverExpr>();
		for (Expr e : freeVars) {
			freeProverVars.add(pack(e));
		}
		return freeProverVars.toArray(new ProverExpr[freeProverVars.size()]);
	}

	private List<Expr> freeVariables(Expr e) {
		List<Expr> result = new LinkedList<Expr>();
		if (e.equals(ctx.mkTrue()) || e.equals(ctx.mkFalse()) || e.isNumeral()) {
			// ignore
		} else if (e.isConst() && e.getArgs().length == 0) {
			if (userDeclaredFunctions.contains(e.getFuncDecl())) {
				// do nothing.
			} else {
				result.add(e);
			}
		} else if (e.isQuantifier()) {
			Quantifier q = (Quantifier) e;
			q.getBoundVariableNames();
			freeVariables(((Quantifier) e).getBody());
			throw new RuntimeException("not implemented");
		} else if (e.isApp()) {
			for (Expr child : e.getArgs()) {
				result.addAll(freeVariables(child));
			}
		} else {
			throw new RuntimeException("not implemented " + e + " " + e.getClass().toString());
		}

		return result;
	}

	@Override
	public ProverExpr substitute(ProverExpr target, ProverExpr[] from, ProverExpr[] to) {
		Expr e = unpack(target).substitute(unpack(from), unpack(to));
		if (target instanceof SpacerBoolExpr) {
			return new SpacerBoolExpr((BoolExpr) e);
		} else {
			return new SpacerTermExpr(e, target.getType());
		}
	}

	@Override
	public void shutdown() {
		// TODO Is this true?
		killThread();
		this.solver.reset();
		this.solver.dispose();
		ctx.dispose();

	}

	@Override
	public void reset() {
		killThread();
		this.solver.reset();
		this.interpolationPattern = new TreeMap<Integer, List<BoolExpr>>();
		this.interpolationPartition = -1;
	}

	// //////////////////////////////////////////////////////////////////////////
	// Horn clause interface

	@Override
	public ProverExpr mkHornVariable(String name, ProverType type) {
		return mkVariable(name, type);
	}

	private int count = 0;

	public ProverFun mkHornPredicate(String name, ProverType[] argTypes) {
		return this.mkUnintFunction(name + (count++), argTypes, this.getBooleanType());
	}

	/**
	 * The head literal can either be constructed using
	 * <code>mkHornPredicate</code>, or be the formula <code>false</code>.
	 */
	public ProverHornClause mkHornClause(ProverExpr head, ProverExpr[] body, ProverExpr constraint) {
		return new SapcerHornExpr(head, body, constraint);
	}

	@Override
	public void setHornLogic(boolean b) {
		useHornLogic = b;
		createSolver(useHornLogic);
	}

	@Override
	public String toString() {
		return "Z3";
	}

    ////////////////////////////////////////////////////////////////////////////
    // Some functions for outputing SMT-LIB

    public String toSMTLIBDeclaration(ProverFun fun) {
        throw new UnsupportedOperationException();
    }

    public String toSMTLIBFormula(ProverHornClause clause) {
        throw new UnsupportedOperationException();
    }
    
    public void parseSMTLIBFormula(final String formula) {
    	throw new UnsupportedOperationException();
    }
}
