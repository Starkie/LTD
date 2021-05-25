package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * Represents a {@link BlockStmt} that is present inside a {@link ControlNodePDG}.
 * This parameters where extracted from it because some ControlNodes can have more than
 * one block. For example: if-else statements can have 2 blocks.
 */
public class ControlNodeBlockStatement {
	// The child nodes present in the current block.
	private List<NodeBase> childNodes;

	// The parent node of this block.
	private ControlNodePDG parentNode;

	public ControlNodeBlockStatement(ControlNodePDG parentNode) {
		this.parentNode = parentNode;
		this.childNodes = new ArrayList<NodeBase>();
	}

	public List<NodeBase> getChildNodes() {
		return childNodes;
	}

	/**
	 * Registers a variable assignment in the given {@link ControlNodePDG}.
	 * @param assignment The variable assignment to register.
	 */
	public void addAssignment(VariableAssignmentNode assignment)
	{
		this.childNodes.add(assignment);
	}

	/**
	 * Returns all the last assignments of a given variable present in this block.
	 * @param variableName The name of the variable.
	 * @return The list of assignments present in the block, if any.
	 */
	public List<VariableAssignmentNode> getLastAssignments(String variableName)
	{
		return this.getLastAssignments(variableName, false);
	}

	/**
	 * Returns all the last assignments of a given variable present in this block.
	 * @param variableName The name of the variable.
	 * @param onlyCurrentBlockAssignments A flag indicating if only the current branch of the control blocks should be visited.
	 * 	Useful, for example, to avoid adding assignments from the then-branch of an if statement when we are visiting the else branch.
	 * @return The list of assignments present in the block, if any.
	 */
	public List<VariableAssignmentNode> getLastAssignments(String variableName, boolean onlyCurrentBlockAssignments)
	{
		List<VariableAssignmentNode> variableAssignments = new ArrayList<VariableAssignmentNode>();

		for (int i = (this.childNodes.size() - 1); i >= 0; i--)
		{
			if (this.childNodes.get(i) instanceof VariableAssignmentNode)
			{
				VariableAssignmentNode va = (VariableAssignmentNode) this.childNodes.get(i);

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

				// Get all the variable assignments of the control node.
				variableAssignments.addAll(node.getLastAssignments(variableName, onlyCurrentBlockAssignments));
			}
		}

		return variableAssignments;
	}

	/**
	 * Returns the names of all the variables assigned in this block.
	 * @param variableName The name of the variable.
	 * @return The list of assignments present in the block, if any.
	 */
	public List<String> getAssignedVariablesName()
	{
		List<String> variableNames = new ArrayList<>();

		for (NodeBase node : this.childNodes)
		{
			if (node instanceof VariableAssignmentNode)
			{
				VariableAssignmentNode va = (VariableAssignmentNode) node;

				if (!variableNames.contains(va.getVariableName()))
				{
					variableNames.add(va.getVariableName());
				}
			}
			else if (node instanceof ControlNodePDG)
			{
				ControlNodePDG cn = (ControlNodePDG)node;

				variableNames.addAll(cn.getAssignedVariablesNames());
			}
		}

		return variableNames;
	}
}
