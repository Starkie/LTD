package grafos.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the control graph
 * is generated. 
 */
public class ControlNode {
	// The type of the node.
	private ControlNodeType type;
	
	// The nodes that must be used when exiting the control node.
	private List<String> exitNodes;

	/**
	 * Creates a new instance of a {@link ControlNode}.
	 * @param type The type of the control node.
	 * @param exitNode The node that must be used when exiting the control node.
	 */
	public ControlNode(ControlNodeType type, String exitNode) {
		this.type = type;
		this.exitNodes = new ArrayList<String>();
		
		if (exitNode != null)
		{
			this.exitNodes.add(exitNode);			
		}
	}

	public ControlNodeType getType() {
		return type;
	}

	public List<String> getExitNodes() {
		return this.exitNodes;
	}
}
