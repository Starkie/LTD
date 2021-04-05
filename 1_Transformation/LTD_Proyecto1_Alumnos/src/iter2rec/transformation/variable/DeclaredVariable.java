package iter2rec.transformation.variable;

import japa.parser.ast.type.Type;

public class DeclaredVariable extends Variable
{
	/********************************************************/
	/************************ Static ************************/
	/********************************************************/
	public static Variable createVariable(int modifiers, Type type, String name, int arrayCount)
	{
		final Variable variable;
		final String id = type.toString() + " " + name;

		if (Variable.variables.containsKey(id))
			variable = Variable.variables.get(id);
		else
		{
			variable = new DeclaredVariable(modifiers, type, name, arrayCount);
			Variable.variables.put(id, variable);
		}

		return variable;
	}

	/********************************************************/
	/************************ Object ************************/
	/********************************************************/
	public DeclaredVariable(int modifiers, Type type, String name, int arrayCount)
	{
		super(modifiers, type, name, arrayCount);
	}
}