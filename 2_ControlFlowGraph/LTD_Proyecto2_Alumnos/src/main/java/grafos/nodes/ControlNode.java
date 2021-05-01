package grafos.nodes;

/**
 * Represents a control instruction from a program.
 * These are special nodes that have an effect on how the control graph
 * is generated. 
 */
public class ControlNode {
	private ControlNodeType type;
	private String nodeInstruction;

	protected ControlNode(ControlNodeType type, String nodeInstruction) {
		this.type = type;
		this.nodeInstruction = nodeInstruction;
	}

	public ControlNodeType getType() {
		return type;
	}

	public String getNodeInstruction() {
		return nodeInstruction;
	}
}
