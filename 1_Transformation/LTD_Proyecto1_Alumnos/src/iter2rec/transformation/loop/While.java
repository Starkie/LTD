package iter2rec.transformation.loop;

import iter2rec.transformation.Method;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;

import java.util.LinkedList;
import java.util.List;

public class While extends Loop
{
	private WhileStmt loop;

	public While(Method method, List<Statement> path, Statement loop)
	{
		super(method, path, loop);

		if (loop instanceof LabeledStmt)
			loop = ((LabeledStmt)loop).getStmt();
		this.loop = (WhileStmt)loop;
	}

	protected List<Expression> getLoopVariables()
	{
		final List<Expression> variables = new LinkedList<Expression>();

		Statement statement = this.loop;
		if (statement instanceof LabeledStmt)
			statement = ((LabeledStmt)statement).getStmt();

		variables.add(((WhileStmt)statement).getCondition());

		return variables;
	}

	protected Expression getCondition()
	{
		return this.loop.getCondition();
	}
	protected List<Statement> getStatements()
	{
		final Statement body = this.loop.getBody();
		final List<Statement> bodyStmts = new LinkedList<Statement>();

		if (body instanceof BlockStmt)
			for (Statement statement : ((BlockStmt)body).getStmts())
				bodyStmts.add(statement);
		else
			bodyStmts.add(body);

		return bodyStmts;
	}

	protected BlockStmt createRecursiveCaller(String typeName)
	{
		// Block
		final BlockStmt blockStmt = new BlockStmt();
		final List<Statement> blockStmts = new LinkedList<Statement>();
		final IfStmt ifStmt = new IfStmt();
		final BlockStmt thenStmt = super.createRecursiveCaller(typeName);

		blockStmt.setStmts(blockStmts);
		blockStmts.add(ifStmt);
		ifStmt.setCondition(this.getCondition());
		ifStmt.setThenStmt(thenStmt);

		return blockStmt;
	}
}