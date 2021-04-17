package transformador;
	
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
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
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
		/**************************/
		/******** LLAMADOR ********/
		/**************************/		
		
		// Nombre del método que crearemos por cada sentencia "while"
		String methodName = "metodo_"+contador++;
		
		// Creamos un objeto Loop que sirve para examinar bucles
		Loop loop = new While(null, null, whileStmt);
		// El objeto Loop nos calcula la lista de variables declaradas en el método y usadas en el bucle (la intersección)
		List<Variable> variables = loop.getUsedVariables(methodDeclaration);
		// Creamos un objeto LoopVariables que sirve para convertir la lista de variables en lista de argumentos y parámetros
		LoopVariables loopVariables = new LoopVariables(variables);
		// El objeto LoopVariables nos calcula la lista de argumentos del método 
		List<Expression> arguments = loopVariables.getArgs();
		
		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////
		//-------------------> CREAR EL nuevo if newIf
		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////
		List<Statement> ifBlockStatements = new ArrayList<Statement>();
		
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
		
		Expression loopCondition = whileStmt.getCondition();		
		IfStmt newIf = new IfStmt(loopCondition, new BlockStmt(ifBlockStatements), null);
		
		/**************************/
		/********* METODO *********/
		/**************************/

		////////////////////////////////////////////////////////////		
		////////////////////////////////////////////////////////////
		//-------------------> CREAR EL nuevo método newMethod
		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////
		
		int recursiveMethodModifiers = getRecursiveMethodModifiers(this.methodDeclaration.getModifiers());

		List<Parameter> methodParameters = variables.stream()
				.map(v -> v.getParameter())
				.collect(Collectors.toList());
		
		BlockStmt methodBody = buildRecursiveMethodBody(loopCondition, whileStmt, arguments, methodCallExpr, methodReturnType);

		MethodDeclaration newMethod = new MethodDeclaration();
		
		newMethod.setBody(methodBody);
		newMethod.setModifiers(recursiveMethodModifiers);
		newMethod.setName(methodName);
		newMethod.setParameters(methodParameters);
		newMethod.setType(methodReturnType.getType());
		newMethod.setArrayCount(methodReturnType.getArrayCount());	
		
		// Añadimos el nuevo método a la clase actual
		this.classDeclaration.getMembers().add(newMethod);
		
		return newIf;
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
			Statement loopBody, 
			List<Expression> loopArguments,
			MethodCallExpr recursiveMethodCall,
			ReferenceType methodReturnType) 
	{
		// The body of the method begins with the code of the loop iteration.
		BlockStmt methodBody = blockWrapper(loopBody);
		
		// Builds the if statement that contains the recursive method call. If the loop continue is still true, 
		// proceeds to a new iteration:
		//
		// if (loopCondition) {
		//	  return metodo_1(x);
		// }
		ReturnStmt returnRecursiveCallResult = new ReturnStmt(recursiveMethodCall);
		IfStmt recursionIf = new IfStmt(loopCondition, blockWrapper(returnRecursiveCallResult), null);
		recursionIf.setCondition(loopCondition);
		
		methodBody.getStmts().add(recursionIf);
		
		// Builds result of the current iteration. To stop iterating: return new Object[] { x };
		ArrayInitializerExpr arrayInitializerExpr = new ArrayInitializerExpr(loopArguments);
		ArrayCreationExpr createResultArray = new ArrayCreationExpr(methodReturnType.getType(), methodReturnType.getArrayCount(), arrayInitializerExpr);
		ReturnStmt returnResultArray = new ReturnStmt(createResultArray);
		
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
}