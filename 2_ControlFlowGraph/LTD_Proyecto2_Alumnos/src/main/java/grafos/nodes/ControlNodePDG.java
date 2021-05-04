package grafos.nodes;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the program dependency graph
 * is generated. 
 */
public class ControlNodePDG extends ControlNodeBase {
	// The node that the instruction represents.
	private String node;

	public ControlNodePDG(ControlNodeType type, String node) {
		super(type);
		
		this.node = node;
	}

	public String getNode() {
		return node;
	}
}
