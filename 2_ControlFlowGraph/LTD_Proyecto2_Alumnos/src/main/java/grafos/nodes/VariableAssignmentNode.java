package grafos.nodes;

import java.util.ArrayList;
import java.util.List;

import grafos.ProgramDependencyGraph;

/**
 * Represents a variable assignment from a program in the {@link ProgramDependencyGraph}.
 */
public class VariableAssignmentNode extends NodeBase {
	// The name of the variable.
	private String variableName;

	// The node were the assignment is located.
	private String node;

	// The parent control node of the assignment.
	private ControlNodePDG parent;

	// All the nodes that reference this variable assignments.
	private List<String> references;

	public VariableAssignmentNode(String variableName, String node, ControlNodePDG parent) {
		super(NodeType.VARIABLE_ASSIGNATION);

		this.variableName = variableName;
		this.node = node;
		this.parent = parent;

		this.references = new ArrayList<String>();
	}

	public String getVariableName() {
		return variableName;
	}

	public String getNode() {
		return node;
	}

	public ControlNodePDG getParent() {
		return parent;
	}

	public List<String> getReferences() {
		return references;
	}
}
