package grafos.nodes;

public class VariableAssignment {	
	private String variableName;
	private String node;
	private ControlNodePDG parent;
	
	public VariableAssignment(String variableName, String node, ControlNodePDG parent) {
		this.variableName = variableName;
		this.node = node;
		this.parent = parent;
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
}
