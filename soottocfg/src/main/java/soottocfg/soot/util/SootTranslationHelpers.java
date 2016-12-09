/**
 * 
 */
package soottocfg.soot.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import soot.ArrayType;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.ClassConstant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.tagkit.AbstractHost;
import soot.tagkit.Host;
import soot.tagkit.SourceFileTag;
import soot.tagkit.Tag;
import soottocfg.Options;
import soottocfg.cfg.Program;
import soottocfg.cfg.SourceLocation;
import soottocfg.cfg.method.Method;
import soottocfg.cfg.statement.Statement;
import soottocfg.cfg.variable.ClassVariable;
import soottocfg.cfg.variable.Variable;
import soottocfg.soot.SootRunner;
import soottocfg.soot.SootToCfg.MemModel;
import soottocfg.soot.memory_model.MemoryModel;
import soottocfg.soot.memory_model.NewMemoryModel;

/**
 * @author schaef
 *
 */
public enum SootTranslationHelpers {
	INSTANCE;

	public static SootTranslationHelpers v() {
		return INSTANCE;
	}

	public static final String HavocClassName = "Havoc_Class";
	public static final String HavocMethodName = "havoc_";

	/**
	 * Get a method that returns an unknown value of type t.
	 * 
	 * @param t
	 * @return
	 */
	public SootMethod getHavocMethod(soot.Type t) {
		if (!Scene.v().containsClass(HavocClassName)) {
			SootClass sClass = new SootClass(HavocClassName, Modifier.PUBLIC | Modifier.PUBLIC);
			sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
			sClass.setResolvingLevel(SootClass.SIGNATURES);
			Scene.v().addClass(sClass);
		}
		SootClass cls = Scene.v().getSootClass(HavocClassName);
		final String havocMethodName = HavocMethodName + t.toString();
		if (!cls.declaresMethodByName(havocMethodName)) {
			cls.addMethod(new SootMethod(havocMethodName, Arrays.asList(new Type[] {}), t,
					Modifier.PUBLIC | Modifier.STATIC));
		}
		return cls.getMethodByName("havoc_" + t.toString());
	}

	public static SootTranslationHelpers v(Program program) {
		final SootTranslationHelpers instance = INSTANCE;
		instance.setMemoryModelKind(Options.v().memModel());
		instance.setProgram(program);
		return instance;
	}

	private static final String parameterPrefix = "$in_";
	public static final String typeFieldName = "$dynamicType";

	public static final String arrayElementTypeFieldName = "$elType";
	public static final String lengthFieldName = "$length";
	// public static final String indexFieldNamePrefix = "$idx_";

	private transient SootMethod currentMethod;
	// private transient SootClass currentClass;
	private transient String currentSourceFileName;

	private transient MemoryModel memoryModel;
	private MemModel memoryModelKind = MemModel.PullPush;

	private transient Program program;

	public void reset() {
		currentMethod = null;
		currentSourceFileName = null;
		memoryModel = null;
		program = null;
	}

	public static boolean isDynamicTypeVar(Variable v) {
		return v.getName().contains(SootTranslationHelpers.typeFieldName);
	}

	public static boolean isDynamicTypeVar(SootField f) {
		return f.getName().contains(SootTranslationHelpers.typeFieldName);
	}

	public static SootField getTypeField(SootClass sc) {

		return Scene.v().getSootClass("java.lang.Object").getFieldByName(SootTranslationHelpers.typeFieldName);

		// return sc.getFieldByName(SootTranslationHelpers.typeFieldName);
	}

	public static void createTypeFields() {
		SootClass sc = Scene.v().getSootClass("java.lang.Object");
		if (!sc.declaresField(SootTranslationHelpers.typeFieldName)) {
			SootField sf = new SootField(SootTranslationHelpers.typeFieldName,
					RefType.v(Scene.v().getSootClass("java.lang.Class")), Modifier.PUBLIC);
			sc.addField(sf);
		}
		// List<SootClass> classes = new
		// LinkedList<SootClass>(Scene.v().getClasses());
		// for (SootClass sc : classes) {
		// createTypeField(sc);
		// }
	}

	public static SootField createTypeField(SootClass sc) {
		return getTypeField(sc);
		// SootField sf = new SootField(SootTranslationHelpers.typeFieldName,
		// RefType.v(Scene.v().getSootClass("java.lang.Class")), Modifier.PUBLIC
		// | Modifier.FINAL);
		// sc.addField(sf);
		// return sf;
	}

	public static List<SootField> findFieldsRecursivelyForRef(Value v) {
		return findFieldsRecursively(((RefType) v.getType()).getSootClass());
	}
	
	public static List<SootField> findFieldsRecursively(SootClass sc) {
		List<SootField> res = new LinkedList<SootField>();
		if (sc.hasSuperclass() && sc.getSuperclass().resolvingLevel() > SootClass.DANGLING) {
			res.addAll(findFieldsRecursively(sc.getSuperclass()));
		}
		res.addAll(sc.getFields());
		return res;
	}

	public static List<SootField> findNonStaticFieldsRecursivelyForRef(Value v) {
		return findNonStaticFieldsRecursively(((RefType) v.getType()).getSootClass());
	}
	
	public static List<SootField> findNonStaticFieldsRecursively(SootClass sc) {
		List<SootField> res = new LinkedList<SootField>();
		for (SootField sf : findFieldsRecursively(sc)) {
			if (!sf.isStatic()) {
				res.add(sf);
			}
		}
		return res;
 	}
	
	
	public Value getDefaultValue(soot.Type t) {
		Value rhs = null;
		if (t instanceof PrimType) {
			if (t instanceof soot.BooleanType) {
				rhs = IntConstant.v(0);
			} else if (t instanceof soot.ByteType) {
				rhs = IntConstant.v(0);
			} else if (t instanceof soot.CharType) {
				rhs = IntConstant.v(0);
			} else if (t instanceof soot.DoubleType) {
				rhs = DoubleConstant.v(0);
			} else if (t instanceof soot.FloatType) {
				rhs = FloatConstant.v(0);
			} else if (t instanceof soot.IntType) {
				rhs = IntConstant.v(0);
			} else if (t instanceof soot.LongType) {
				rhs = LongConstant.v(0);
			} else if (t instanceof soot.ShortType) {
				rhs = IntConstant.v(0);
			} else {
				throw new RuntimeException("Unknown type " + t);
			}
		} else {
			rhs = NullConstant.v();
		}
		return rhs;
	}

	public ClassVariable getClassVariable(SootClass sc) {
		return memoryModel.lookupClassVariable(getClassConstant(sc.getType()));
	}

	public ClassVariable getClassVariable(Type t) {
		return getClassVariable(((RefType)t).getSootClass());
	}
	
	public ClassConstant getClassConstant(Type t) {
		if (t instanceof RefType) {
			final String className = ((RefType) t).getClassName().replace(".", "/");
			return ClassConstant.v(className);
		} else if (t instanceof ArrayType) {
			// final String className =
			// getFakeArrayClass((ArrayType)t).getName().replace(".", "/");
			// return ClassConstant.v(className);
			throw new RuntimeException("Remove Arrays first! " + t);
		} else if (t instanceof PrimType) {
			final String className = ((PrimType) t).toString();
			return ClassConstant.v(className);
		}
		throw new RuntimeException("Not implemented");
	}

	public Method lookupOrCreateMethod(SootMethod m) {
		if (this.program.lookupMethod(m.getSignature()) != null) {
			return this.program.lookupMethod(m.getSignature());
		}
		int parameterCount = 0;
		final List<Variable> parameterList = new LinkedList<Variable>();
		if (!m.isStatic()) {
			parameterList.add(new Variable(parameterPrefix + (parameterCount++),
					getMemoryModel().lookupType(m.getDeclaringClass().getType())));
		}

		// if (Options.v().passCallerIdIntoMethods()) {
		// parameterList.add(new Variable(parameterPrefix + (parameterCount++),
		// IntType.instance()));
		// }

		for (int i = 0; i < m.getParameterCount(); i++) {
			parameterList.add(new Variable(parameterPrefix + (parameterCount++),
					getMemoryModel().lookupType(m.getParameterType(i))));
		}

		List<soottocfg.cfg.type.Type> outVarTypes = new LinkedList<soottocfg.cfg.type.Type>();
		if (!m.getReturnType().equals(VoidType.v())) {
			outVarTypes.add(memoryModel.lookupType(m.getReturnType()));
		} else if (m.isConstructor()) {
			/*
			 * For constructors, we assume that they return all fields
			 * that are assigned in this constructor
			 */			
			for (SootField sf : SootTranslationHelpers.findNonStaticFieldsRecursively(m.getDeclaringClass())) {
				outVarTypes.add(memoryModel.lookupType(sf.getType()));
			}
			
//			ClassVariable cv = ((ReferenceType) memoryModel.lookupType(m.getDeclaringClass().getType()))
//					.getClassVariable();
//			for (Variable fieldVar : cv.getAssociatedFields()) {
//				outVarTypes.add(fieldVar.getType());
//			}
		}
		return Method.createMethodInProgram(program, m.getSignature(), parameterList, outVarTypes,
				SootTranslationHelpers.v().getSourceLocation(m));
	}

	public Stmt getDefaultReturnStatement(Type returnType, Host createdFrom) {
		Stmt stmt;
		if (returnType instanceof VoidType) {
			stmt = Jimple.v().newReturnVoidStmt();
		} else {
			stmt = Jimple.v().newReturnStmt(getDefaultValue(returnType));
		}
		stmt.addAllTagsOf(createdFrom);
		return stmt;
	}

	void setProgram(Program p) {
		this.program = p;
	}

	public Program getProgram() {
		return this.program;
	}

	public SootClass getAssertionClass() {
		return Scene.v().getSootClass(SootRunner.assertionClassName);
	}

	public SootMethod getAssertMethod() {
		SootClass assertionClass = Scene.v().getSootClass(SootRunner.assertionClassName);
		return assertionClass.getMethodByName(SootRunner.assertionProcedureName);
	}

	public SootField getExceptionGlobal() {
		SootClass assertionClass = Scene.v().getSootClass(SootRunner.assertionClassName);
		return assertionClass.getFieldByName(SootRunner.exceptionGlobalName);
	}

	public Value getExceptionGlobalRef() {
		return Jimple.v().newStaticFieldRef(getExceptionGlobal().makeRef());
	}

	public Unit makeAssertion(Value cond, Host createdFrom) {
		List<Value> args = new LinkedList<Value>();
		args.add(cond);
		InvokeStmt stmt = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(getAssertMethod().makeRef(), args));
		stmt.addAllTagsOf(createdFrom);
		return stmt;
	}

	public SourceLocation getSourceLocation(Unit u) {
		int lineNumber = u.getJavaSourceStartLineNumber();

		if (lineNumber < 0) {
			lineNumber = SootTranslationHelpers.v().getJavaSourceLine(SootTranslationHelpers.v().getCurrentMethod());
		}
		return new SourceLocation(this.currentSourceFileName, lineNumber);
	}

	public SourceLocation getSourceLocation(SootMethod sm) {
		int lineNumber = sm.getJavaSourceStartLineNumber();

		if (lineNumber < 0) {
			lineNumber = SootTranslationHelpers.v().getJavaSourceLine(SootTranslationHelpers.v().getCurrentMethod());
		}

		return new SourceLocation(this.currentSourceFileName, lineNumber);
	}

	void setMemoryModelKind(MemModel kind) {
		memoryModelKind = kind;
	}

	public MemoryModel getMemoryModel() {
		if (this.memoryModel == null) {
			// TODO:
			if (memoryModelKind == MemModel.PullPush) {
				this.memoryModel = new NewMemoryModel();
			} else {
				throw new RuntimeException("Unknown memory model");
			}
		}
		return this.memoryModel;
	}

	public void setCurrentClass(SootClass currentClass) {
		String fn = findFileName(currentClass.getTags());
		if (fn != null) {
			this.currentSourceFileName = fn;
		}

		// this.currentClass = currentClass;
	}

	private String findFileName(List<Tag> tags) {
		String fileName = null;
		for (Tag tag : tags) {
			if (tag instanceof SourceFileTag) {
				SourceFileTag t = (SourceFileTag) tag;
				if (t.getAbsolutePath() != null) {
					fileName = t.getAbsolutePath();
				} else {
					if (t.getSourceFile() != null) {
						fileName = t.getSourceFile();
					}
				}
			} else {
				// System.err.println("Unprocessed tag " + tag.getClass() + " -
				// " + tag);
			}
		}
		return fileName;
	}

	public SootMethod getCurrentMethod() {
		return currentMethod;
	}

	public void setCurrentMethod(SootMethod currentMethod) {
		String fn = findFileName(currentMethod.getTags());
		if (fn != null) {
			this.currentSourceFileName = fn;
		}
		this.currentMethod = currentMethod;
	}

	public String getCurrentSourceFileName() {
		return this.currentSourceFileName;
	}

	public int getJavaSourceLine(AbstractHost ah) {
		return ah.getJavaSourceStartLineNumber();
	}

	public int getUniqueNumberForUnit(Unit u) {
		return u.hashCode();
	}

	public int getUniqueNumberForUnit(Statement s) {
		return s.hashCode();
	}

}
