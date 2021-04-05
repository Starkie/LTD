package iter2rec.transformation.variable;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;

public abstract class Variable
{
	/********************************************************/
	/************************ Static ************************/
	/********************************************************/
	protected static Hashtable<String, Variable> variables = new Hashtable<String, Variable>();

	public static void clearVariables()
	{
		Variable.variables.clear();
	}
	public static Variable createVariable(int modifiers, String name)
	{
		return UsedVariable.createVariable(modifiers, name);
	}
	public static Variable createVariable(int modifiers, Type type, String name, int arrayCount)
	{
		return DeclaredVariable.createVariable(modifiers, type, name, arrayCount);
	}

	/********************************************************/
	/************************ Object ************************/
	/********************************************************/
	protected int modifiers;
	protected Type type;
	protected String name;

	public Variable(int modifiers, Type type, String name, int arrayCount)
	{
		this.modifiers = modifiers;
		this.type = arrayCount == 0 ? type : new ReferenceType(type, arrayCount);
		this.name = name;
	}

	public Type getType()
	{
		return this.type;
	}
	public String getName()
	{
		return this.name;
	}
	public boolean isFinal()
	{
		return ModifierSet.isFinal(this.modifiers);
	}
	public boolean isArray()
	{
		return this.type instanceof ReferenceType && ((ReferenceType)this.type).getArrayCount() > 0;
	}

	public String toString()
	{
		if (this.type == null)
			return this.name;
		return this.type.toString() + " " + this.name;
	}
	public boolean equals(Object object)
	{
		if (!(object instanceof Variable))
			return false;

		final Variable variable = (Variable)object;
		if (this.type != variable.type)
			return false;
		if (!this.name.equals(variable.name))
			return false;
		return true;
	}
	public boolean sameVariable(Variable variable)
	{
		return this.name.equals(variable.name);
	}

	// Expressions
		// Cast
	public CastExpr getCastExpr(Type type)
	{
		return this.getCastExpr(type, new NameExpr(this.name));
	}
	public CastExpr getCastExpr(Expression expr)
	{
		return this.getCastExpr(this.type, expr);
	}
	public CastExpr getCastExpr(Type type, Expression expr)
	{
		final CastExpr castExpr = new CastExpr();
		final Type castType;

		if (type instanceof PrimitiveType)
		{
			String name = ((PrimitiveType)type).getType().name();
			if (name.equals("Int"))
				name += "eger";
			castType = new ClassOrInterfaceType(name);
		}
		else
			castType = type;
		castExpr.setType(castType);
		castExpr.setExpr(expr);

		return castExpr;
	}
		// Assignation
	public AssignExpr getAssignationExpr(Type type, Expression value)
	{
		final AssignExpr assignExpr = new AssignExpr();
		final NameExpr variable = new NameExpr(this.name);

		assignExpr.setTarget(variable);
		assignExpr.setOperator(Operator.assign);
		if (this.type.toString().equals(type.toString()))
			assignExpr.setValue(value);
		else
			assignExpr.setValue(this.getCastExpr(value));

		return assignExpr;
	}
	public AssignExpr getAssignationExpr(Expression value)
	{
		final AssignExpr assignExpr = new AssignExpr();
		final NameExpr variable = new NameExpr(this.name);

		assignExpr.setTarget(variable);
		assignExpr.setOperator(Operator.assign);
		assignExpr.setValue(value);

		return assignExpr;
	}
	public AssignExpr getAssignationExpr(Variable arrayValue, int index)
	{
		if (!arrayValue.isArray())
			throw new RuntimeException("The variable is not an array");

		final Type type = ((ReferenceType)arrayValue.type).getType();
		final ArrayAccessExpr arrayAccess = new ArrayAccessExpr();
		arrayAccess.setName(new NameExpr(arrayValue.name));
		arrayAccess.setIndex(new IntegerLiteralExpr(index + ""));

		return this.getAssignationExpr(type, arrayAccess);
	}
		// Declaration
	public Expression getDeclarationExpr(Variable value)
	{
		if (this.type == value.type)
			return this.getDeclarationExpr(new NameExpr(value.name));
		return this.getDeclarationExpr(value.getCastExpr(this.type));
	}
	public AssignExpr getDeclarationExpr(Type type, Expression value)
	{
		final AssignExpr assignExpr = new AssignExpr();
		final VariableDeclarationExpr variable = this.getDeclarationExpr();

		assignExpr.setTarget(variable);
		assignExpr.setOperator(Operator.assign);
		if (this.type.toString().equals(type.toString()))
			assignExpr.setValue(value);
		else
			assignExpr.setValue(this.getCastExpr(value));

		return assignExpr;
	}
	public AssignExpr getDeclarationExpr(Expression value)
	{
		final AssignExpr assignExpr = new AssignExpr();
		final VariableDeclarationExpr variable = this.getDeclarationExpr();

		assignExpr.setTarget(variable);
		assignExpr.setOperator(Operator.assign);
		assignExpr.setValue(value);

		return assignExpr;
	}
	public AssignExpr getDeclarationExpr(int value)
	{
		final Expression valueExpr = new IntegerLiteralExpr(value + "");

		return this.getDeclarationExpr(valueExpr);
	}
	public VariableDeclarationExpr getDeclarationExpr()
	{
		final VariableDeclarationExpr variable = new VariableDeclarationExpr();
		final List<VariableDeclarator> vars = new LinkedList<VariableDeclarator>();

		variable.setModifiers(this.modifiers);
		variable.setType(this.type);
		variable.setVars(vars);
		vars.add(new VariableDeclarator(new VariableDeclaratorId(this.name)));

		return variable;
	}
		// Parameter
	public Parameter getParameter()
	{
		final Parameter parameter = new Parameter();

		parameter.setType(this.type);
		parameter.setId(new VariableDeclaratorId(this.name));

		return parameter;
	}
}