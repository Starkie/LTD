package grafos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
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
import grafos.nodes.VariableAssignment;


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
		// If the variable declaration does not have an initializer, do not register it as a dependency.
		if (variableDeclarator.getInitializer().isEmpty())
		{
			return;
		}

		// Add the variable to the data dependency dictionary.
		String variableName = variableDeclarator.getNameAsString();

		registerVariableReference(variableName, this.currentNode, programDependencyGraph);

		registerVariableAssignment(variableName, this.currentNode, programDependencyGraph);

		super.visit(variableDeclarator, programDependencyGraph);
	}

	@Override
	public void visit(NameExpr nameExpr, ProgramDependencyGraph programDependencyGraph) {
		String variableName = nameExpr.getNameAsString();

		// Only add the variable when visiting from another another node from a special type.
		if (this.isInsideAssign 
			|| this.isPartOfCondition 
			|| this.isParameterOfMethodCall)
		{
			registerVariableReference(variableName, this.currentNode, programDependencyGraph);
		}

		super.visit(nameExpr, programDependencyGraph);
	}

	public void visit(UnaryExpr unaryExpr, ProgramDependencyGraph programDependencyGraph)
	{
		if (unaryExpr.getExpression() instanceof NameExpr)
		{
			NameExpr nameExpr = (NameExpr) unaryExpr.getExpression();

			String variableName = nameExpr.getNameAsString();

			// Add a data dependency to previous variable definition.
			registerVariableReference(variableName, this.currentNode, programDependencyGraph);

			this.registerVariableAssignment(variableName, this.currentNode, programDependencyGraph);
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
			// Add a data dependency to previous variable definition.
			registerVariableReference(variableName, this.currentNode, programDependencyGrah);
		}

		this.registerVariableAssignment(variableName, this.currentNode, programDependencyGrah);
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

		// TODO: Separate IF and ELSE branches.

		createEdges(ifNode, programDependencyGraph);
		// Push the if control node to the stack.
		ControlNodePDG ifControlNode = new ControlNodePDG(ControlNodeType.IF, ifNode);
		addNewControlNode(ifControlNode);

        registerConditionDataDependencies(ifNode, ifStmt.getCondition(), programDependencyGraph);

		// First visit the 'then' statement, that will always be present.
		super.visit(convertirEnBloque(ifStmt.getThenStmt()), programDependencyGraph);

		// If it is present, also visit the 'else' statement.
		Optional<Statement> elseStmt = ifStmt.getElseStmt();

		if (elseStmt.isPresent())
		{
			// Mark the start of new block, to avoid referecing variables from the then-block.
			ifControlNode.startNewBlock();
			
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

		ControlNodePDG whileControlNode = new ControlNodePDG(ControlNodeType.WHILE,  whileNode);

		visitLoop(whileControlNode, whileStmt.getCondition(), whileStmt.getBody(), programDependencyGraph);
	}

	/**
	 * Visits a {@link DoStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param doStmt The do statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(DoStmt doStmt, ProgramDependencyGraph programDependencyGraph) {
		String doWhileNode = crearNodo("do-while (" + doStmt.getCondition() + ")");

		ControlNodePDG doWhileControlNode = new ControlNodePDG(ControlNodeType.DO,  doWhileNode);

		visitLoop(doWhileControlNode, doStmt.getCondition(), doStmt.getBody(), programDependencyGraph);
	}

	/**
	 * Visits a {@link ForStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param forStmt The for statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(ForStmt forStmt, ProgramDependencyGraph programDependencyGraph) {
		String forNode = crearNodo("for (" + forStmt.getCompare().get() + ")");

		ControlNodePDG forControlNode = new ControlNodePDG(ControlNodeType.FOR, forNode);
		
		// Add the edges for the initialization nodes.
		for (Expression node : forStmt.getInitialization())
		{
			this.visit(new ExpressionStmt(node), programDependencyGraph);
		}

		// TODO: Can the Compare be null?
		visitLoop(forControlNode, forStmt.getCompare().get(), forStmt.getBody(), programDependencyGraph);
	}

	/**
	 * Visits a {@link 	ForeachStmt} and registers all the nodes into the {@link ProgramDependencyGraph}.
	 * @param forEachStmt The foreach statement to visit.
	 * @param programDependencyGraph The program dependency graph.
	 */
	@Override
	public void visit(ForeachStmt forEachStmt, ProgramDependencyGraph programDependencyGraph) {
		String foreachNode = crearNodo("foreach (" + forEachStmt.getVariable() + " : " + forEachStmt.getIterable() + ")");

		ControlNodePDG foreachControlNode = new ControlNodePDG(ControlNodeType.FOREACH,  foreachNode);

		// Register the variable assignments from the foreach node.
		this.currentNode = foreachNode;

		forEachStmt.getVariable()
			.getVariables()
			.forEach(v -> this.visit(v, programDependencyGraph));

		visitLoop(foreachControlNode, forEachStmt.getIterable(), forEachStmt.getBody(), programDependencyGraph);
	}

	/**
	 * Visits a generic loop given all the required parameters.
	 * @param controlNode The control node that represents the loop.
	 * @param loopCondition The loop's condition expression.
	 * @param loopBody The loop's body statement.
	 * @param programDependencyGraph The program dependency graph.
	 * @param initializationExpressions (Optional) The initialization expressions, if any, for the loop counter variables.
	 */
	private void visitLoop(ControlNodePDG controlNode, Expression loopCondition, Statement loopBody, ProgramDependencyGraph programDependencyGraph)
	{
		// Create the edges from the previous node to the loop.
		createEdges(controlNode.getNode(), programDependencyGraph);

		addNewControlNode(controlNode);

		// Register the variable references from the condition, if any exists.
		if (controlNode.getType() != ControlNodeType.DO)
		{
			// The first reference should not be registered in DO-WHILE statements.
			// The body is always executed one time before evaluating the condition.
			registerConditionDataDependencies(controlNode.getNode(), loopCondition, programDependencyGraph);
		}

		// Create the edges to the loop's child nodes.
		super.visit(convertirEnBloque(loopBody), programDependencyGraph);

		// Register the data dependencies from the condition again,
		registerConditionDataDependencies(controlNode.getNode(), loopCondition, programDependencyGraph);

		// The variables modified inside the loop are referenced as dependencies for the next iterations.
		registerLoopNextIterationDataDependencies(controlNode, loopCondition, programDependencyGraph);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	private void addNewControlNode(ControlNodePDG controlNode) {
		// Add as a children of the current parent node.
		this.controlNodes.peek().getAllChildNodes().add(controlNode);

		// Add it to the stack.
		this.controlNodes.push(controlNode);
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

		registerConditionDataDependencies(switchNode, switchStmt.getSelector(), programDependencyGraph);

		createEdges(switchNode, programDependencyGraph);

		// Stack the switch control node.
		ControlNodePDG switchControlNode = new ControlNodePDG(ControlNodeType.SWITCH,  switchNode);
		addNewControlNode(switchControlNode);

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
		addNewControlNode(switchEntryControlNode);

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

	/**
	 * Registers the data dependencies found in the given condition expression.
	 * @param conditionNode The node where the condition is located.
	 * @param conditionExpression The condition expression.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerConditionDataDependencies(String conditionNode, Expression conditionExpression, ProgramDependencyGraph programDependencyGraph) {
		// A flag to indicate to the visitor methods that we are currently visiting a condition expression.
		this.isPartOfCondition = true;

		this.currentNode = conditionNode;
		super.visit(new ExpressionStmt(conditionExpression), programDependencyGraph);

		this.isPartOfCondition = false;
	}

	/**
	 * Registers the data dependencies of the next iterations of a loop. After visiting a loop, if a variable
	 * is modified inside its body, the dependencies should retroactively added to all the variable references in it.
	 * @param controlNode The loop's control node.
	 * @param loopCondition The loop's condition expression.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerLoopNextIterationDataDependencies(ControlNodePDG controlNode, Expression loopCondition, ProgramDependencyGraph programDependencyGraph) {
		// Get all the variable assignment from this control node and its children.
		Map<String, List<VariableAssignment>> loopAssignments = controlNode.getCurrentBlock().getAssignments();

		// For each variable that has been assigned inside the loop or its children.
		for (String variableName : loopAssignments.keySet())
		{
			ControlNodePDG node = null;

			List<VariableAssignment> variableAssignments = loopAssignments.get(variableName);

			// Get the last variable assignment that was performed.
			VariableAssignment lastAssignment = variableAssignments.get(variableAssignments.size() - 1);

			// From the current node, go backwards adding references to this variable assignment if needed.
			for (int i = this.controlNodes.size() -1; i >= 0; i--)
			{
				node = this.controlNodes.get(i);

				// If there was no assignment of the current variable in this node, continue upwards.
				if (!node.getCurrentBlock().getAssignments().containsKey(variableName))
				{
					continue;
				}

				List<VariableAssignment> assignments = node.getCurrentBlock().getAssignments().get(variableName);
				
				VariableAssignment assignment = null; 
				
				// If the node type is a loop, we want to retrieve its first assignment.
				// That way, we can copy its references and link them to the last assignment from the loop.
				if (node.getType().isLoopType())
				{
					assignment = assignments.get(0);
				}
				else 
				{
					// If it wasn't a loop node, get the last assignment from the variables.
					assignment = assignments.get(assignments.size() -1);
				}

				// Copy the assignment variables from the last assignment in the loop to the references of the first declaration.
				for(String reference : assignment.getReferences())
				{
					addDataDependencyEdges(lastAssignment.getNode(), reference, programDependencyGraph);
				}
			}
		}
	}

	/**
	 * Registers the assignment of a given variable. Depending on the nesting level, it might override other
	 * assignments.
	 * @param variableName The name of the variable.
	 * @param assignmentNode The node where the variable is assigned.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerVariableAssignment(String variableName, String assignmentNode, ProgramDependencyGraph programDependencyGraph) {
		ControlNodePDG currentControlNode = this.controlNodes.peek();

		Map<String, List<VariableAssignment>> assignments = currentControlNode.getCurrentBlock().getAssignments();

		if (!assignments.containsKey(variableName))
		{
			assignments.put(variableName, new ArrayList<VariableAssignment>());
		}

		// Add at the end of the list the new variable assignment. The last assignment will be the only valid one.
		// The others are kept for reference in case they are needed for loops.
		VariableAssignment newAssignment  = new VariableAssignment(variableName, assignmentNode, currentControlNode);
		
		assignments.get(variableName).add(newAssignment);
	}

	/**
	 * Registers a data dependency from the target node to the given variable assignments.
	 * @param sourceVariableAssignments The nodes where the current variable was assigned.
	 * @param targetNode The node which references the variable.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerVariableReference(String variableName, String targetNode, ProgramDependencyGraph programDependencyGraph) {
		ControlNodePDG controlNode = null;

		for (int i = this.controlNodes.size() -1; i >= 0; i--)
		{
			controlNode = this.controlNodes.get(i);

			if (controlNode.getCurrentBlock().getAssignments().containsKey(variableName))
			{
				List<VariableAssignment> assignments = controlNode.getCurrentBlock().getAssignments().get(variableName);

				// Retrieve the last assignment only.
				VariableAssignment variableAssignment = assignments.get(assignments.size() - 1);

				List<String> nodeReferences = variableAssignment.getReferences();

				if (!nodeReferences.contains(targetNode))
				{
					nodeReferences.add(targetNode);

					addDataDependencyEdges(variableAssignment.getNode(), this.currentNode, programDependencyGraph);
				}

				// TODO: Solo salimos si la variable ha sido definida en este nivel o uno superior.
				if (i < this.controlNodes.size() - 1)
				{
					return;
				}
			}
		}
	}

	// Crear arcos
	private void createEdges(String currentNode, ProgramDependencyGraph programDependencyGraph)
	{
		// Register the node and its nesting level if they haven't been registered yet.
		addNode(currentNode, programDependencyGraph);

		addControlEdge(currentNode, programDependencyGraph);
	}

	/**
	 * Registers the given node in the {@link ProgramDependencyGraph}.
	 * @param currentNode The node to register.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void addNode(String currentNode, ProgramDependencyGraph programDependencyGraph) {
		int nestingLevel = this.controlNodes.size();

		if (programDependencyGraph.nodes.containsKey(nestingLevel))
		{
			List<String> nodes = programDependencyGraph.nodes.get(nestingLevel);

			if (!nodes.contains(currentNode))
			{
				nodes.add(currentNode);
			}
		}
		else {
			List<String> nodes = new ArrayList<String>();

			nodes.add(currentNode);

			programDependencyGraph.nodes.put(nestingLevel, nodes);
		}
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
	private void addDataDependencyEdges(String sourceNode, String targetNode, ProgramDependencyGraph programDependencyGraph) {
		System.out.println("DATOS NODO: " + targetNode);

		String edge = sourceNode + "->" + targetNode + "[color=red, constraint = false];";

		if (!programDependencyGraph.dataEdges.contains(edge))
		{
			programDependencyGraph.dataEdges.add(edge);
		}
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
