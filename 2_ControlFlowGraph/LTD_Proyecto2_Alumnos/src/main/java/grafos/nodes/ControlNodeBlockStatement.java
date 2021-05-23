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
	public void addAssignment(VariableAssignment assignment)
	{
		this.childNodes.add(assignment);
	}

	public List<VariableAssignment> getLastAssignments(String variableName)
	{
		return this.getLastAssignments(variableName, false);
	}

	public List<VariableAssignment> getLastAssignments(String variableName, boolean onlyCurrentBlockAssignments)
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

				variableAssignments.addAll(node.getLastAssignments(variableName, onlyCurrentBlockAssignments));
			}
		}

		return variableAssignments;
	}

	public List<String> getAssignedVariablesName()
	{
		List<String> variableNames = new ArrayList<>();

		for (NodeBase node : this.childNodes)
		{
			if (node instanceof VariableAssignment)
			{
				VariableAssignment va = (VariableAssignment) node;

				if (!variableNames.contains(va.getVariableName()))
				{
					variableNames.add(va.getVariableName());
				}
			}
			else if (node instanceof ControlNodePDG)
			{
				ControlNodePDG cn = (ControlNodePDG)node;

				variableNames.addAll(cn.getAssignedVariablesName());
			}
		}

		return variableNames;
	}
}
