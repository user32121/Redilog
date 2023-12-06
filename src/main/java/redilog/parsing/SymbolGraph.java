package redilog.parsing;

import java.util.HashSet;
import java.util.Set;

import redilog.synthesis.LogicGraph;
import redilog.synthesis.RedilogSynthesisException;

/**
 * Represents the variables of a graph and how they relate to each other.<p>
 * This class is the intermediate between redilog code and a {@link LogicGraph},
 * and is reponsible for resolving values such as determining the range of all expressions.
 */
public class SymbolGraph {
    public Set<NamedExpression> expressions = new HashSet<>();

    /**
     * Ensure all {@link Expression expressions} have a nonempty value for their {@link Expression#range}
     */
    public void ResolveRanges() throws RedilogSynthesisException {
        for (NamedExpression ne : expressions) {
            ne.resolveRange();
        }
    }
}
