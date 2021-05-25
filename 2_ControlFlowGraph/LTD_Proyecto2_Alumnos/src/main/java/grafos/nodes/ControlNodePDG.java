package grafos.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the program dependency graph
 * is generated.
 */
public class ControlNodePDG extends NodeBase {
	// The node that the instruction represents.
	private String node;

	private List<ControlNodeBlockStatement> blocks;

	private ControlNodePDG parent;

	public ControlNodePDG(NodeType type, String node, ControlNodePDG parent) {
		super(type);

		this.node = node;

		this.parent = parent;

		this.blocks = new ArrayList<ControlNodeBlockStatement>();

		// Create the default block.
		this.blocks.add(new ControlNodeBlockStatement(this));
	}

	public String getNode() {
		return node;
	}

	public void addChildNode(NodeBase node)
	{
		// Add the node to the current block.
		this.getCurrentBlock().getChildNodes().add(node);
	}

	public List<ControlNodeBlockStatement> getBlocks() {
		return blocks;
	}

	public void startNewBlock()
	{
		this.blocks.add(new ControlNodeBlockStatement(this));
	}

	public ControlNodeBlockStatement getCurrentBlock() {
		// Get the last block in the list.
		return this.blocks.get(this.blocks.size() - 1);
	}

	public ControlNodePDG getParent() {
		return parent;
	}

	/**
	 * Returns all the last assignments all the variables present in this control node.
	 * @param variableName The name of the variable.
	 * @return The list of assignments present in the control node, if any.
	 */
	public Map<String, List<VariableAssignmentNode>> getLastAssignments()
	{
		Map<String, List<VariableAssignmentNode>> variableAssignments = new HashMap<String, List<VariableAssignmentNode>>();

		List<String> variableNames = this.getAssignedVariablesNames();

		for (String variable : variableNames)
		{
			variableAssignments.put(variable, this.getLastAssignments(variable));
		}

		return variableAssignments;
	}

	/**
	 * Returns the names of all the variables assigned in this control node .
	 * @param variableName The name of the variable.
	 * @return The list of assignments present in the node, if any.
	 */
	public List<String> getAssignedVariablesNames() {
		List<String> variableNames = new ArrayList<String>();

		for (ControlNodeBlockStatement block : this.blocks)
		{
			variableNames.addAll(block.getAssignedVariablesName());
		}

		return variableNames;
	}

	/**
	 * Returns all the last assignments of the given variable that are present in this control node.
	 * @param variableName The name of the variable.
	 * @return The list of assignments present in the control node, if any.
	 */
	public List<VariableAssignmentNode> getLastAssignments(String variableName)
	{
		return this.getLastAssignments(variableName, false);
	}

	/**
	 * Returns all the last assignments of the given variable that are present in this control node.
	 * @param variableName The name of the variable.
	 * @param onlyCurrentBlockAssignments A flag indicating if only the current branch of the control blocks should be visited.
	 * 	Useful, for example, to avoid adding assignments from the then-branch of an if statement when we are visiting the else branch.
	 * @return The list of assignments present in the control node, if any.
	 */
	public List<VariableAssignmentNode> getLastAssignments(String variableName, boolean onlyCurrentBlockAssignments)
	{
		List<VariableAssignmentNode> variableAssignments = new ArrayList<VariableAssignmentNode>();

		if (onlyCurrentBlockAssignments)
		{
			variableAssignments.addAll(this.getCurrentBlock().getLastAssignments(variableName, onlyCurrentBlockAssignments));
		}
		else {
			for (ControlNodeBlockStatement block : this.blocks)
			{
				variableAssignments.addAll(block.getLastAssignments(variableName, onlyCurrentBlockAssignments));
			}
		}

		return variableAssignments;
	}
}
