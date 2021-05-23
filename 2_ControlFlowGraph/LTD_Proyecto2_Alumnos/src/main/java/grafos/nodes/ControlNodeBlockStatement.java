package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * Represents a {@link BlockStmt} that is present inside {@link ControlNodePDG}.
 * This parameters where extracted from it because some ControlNodes can have more than 
 * one block. For example: if-else statements can have 2 blocks.
 */
public class ControlNodeBlockStatement {
	private List<NodeBase> childNodes;

	private Map<String, Map<ControlNodePDG, List<VariableAssignment>>> assignments;

	private ControlNodePDG parentNode;

	public ControlNodeBlockStatement(ControlNodePDG parentNode) {
		this.parentNode = parentNode;
		this.childNodes = new ArrayList<NodeBase>();
		this.assignments = new HashMap<String, Map<ControlNodePDG, List<VariableAssignment>>>();
	}

	public List<NodeBase> getChildNodes() {
		return childNodes;
	}
	
	/**
	 * Registers a variable assignment in the given {@link ControlNodePDG}.
	 * @param assignment The variable assignment to register.
	 */
	public void addAssignment(VariableAssignment assignment)
	{		
		this.childNodes.add(assignment);
	}
	
	public List<VariableAssignment> getLastAssignments(String variableName)
	{
		List<VariableAssignment> variableAssignments = new ArrayList<VariableAssignment>();
		
		for (int i = (this.childNodes.size() - 1); i >= 0; i--)
		{
			if (this.childNodes.get(i) instanceof VariableAssignment)
			{
				VariableAssignment va = (VariableAssignment) this.childNodes.get(i);
				
				if (va.getVariableName().equals(variableName))
				{
					variableAssignments.add(va);
					
					// Stop the search since it was a top level assignment.
					break;
				}
			}
			else if (this.childNodes.get(i) instanceof ControlNodePDG)
			{
				ControlNodePDG node = (ControlNodePDG) this.childNodes.get(i);
				
				variableAssignments.addAll(node.getLastAssignments(variableName));
			}
		}
		
		return variableAssignments;
	}

	public Map<String, Map<ControlNodePDG, List<VariableAssignment>>> getAssignments() {
		return assignments;
	}
}