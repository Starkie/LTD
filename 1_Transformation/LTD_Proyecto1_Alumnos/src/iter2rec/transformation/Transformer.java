package iter2rec.transformation;

import iter2rec.transformation.loop.Loop;

import java.util.LinkedList;
import java.util.List;


import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;

public class Transformer
{
	private CompilationUnit cu;

	public Transformer(CompilationUnit cu)
	{
		this.cu = cu;
	}

	public int transform()
	{
		final List<Method> methods = this.lookForMethods();
		final List<Loop> loops = new LinkedList<Loop>();
		int loopsTransformed = 0;

		while (methods.size() > 0)
		{
			for (Method method : methods)
				loops.addAll(method.lookForLoops());
			methods.clear();
			for (Loop loop : loops)
				methods.addAll(loop.transformLoop());
			loopsTransformed += loops.size();
			loops.clear();
		}

		return loopsTransformed;
	}
	private List<Method> lookForMethods()
	{
		final List<Method> methodDeclarations = new LinkedList<Method>();
		final List<TypeDeclaration> types = this.cu.getTypes();

		for (TypeDeclaration type : types)
			for (BodyDeclaration member : type.getMembers())
				if (member instanceof MethodDeclaration)
					methodDeclarations.add(new Method(this.cu, type, (MethodDeclaration)member));

		return methodDeclarations;
	}
}