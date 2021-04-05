package iter2rec.transformation.loop;

import iter2rec.transformation.Method;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;

import java.util.LinkedList;
import java.util.List;

public class For extends Loop
{
	private ForStmt loop;

	public For(Method method, List<Statement> path, Statement loop)
	{
		super(method, path, loop);

		if (loop instanceof LabeledStmt)
			loop = ((LabeledStmt)loop).getStmt();
		this.loop = (ForStmt)loop;
	}

	protected List<Expression> getLoopDeclaredVariables()
	{
		final List<Expression> init = this.loop.getInit();

		if (init != null)
			return init;
		return new LinkedList<Expression>();
	}
	protected List<Expression> getLoopVariables()
	{
		final List<Expression> variables = new LinkedList<Expression>();

		variables.add(this.loop.getCompare());
		final List<Expression> forVariables = this.loop.getUpdate();
		if (forVariables != null)
			variables.addAll(forVariables);

		return variables;
	}

	protected Expression getCondition()
	{
		return this.loop.getCompare();
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
		final List<Expression> init = this.loop.getInit();

		// For Init
		blockStmt.setStmts(blockStmts);
		if (init != null)
			for (Expression expr : init)
				blockStmts.add(init.indexOf(expr), new ExpressionStmt(expr));

		// If
		final IfStmt ifStmt = new IfStmt();
		final BlockStmt thenStmt = super.createRecursiveCaller(typeName);

		blockStmts.add(ifStmt);
		ifStmt.setCondition(this.getCondition());
		ifStmt.setThenStmt(thenStmt);

		return blockStmt;
	}
	protected MethodDeclaration createRecursiveMethod(String typeName)
	{
		// Method
		final MethodDeclaration method = super.createRecursiveMethod(typeName);
		final BlockStmt blockStmt = method.getBody();
		final List<Statement> blockStmts = blockStmt.getStmts();
		final List<Expression> update = this.loop.getUpdate();
		final int position = blockStmts.size()  - 2;

		// For Update
		if (update != null)
			for (Expression expr : update)
				blockStmts.add(position + update.indexOf(expr), new ExpressionStmt(expr));

		return method;
	}
}