package grafos.nodes;

/**
 * Represents the type of a control node.
 * The type is needed because each of them requires a different treatment.
 */
public enum ControlNodeType {
	METHOD,
	IF,
	WHILE,
	DO,
	FOR,
	FOREACH,
	SWITCH,
	SWITCH_CASE;

	public boolean isLoopType()
	{
		return this == WHILE
			|| this == DO
			|| this == FOR
			|| this == FOREACH;
	}
}
