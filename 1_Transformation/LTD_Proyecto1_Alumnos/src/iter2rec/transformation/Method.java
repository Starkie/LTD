package iter2rec.transformation;

import iter2rec.transformation.loop.Loop;

import java.util.LinkedList;
import java.util.List;


import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.stmt.LabeledStmt;
import japa.parser.ast.stmt.Statement;

public class Method
{
	private CompilationUnit cu;
	private TypeDeclaration type;
	private MethodDeclaration method;
	private int methodsCreated = 0;

	public Method(CompilationUnit cu, TypeDeclaration type, MethodDeclaration method)
	{
		this.cu = cu;
		this.type = type;
		this.method = method;
	}

	public CompilationUnit getCompilationUnit()
	{
		return this.cu;
	}
	public TypeDeclaration getType()
	{
		return this.type;
	}
	public MethodDeclaration getMethod()
	{
		return this.method;
	}

	public List<Loop> lookForLoops()
	{
		int numHijoAVisitar = 0;
		final LinkedList<Statement> path = new LinkedList<Statement>();
		final List<Integer> bifurcaciones = new LinkedList<Integer>();

		final List<Loop> loops = new LinkedList<Loop>();
		Statement root = this.method.getBody();
		Statement statement = root;

		do
		{
			final List<Statement> statements = Sentence.getAllStatements(statement);
			int numeroDeHijos = statements.size();

			// Evaluar nodo
			if (numHijoAVisitar == 0)
				if (Loop.isALoop(statement))
				{
					final LinkedList<Statement> clonedPath = new LinkedList<Statement>();
					for (Statement pathStatement : path)
						clonedPath.add(pathStatement);

					loops.add(Loop.createLoop(this, clonedPath, statement));
					numHijoAVisitar = numeroDeHijos;
				}

			if (numHijoAVisitar == numeroDeHijos)
			{
				// Ir al padre
				// Si es la raiz terminar directamente
				if (statement == root)
					break;
				else
				{
					// Si no, volver al padre
					statement = path.removeLast();
					numHijoAVisitar = bifurcaciones.remove(bifurcaciones.size() - 1);
				}
			}
			else
			{
				// Ir al hijo
				path.add(statement);
				statement = statements.get(numHijoAVisitar);
				bifurcaciones.add(numHijoAVisitar + 1);
				numHijoAVisitar = 0;
			}
		}
		while (true);

		return loops;
	}
	public String getLoopName(Statement loop)
	{
		String name = this.method.getName() + "_";

		if (loop instanceof LabeledStmt)
			name += ((LabeledStmt)loop).getLabel();
		else
			name += "loop" + ++this.methodsCreated;

		return name;
	}
}