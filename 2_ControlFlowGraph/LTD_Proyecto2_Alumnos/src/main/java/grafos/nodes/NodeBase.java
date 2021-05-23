package grafos.nodes;

/**
 * Base class for implementing Control Nodes. Represents the control instruction from a program.
 * These are special nodes that have an effect on how the graphs generated. 
 */
public abstract class NodeBase {
	// The instruction type of the node.
	protected NodeType type;

	protected NodeBase(NodeType type) {
		this.type = type;
	}

	public NodeType getType() {
		return type;
	}

}