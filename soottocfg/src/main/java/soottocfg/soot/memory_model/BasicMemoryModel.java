/**
 * 
 */
package soottocfg.soot.memory_model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.lang.model.type.NullType;

import com.google.common.base.Verify;

import soot.ArrayType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.StringConstant;
import soottocfg.cfg.Program;
import soottocfg.cfg.expression.BinaryExpression;
import soottocfg.cfg.expression.BinaryExpression.BinaryOperator;
import soottocfg.cfg.expression.Expression;
import soottocfg.cfg.expression.IdentifierExpression;
import soottocfg.cfg.expression.literal.NullLiteral;
import soottocfg.cfg.method.Method;
import soottocfg.cfg.statement.AssumeStatement;
import soottocfg.cfg.statement.CallStatement;
import soottocfg.cfg.type.BoolType;
import soottocfg.cfg.type.IntType;
import soottocfg.cfg.type.ReferenceLikeType;
import soottocfg.cfg.type.ReferenceType;
import soottocfg.cfg.type.Type;
import soottocfg.cfg.variable.ClassVariable;
import soottocfg.cfg.variable.Variable;
import soottocfg.soot.util.MethodInfo;
import soottocfg.soot.util.SootTranslationHelpers;

/**
 * @author schaef
 *
 */
public abstract class BasicMemoryModel extends MemoryModel {

	protected Program program;
	protected final Map<soot.Type, soottocfg.cfg.type.Type> types = new HashMap<soot.Type, soottocfg.cfg.type.Type>();
	protected final Map<SootField, Variable> fieldGlobals = new HashMap<SootField, Variable>();

	protected final Map<Constant, Variable> constantDictionary = new HashMap<Constant, Variable>();

	// protected final Type nullType;

	public BasicMemoryModel() {
		this.program = SootTranslationHelpers.v().getProgram();
	}

	@Override
	public boolean isNullReference(Expression e) {
		return e instanceof NullLiteral;
	}

	@Override
	public void mkArrayWriteStatement(Unit u, ArrayRef arrayRef, Value rhs) {
		throw new RuntimeException("This should have been removed by the array abstraction.");
	}

	@Override
	public void mkArrayReadStatement(Unit u, ArrayRef arrayRef, Value lhs) {
		throw new RuntimeException("This should have been removed by the array abstraction.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkNewExpr(soot.jimple.NewExpr)
	 */
	@Override
	public Expression mkNewExpr(NewExpr arg0) {
		Type newType = this.lookupType(arg0.getBaseType());
		// make this an application class to make sure that we analyze the
		// constructor
		arg0.getBaseType().getSootClass().setApplicationClass();
		MethodInfo mi = this.statementSwitch.getMethodInfo();

		Variable newLocal = mi.createFreshLocal("$new", newType, true, true);
		// add: assume newLocal!=null
//		this.statementSwitch.push(new AssumeStatement(statementSwitch.getCurrentLoc(),
//				new BinaryExpression(this.statementSwitch.getCurrentLoc(), BinaryOperator.Ne,
//						new IdentifierExpression(this.statementSwitch.getCurrentLoc(), newLocal),
//						this.mkNullConstant())));

		return new IdentifierExpression(this.statementSwitch.getCurrentLoc(), newLocal);
	}

	// new InstanceOfExpression(, new
	// IdentifierExpression(this.statementSwitch.getCurrentLoc(), newLocal),
	// lookupRefLikeType(arg0.getBaseType()));

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkNewArrayExpr(soot.jimple.
	 * NewArrayExpr )
	 */
	@Override
	public Expression mkNewArrayExpr(NewArrayExpr arg0) {
		Type newType = this.lookupType(arg0.getType());
		MethodInfo mi = this.statementSwitch.getMethodInfo();
		Variable newLocal = mi.createFreshLocal("$newArr", newType, true, true);

		this.statementSwitch.push(new AssumeStatement(statementSwitch.getCurrentLoc(),
				new BinaryExpression(this.statementSwitch.getCurrentLoc(), BinaryOperator.Ne,
						new IdentifierExpression(this.statementSwitch.getCurrentLoc(), newLocal),
						this.mkNullConstant())));

		// TODO
		// arg0.getSize().apply(valueSwitch);
		// Expression sizeExpression = valueSwitch.popExpression();
		// this.statementSwitch.push(
		// new AssumeStatement(statementSwitch.getCurrentLoc(),
		// new BinaryExpression(this.statementSwitch.getCurrentLoc(),
		// BinaryOperator.Eq,
		// new ArrayLengthExpression(this.statementSwitch.getCurrentLoc(),
		// new IdentifierExpression(this.statementSwitch.getCurrentLoc(),
		// newLocal)),
		// sizeExpression)));

		// return new IdentifierExpression(this.statementSwitch.getCurrentLoc(),
		// newLocal);
		throw new RuntimeException("This should have been removed by the array abstraction.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jayhorn.soot.memory_model.MemoryModel#mkNewMultiArrayExpr(soot.jimple
	 * .NewMultiArrayExpr)
	 */
	@Override
	public Expression mkNewMultiArrayExpr(NewMultiArrayExpr arg0) {
		// TODO Auto-generated method stub
		// System.err.println("New Multi-Array still not implemented");
		// return new IdentifierExpression(this.statementSwitch.getCurrentLoc(),
		// SootTranslationHelpers.v().getProgram().createFreshGlobal("TODO",
		// lookupType(arg0.getType())));
		throw new RuntimeException("This should have been removed by the array abstraction.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkStringLengthExpr(soot.Value)
	 */
	@Override
	public Expression mkStringLengthExpr(Value arg0) {
		return new IdentifierExpression(this.statementSwitch.getCurrentLoc(),
				SootTranslationHelpers.v().getProgram().createFreshGlobal("TODO", IntType.instance()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkNullConstant()
	 */
	@Override
	public Expression mkNullConstant() {
		return new NullLiteral(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkStringConstant(soot.jimple.
	 * StringConstant)
	 */
	@Override
	public Expression mkStringConstant(StringConstant arg0) {
		if (!constantDictionary.containsKey(arg0)) {
			constantDictionary.put(arg0, SootTranslationHelpers.v().getProgram().lookupGlobalVariable(
					"$string" + constantDictionary.size(), lookupType(arg0.getType()), true, true));
		}
		return new IdentifierExpression(this.statementSwitch.getCurrentLoc(), constantDictionary.get(arg0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkDoubleConstant(soot.jimple.
	 * DoubleConstant)
	 */
	@Override
	public Expression mkDoubleConstant(DoubleConstant arg0) {
		if (!constantDictionary.containsKey(arg0)) {
			constantDictionary.put(arg0, SootTranslationHelpers.v().getProgram().lookupGlobalVariable(
					"$double" + constantDictionary.size(), lookupType(arg0.getType()), true, true));
		}
		return new IdentifierExpression(this.statementSwitch.getCurrentLoc(), constantDictionary.get(arg0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jayhorn.soot.memory_model.MemoryModel#mkFloatConstant(soot.jimple.
	 * FloatConstant)
	 */
	@Override
	public Expression mkFloatConstant(FloatConstant arg0) {
		if (!constantDictionary.containsKey(arg0)) {
			constantDictionary.put(arg0, SootTranslationHelpers.v().getProgram().lookupGlobalVariable(
					"$float" + constantDictionary.size(), lookupType(arg0.getType()), true, true));
		}
		return new IdentifierExpression(this.statementSwitch.getCurrentLoc(), constantDictionary.get(arg0));
	}

	// @Override
	// public Expression lookupClassConstant(ClassConstant arg0) {
	// if (!constantDictionary.containsKey(arg0)) {
	// constantDictionary.put(arg0,
	// SootTranslationHelpers.v().getProgram().lookupGlobalVariable(
	// "$cc" + arg0.getValue(), lookupType(arg0.getType()), true, true));
	// }
	// return new IdentifierExpression(this.statementSwitch.getCurrentLoc(),
	// constantDictionary.get(arg0));
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see soottocfg.soot.memory_model.MemoryModel#lookupType(soot.Type)
	 * TODO: check which types to use for Short, Lond, Double, and Float.
	 */
	@Override
	public Type lookupType(soot.Type t) {
		if (!types.containsKey(t)) {
			Type type = null;
			if (t instanceof soot.BooleanType) {
				type = BoolType.instance();
			} else if (t instanceof soot.ByteType) {
				type = IntType.instance();
			} else if (t instanceof soot.CharType) {
				type = IntType.instance();
			} else if (t instanceof soot.DoubleType) {
				type = IntType.instance();
			} else if (t instanceof soot.FloatType) {
				type = IntType.instance();
			} else if (t instanceof soot.IntType) {
				type = IntType.instance();
			} else if (t instanceof soot.LongType) {
				type = IntType.instance();
			} else if (t instanceof soot.ShortType) {
				type = IntType.instance();
			} else if (t instanceof RefLikeType) {
				type = lookupRefLikeType((RefLikeType) t);
			} else {
				throw new RuntimeException("Don't know what to do with type " + t);
			}
			types.put(t, type);
		}
		return types.get(t);
	}

	@Override
	public void mkConstructorCall(Unit u, SootMethod constructor, List<Expression> args) {
		List<Expression> receiver = new LinkedList<Expression>();
		Method method = SootTranslationHelpers.v().lookupOrCreateMethod(constructor);
		CallStatement stmt = new CallStatement(SootTranslationHelpers.v().getSourceLocation(u), method, args, receiver);
		this.statementSwitch.push(stmt);
	}

	protected ReferenceLikeType lookupRefLikeType(RefLikeType t) {
		if (t instanceof ArrayType) {
			throw new RuntimeException("Remove Arrays first. " + t);
		} else if (t instanceof RefType) {
			return new ReferenceType(lookupClassVariable(SootTranslationHelpers.v().getClassConstant(t)));
		} else if (t instanceof NullType) {
			return (ReferenceType) (new NullLiteral(null)).getType();
		}
		throw new UnsupportedOperationException("Unsupported type " + t.getClass());
	}

	private String classNameToSootName(String className) {
		return className.replace('/', '.');
	}

	public ClassVariable lookupClassVariable(ClassConstant cc) {
		if (!this.constantDictionary.containsKey(cc)) {
			final String name = cc.getValue();
			final String sootClassName = classNameToSootName(name);

			if (Scene.v().containsClass(sootClassName)) {
				SootClass c = Scene.v().getSootClass(sootClassName);
				Collection<ClassVariable> parents = new HashSet<ClassVariable>();
				if (c.resolvingLevel() >= SootClass.HIERARCHY) {
					if (c.hasSuperclass()) {
						/**
						 * TODO: WARNING: we had bugs before from mixing
						 * qualified and non-qualified class
						 * names. E.g., we had:
						 * java.lang.Object and Object in the class hierarchy.
						 */
						SootClass superClass = c.getSuperclass();
						final String bytecodeName = superClass.getType().getClassName().replace('.', '/');
						parents.add(lookupClassVariable(ClassConstant.v(bytecodeName)));
					} else {
						/*
						 * This is a hack because, for whatever reason,
						 * Throwable and some other classes
						 * do not have Object as their superclass.
						 */
						if (c != Scene.v().getSootClass(Object.class.getName())) {
							SootClass superClass = Scene.v().getSootClass(Object.class.getName());
							final String bytecodeName = superClass.getType().getClassName().replace('.', '/');
							parents.add(lookupClassVariable(ClassConstant.v(bytecodeName)));
						}
					}
				}
				ClassVariable cv = new ClassVariable(name, parents);
				this.constantDictionary.put(cc, cv);

				List<Variable> fields = new LinkedList<Variable>();
				if (c.resolvingLevel() > SootClass.DANGLING) {
					for (SootField f : c.getFields()) {
						if (!f.isStatic()) {
							fields.add(lookupField(f));
						}
					}
				}
				cv.setAssociatedFields(fields);
			} else {
				// System.err.println("Class not in scene: "+sootClassName);
				this.constantDictionary.put(cc, new ClassVariable(name, new HashSet<ClassVariable>()));

				// sc.addField(new
				// SootField(SootTranslationHelpers.typeFieldName,
				// RefType.v(Scene.v().getSootClass("java.lang.Class"))));
			}
			Variable v = this.constantDictionary.get(cc);
			Verify.verifyNotNull(v);
			this.program.addClassVariable((ClassVariable) v);
		}
		return (ClassVariable) this.constantDictionary.get(cc);
	}

	protected Variable lookupField(SootField field) {
		if (!this.fieldGlobals.containsKey(field)) {
			final String fieldName = field.getDeclaringClass().getName() + "." + field.getName();
			Variable fieldVar = this.program.lookupGlobalVariable(fieldName, this.lookupType(field.getType()),
					field.isFinal(), field.isStatic());
			this.fieldGlobals.put(field, fieldVar);
		}
		return this.fieldGlobals.get(field);
	}
}
