/**
 * 
 */
package soottocfg.cfg.method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.AbstractBaseGraph;
import org.jgrapht.graph.ClassBasedEdgeFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import soottocfg.cfg.LiveVars;
import soottocfg.cfg.Node;
import soottocfg.cfg.Variable;
import soottocfg.cfg.expression.Expression;
import soottocfg.util.SetOperations;

/**
 * @author schaef extends DefaultDirectedGraph<Statement, DefaultEdge>
 */
public class Method extends AbstractBaseGraph<CfgBlock, CfgEdge> implements Node, DirectedGraph<CfgBlock, CfgEdge> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3367382274895641548L;

	private final String methodName;
	private Variable thisVariable, returnVariable;
	private List<Variable> parameterList = new LinkedList<Variable>();
	private Collection<Variable> locals;
	private Collection<Variable> modifiedGlobals;
	private CfgBlock source, sink;
	private boolean isProgramEntry = false;

	public Method(String uniqueName) {
		super(new ClassBasedEdgeFactory<CfgBlock, CfgEdge>(CfgEdge.class), true, true);
		methodName = uniqueName;
	}

	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Returns the set of predecessor vertices of b
	 * 
	 * @param b
	 * @return
	 */
	public Set<CfgBlock> getPredsOf(CfgBlock b) {
		Set<CfgBlock> result = new LinkedHashSet<CfgBlock>();
		for (CfgEdge e : incomingEdgesOf(b)) {
			result.add(getEdgeSource(e));
		}
		return result;
	}

	/**
	 * Returns the set of successor vertices of b
	 * 
	 * @param b
	 * @return
	 */
	public Set<CfgBlock> getSuccsOf(CfgBlock b) {
		Set<CfgBlock> result = new LinkedHashSet<CfgBlock>();
		for (CfgEdge e : outgoingEdgesOf(b)) {
			result.add(getEdgeTarget(e));
		}
		return result;
	}

	public void initialize(Variable thisVariable, Variable returnVariable, List<Variable> parameterList,
			Collection<Variable> locals, CfgBlock source, boolean isEntryPoint) {
		Preconditions.checkNotNull(parameterList, "Parameter list must not be null");
		this.thisVariable = thisVariable;
		this.returnVariable = returnVariable;
		this.parameterList = parameterList;
		this.locals = locals;
		this.source = source;
		this.isProgramEntry = isEntryPoint;
		
		// compute the modifies clause.
		// TODO: this has to be done transitive at some point!
		this.modifiedGlobals = new HashSet<Variable>();
		this.modifiedGlobals.addAll(this.getDefVariables());
		this.modifiedGlobals.removeAll(locals);
		this.modifiedGlobals.removeAll(parameterList);
		this.modifiedGlobals.remove(returnVariable);
		this.modifiedGlobals.remove(thisVariable);

	}
	
	
//	private void createUnifiedExitIfNecessary() {		
//		Set<CfgBlock> sinks = new HashSet<CfgBlock>();
//		for (CfgBlock b : vertexSet()) {
//			if (outDegreeOf(b)==0) {
//				sinks.add(b);
//			}
//		}
//		if (sinks.size()>1) {
//			System.err.println("Warning: "+getMethodName()+ " has multiple exits. Creating unified exit.");
//			//create a new unique source.
//			CfgBlock newSink = new CfgBlock(this);
//			for (CfgBlock pre : sinks) {
//				addEdge(newSink, pre);
//			}
//			sink = newSink;
//		}
//		
//	}

	/**
	 * Adds a guard expression as label to an edge. The label must not be null
	 * 
	 * @param edge
	 *            Existing edge in this Method.
	 * @param label
	 *            Non-null guard expression.
	 */
	public void setEdgeLabel(CfgEdge edge, Expression label) {
		edge.setLabel(label);
	}

	public boolean isProgramEntryPoint() {
		return this.isProgramEntry;
	}

	public CfgBlock getSource() {
		if (source==null) {
			for (CfgBlock b : vertexSet()) {				
				if (inDegreeOf(b)==0) {
					Verify.verify(source==null, "More than one source in graph!");
					source = b;
				}
			}
		}
		return source;
	}

	public CfgBlock getSink() {
		if (sink==null) {
			for (CfgBlock b : vertexSet()) {				
				if (outDegreeOf(b)==0) {
					Verify.verify(sink==null, "More than one source in graph!");
					sink = b;
				}
			}
		}		
		return sink;
	}

	/**
	 * Checks if the graph has a unique sink vertex and returns it. If more than
	 * one such vertex exists, it collects all sink vertices and connects them
	 * to a new unique sink.
	 * 
	 * @return The unique sink vertex of the graph.
	 */
	public CfgBlock findOrCreateUniqueSink() {
		if (sink == null) {
			Set<CfgBlock> currentSinks = new HashSet<CfgBlock>();
			for (CfgBlock b : this.vertexSet()) {
				if (this.outDegreeOf(b) == 0) {
					currentSinks.add(b);
				}
			}
			if (currentSinks.isEmpty()) {
				System.err.println("No exit for " + this.methodName);
				sink = null;
			} else if (currentSinks.size() == 1) {
				sink = currentSinks.iterator().next();
			} else {
				CfgBlock newSink = new CfgBlock(this);
				for (CfgBlock b : currentSinks) {
					this.addEdge(b, newSink);
				}
				sink = newSink;
			}
		}
		return sink;
	}

	public Set<CfgBlock> getExitBlocks() {
		Set<CfgBlock> ret = new HashSet<CfgBlock>();
		for (CfgBlock b : this.vertexSet()) {
			if (this.outDegreeOf(b) == 0) {
				ret.add(b);
			}
		}
		return ret;
	}

	public Collection<Variable> getInParams() {
		final List<Variable> rtr = new ArrayList<Variable>();
		if (thisVariable != null) {
			rtr.add(thisVariable);
		}
		rtr.addAll(parameterList);
		return rtr;
	}

	public Collection<Variable> getOutParams() {
		final List<Variable> rtr = new ArrayList<Variable>();
		if (returnVariable != null) {
			rtr.add(returnVariable);
		}
		return rtr;
	}

	public Collection<Variable> getModifiedGlobals() {
		return modifiedGlobals;
	}

	public Collection<Variable> getLocals() {
		return locals;
	}

	public void toDot(File dotFile) {
		try (FileOutputStream fileStream = new FileOutputStream(dotFile);
				OutputStreamWriter writer = new OutputStreamWriter(fileStream, "UTF-8");) {
			DOTExporter<CfgBlock, CfgEdge> dot = new DOTExporter<CfgBlock, CfgEdge>(new StringNameProvider<CfgBlock>(),
					null, null);
			dot.export(writer, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Method ");
		sb.append(this.methodName);
		String comma = "";
		sb.append("(");
		if (this.thisVariable != null) {
			sb.append(this.thisVariable.getName());
			comma = ", ";
		}
		for (Variable v : this.parameterList) {
			sb.append(comma);
			sb.append(v.getName());
			comma = ", ";
		}
		sb.append(")\n");
		if (this.returnVariable != null) {
			sb.append("\treturns: ");
			sb.append(this.returnVariable.getName());
			sb.append("\n");
		}
		if (this.modifiedGlobals != null && this.modifiedGlobals.size() > 0) {
			sb.append("\tmodifies: ");
			for (Variable v : this.modifiedGlobals) {
				sb.append(comma);
				sb.append(v.getName());
				comma = ", ";
			}
			sb.append("\n");
		}
		comma = "";
		sb.append("\n");
		if (!this.locals.isEmpty()) {
			sb.append("\tlocals:\n");
			for (Variable v : this.locals) {
				sb.append("\t\t");
				sb.append(v.getName());
				sb.append("\n");
			}
		}

		for (CfgBlock b : this.vertexSet()) {
			if (this.source == b) {
				sb.append("Root ->");
			}
			sb.append(b);
		}

		return sb.toString();
	}

	@Override
	public Set<Variable> getUseVariables() {
		Set<Variable> used = new HashSet<Variable>();
		for (CfgBlock b : this.vertexSet()) {
			used.addAll(b.getUseVariables());
		}
		return used;
	}

	@Override
	public Set<Variable> getDefVariables() {
		Set<Variable> rval = new HashSet<Variable>();
		for (CfgBlock b : this.vertexSet()) {
			rval.addAll(b.getDefVariables());
		}
		return rval;
	}


	/**
	 * Return the set of live variable at the entry of each block. A variable is
	 * live between its first and last use. Following the algorithm on p610 of
	 * the dragon book, 2nd ed.
	 * 
	 * @return
	 */
	public LiveVars<CfgBlock> computeBlockLiveVariables() {
		Set<CfgBlock> cfg = this.vertexSet();

		// Reserve the necessary size in the hashmap
		Map<CfgBlock, Set<Variable>> in = new HashMap<CfgBlock, Set<Variable>>(cfg.size());
		Map<CfgBlock, Set<Variable>> out = new HashMap<CfgBlock, Set<Variable>>(cfg.size());

		// cache these to save time
		Map<CfgBlock, Set<Variable>> use = new HashMap<CfgBlock, Set<Variable>>(cfg.size());
		Map<CfgBlock, Set<Variable>> def = new HashMap<CfgBlock, Set<Variable>>(cfg.size());

		// Start by initializing in to empty. The book does this separately for
		// exit and non exit blocks, but that's not necessary
		// TODO can exit blocks have variables? E.g. can they return values? In
		// which case we should actually recurse over all blocks!
		for (CfgBlock b : cfg) {
			in.put(b, new HashSet<Variable>());
			use.put(b, b.getUseVariables());
			def.put(b, b.getDefVariables());
		}

		boolean changed = false;

		do {
			changed = false;
			for (CfgBlock b : cfg) {
				out.put(b, b.computeLiveOut(in));
				Set<Variable> newIn = SetOperations.union(use.get(b), SetOperations.minus(out.get(b), def.get(b)));

				if (!newIn.equals(in.get(b))) {
					changed = true;
					in.put(b, newIn);
				}
			}
		} while (changed);

		return new LiveVars<CfgBlock>(in, out);
	}

}
