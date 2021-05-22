package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the program dependency graph
 * is generated.
 */
public class ControlNodePDG extends ControlNodeBase {
	// The node that the instruction represents.
	private String node;

	private List<ControlNodePDG> childNodes;

	private Map<String, List<VariableAssignment>> assignments;

	public ControlNodePDG(ControlNodeType type, String node) {
		super(type);

		this.node = node;

		this.childNodes = new ArrayList<>();

		this.assignments = new HashMap<String, List<VariableAssignment>>();
	}

	public String getNode() {
		return node;
	}

	public Map<String, List<VariableAssignment>> getAssignments() {
		return assignments;
	}

	public List<ControlNodePDG> getChildNodes() {
		return childNodes;
	}
}
