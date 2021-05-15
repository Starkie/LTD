package grafos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import grafos.nodes.ControlNodePDG;
import grafos.nodes.ControlNodeType;


public class VisitadorPDG extends VoidVisitorAdapter<ProgramDependencyGraph>
{
	/********************************************************/
	/********************** Atributos ***********************/
	/********************************************************/

	// Usamos un contador para numerar las instrucciones
	int contador=1;

	// The collection of control nodes that we are currently analysing.
	// Each control node in the stack represents a nesting level.
	Stack<ControlNodePDG> controlNodes = new Stack<ControlNodePDG>();

	HashMap<String, List<String>> dataDependencies = new HashMap<String, List<String>>();

	String currentNode = "Entry";

	boolean isInsideAssign = false;
    boolean isPartOfCondition = false;
	boolean isParameterOfMethodCall = false;

	/********************************************************/
	/*********************** Metodos ************************/
	/********************************************************/

	// Visitador de métodos
	// Este visitador añade el nodo final al ProgramDependencyGraph
	@Override
	public void visit(MethodDeclaration methodDeclaration, ProgramDependencyGraph programDependencyGraph)
	{
		this.controlNodes.add(new ControlNodePDG(ControlNodeType.METHOD, "Entry"));

	    // Visitamos el método
		super.visit(methodDeclaration, programDependencyGraph);
	}

	// Visitador de expresiones
	// Cada expresión encontrada genera un nodo en el ProgramDependencyGraph
	@Override
	public void visit(ExpressionStmt expressionStmt, ProgramDependencyGraph programDependencyGraph)
	{
		// Creamos el nodo actual
		this.currentNode = crearNodo(expressionStmt);

		createEdges(this.currentNode, programDependencyGraph);

		// Seguimos visitando...
		super.visit(expressionStmt, programDependencyGraph);
	}

	@Override
	public void visit(VariableDeclarator variableDeclarator, ProgramDependencyGraph programDependencyGraph) {
		// Add the variable to the data dependency dictionary.
		String variableName = variableDeclarator.getNameAsString();

		if (dataDependencies.containsKey(variableName))
		{
			addDataDependencyEdges(dataDependencies.get(variableName), this.currentNode, programDependencyGraph);
		}

		registerVariableAssignation(variableName, programDependencyGraph);

		super.visit(variableDeclarator, programDependencyGraph);
	}

	private void registerVariableAssignation(String variableName, ProgramDependencyGraph programDependencyGraph) {
		// TODO: Convertir a lista.
		// 1: Si está en nivel de entry: vaciar la lista y añadir la nueva entrada
		// 2: Si está dentro de una condición de control, comprobar si las asignaciones tienen el mismo nodo de control.
		//	- Reemplazar todas las asignaciones en la misma condición de control. SI NO ESTÁN EN EL MISMO, SE APENDE AL FINAL.
		//	- SOLO LAS QUE ESTÁN A NIVEL DE ENTRY BORRAN TODO.

		// If the key is not present or there is currently no nesting of a control node.
		if (!this.dataDependencies.containsKey(variableName)
			|| this.controlNodes.peek().getType() == ControlNodeType.METHOD)
		{
			List<String> nodes = new ArrayList<String>();

			nodes.add(this.currentNode);

			this.dataDependencies.put(variableName, nodes);
		}
		else
		{
			List<String> asdf = this.dataDependencies.get(variableName);

			asdf.add(this.currentNode);
		}

	}

	@Override
	public void visit(NameExpr nameExpr, ProgramDependencyGraph programDependencyGraph) {
		String variableName = nameExpr.getNameAsString();

		// Only add the variable when visiting from another another node from a special type.
		if (dataDependencies.containsKey(variableName)
			&& (this.isInsideAssign || this.isPartOfCondition || this.isParameterOfMethodCall))
		{
			addDataDependencyEdges(dataDependencies.get(variableName), this.currentNode, programDependencyGraph);
		}

		super.visit(nameExpr, programDependencyGraph);
	}

	public void visit(UnaryExpr unaryExpr, ProgramDependencyGraph programDependencyGraph)
	{
		if (unaryExpr.getExpression() instanceof NameExpr)
		{
			NameExpr nameExpr = (NameExpr) unaryExpr.getExpression();

			String variableName = nameExpr.getNameAsString();

			if (this.dataDependencies.containsKey(variableName))
			{
				// Add a data dependency to previous variable definition.
				addDataDependencyEdges(this.dataDependencies.get(variableName), this.currentNode, programDependencyGraph);
			}

			this.registerVariableAssignation(variableName, programDependencyGraph);
		}
	}

	@Override
	public void visit(AssignExpr assignExpr, ProgramDependencyGraph programDependencyGrah) {
		isInsideAssign = true;

		super.visit(new ExpressionStmt(assignExpr.getValue()), programDependencyGrah);

		isInsideAssign = false;

		NameExpr nameExpr = (NameExpr) assignExpr.getTarget();

		String variableName = nameExpr.getNameAsString();

		// If an assign operation does more than just assigning values, it would read the value from the previous variable.
		if (assignExpr.getOperator() != Operator.ASSIGN)
		{
			if (this.dataDependencies.containsKey(variableName))
			{
				// Add a data dependency to previous variable definition.
				addDataDependencyEdges(this.dataDependencies.get(variableName), this.currentNode, programDependencyGrah);
			}
		}

		this.registerVariableAssignation(variableName, programDependencyGrah);
	}

	/**
	 * Visits an {@link IfStmt} and registers all its child nodes into the {@link ProgramDependencyGraph}.
	 * @param ifStmt The if statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(IfStmt ifStmt, ProgramDependencyGraph programDependencyGraph) {
		// Create the edges to the if node.
		String ifNode = crearNodo("if (" + ifStmt.getCondition() + ")");

		createEdges(ifNode, programDependencyGraph);

        // Check the data dependencies inside the condition.
		this.isPartOfCondition = true;

		this.currentNode = ifNode;
		super.visit(new ExpressionStmt(ifStmt.getCondition()), programDependencyGraph);

		this.isPartOfCondition = false;

		// Push the if control node to the stack.
		ControlNodePDG ifControlNode = new ControlNodePDG(ControlNodeType.IF, ifNode);
		this.controlNodes.push(ifControlNode);

		// First visit the 'then' statement, that will always be present.
		super.visit(convertirEnBloque(ifStmt.getThenStmt()), programDependencyGraph);

		// If it is present, also visit the 'else' statement.
		Optional<Statement> elseStmt = ifStmt.getElseStmt();

		if (elseStmt.isPresent())
		{
			super.visit(convertirEnBloque(elseStmt.get()), programDependencyGraph);
		}

		// Pop the node since it's not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link WhileStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param whileStmt The while statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(WhileStmt whileStmt, ProgramDependencyGraph programDependencyGraph) {
		String whileNode = crearNodo("while (" + whileStmt.getCondition() + ")");

		// TODO: Can the loops be refactored to a single method?

		// Create the edges from the previous node to the loop.
		createEdges(whileNode, programDependencyGraph);

		ControlNodePDG whileControlNode = new ControlNodePDG(ControlNodeType.WHILE,  whileNode);
		this.controlNodes.push(whileControlNode);

		// Create the edges to the loop's child nodes.
		super.visit(convertirEnBloque(whileStmt.getBody()), programDependencyGraph);

		// Remove the while control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link ForStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param forStmt The for statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(ForStmt forStmt, ProgramDependencyGraph programDependencyGraph) {
		String forNode = crearNodo("for (" + forStmt.getCompare().get() + ")");

		// Create the edges from the previous node to the loop.
		createEdges(forNode, programDependencyGraph);

		ControlNodePDG forControlNode = new ControlNodePDG(ControlNodeType.FOR,  forNode);
		this.controlNodes.push(forControlNode);

		// Add the update statements to the end of the body.
		BlockStmt forBody = convertirEnBloque(forStmt.getBody());

		// Add the edges for the initialization nodes.
		for (Expression node : forStmt.getInitialization().toArray(new Expression[0]))
		{
			String currentNode = crearNodo(node);

			createEdges(currentNode, programDependencyGraph);
		}

		List<Statement> updateStatements = forStmt.getUpdate()
				.stream()
				.map(u -> new ExpressionStmt(u))
				.collect(Collectors.toList());

		forBody.getStatements().addAll(updateStatements);

		// Create the edges to the loop's child nodes.
		super.visit(convertirEnBloque(forBody), programDependencyGraph);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link 	ForeachStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param forEachStmt The foreach statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(ForeachStmt forEachStmt, ProgramDependencyGraph programDependencyGraph) {
		String foreachNode = crearNodo("foreach (" + forEachStmt.getVariable() + " : " + forEachStmt.getIterable() + ")");

		// Create the edges from the previous node to the loop.
		createEdges(foreachNode, programDependencyGraph);

		ControlNodePDG foreachControlNode = new ControlNodePDG(ControlNodeType.FOREACH,  foreachNode);
		this.controlNodes.push(foreachControlNode);

		super.visit(convertirEnBloque(forEachStmt.getBody()), programDependencyGraph);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link DoStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param doStmt The do statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(DoStmt doStmt, ProgramDependencyGraph programDependencyGraph) {
		String doWhileNode = crearNodo("while (" + doStmt.getCondition() + ")");

		// Create the edges from the previous node to the loop.
		createEdges(doWhileNode, programDependencyGraph);

		ControlNodePDG doWhileControlNode = new ControlNodePDG(ControlNodeType.DO,  doWhileNode);
		this.controlNodes.push(doWhileControlNode);

		super.visit(convertirEnBloque(doStmt.getBody()), programDependencyGraph);

		// Remove the do-while statement from the control nodes, since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link SwitchStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param switchStatement The switch statement.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(SwitchStmt switchStmt, ProgramDependencyGraph programDependencyGraph) {
		// Create the edges from the previous node to the switch.
		String switchNode = crearNodo("switch (" + switchStmt.getSelector() + ")");

		createEdges(switchNode, programDependencyGraph);

		// Stack the switch control node.
		ControlNodePDG switchControlNode = new ControlNodePDG(ControlNodeType.SWITCH,  switchNode);
		this.controlNodes.push(switchControlNode);

		// Visit all the nested entries.
		super.visit(switchStmt, programDependencyGraph);

		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link SwitchEntryStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param switchEntryStatement The switch entry statement.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(SwitchEntryStmt switchEntryStatement, ProgramDependencyGraph programDependencyGraph) {
		String switchLabel = switchEntryStatement.getLabel().isPresent()?
				"case " + switchEntryStatement.getLabel().get()
				: "default";

		String switchEntryNode = crearNodo(switchLabel);

		createEdges(switchEntryNode, programDependencyGraph);

		// Stack the switch control node.
		ControlNodePDG switchEntryControlNode = new ControlNodePDG(ControlNodeType.SWITCH_CASE,  switchEntryNode);
		this.controlNodes.push(switchEntryControlNode);

		// Visit the switch entry.
		super.visit(switchEntryStatement, programDependencyGraph);

		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link MethodCallExpr} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 */
	@Override
	public void visit(MethodCallExpr methodCallExpr, ProgramDependencyGraph programDependencyGraph) {
		// Visiting the method arguments with this flag enabled will add the corresponding data dependency edges to any visited variable reference.
		this.isParameterOfMethodCall = true;

		super.visit(methodCallExpr, programDependencyGraph);

		this.isParameterOfMethodCall = false;
	}


	// Crear arcos
	private void createEdges(String currentNode, ProgramDependencyGraph programDependencyGraph)
	{
		addControlEdge(currentNode, programDependencyGraph);
	}


	// Añade un arco desde el último nodo hasta el nodo actual (se le pasa como parametro)
	private void addControlEdge(String currentNode, ProgramDependencyGraph programDependencyGraph)
	{
		System.out.println("NODO: " + currentNode);

		String edge = this.controlNodes.peek().getNode() + "->" + currentNode + ";";

		programDependencyGraph.controlEdges.add(edge);
	}

	/**
	 * Adds a data dependency edge from the previous node to the current node.
	 * @param sourceNode The source node for the data dependency edge.
	 * @param targetNode The target of the data dependency edge.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void addDataDependencyEdges(List<String> sourceNodes, String targetNode, ProgramDependencyGraph programDependencyGraph) {
		for (String node : sourceNodes) {
			addDataDependencyEdges(node, this.currentNode, programDependencyGraph);
		}
	}

	/**
	 * Adds a data dependency edge from the previous node to the current node.
	 * @param sourceNode The source node for the data dependency edge.
	 * @param targetNode The target of the data dependency edge.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void addDataDependencyEdges(String sourceNode, String targetNode, ProgramDependencyGraph programDependencyGraph) {
		System.out.println("DATOS NODO: " + targetNode);

		String edge = sourceNode + "->" + targetNode + "[style=dashed];";

		programDependencyGraph.dataEdges.add(edge);
	}

	// Crear nodo
	// Añade un arco desde el nodo actual hasta el último control
	private String crearNodo(Object objeto)
	{
		return "\"("+ contador++ +") "+quitarComillas(objeto.toString())+"\"";
	}

	// Sustituye " por \" en un string: Sirve para eliminar comillas.
	private static String quitarComillas(String texto)
	{
	    return texto.replace("\"", "\\\"");
	}

	// Dada una sentencia,
	// Si es una única instrucción, devuelve un bloque equivalente
	// Si es un bloque, lo devuelve
	private BlockStmt convertirEnBloque(Statement statement)
	{
		if (statement instanceof BlockStmt)
			return (BlockStmt) statement;

		BlockStmt block = new BlockStmt();
		NodeList<Statement> blockStmts = new NodeList<Statement>();
		blockStmts.add(statement);

		block.setStatements(blockStmts);

		return block;
	}

}
