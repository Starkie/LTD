package grafos.nodes;

/**
 * Represents the type of a control node.
 * The type is needed because each of them requires a different treatment.
 */
public enum ControlNodeType {
	IF,
	WHILE,
	DO,
	FOR
}
