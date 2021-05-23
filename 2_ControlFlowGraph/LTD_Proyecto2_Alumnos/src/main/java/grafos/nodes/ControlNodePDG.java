package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the program dependency graph
 * is generated.
 */
public class ControlNodePDG extends ControlNodeBase {
	// The node that the instruction represents.
	private String node;
	
	private List<ControlNodeBlockStatement> blocks;

	public ControlNodePDG(ControlNodeType type, String node) {
		super(type);

		this.node = node;
		
		this.blocks = new ArrayList<ControlNodeBlockStatement>();
		
		// Create the default block.
		this.blocks.add(new ControlNodeBlockStatement());
	}

	public String getNode() {
		return node;
	}

	public List<ControlNodeBlockStatement> getBlocks() {
		return blocks;
	}
	
	public void startNewBlock()
	{
		this.blocks.add(new ControlNodeBlockStatement());
	}
	
	public ControlNodeBlockStatement getCurrentBlock() {
		// Get the last block in the list.
		return this.blocks.get(this.blocks.size() - 1);
	}
	
	public List<ControlNodePDG> getAllChildNodes() {
		return this.blocks.stream()
			.map(cnbs -> cnbs.getChildNodes())
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}
	
	/**
	 * Returns the variable assignments inside the given control node or its children.
	 * @param controlNode The control node.
	 * @return The variable assignments that were performed in the control node.
	 */
	public Map<String, List<VariableAssignment>> getAllAssignments() {
		return this.getChildNodesAssignments(this, new HashMap<String, List<VariableAssignment>>());
	}
	
	/**
	 * Returns the variable assignments inside the given control node or its children.
	 * @param controlNode The control node.
	 * @return The variable assignments that were performed in the control node.
	 */
	private Map<String, List<VariableAssignment>> getChildNodesAssignments(ControlNodePDG controlNode, Map<String, List<VariableAssignment>> variableAssignments) {
		for (ControlNodePDG node : controlNode.getAllChildNodes())
		{
			getChildNodesAssignments(node, variableAssignments);
		}
		
		for (ControlNodeBlockStatement block : controlNode.getBlocks())
		{
			Map<String, List<VariableAssignment>> nodeVariableAssignments = block.getAssignments();
			
			for (String variable : nodeVariableAssignments.keySet())
			{
				if (variableAssignments.containsKey(variable))
				{
					variableAssignments.get(variable).addAll(nodeVariableAssignments.get(variable));
				}
				else
				{
					variableAssignments.put(variable, nodeVariableAssignments.get(variable));
				}
				
			}
		}

		return variableAssignments;
	}

}
