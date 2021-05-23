package grafos.nodes;

import java.util.ArrayList;
import java.util.List;
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
	
	public List<NodeBase> getAllChildNodes() {
		return this.blocks.stream()
			.map(cnbs -> cnbs.getChildNodes())
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public ControlNodePDG getParent() {
		return parent;
	}
	
	public List<VariableAssignment> getLastAssignments(String variableName)
	{
		return this.getLastAssignments(variableName, false);
	}
	
	public List<VariableAssignment> getLastAssignments(String variableName, boolean onlyCurrentBlockAssignments)
	{
		List<VariableAssignment> variableAssignments = new ArrayList<VariableAssignment>();
		
		if (onlyCurrentBlockAssignments)
		{
			variableAssignments.addAll(this.getCurrentBlock().getLastAssignments(variableName));
		}
		else {			
			for (ControlNodeBlockStatement block : this.blocks)
			{
				variableAssignments.addAll(block.getLastAssignments(variableName));
			}
		}		
						
		return variableAssignments;
	}
}