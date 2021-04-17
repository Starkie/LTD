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
		
		boolean isCallerMethodStatic = (this.methodDeclaration.getModifiers() & ModifierSet.STATIC) != 0;
		
		Expression methodCallScope = isCallerMethodStatic ? null : new ThisExpr(); 
		
		// Method call expresion: this.method_x(args);
		MethodCallExpr methodCallExpr = new MethodCallExpr(methodCallScope, methodName, arguments);
		
		// Method call result assignment.
		ClassOrInterfaceType objectType = new ClassOrInterfaceType("Object");
		ReferenceType methodReturnType = new ReferenceType(objectType, 1);
		
		// TODO: Check that there are no variables named result already in the scope.
		String methodCallResultName = "result";
		
		VariableDeclaratorId methodCallResultVariableId = new VariableDeclaratorId(methodCallResultName);
		VariableDeclarator resultAssignment = new VariableDeclarator(methodCallResultVariableId, methodCallExpr); 
		VariableDeclarationExpr resultDeclaration = new VariableDeclarationExpr(methodReturnType, Arrays.asList(resultAssignment));

		ExpressionStmt loopMethodCallStatement = new ExpressionStmt(resultDeclaration);
		
		ifBlockStatements.add(loopMethodCallStatement);
		
		// Unbox and assign the return values from the method call to the corresponding arguments.
		List<String> returnNames = loopVariables.getReturnNames();
		List<Type> returnTypes = loopVariables.getReturnTypes();
		
		for (int i = 0; i < returnNames.size(); i++)
		{
			// Access the results array: result[i].
			ArrayAccessExpr resultValue = new ArrayAccessExpr(new NameExpr(methodCallResultName), new IntegerLiteralExpr("" + i));
			
			// Unbox the value with a casting to the expected type: (Type) result[i]
			CastExpr valueCast = new CastExpr(returnTypes.get(i) , resultValue );
			
			// Assign the value to the input parameter: arg = (Type) result[i]
			NameExpr parameterNameExpr = new NameExpr(returnNames.get(i));
			AssignExpr assignExpr = new AssignExpr(parameterNameExpr, valueCast, Operator.assign);
			
			// Add the statement to the if block.
			ifBlockStatements.add(new ExpressionStmt(assignExpr));
		}
				
		Expression condition = whileStmt.getCondition();
		
		IfStmt newIf = new IfStmt(condition, new BlockStmt(ifBlockStatements), null);
		
		/**************************/
		/********* METODO *********/
		/**************************/

		////////////////////////////////////////////////////////////		
		////////////////////////////////////////////////////////////
		//-------------------> CREAR EL nuevo método newMethod
		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////
		// MethodDeclaration methodDeclaration = new MethodDeclaration(0, methodReturnType.getType(), );
				
		
		// Añadimos el nuevo método a la clase actual
		
		List<Parameter> methodParameters = variables.stream()
				.map(v -> v.getParameter())
				.collect(Collectors.toList());
		
		BlockStmt methodBody = blockWrapper(whileStmt.getBody());
		
		ReturnStmt methodReturnStmt = new ReturnStmt(methodCallExpr);
		IfStmt recursionIf = new IfStmt(whileStmt.getCondition(), blockWrapper(methodReturnStmt), null);
		recursionIf.setCondition(condition);
		methodBody.getStmts().add(recursionIf);
		
		ArrayInitializerExpr arrayInitializerExpr = new ArrayInitializerExpr(arguments);
		ArrayCreationExpr createResultArray = new ArrayCreationExpr(methodReturnType.getType(), methodReturnType.getArrayCount(), arrayInitializerExpr);
		ReturnStmt returnResultArray = new ReturnStmt(createResultArray);
		methodBody.getStmts().add(returnResultArray);
		
		// The method created should only be locally accessible by the class that declares the loop.
		int recursiveMethodModifiers = ModifierSet.PRIVATE;
		
		recursiveMethodModifiers =  isCallerMethodStatic ?
				recursiveMethodModifiers | ModifierSet.STATIC 
				: recursiveMethodModifiers;

		BodyDeclaration newMethod = new MethodDeclaration(
				null,
				recursiveMethodModifiers,
				null,
				null,
				methodReturnType.getType(),
				methodName,
				methodParameters,
				methodReturnType.getArrayCount(),
				null,
				methodBody);
		
		this.classDeclaration.getMembers().add(newMethod);
		
		return newIf;
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