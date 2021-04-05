package transformador.util;

import japa.parser.ast.stmt.*;

public class Util {
	/** @see #isEnding(Statement, boolean) */
	public static boolean isEnding(Statement statement) {
		return isEnding(statement, false);
	}

	/**
	 * Checks if a statement cannot have another statement after it.
	 * Not complete (while(true){return;} is a last statement).
	 * @param statement Any statement (normally the last one in a block)
	 * @return Whether more statements can be added after this one without triggering a compile error.
	 */
	// TODO: consider loops that end in endings and have a condition which evaluates to true
	public static boolean isEnding(Statement statement, boolean inSwitch) {
		if (statement == null) return false;
		if (statement instanceof BreakStmt) return !inSwitch || ((BreakStmt) statement).getId() != null;
		if (statement instanceof ContinueStmt) return true;
		if (statement instanceof ThrowStmt) return true;
		if (statement instanceof ReturnStmt) return true;
		if (statement instanceof BlockStmt) {
			BlockStmt blockStmt = (BlockStmt) statement;
			if (blockStmt.getStmts() != null && blockStmt.getStmts().size() > 0)
				return isEnding(blockStmt.getStmts().get(blockStmt.getStmts().size() - 1));
			else
				return false;
		}
		if (statement instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) statement;
			if (ifStmt.getThenStmt() != null && ifStmt.getElseStmt() != null)
				return isEnding(ifStmt.getThenStmt()) && isEnding(ifStmt.getElseStmt());
			// TODO: consider then the condition is constant and the corresponding branch ends
		}
		if (statement instanceof SwitchStmt) {
			if (((SwitchStmt) statement).getEntries() != null)
			for (SwitchEntryStmt entry : ((SwitchStmt) statement).getEntries())
				if (!isEnding(entry.getStmts().get(entry.getStmts().size() - 1), true))
					return false;
			return true;
		}
		if (statement instanceof TryStmt) {
			TryStmt tryStmt = (TryStmt) statement;
			if (isEnding(tryStmt.getFinallyBlock()))
				return true;
			if (!isEnding(tryStmt.getTryBlock()))
				return false;
			if (tryStmt.getCatchs() != null)
				for (CatchClause cc : tryStmt.getCatchs())
					if (!isEnding(cc.getCatchBlock()))
						return false;
			return true;
		}
		return false;
	}
}
