package transformador;

import iter2rec.transformation.loop.Do;
import iter2rec.transformation.loop.For;
import iter2rec.transformation.loop.Foreach;
import iter2rec.transformation.loop.Loop;
import iter2rec.transformation.loop.While;
import iter2rec.transformation.variable.LoopVariables;
import iter2rec.transformation.variable.Variable;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.BreakStmt;
import japa.parser.ast.stmt.ContinueStmt;
import japa.parser.ast.stmt.DoStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.PrimitiveType.Primitive;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Visitador extends ModifierVisitorAdapter<Object>
{
	/********************************************************/
	/********************** Atributos ***********************/
	/********************************************************/

	// Usamos un contador para numerar los métodos que creemos
	int contador=1;
	// Usamos un contador para numerar los index que creemos
	int contador_index=1;
	// Variable usada para conocer la lista de métodos visitados
	LinkedList<MethodDeclaration> previousMethodDeclarations = new LinkedList<MethodDeclaration>();
	// Variable usada para saber cuál es el último método visitado (el que estoy visitando ahora)
	MethodDeclaration methodDeclaration;
	// Variable usada para conocer la lista de clases visitadas
	LinkedList<ClassOrInterfaceDeclaration> previousClassDeclarations = new LinkedList<ClassOrInterfaceDeclaration>();
	// Variable usada para saber cuál es la última clase visitada (la que estoy visitando ahora)
	ClassOrInterfaceDeclaration classDeclaration;

	/********************************************************/
	/*********************** Metodos ************************/
	/********************************************************/

	// Visitador de clases
	// Este visitador no hace nada, simplemente registra en una lista las clases que se van visitando
	public Node visit(ClassOrInterfaceDeclaration classDeclaration, Object args)
	{
		this.previousClassDeclarations.add(classDeclaration);
		this.classDeclaration = classDeclaration;
		Node newClassDeclaration = super.visit(classDeclaration, args);
		this.previousClassDeclarations.removeLast();
		this.classDeclaration = this.previousClassDeclarations.isEmpty() ? null : this.previousClassDeclarations.getLast();

		return newClassDeclaration;
	}
	// Visitador de métodos
	// Este visitador no hace nada, simplemente registra en una lista los métodos que se van visitando
	public Node visit(MethodDeclaration methodDeclaration, Object args)
	{
		this.previousMethodDeclarations.add(methodDeclaration);
		this.methodDeclaration = methodDeclaration;
		Node newMethodDeclaration = super.visit(methodDeclaration, args);
		this.previousMethodDeclarations.removeLast();
		this.methodDeclaration = this.previousMethodDeclarations.isEmpty() ? null : this.previousMethodDeclarations.getLast();

		return newMethodDeclaration;
	}

	// Visitador de sentencias "while"
	public Node visit(WhileStmt whileStmt, Object args)
	{
		// Creamos un objeto Loop que sirve para examinar bucles
		Loop loop = new While(null, null, whileStmt);

		return transformLoopsToRecursiveMethods(loop, LoopType.WHILE, whileStmt.getCondition(), blockWrapper(whileStmt.getBody()), null, null);
	}

	// Visitador de sentencias "do-while"
	public Node visit(DoStmt doStmt, Object args)
	{
		// Creamos un objeto Loop que sirve para examinar bucles
		Loop loop = new Do(null, null, doStmt);

		return transformLoopsToRecursiveMethods(loop, LoopType.DO_WHILE, doStmt.getCondition(), blockWrapper(doStmt.getBody()), null, null);
	}

	// Visitador de sentencias "for"
	public Node visit(ForStmt forStmt, Object args)
	{
		// Creamos un objeto Loop que sirve para examinar bucles
		Loop loop = new For(null, null, forStmt);

		// Convert the initialization expressions to statements.
		List<Statement> initStatements = forStmt.getInit()
				.stream()
				.map(i -> new ExpressionStmt(i))
				.collect(Collectors.toList());

		List<Statement> originalBodyStatements = blockWrapper(forStmt.getBody()).getStmts();

		List<Statement> loopBodyStmts = new ArrayList<Statement>(originalBodyStatements);
		BlockStmt forBody = new BlockStmt(loopBodyStmts);

		// Add the update statements to the end of the body.
		List<Statement> updateStatements = forStmt.getUpdate()
				.stream()
				.map(u -> new ExpressionStmt(u))
				.collect(Collectors.toList());

		loopBodyStmts.addAll(updateStatements);

		return transformLoopsToRecursiveMethods(loop, LoopType.FOR, forStmt.getCompare(), forBody, initStatements, null);
	}

	// Visitador de sentencias "for-each"
	public Node visit(ForeachStmt forEachStmt, Object args)
	{
		// Creamos un objeto Loop que sirve para examinar bucles
		Loop loop = new Foreach(null, null, forEachStmt);

		// To transform the foreach loop into an equivalent recursive method, it should be first transformed into an equivalent for-loop.
		// This way, the conversor already implemented can be reused.
		// There are 3 steps for this conversion:
		//		1. Create a counter variable. This variable will indicate the current iteration.
		//		   It will also allow the access to the current element of the iterable collection.
		//		2. Create a loop condition: The loop condition will a bounds check against the collection.
		//		3. Create an increment expression: This expression will increment the counter, to progress to the next iteration.
		//
		// With these three instructions, the foreach loop can be rewritten as a for loop; and then transformed
		// into an equivalent recursive method.

		// Declare the counter variable: int index = 0;
		String indexVariableName = "index_" + contador_index++;

		PrimitiveType integerType = new PrimitiveType(Primitive.Int);
		VariableDeclaratorId indexVariableId = new VariableDeclaratorId(indexVariableName);
		VariableDeclarator indexAssignment = new VariableDeclarator(indexVariableId, new IntegerLiteralExpr("0"));
		VariableDeclarationExpr indexDeclarationExpr = new VariableDeclarationExpr(integerType, Arrays.asList(indexAssignment));

		// The index variable will be an initialization statement that should be executed before the first iteration.
		List<Statement> initStatements = Arrays.asList(new ExpressionStmt(indexDeclarationExpr));

		// Build the loop condition: index < iterable.length
		NameExpr indexRef = new NameExpr(indexVariableName);
		Expression foreachIterableCollection = forEachStmt.getIterable();

		FieldAccessExpr fieldAcces = new FieldAccessExpr(foreachIterableCollection, "length");
		BinaryExpr indexComparison = new BinaryExpr(indexRef, fieldAcces, japa.parser.ast.expr.BinaryExpr.Operator.less);

		// Declare the update statements: index++.
		List<Statement> originalBodyStatements = blockWrapper(forEachStmt.getBody()).getStmts();

		// Add the increments to the body of the loop.
		List<Statement> loopBodyStmts = new ArrayList<Statement>(originalBodyStatements);
		BlockStmt foreachBody = new BlockStmt(loopBodyStmts);

		UnaryExpr indexIncrement = new UnaryExpr(indexRef, japa.parser.ast.expr.UnaryExpr.Operator.posIncrement);
		loopBodyStmts.add(new ExpressionStmt(indexIncrement));

		// Assign the fist statetement to the iterable loop String animal = animals[index];
		VariableDeclarationExpr iteratorVariable = forEachStmt.getVariable();
		ArrayAccessExpr iterableCollectionAccess = new ArrayAccessExpr(foreachIterableCollection, indexRef);
		AssignExpr iteratorVariableAssign = new AssignExpr(iteratorVariable, iterableCollectionAccess, Operator.assign);

		loopBodyStmts.add(0, new ExpressionStmt(iteratorVariableAssign));

		Variable indexVariableRef = Variable.createVariable(0, integerType, indexVariableName, 0);

		return transformLoopsToRecursiveMethods(loop, LoopType.FOREACH, indexComparison, foreachBody, initStatements, Arrays.asList(indexVariableRef));
	}

	/**
	 * Transforms the given loop condition into an equivalent tail-recursive method.
	 * @param loop The loop object, used to analyse its variable references.
	 * @param loopType The type of the loop. It affects the generated code depending on the loop.
	 * @param loopCondition The expression of the condition to continue iterating in the loop.
	 * @param loopBody The body of the loop. Contains all the instructions that could be executed in an iteration.
	 * @param loopInitialization ({@link LoopType.For} only) The initialization statements for the loop.
	 * @param additionalVariables ({@link LoopType.Foreach} only) The additional variables that should be registered as arguments to the loop.
	 * @return The node node with the equivalent method call, used to replace the loop statement.
	 */
	private Node transformLoopsToRecursiveMethods(Loop loop, LoopType loopType, Expression loopCondition, BlockStmt loopBody, List<Statement> loopInitialization, List<Variable> additionalVariables) {
		/**************************/
		/******** LLAMADOR ********/
		/**************************/

		// Nombre del método que crearemos por cada sentencia "while"
		String methodName = "metodo_"+contador++;

		// El objeto Loop nos calcula la lista de variables declaradas en el método y usadas en el bucle (la intersección)
		List<Variable> variables = loop.getUsedVariables(methodDeclaration);
		// Creamos un objeto LoopVariables que sirve para convertir la lista de variables en lista de argumentos y parámetros
		LoopVariables loopVariables = new LoopVariables(variables);
		// El objeto LoopVariables nos calcula la lista de argumentos del método
		List<Expression> arguments = loopVariables.getArgs();

		List<Statement> ifBlockStatements = new ArrayList<Statement>();

		// In a for-each loop, the extra variables created in the caller method must be registered.
		if (loopType == LoopType.FOREACH)
		{
			variables.addAll(additionalVariables);

			arguments.addAll(
				additionalVariables.stream()
					.map(v -> new NameExpr(v.getName()))
					.collect(Collectors.toList()));
		}

		// In a do-while instruction, the first iteration is executed unconditionally.
		if (loopType == LoopType.DO_WHILE)
		{
			ifBlockStatements.add(blockWrapper(loopBody));
		}

		// TODO: Check that there are no variables named result already in the scope.
		String methodCallResultName = "result";

		boolean isCallerMethodStatic = (this.methodDeclaration.getModifiers() & ModifierSet.STATIC) != 0;
		Expression methodCallScope = isCallerMethodStatic ? null : new ThisExpr();

		// Method call expression: this.method_x(args);
		MethodCallExpr methodCallExpr = new MethodCallExpr(methodCallScope, methodName, arguments);

		// Method call result assignment.
		ClassOrInterfaceType objectType = new ClassOrInterfaceType("Object");
		ReferenceType methodReturnType = new ReferenceType(objectType, 1);

		Statement loopMethodCallStatement = buildMethodCallExpression(methodCallResultName, methodCallExpr, methodReturnType);
		ifBlockStatements.add(loopMethodCallStatement);

		Collection<Statement> resultVariablesUnboxed = buildLoopResultCastingStatements(loopVariables, methodCallResultName);
		ifBlockStatements.addAll(resultVariablesUnboxed);

		IfStmt newIf = new IfStmt(loopCondition, new BlockStmt(ifBlockStatements), null);

		Statement result = newIf;

		// In a for or foreach loops, there is an initialization step before the first iteration.
		if (loopType == LoopType.FOR
			|| loopType == LoopType.FOREACH)
		{
			ArrayList<Statement> blockStatements = new ArrayList<Statement>();
			result = new BlockStmt(blockStatements);

			blockStatements.addAll(loopInitialization);
			blockStatements.add(newIf);
		}

		/**************************/
		/********* METODO *********/
		/**************************/
		int recursiveMethodModifiers = getRecursiveMethodModifiers(this.methodDeclaration.getModifiers());

		List<Parameter> methodParameters = variables.stream()
				.map(v -> v.getParameter())
				.collect(Collectors.toList());

		BlockStmt methodBody = buildRecursiveMethodBody(loopCondition, loopBody, arguments, methodCallExpr, methodReturnType);

		MethodDeclaration newMethod = new MethodDeclaration();

		// Check if the method has any throws statement.
		long numberOfThrows = methodBody.getStmts()
				.stream()
				.filter(stmt -> stmt instanceof ThrowStmt)
				.count();

		List<NameExpr> loopMethodThrows = numberOfThrows > 0?
				Arrays.asList(new NameExpr("Exception"))
				: null;

		// If the top level method does not have a throws statement in its declaration, it should be added.
		if (loopMethodThrows != null && loopMethodThrows.size() > 0
				&& (this.methodDeclaration.getThrows() == null
				|| this.methodDeclaration.getThrows().size() == 0))
		{
			this.methodDeclaration.setThrows(loopMethodThrows);
		}

		newMethod.setBody(methodBody);
		newMethod.setModifiers(recursiveMethodModifiers);
		newMethod.setName(methodName);
		newMethod.setParameters(methodParameters);
		newMethod.setType(methodReturnType.getType());
		newMethod.setArrayCount(methodReturnType.getArrayCount());
		newMethod.setThrows(loopMethodThrows);

		// Añadimos el nuevo método a la clase actual
		this.classDeclaration.getMembers().add(newMethod);

		return result;
	}

	/**
	 * Given an expression to call the recursive method, it builds another expression to declare a variable
	 * and assign the results of the method call.
	 * @param resultVariableName The name of the variable where the results should be stored.
	 * @param methodCallExpr The expression that represents the recursive method call.
	 * @param methodReturnType The expected return type of the method.
	 * @return The statement of the result assignment of the method call.
	 */
	private Statement buildMethodCallExpression(String resultVariableName, MethodCallExpr methodCallExpr, ReferenceType methodReturnType) {
		VariableDeclaratorId methodCallResultVariableId = new VariableDeclaratorId(resultVariableName);
		VariableDeclarator resultAssignment = new VariableDeclarator(methodCallResultVariableId, methodCallExpr);

		VariableDeclarationExpr resultDeclaration = new VariableDeclarationExpr(methodReturnType, Arrays.asList(resultAssignment));

		ExpressionStmt loopMethodCallStatement = new ExpressionStmt(resultDeclaration);

		return loopMethodCallStatement;
	}

	/**
	 * Builds the necessary castings for each of the variables modified on the loop.
	 * @param loopVariables The collection of variables referenced by the loop.
	 * @param resultObjectName The name of the object containing the results of the method call.
	 * @return The built statements for each modified variable.
	 */
	private static Collection<Statement> buildLoopResultCastingStatements(LoopVariables loopVariables, String resultObjectName) {
		// Unbox and assign the return values from the method call to the corresponding arguments.
		List<String> returnNames = loopVariables.getReturnNames();
		List<Type> returnTypes = loopVariables.getReturnTypes();

		List<Statement> assignmentExpressions = new ArrayList<Statement>();

		for (int i = 0; i < returnNames.size(); i++)
		{
			// Access the results array: result[i].
			ArrayAccessExpr resultValue = new ArrayAccessExpr(new NameExpr(resultObjectName), new IntegerLiteralExpr("" + i));

			// Unbox the value with a casting to the expected type: (Type) result[i]
			CastExpr valueCast = new CastExpr(returnTypes.get(i) , resultValue );

			// Assign the value to the input parameter: arg = (Type) result[i]
			NameExpr parameterNameExpr = new NameExpr(returnNames.get(i));
			AssignExpr assignExpr = new AssignExpr(parameterNameExpr, valueCast, Operator.assign);

			assignmentExpressions.add(new ExpressionStmt(assignExpr));
		}

		return assignmentExpressions;
	}

	/**
	 * Builds the body of the equivalent recursive method based on the input parameters.
	 * @param loopCondition The condition to iterate in the loop.
	 * @param loopBody The instructions executed in the body of the loop.
	 * @param loopArguments The name of the parameters modified in the loop.
	 * @param recursiveMethodCall The method call expression to invoke the recursive method.
	 * @param methodReturnType The return type of the recursive method.
	 * @return The body of the equivalent recursive method.
	 */
	private BlockStmt buildRecursiveMethodBody(
			Expression loopCondition,
			BlockStmt loopBody,
			List<Expression> loopArguments,
			MethodCallExpr recursiveMethodCall,
			ReferenceType methodReturnType)
	{
		BlockStmt methodBody = new BlockStmt(new ArrayList<Statement>());

		// Builds result of the current iteration. To stop iterating: return new Object[] { x };
		ArrayInitializerExpr arrayInitializerExpr = new ArrayInitializerExpr(loopArguments);
		ArrayCreationExpr createResultArray = new ArrayCreationExpr(methodReturnType.getType(), methodReturnType.getArrayCount(), arrayInitializerExpr);
		ReturnStmt returnResultArray = new ReturnStmt(createResultArray);

		// Builds the if statement that contains the recursive method call. If the loop continue is still true,
		// proceeds to a new iteration:
		//
		// if (loopCondition) {
		//	  return metodo_1(x);
		// }
		ReturnStmt returnRecursiveCallResult = new ReturnStmt(recursiveMethodCall);
		IfStmt recursionIf = new IfStmt(loopCondition, blockWrapper(returnRecursiveCallResult), null);
		recursionIf.setCondition(loopCondition);

		boolean hasFirstLevelReturnOrThrowStatement = false;

		// The body of the method begins with the code of the loop iteration.
		List<Statement> loopBodyStatements = new ArrayList<Statement>();

		// Clone the loop body inside the method body, to avoid modifying the original.
		for (Statement stmt : loopBody.getStmts())
		{
			Statement equivalentStatement = getEquivalentStatement(stmt, recursionIf, returnResultArray);

			if (equivalentStatement == null)
			{
				// Stop copying statements after the continue, since they would be unreachable.
				break;
			}

			loopBodyStatements.add(equivalentStatement);

			if (equivalentStatement instanceof ReturnStmt
					|| equivalentStatement instanceof ThrowStmt)
			{
				// Stop copying statements after this statement, since they would be unreachable.
				hasFirstLevelReturnOrThrowStatement = true;

				break;
			}
		}

		methodBody.getStmts().addAll(loopBodyStatements);

		if (hasFirstLevelReturnOrThrowStatement)
		{
			// Since there was a first-level return statement, it makes no
			// sense to add the rest of the loop statements.
			// Otherwise, they would be unreachable code.
			return methodBody;
		}

		methodBody.getStmts().add(recursionIf);
		methodBody.getStmts().add(returnResultArray);

		return methodBody;
	}


	/**
	 * Gets the {@link ModifierSet} for the recursive method. It takes into account the
	 * modifiers of the caller method, which will limit things like its membership to a class.
	 * @param callerMethodModifiers The caller method modifiers.
	 * @return The recursive method modifiers.
	 */
	private static int getRecursiveMethodModifiers(int callerMethodModifiers) {
		boolean isCallerMethodStatic = (callerMethodModifiers & ModifierSet.STATIC) != 0;

		// The method created should only be locally accessible by the class that declares the loop.
		int recursiveMethodModifiers = ModifierSet.PRIVATE;

		recursiveMethodModifiers =  isCallerMethodStatic ?
				recursiveMethodModifiers | ModifierSet.STATIC
				: recursiveMethodModifiers;

		return recursiveMethodModifiers;
	}

	/**
	 * Given a statement from a loop's body, returns the equivalent statement for the body of a recursive method.
	 * @param stmt The statement to transform.
	 * @param continueStatementReplacement The statement to replace the continue statements.
	 * @param returnOrBreakStatementReplacement The statement to replace the break or return statements.
	 * @return The equivalent statement for a recursive method.
	 */
	private static Statement getEquivalentStatement(Statement stmt, Statement continueStatementReplacement, Statement returnOrBreakStatementReplacement)
	{
		return getEquivalentStatement(stmt, continueStatementReplacement, returnOrBreakStatementReplacement, true);
	}

	/**
	 * Recursive method that transform statements from a loop's body into their equivalent statement for the body of a recursive method.
	 * @param stmt The statement to transform.
	 * @param continueStatementReplacement The statement to replace the continue statements.
	 * @param returnOrBreakStatementReplacement The statement to replace the break or return statements.
	 * @param isTopLevelCall Indicates if it is the top level call of the recursion. Has an effect on how some instructions are transformed.
	 * @return The equivalent statement for a recursive method.
	 */
	private static Statement getEquivalentStatement(Statement stmt, Statement continueStatementReplacement, Statement returnOrBreakStatementReplacement, boolean isTopLevelCall)
	{
		Statement result = stmt;

		if (stmt instanceof BlockStmt)
		{
			BlockStmt block = (BlockStmt)stmt;
			List<Statement> blockStatements = block.getStmts();

			if (blockStatements.size() == 0)
			{
				return block;
			}
			// Transform the block instructions recursively.
			ArrayList<Statement> newBlockStatements = new ArrayList<Statement>();
			BlockStmt newBlock = new BlockStmt(newBlockStatements);

			for (Statement s : blockStatements)
			{
				newBlockStatements.add(getEquivalentStatement(s, continueStatementReplacement, returnOrBreakStatementReplacement, false));
			}

			return newBlock;
		}

		// Any return statement should be replaced with the equivalent
		// recursive method result return statement.
		if (stmt instanceof ReturnStmt
			|| stmt instanceof BreakStmt)
		{
			result = returnOrBreakStatementReplacement;
		}
		else if (stmt instanceof ContinueStmt)
		{
			// If the continue statement is not in a top-level statement, it should be replaced with the continue replacement statement.
			result = isTopLevelCall?
					null
					: continueStatementReplacement;
		}
		else if (stmt instanceof IfStmt)
		{
			// Stop copying statements after the continue, since they would be unreachable.
			IfStmt ifStmt = (IfStmt) stmt;

			IfStmt newIfStmt = new IfStmt(
					ifStmt.getCondition(),
					getEquivalentStatement(ifStmt.getThenStmt(), continueStatementReplacement, returnOrBreakStatementReplacement, false),
					getEquivalentStatement(ifStmt.getElseStmt(), continueStatementReplacement, returnOrBreakStatementReplacement, false));

			return newIfStmt;

		}

		return result;
	}

	// Dada un tipo,
	// Si es un tipo primitivo, devuelve el wrapper correspondiente
	// Si es un tipo no primitivo, lo devuelve
	private Type getWrapper(Type type)
	{
		if (!(type instanceof PrimitiveType))
			return type;

		PrimitiveType primitiveType = (PrimitiveType) type;
		String primitiveName = primitiveType.getType().name();
		String wrapperName = primitiveName;

		if (wrapperName.equals("Int"))
			wrapperName = "Integer";
		else if (wrapperName.equals("Char"))
			wrapperName = "Character";

		return new ClassOrInterfaceType(wrapperName);
	}
	// Dada una sentencia,
	// Si es una única instrucción, devuelve un bloque equivalente
	// Si es un bloque, lo devuelve
	private BlockStmt blockWrapper(Statement statement)
	{
		if (statement instanceof BlockStmt)
			return (BlockStmt) statement;

		BlockStmt block = new BlockStmt();
		List<Statement> blockStmts = new LinkedList<Statement>();
		blockStmts.add(statement);

		block.setStmts(blockStmts);

		return block;
	}

	/**
	 * The type loop that is being analysed.
	 *
	 */
	private enum LoopType
	{
		WHILE,
		DO_WHILE,
		FOR,
		FOREACH,
	}
}
