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

	private ControlNodePDG parent;

	public ControlNodePDG(ControlNodeType type, String node, ControlNodePDG parent) {
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
	
	public List<ControlNodePDG> getAllChildNodes() {
		return this.blocks.stream()
			.map(cnbs -> cnbs.getChildNodes())
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	ControlNodePDG getParent() {
		return parent;
	}
}