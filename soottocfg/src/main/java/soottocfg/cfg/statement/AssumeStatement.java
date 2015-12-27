/**
 * 
 */
package soottocfg.cfg.statement;

import java.util.HashSet;
import java.util.Set;

import soottocfg.cfg.SourceLocation;
import soottocfg.cfg.expression.Expression;
import soottocfg.cfg.expression.IdentifierExpression;
import soottocfg.cfg.type.BoolType;

/**
 * @author teme
 *
 */
public class AssumeStatement extends Statement {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4719730863944690585L;
	private final Expression expression;

	/**
	 * @param createdFrom
	 */
	public AssumeStatement(SourceLocation loc, Expression expr) {
		super(loc);
		assert (expr.getType() == BoolType.instance());
		this.expression = expr;
	}

	public Expression getExpression() {
		return this.expression;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("assume ");
		sb.append(this.expression);
		return sb.toString();
	}

	@Override
	public Set<IdentifierExpression> getUseIdentifierExpressions() {
		Set<IdentifierExpression> used = new HashSet<IdentifierExpression>();
		used.addAll(expression.getUseIdentifierExpressions());
		return used;
	}

	@Override
	public Set<IdentifierExpression> getDefIdentifierExpressions() {
		return new HashSet<IdentifierExpression>();
	}

}
