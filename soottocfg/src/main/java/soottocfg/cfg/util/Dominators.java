/**
 * 
 */
package soottocfg.cfg.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

/**
 * @author schaef
 *
 */
public class Dominators<V> extends AbstractDominators<V> {

	private final Map<V, Set<V>> dominators;
	private final Map<V, V> iDominators;
	private final Tree<V> dominatorTree;

	
	public Dominators(DirectedGraph<V, ?> g, V source) {
		super(g);		
		dominators = computeDominators(source, true);
		iDominators = computeImmediateDominators(dominators);
		dominatorTree = computeDominatorTree(iDominators);		
	}

	@Override
	public boolean isDominatedBy(V node, V dominator) {
		if (!dominators.containsKey(node)) {
			throw new IllegalArgumentException("Node is not part of the graph: "+node);
		}
		if (!dominators.containsKey(dominator)) {
			throw new IllegalArgumentException("Node is not part of the graph: "+node);
		}

		return dominators.get(node).contains(dominator);
	}

	@Override
	public V getImmediateDominator(V node) {
		if (!iDominators.containsKey(node)) {
			throw new IllegalArgumentException("Node is not part of the graph: "+node);
		}
		return iDominators.get(node);
	}

	@Override
	public Set<V> getDominators(V node) {
		if (!dominators.containsKey(node)) {
			throw new IllegalArgumentException("Node is not part of the graph: "+node);
		}
		return new HashSet<V>(dominators.get(node));
	}

	@Override
	public Map<V, Set<V>> getDominators() {
		return new HashMap<V, Set<V>>(dominators);
	}

	
	@Override
	public Tree<V> getDominatorTree() {
		return dominatorTree;
	}

}
