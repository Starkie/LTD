package iter2rec.transformation;

import japa.parser.ast.stmt.*;

import java.util.LinkedList;
import java.util.List;

public class Sentence
{
	public static List<Statement> getAllStatements(Statement statement)
	{
		final List<Statement> statements = new LinkedList<Statement>();

		statements.addAll(Sentence.getStatements(statement));
		statements.addAll(Sentence.getBifurcationStatements(statement));

		return statements;
	}
	public static List<Statement> getStatements(Statement statement)
	{
		final List<Statement> statements = new LinkedList<Statement>();

		if (statement instanceof BlockStmt)
		{
			final List<Statement> blockStmts = ((BlockStmt)statement).getStmts();
			if (blockStmts != null)
				statements.addAll(blockStmts);
		}
		else if (statement instanceof LabeledStmt)
			statements.add(((LabeledStmt)statement).getStmt());
		else if (statement instanceof ForStmt)
			statements.add(((ForStmt)statement).getBody());
		else if (statement instanceof WhileStmt)
			statements.add(((WhileStmt)statement).getBody());
		else if (statement instanceof DoStmt)
			statements.add(((DoStmt)statement).getBody());
		else if (statement instanceof ForeachStmt)
			statements.add(((ForeachStmt)statement).getBody());

		return statements;
	}
	public static List<Statement> getBifurcationStatements(Statement statement)
	{
		final List<Statement> statements = new LinkedList<Statement>();

		if (statement instanceof SwitchEntryStmt) {
			// FIXME: getStmts may be null!!!
			if (((SwitchEntryStmt) statement).getStmts() != null)
				statements.addAll(((SwitchEntryStmt) statement).getStmts());
		} else if (statement instanceof SwitchStmt)
			statements.addAll(((SwitchStmt) statement).getEntries());
		else if (statement instanceof IfStmt)
		{
			final IfStmt ifStmt = (IfStmt)statement;
			statements.add(ifStmt.getThenStmt());
			statements.add(ifStmt.getElseStmt());
		}
		else if (statement instanceof TryStmt)
		{
			final TryStmt tryStmt = (TryStmt)statement;
			statements.add(tryStmt.getTryBlock());
			for (CatchClause catchClause : tryStmt.getCatchs())
				statements.add(catchClause.getCatchBlock());
			statements.add(tryStmt.getFinallyBlock());
		}

		return statements;
	}
}