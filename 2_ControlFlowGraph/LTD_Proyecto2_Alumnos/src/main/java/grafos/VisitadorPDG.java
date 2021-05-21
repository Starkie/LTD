package grafos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

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

	HashMap<String, List<VariableAssignment>> variableAssignments = new HashMap<String, List<VariableAssignment>>();
	HashMap<VariableAssignment, List<String>> variableReferences = new HashMap<VariableAssignment, List<String>>();

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

		if (variableAssignments.containsKey(variableName))
		{
			registerVariableReference(variableAssignments.get(variableName), this.currentNode, programDependencyGraph);
		}

		registerVariableAssignment(variableName, programDependencyGraph);

		super.visit(variableDeclarator, programDependencyGraph);
	}

	@Override
	public void visit(NameExpr nameExpr, ProgramDependencyGraph programDependencyGraph) {
		String variableName = nameExpr.getNameAsString();

		// Only add the variable when visiting from another another node from a special type.
		if (variableAssignments.containsKey(variableName)
			&& (this.isInsideAssign || this.isPartOfCondition || this.isParameterOfMethodCall))
		{
			registerVariableReference(variableAssignments.get(variableName), this.currentNode, programDependencyGraph);
		}

		super.visit(nameExpr, programDependencyGraph);
	}

	public void visit(UnaryExpr unaryExpr, ProgramDependencyGraph programDependencyGraph)
	{
		if (unaryExpr.getExpression() instanceof NameExpr)
		{
			NameExpr nameExpr = (NameExpr) unaryExpr.getExpression();

			String variableName = nameExpr.getNameAsString();

			if (this.variableAssignments.containsKey(variableName))
			{
				// Add a data dependency to previous variable definition.
				registerVariableReference(this.variableAssignments.get(variableName), this.currentNode, programDependencyGraph);
			}

			this.registerVariableAssignment(variableName, programDependencyGraph);
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
			if (this.variableAssignments.containsKey(variableName))
			{
				// Add a data dependency to previous variable definition.
				registerVariableReference(this.variableAssignments.get(variableName), this.currentNode, programDependencyGrah);
			}
		}

		this.registerVariableAssignment(variableName, programDependencyGrah);
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
		this.controlNodes.push(ifControlNode);

        registerConditionDataDependencies(ifNode, ifStmt.getCondition(), programDependencyGraph);

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

		ControlNodePDG whileControlNode = new ControlNodePDG(ControlNodeType.WHILE,  whileNode);
		
		visitLoop(whileControlNode, whileStmt.getCondition(), whileStmt.getBody(), programDependencyGraph, null);
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

		// Add the update statements to the end of the body.
		BlockStmt forBody = convertirEnBloque(forStmt.getBody());
		
		List<Statement> updateStatements = forStmt.getUpdate()
				.stream()
				.map(u -> new ExpressionStmt(u))
				.collect(Collectors.toList());

		forBody.getStatements().addAll(updateStatements);
		
		// TODO: Can the Compare be null?
		visitLoop(forControlNode, forStmt.getCompare().get(), forBody, programDependencyGraph, forStmt.getInitialization());
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
		
		visitLoop(foreachControlNode, forEachStmt.getIterable(), forEachStmt.getBody(), programDependencyGraph, null);
	}

	/**
	 * Visits a generic loop given all the required parameters.
	 * @param controlNode The control node that represents the loop.
	 * @param loopCondition The loop's condition expression.
	 * @param loopBody The loop's body statement.
	 * @param programDependencyGraph The program dependency graph.
	 * @param initializationExpressions (Optional) The initialization expressions, if any, for the loop counter variables.
	 */
	private void visitLoop(ControlNodePDG controlNode, Expression loopCondition, Statement loopBody, ProgramDependencyGraph programDependencyGraph, List<Expression> initializationExpressions) 
	{
		// Create the edges from the previous node to the loop.
		createEdges(controlNode.getNode(), programDependencyGraph);

		this.controlNodes.push(controlNode);
		
		// Register the variable references from the condition, if any exists.
		registerConditionDataDependencies(controlNode.getNode(), loopCondition, programDependencyGraph);
		
		// Add the edges for the initialization nodes.
		if (initializationExpressions != null)		
		{
			for (Expression node : initializationExpressions)
			{
				String currentNode = crearNodo(node);
				
				createEdges(currentNode, programDependencyGraph);
			}	
		}

		// Create the edges to the loop's child nodes.
		super.visit(convertirEnBloque(loopBody), programDependencyGraph);
		
		// The variables modified inside the loop are referenced as dependencies for the next iterations.		
		registerLoopNextIterationDataDependencies(controlNode, loopCondition, programDependencyGraph);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
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
		// The variables modified inside the loop are referenced as dependencies for the next iterations.
		
		// Register the data dependencies from the condition again,
		registerConditionDataDependencies(controlNode.getNode(), loopCondition, programDependencyGraph);
		
		// Get the variable assignments declared in the loop node.
		List<VariableAssignment> lastLoopAssignments = this.variableAssignments.values()
			.stream()
			.flatMap(List::stream)
			.filter(va -> this.controlNodes.indexOf(va.getParent()) >= this.controlNodes.indexOf(controlNode)
					|| this.controlNodes.indexOf(va.getParent()) == -1)
			.collect(Collectors.toList());

		for (VariableAssignment referencedVariableAssignment : this.variableReferences.keySet())
		{
			// Check if any variable was re-defined in the loop's body.
			Optional<VariableAssignment> sameVariableAssignment =  lastLoopAssignments.stream()
				.filter(la -> !la.equals(referencedVariableAssignment) && la.getVariableName().equals(referencedVariableAssignment.getVariableName()))
				.findFirst();

			if (sameVariableAssignment.isPresent())
			{
				// Add the corresponding references to the redefined variables.
				for (String referencingNode : this.variableReferences.get(referencedVariableAssignment))
				{
					addDataDependencyEdges(sameVariableAssignment.get().getNode(), referencingNode, programDependencyGraph);
				}
			}
		}
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

		registerConditionDataDependencies(doWhileNode, doStmt.getCondition(), programDependencyGraph);

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

		registerConditionDataDependencies(switchNode, switchStmt.getSelector(), programDependencyGraph);

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
	 * Registers the assignment of a given variable. Depending on the nesting level, it might override other
	 * assignments.
	 * @param variableName The name of the variable.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerVariableAssignment(String variableName, ProgramDependencyGraph programDependencyGraph) {
		// If the variable has not been registered or the current control node is ENTRY.
		if (!this.variableAssignments.containsKey(variableName)
			|| this.controlNodes.size() == 1)
		{
			// This will override all the existing assignments of the current variable, if there was any.
			List<VariableAssignment> nodes = new ArrayList<VariableAssignment>();

			nodes.add(new VariableAssignment(variableName, this.currentNode, this.controlNodes.peek()));

			this.variableAssignments.put(variableName, nodes);
		}
		else
		{
			// Simply append the new assignment to the existing ones.
			// This case appears when there are different branches or scopes at play.
			List<VariableAssignment> assignationNodes = this.variableAssignments.get(variableName);

			assignationNodes.add(new VariableAssignment(variableName, this.currentNode, this.controlNodes.peek()));
		}

	}

	/**
	 * Registers a data dependency from the target node to the given variable assignments.
	 * @param sourceVariableAssignments The nodes where the current variable was assigned.
	 * @param targetNode The node which references the variable.
	 * @param programDependencyGraph The program dependency graph.
	 */
	private void registerVariableReference(List<VariableAssignment> sourceVariableAssignments, String targetNode, ProgramDependencyGraph programDependencyGraph) {
		int currentIndex = GetVariableScopeLevel(sourceVariableAssignments);

		for (VariableAssignment assignment : sourceVariableAssignments) {

			// TODO: Eval 1: 6 x++. El if está desapilado y devuelve -1. ¿Qué hacer?
			// ¿Me guardo el nesting level en el assignment?
			// If the variable assignment is outside the current scope
			if (this.controlNodes.indexOf(assignment.getParent()) < currentIndex
				&& this.controlNodes.indexOf(assignment.getParent()) != -1)
			{
				continue;
			}

			if (this.variableReferences.containsKey(assignment))
			{
				List<String> nodeReferences = this.variableReferences.get(assignment);

				if (!nodeReferences.contains(targetNode))
				{
					nodeReferences.add(targetNode);
				}
			}
			else
			{
				List<String> nodeReferences = new ArrayList<String>();

				nodeReferences.add(targetNode);

				this.variableReferences.put(assignment, nodeReferences);
			}

			addDataDependencyEdges(assignment.getNode(), this.currentNode, programDependencyGraph);
		}
	}

	/**
	 * Gets the minimum nesting level allowed for the assignment.
	 * @param sourceVariableAssignments The nodes where the current variable was assigned.
	 * @return The minimum allowed nesting level.
	 */
	private int GetVariableScopeLevel(List<VariableAssignment> sourceVariableAssignments) {
		int currentIndex = 0;

		for (VariableAssignment assignment : sourceVariableAssignments)
		{
			ControlNodePDG parentControlNode = assignment.getParent();

			int index = this.controlNodes.indexOf(parentControlNode);

			// If the current node is deeper.
			if (index > currentIndex)
			{
				currentIndex = index;
			}
		}

		return currentIndex;
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
