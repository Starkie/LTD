package grafos.nodes;

/**
 * Base class for implementing Control Nodes. Represents the control instruction from a program.
 * These are special nodes that have an effect on how the graphs generated. 
 */
public abstract class ControlNodeBase {
	// The instruction type of the node.
	protected ControlNodeType type;

	protected ControlNodeBase(ControlNodeType type) {
		this.type = type;
	}

	public ControlNodeType getType() {
		return type;
	}

}