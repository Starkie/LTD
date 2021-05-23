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
	private List<ControlNodePDG> childNodes;

	private Map<String, Map<ControlNodePDG, List<VariableAssignment>>> assignments;

	private ControlNodePDG parentNode;

	public ControlNodeBlockStatement(ControlNodePDG parentNode) {
		this.parentNode = parentNode;
		this.childNodes = new ArrayList<ControlNodePDG>();
		this.assignments = new HashMap<String, Map<ControlNodePDG, List<VariableAssignment>>>();
	}

	public List<ControlNodePDG> getChildNodes() {
		return childNodes;
	}
	
	/**
	 * Registers a variable assignment in the given {@link ControlNodePDG}.
	 * @param assignment The variable assignment to register.
	 * @param controlNode The control node where the variable assignment occured.
	 */
	public void addAssignment(VariableAssignment assignment, ControlNodePDG controlNode)
	{		
		String variableName = assignment.getVariableName();
		
		if (!getAssignments().containsKey(variableName))
		{
			this.getAssignments().put(variableName, new HashMap<ControlNodePDG, List<VariableAssignment>>());
		}
		
		Map<ControlNodePDG, List<VariableAssignment>> variableAssignments = this.getAssignments().get(variableName); 
		
		// An assignment in the parent node overrides all the assignments on this block.
		if (controlNode.equals(this.parentNode))
		{
			variableAssignments.clear();
		}
		
		if (!variableAssignments.containsKey(controlNode))
		{
			variableAssignments.put(controlNode, new ArrayList<VariableAssignment>());
		}

		// Add at the end of the list the new variable assignment. The last assignment will be the only valid one.
		// The others are kept for reference in case they are needed for loops.
		variableAssignments.get(controlNode).add(assignment);
		
		// Propagate the assignment to the parent nodes.
		ControlNodePDG parentNode = this.parentNode.getParent();
		
		if (parentNode != null)
		{
			parentNode.getCurrentBlock().addAssignment(assignment, controlNode);
		}
	}

	public Map<String, Map<ControlNodePDG, List<VariableAssignment>>> getAssignments() {
		return assignments;
	}
}