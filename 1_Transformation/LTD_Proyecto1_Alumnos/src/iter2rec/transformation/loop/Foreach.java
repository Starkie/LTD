package iter2rec.transformation.loop;

import iter2rec.transformation.Method;
import iter2rec.transformation.variable.DeclaredVariable;
import iter2rec.transformation.variable.Variable;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.*;

import java.util.LinkedList;
import java.util.List;

public class Foreach extends Loop
{
	protected ForeachStmt loop;
	protected Variable variable;
	protected Variable iterableVariable;
	protected enum Mode{None, Iterator, Array}
	protected Mode mode = Mode.None;

	// Iterator
	private Variable iteratorListVariable;
	private MethodCallExpr iteratorListCondition;
	private MethodCallExpr iteratorListNext;

	// Array
	private Variable indexVariable;
	private Variable iteratorArrayVariable;
	private BinaryExpr iteratorArrayCondition;
	private ArrayAccessExpr iteratorArrayNext;
	private UnaryExpr indexIncrement;

	public Foreach(Method method, List<Statement> path, Statement loop)
	{
		super(method, path, loop);

		if (loop instanceof LabeledStmt)
			loop = ((LabeledStmt)loop).getStmt();
		this.loop = (ForeachStmt)loop;
	}

	protected List<ImportDeclaration> getImports()
	{
		final List<ImportDeclaration> imports = new LinkedList<ImportDeclaration>();
		final ImportDeclaration importDeclaration = new ImportDeclaration();

		importDeclaration.setName(new NameExpr("java.util.Iterator"));
		imports.add(importDeclaration);

		return imports;
	}
	protected void setLoopVariables(List<Variable> loopVariables)
	{
		super.setLoopVariables(loopVariables);

		final VariableDeclarationExpr variableDeclarationExpr = this.loop.getVariable();
		final int modifiersVariable = variableDeclarationExpr.getModifiers();
		final Type typeVariable = variableDeclarationExpr.getType();
		final String nameVariable = variableDeclarationExpr.getVars().get(0).getId().getName();
		this.variable = Variable.createVariable(modifiersVariable, typeVariable, nameVariable, 0);

		final String indexName = "index";
		final String iterableName = "iterable";
		final String iteratorName = "iterator";
		final NameExpr iterator = new NameExpr(iteratorName);
		int finalModifier = ModifierSet.addModifier(0, ModifierSet.FINAL);

		// Iterable variable (Object iterable)
		final ClassOrInterfaceType objectType = new ClassOrInterfaceType("Object");
		this.iterableVariable = Variable.createVariable(finalModifier, objectType, iterableName, 0);

		// Index variable (int index)
		final PrimitiveType integerType = new PrimitiveType(PrimitiveType.Primitive.Int);
		this.indexVariable = Variable.createVariable(finalModifier, integerType, indexName, 0);

		// Iterator variable (Iterator<Type> iterator)
		final ClassOrInterfaceType classType = new ClassOrInterfaceType();
		final List<Type> typeArgs = new LinkedList<Type>();
		classType.setName("Iterator");
		classType.setTypeArgs(typeArgs);
		typeArgs.add(this.loop.getVariable().getType());
		this.iteratorListVariable = new DeclaredVariable(finalModifier, classType, iteratorName, 0);

		// Array variable (Type[] iterator)
		final ReferenceType arrayType = new ReferenceType();
		arrayType.setArrayCount(1);
		arrayType.setType(this.variable.getType());
		this.iteratorArrayVariable = new DeclaredVariable(finalModifier, arrayType, iteratorName, 0);

		// Iterable condition (iterator.hasNext())
		this.iteratorListCondition = new MethodCallExpr();
		this.iteratorListCondition.setScope(iterator);
		this.iteratorListCondition.setName("hasNext");

		// Array condition (index < iterator.lenght)
		final FieldAccessExpr fieldAccess = new FieldAccessExpr();
		this.iteratorArrayCondition = new BinaryExpr();
		this.iteratorArrayCondition.setLeft(new NameExpr(indexName));
		this.iteratorArrayCondition.setOperator(BinaryExpr.Operator.less);
		this.iteratorArrayCondition.setRight(fieldAccess);
		fieldAccess.setScope(iterator);
		fieldAccess.setField("length");

		// Iterable next (iterator.next())
		this.iteratorListNext = new MethodCallExpr();
		this.iteratorListNext.setScope(iterator);
		this.iteratorListNext.setName("next");

		// Array next (iterator[index])
		this.iteratorArrayNext = new ArrayAccessExpr();
		this.iteratorArrayNext.setName(iterator);
		this.iteratorArrayNext.setIndex(new NameExpr(indexName));

		// Index increment (index++)
		final UnaryExpr indexExpr = new UnaryExpr();
		indexExpr.setExpr(new NameExpr(this.indexVariable.getName()));
		indexExpr.setOperator(UnaryExpr.Operator.posIncrement);
		this.indexIncrement = indexExpr;
	}

	protected Expression getCondition()
	{
		if (this.mode == Mode.Iterator)
			return this.iteratorListCondition;
		else if (this.mode == Mode.Array)
			return this.iteratorArrayCondition;
		throw new RuntimeException("Not contempled yet");
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
		blockStmt.setStmts(blockStmts);

		// Object iterable = iterableVar;
		final ExpressionStmt iterableStmt = new ExpressionStmt();

		blockStmts.add(iterableStmt);
		iterableStmt.setExpression(this.iterableVariable.getDeclarationExpr(this.loop.getIterable()));

		// if (iterable instanceof Iterable)
		final IfStmt typeComprobationStmt = new IfStmt();
		final InstanceOfExpr typeComprobation = new InstanceOfExpr();

		blockStmts.add(typeComprobationStmt);
		typeComprobationStmt.setCondition(typeComprobation);
		typeComprobation.setExpr(new NameExpr(this.iterableVariable.getName()));
		typeComprobation.setType(new ClassOrInterfaceType("Iterable"));
		typeComprobationStmt.setThenStmt(this.createIteratorRecursiveCaller(typeName));
		typeComprobationStmt.setElseStmt(this.createArrayRecursiveCaller(typeName));

		return blockStmt;
	}
	protected BlockStmt createIteratorRecursiveCaller(String typeName)
	{
		final BlockStmt blockStmt = new BlockStmt();
		final List<Statement> blockStmts = new LinkedList<Statement>();
		blockStmt.setStmts(blockStmts);

		// Iterator<Type> iterator = ((Iterable)iterable).iterator();
		final MethodCallExpr iteratorInvocation = new MethodCallExpr();
		final ClassOrInterfaceType iterable = new ClassOrInterfaceType();
		final List<Type> listIteratorTypeArgs = new LinkedList<Type>();

		// TODO @SuppressWarnings("unchecked")
		blockStmts.add(new ExpressionStmt(this.iteratorListVariable.getDeclarationExpr(iteratorInvocation)));
		iteratorInvocation.setScope(new EnclosedExpr(this.iterableVariable.getCastExpr(iterable)));
		iteratorInvocation.setName("iterator");
		iterable.setName("Iterable");
		iterable.setTypeArgs(listIteratorTypeArgs);
		listIteratorTypeArgs.add(this.variable.getType());

		// If
		final IfStmt ifStmt = new IfStmt();
		blockStmts.add(ifStmt);
		ifStmt.setCondition(this.iteratorListCondition);

		// Include foreach variables
		this.loopVariables.add(this.iteratorListVariable);
		ifStmt.setThenStmt(super.createRecursiveCaller(typeName));
		this.loopVariables.remove(this.iteratorListVariable);

		return blockStmt;
	}
	protected BlockStmt createArrayRecursiveCaller(String typeName)
	{
		final BlockStmt blockStmt = new BlockStmt();
		final List<Statement> blockStmts = new LinkedList<Statement>();
		blockStmt.setStmts(blockStmts);

		// final int index = 0
		// final Type[] iterator = (Type[])iterable;
		blockStmts.add(new ExpressionStmt(this.indexVariable.getDeclarationExpr(0)));
		blockStmts.add(new ExpressionStmt(this.iteratorArrayVariable.getDeclarationExpr(this.iterableVariable)));

		// If
		final IfStmt ifStmt = new IfStmt();
		blockStmts.add(ifStmt);
		ifStmt.setCondition(this.iteratorArrayCondition);

			// Include foreach variables
		this.loopVariables.add(this.iteratorArrayVariable);
		this.loopVariables.add(this.indexVariable);
		ifStmt.setThenStmt(super.createRecursiveCaller(typeName));
		this.loopVariables.remove(this.iteratorArrayVariable);
		this.loopVariables.remove(this.indexVariable);

		return blockStmt;
	}
	protected List<MethodDeclaration> createRecursiveMethods(String typeName)
	{
		final List<MethodDeclaration> methods = new LinkedList<MethodDeclaration>();

		this.mode = Mode.Iterator;
		methods.add(this.createIteratorRecursiveMethod(typeName));
		this.mode = Mode.Array;
		methods.add(this.createArrayRecursiveMethod(typeName));
		this.mode = Mode.None;

		return methods;
	}
	protected MethodDeclaration createIteratorRecursiveMethod(String typeName)
	{
		// Method
			// Include foreach variables
		this.loopVariables.add(this.iteratorListVariable);
		final MethodDeclaration method = super.createRecursiveMethod(typeName);
		this.loopVariables.remove(this.iteratorListVariable);

		// Type var = iterator.next();
		final BlockStmt blockStmt = method.getBody();
		final List<Statement> blockStmts = blockStmt.getStmts();
		blockStmts.add(0, new ExpressionStmt(this.variable.getDeclarationExpr(this.iteratorListNext)));

		return method;
	}
	protected MethodDeclaration createArrayRecursiveMethod(String typeName)
	{
		// Method
			// Include foreach variables
		this.loopVariables.add(this.iteratorArrayVariable);
		this.loopVariables.add(this.indexVariable);
		final MethodDeclaration method = super.createRecursiveMethod(typeName);
		this.loopVariables.remove(this.iteratorArrayVariable);
		this.loopVariables.remove(this.indexVariable);

		// Type var = vars[index];
		// index++;
		final BlockStmt blockStmt = method.getBody();
		final List<Statement> blockStmts = blockStmt.getStmts();
		blockStmts.add(0, new ExpressionStmt(this.variable.getDeclarationExpr(this.iteratorArrayNext)));
		blockStmts.add(blockStmts.size()  - 2, new ExpressionStmt(this.indexIncrement));

		return method;
	}
}