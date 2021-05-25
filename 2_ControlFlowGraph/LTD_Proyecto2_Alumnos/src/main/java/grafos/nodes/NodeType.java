package grafos.nodes;

/**
 * Represents the type of a control node.
 * The type is needed because each of them requires a different treatment.
 */
public enum NodeType {
	METHOD,
	IF,
	WHILE,
	DO,
	FOR,
	FOREACH,
	SWITCH,
	SWITCH_CASE,
	VARIABLE_ASSIGNATION;

	/**
	 * Returns a value indicating whether the {@link NodeType} is a loop type.
	 * @return True if it is a loop type, false otherwise.
	 */
	public boolean isLoopType()
	{
		return this == WHILE
			|| this == DO
			|| this == FOR
			|| this == FOREACH;
	}
}
