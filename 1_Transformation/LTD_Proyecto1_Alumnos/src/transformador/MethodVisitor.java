package transformador;

import java.util.ArrayList;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

/**
 * Visits the existing method declarations to collect their names.
 */
public class MethodVisitor extends ModifierVisitorAdapter<Object> {
	
	private ArrayList<String> methodNames;

	public MethodVisitor()
	{
		this.methodNames = new ArrayList<String>();
	}
	
	/**
	 * Visits every {@link MethodDeclaration} and registers its name.
	 */
	public Node visit(MethodDeclaration methodDeclaration, Object args)
	{		
		this.methodNames.add(methodDeclaration.getName());

		return methodDeclaration;
	}

	/**
	 * Returns the collection of the names of the found method declarations in the {@link CompilationUnit}.
	 * @return A collection of method names.
	 */
	public ArrayList<String> getMethodNames() {
		return methodNames;
	}
}
