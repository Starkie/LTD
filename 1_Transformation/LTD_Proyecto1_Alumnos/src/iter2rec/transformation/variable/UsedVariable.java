package iter2rec.transformation.variable;

public class UsedVariable extends Variable
{
	/********************************************************/
	/************************ Static ************************/
	/********************************************************/
	public static Variable createVariable(int modifiers, String name)
	{
		final Variable variable;
		final String id = name;

		if (Variable.variables.containsKey(id))
			variable = Variable.variables.get(id);
		else
		{
			variable = new UsedVariable(modifiers, name);
			Variable.variables.put(id, variable);
		}

		return variable;
	}

	/********************************************************/
	/************************ Object ************************/
	/********************************************************/
	public UsedVariable(int modifiers, String name)
	{
		super(modifiers, null, name, 0);
	}
}