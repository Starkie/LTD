package iter2rec.transformation.variable;

import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;

import java.util.LinkedList;
import java.util.List;

public class LoopVariables
{
	final List<Variable> variables;
	final List<Variable> returnVariables;

	public LoopVariables(Variable... variables)
	{
		this.variables = new LinkedList<Variable>();
		for (Variable variable : variables)
			this.variables.add(variable);

		this.returnVariables = new LinkedList<Variable>();
		for (Variable variable : variables)
			if (!variable.isFinal())
				this.returnVariables.add(variable);
	}
	public LoopVariables(List<Variable> variables)
	{
		this.variables = new LinkedList<Variable>();
		for (Variable variable : variables)
			this.variables.add(variable);

		this.returnVariables = new LinkedList<Variable>();
		for (Variable variable : variables)
			if (!variable.isFinal())
				this.returnVariables.add(variable);
	}

	public void add(Variable variable)
	{
		this.variables.add(variable);
		if (!variable.isFinal())
			this.returnVariables.add(variable);
	}
	public void remove(Variable variable)
	{
		this.variables.remove(variable);
		if (!variable.isFinal())
			this.returnVariables.remove(variable);
	}

	public Variable getResult()
	{
		if (this.returnVariables.size() > 1)
			return Variable.createVariable(0, this.getReturnType(), "result", 0);
		if (this.returnVariables.size() == 1)
			return this.returnVariables.get(0);
		return null;
	}
	public boolean hasReturnVariable()
	{
		return this.returnVariables.size() > 0;
	}
	public boolean hasBridgeVariable()
	{
		return this.returnVariables.size() > 1;
	}

	public List<Expression> getArgs()
	{
		final List<Expression> args = new LinkedList<Expression>();

		for (Variable variable : this.variables)
			args.add(new NameExpr(variable.getName()));

		return args;
	}
	public List<Type> getReturnTypes()
	{
		List<Type> types = new LinkedList<Type>();
		for (Variable returnVariable : this.returnVariables)
		{
			types.add(returnVariable.getType());
		}
		return types;
	}
	public List<String> getReturnNames()
	{
		List<String> names = new LinkedList<String>();
		for (Variable returnVariable : this.returnVariables)
		{
			names.add(returnVariable.getName());
		}
		return names;
	}
		
	public List<Statement> getCastings(Variable caster)
	{
		final List<Statement> statements = new LinkedList<Statement>();

		for (Variable variable : this.returnVariables)
			statements.add(new ExpressionStmt(variable.getAssignationExpr(caster, this.variables.indexOf(variable))));

		return statements;
	}
	public List<Parameter> getParameters()
	{
		final List<Parameter> parameters = new LinkedList<Parameter>();

		for (Variable variable : this.variables)
			parameters.add(variable.getParameter());

		return parameters;
	}

	public Type getReturnType()
	{
		final ClassOrInterfaceType object = new ClassOrInterfaceType("Object");
		if (this.returnVariables.size() == 0)
			return object;
		final Type initialType = this.returnVariables.get(0).getType();
		if (this.returnVariables.size() == 1)
			return initialType;

		final ReferenceType returnType = new ReferenceType(null, 1);
		for (Variable variable : this.returnVariables)
			if (!variable.getType().toString().equals(initialType.toString()))
			{
				returnType.setType(object);
				return returnType;
			}
		returnType.setType(initialType);
		return returnType;
	}
	public Expression getReturnExpr()
	{
		if (this.returnVariables.size() == 0)
			return new NullLiteralExpr();
		if (this.returnVariables.size() == 1)
			return this.getReturnVariables().get(0);
		return this.getReturnMultiVariableExpr();
	}
	protected ArrayCreationExpr getReturnMultiVariableExpr()
	{
		final ArrayCreationExpr returnObject = new ArrayCreationExpr();

		returnObject.setType(this.getReturnType());
		returnObject.setInitializer(new ArrayInitializerExpr(this.getReturnVariables()));

		return returnObject;
	}
	protected List<Expression> getReturnVariables()
	{
		final List<Expression> args = new LinkedList<Expression>();

		for (Variable variable : this.returnVariables)
			args.add(new NameExpr(variable.getName()));

		return args;
	}
}