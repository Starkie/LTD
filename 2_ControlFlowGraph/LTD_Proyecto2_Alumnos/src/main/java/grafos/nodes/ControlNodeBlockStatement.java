package grafos.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.stmt.BlockStmt;

/**
 * Represents a {@link BlockStmt} that is present inside {@link ControlNodePDG}.
 * This parameters where extracted from it because some ControlNodes can have more than 
 * one block. For example: if-else statements can have 2 blocks.
 */
public class ControlNodeBlockStatement {
	private List<ControlNodePDG> childNodes;

	private Map<String, List<VariableAssignment>> assignments;

	public ControlNodeBlockStatement() {
		this.childNodes = new ArrayList<ControlNodePDG>();
		this.assignments = new HashMap<String, List<VariableAssignment>>();
	}

	public List<ControlNodePDG> getChildNodes() {
		return childNodes;
	}

	public Map<String, List<VariableAssignment>> getAssignments() {
		return assignments;
	}
}
