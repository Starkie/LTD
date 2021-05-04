package grafos;

import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
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

import grafos.nodes.ControlNodeCFG;
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

	/********************************************************/
	/*********************** Metodos ************************/
	/********************************************************/

	// Visitador de métodos
	// Este visitador añade el nodo final al CFG
	@Override
	public void visit(MethodDeclaration methodDeclaration, ProgramDependencyGraph programDependencyGraph)
	{
		this.controlNodes.add(new ControlNodePDG(ControlNodeType.METHOD, "Entry"));

	    // Visitamos el método
		super.visit(methodDeclaration, programDependencyGraph);
	}

	// Visitador de expresiones
	// Cada expresión encontrada genera un nodo en el CFG
	@Override
	public void visit(ExpressionStmt expressionStmt, ProgramDependencyGraph programDependencyGraph)
	{
		// Creamos el nodo actual
		String nodoActual = crearNodo(expressionStmt);

		createEdges(nodoActual, programDependencyGraph);

		// Seguimos visitando...
		super.visit(expressionStmt, programDependencyGraph);
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
		// Add the edges for the initialization nodes.
		for (Expression node : forStmt.getInitialization().toArray(new Expression[0]))
		{
			String currentNode = crearNodo(node);

			createEdges(currentNode, programDependencyGraph);
		}

		String forNode = crearNodo("for (" + forStmt.getCompare().get() + ")");

		// Create the edges from the previous node to the loop.
		createEdges(forNode, programDependencyGraph);

		ControlNodePDG forControlNode = new ControlNodePDG(ControlNodeType.FOR,  forNode);
		this.controlNodes.push(forControlNode);

		// Add the update statements to the end of the body.
		BlockStmt forBody = convertirEnBloque(forStmt.getBody());

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
	 * Visits a {@link 	ForeachStmt} and registers all the nodes into the {@link CFG}.
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
	 * Visits a {@link SwitchEntryStmt} and registers all the nodes into the {@link CFG}.
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
