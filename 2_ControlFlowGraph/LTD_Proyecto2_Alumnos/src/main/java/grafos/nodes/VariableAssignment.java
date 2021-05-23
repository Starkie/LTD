package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableAssignment {	
	private String variableName;
	private String node;
	private ControlNodePDG parent;
	
	// All the nodes that reference this variable assignments.
	private List<String> references;
	
	public VariableAssignment(String variableName, String node, ControlNodePDG parent) {
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
