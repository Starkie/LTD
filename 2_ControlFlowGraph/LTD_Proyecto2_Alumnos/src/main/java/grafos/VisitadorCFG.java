package grafos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import grafos.nodes.ControlNodeCFG;
import grafos.nodes.NodeType;


public class VisitadorCFG extends VoidVisitorAdapter<CFG>
{
	/********************************************************/
	/********************** Atributos ***********************/
	/********************************************************/

	// Usamos un contador para numerar las instrucciones
	int contador=1;
	String nodoAnterior = "Start";
	String nodoActual = "";

	// The collection of control nodes that we are currently analysing.
	// Each control node in the stack represents a nesting level.
	Stack<ControlNodeCFG> controlNodes = new Stack<ControlNodeCFG>();

	// The amount of control nodes that can be unstacked.
	int exitDepth = 0;

	// A flag that indicates that a break expression has been visited.
	boolean breakExpressionVisited = false;

	/********************************************************/
	/*********************** Metodos ************************/
	/********************************************************/

	// Visitador de métodos
	// Este visitador añade el nodo final al CFG
	@Override
	public void visit(MethodDeclaration methodDeclaration, CFG cfg)
	{
	    // Visitamos el método
		super.visit(methodDeclaration, cfg);

		// Añadimos el nodo final al CFG
		cfg.arcos.add(nodoAnterior+"-> Stop;");
	}

	// Visitador de expresiones
	// Cada expresión encontrada genera un nodo en el CFG
	@Override
	public void visit(ExpressionStmt es, CFG cfg)
	{
		// Creamos el nodo actual
		nodoActual = crearNodo(es);

		crearArcos(cfg);

		nodoAnterior = nodoActual;

		// Seguimos visitando...
		super.visit(es, cfg);
	}

	/**
	 * Visits a {@link IfStmt} and registers all the nodes into the {@link CFG}.
	 * @param ifStmt The if statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(IfStmt ifStmt, CFG cfg) {
		String ifNode = crearNodo("if (" + ifStmt.getCondition() + ")");

		// Create the arcs with the previous node.
		this.nodoActual = ifNode;

		crearArcos(cfg);

		// Create the arcs to the 'if' child nodes.
		this.nodoAnterior = ifNode;

		ControlNodeCFG ifControlNode = new ControlNodeCFG(NodeType.IF,  ifNode);
		this.controlNodes.push(ifControlNode);

		// First visit the 'then' statement, that will always be present.
		super.visit(convertirEnBloque(ifStmt.getThenStmt()), cfg);

		// If it is present, also visit the 'else' statement.
		Optional<Statement> elseStmt = ifStmt.getElseStmt();

		if (elseStmt.isPresent())
		{
			// Replace the exit node with a reference to the last node in the 'then' branch.
			// This way, both branches will converge in the instruction after the if statement.
			ifControlNode.getExitNodes().set(0, this.nodoAnterior);

			this.nodoAnterior = ifNode;

			// Set the exit depth to 0, to avoid unstacking control nodes that cannot be unstacked yet.
			int aux = this.exitDepth;
			this.exitDepth = 0;

			super.visit(convertirEnBloque(elseStmt.get()), cfg);

			// Restore the initial exit depth.
			this.exitDepth += aux;
		}

		// Indicate that this control instruction can be removed from the stack.
		this.exitDepth++;
	}

	/**
	 * Visits a {@link WhileStmt} and registers all the nodes into the {@link CFG}.
	 * @param whileStmt The while statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(WhileStmt whileStmt, CFG cfg) {
		String whileNode = crearNodo("while (" + whileStmt.getCondition() + ")");

		ControlNodeCFG whileControlNode = new ControlNodeCFG(NodeType.WHILE,  whileNode);
		this.controlNodes.push(whileControlNode);

		visitLoop(whileStmt.getBody(), cfg, whileNode);

		// Remove the while control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link ForStmt} and registers all the nodes into the {@link CFG}.
	 * @param forStmt The for statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(ForStmt forStmt, CFG cfg) {
		// Add the edges for the initialization nodes.
		for (Expression node : forStmt.getInitialization().toArray(new Expression[0]))
		{
			this.nodoActual = crearNodo(node);

			crearArcos(cfg);

			this.nodoAnterior = this.nodoActual;
		}

		String forNode = crearNodo("for (" + forStmt.getCompare().get() + ")");

		ControlNodeCFG forControlNode = new ControlNodeCFG(NodeType.FOR,  forNode);
		this.controlNodes.push(forControlNode);

		// Add the update statements to the end of the body.
		BlockStmt forBody = convertirEnBloque(forStmt.getBody());

		List<Statement> updateStatements = forStmt.getUpdate()
				.stream()
				.map(u -> new ExpressionStmt(u))
				.collect(Collectors.toList());

		forBody.getStatements().addAll(updateStatements);

		this.visitLoop(forBody, cfg, forNode);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link 	ForeachStmt} and registers all the nodes into the {@link CFG}.
	 * @param forEachStmt The foreach statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(ForeachStmt forEachStmt, CFG cfg) {
		String foreachNode = crearNodo("foreach (" + forEachStmt.getVariable() + " : " + forEachStmt.getIterable() + ")");

		ControlNodeCFG foreachControlNode = new ControlNodeCFG(NodeType.FOREACH,  foreachNode);
		this.controlNodes.push(foreachControlNode);

		visitLoop(forEachStmt.getBody(), cfg, foreachNode);

		// Remove the for control node since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits the given loop and registers all the nodes into the {@link CFG}.
	 * @param loopBody The body of the loop.
	 * @param cfg The control flow graph.
	 * @param loopNode The node that represents the loop.
	 */
	private void visitLoop(Statement loopBody, CFG cfg, String loopNode) {
		// Create the edges from the previous node to the loop.
		this.nodoActual = loopNode;

		crearArcos(cfg);

		this.nodoAnterior = loopNode;

		// Create the edges to the loop's child nodes.
		super.visit(convertirEnBloque(loopBody), cfg);

		// Create the edge from the last node of the body to the loop node.
		this.nodoActual = loopNode;

		crearArcos(cfg);

		// The CFG analysis continues from the loopNode.
		this.nodoAnterior = loopNode;
	}

	/**
	 * Visits a {@link DoStmt} and registers all the nodes into the {@link CFG}.
	 * @param doStmt The do statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(DoStmt doStmt, CFG cfg) {
		ControlNodeCFG doWhileControlNode = new ControlNodeCFG(NodeType.DO,  null);
		this.controlNodes.push(doWhileControlNode);

		// Create the arcs to the 'do' statement child nodes.
		// The first iteration is executed unconditionally.
		super.visit(convertirEnBloque(doStmt.getBody()), cfg);

		String doWhileNode = crearNodo("while (" + doStmt.getCondition() + ")");

		// Create the arcs with the previous node.
		this.nodoActual = doWhileNode;

		crearArcos(cfg);

		// The while statement is the node to continue the CFG analysis.
		this.nodoAnterior = doWhileNode;

		// Create the edges to loop to the first instruction of the body.
		this.nodoActual = doWhileControlNode.getExitNodes().get(0);

		crearArcos(cfg);

		// Remove the while statement from the control nodes, since it is not needed anymore.
		this.controlNodes.pop();
	}

	/**
	 * Visits a {@link SwitchStmt} and registers all the nodes into the {@link CFG}.
	 * @param switchStmt The switch statement.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(SwitchStmt switchStmt, CFG cfg) {
		// Create the edges from the previous node to the switch.
		String switchNode = crearNodo("switch (" + switchStmt.getSelector() + ")");

		this.nodoActual = switchNode;

		crearArcos(cfg);

		// Stack the switch control node.
		ControlNodeCFG switchControlNode = new ControlNodeCFG(NodeType.SWITCH,  null);
		this.controlNodes.push(switchControlNode);

		int aux = 0;

		boolean breakStmtVisited = false;
		this.nodoAnterior = switchNode;

		// Explore each case statement.
		for (SwitchEntryStmt entry : switchStmt.getEntries())
		{
			// Since we are exploring other cases, do not unstack any control nodes from
			// the previous branches.
			aux += this.exitDepth;
			this.exitDepth = 0;

			List<String> previousNodes = new ArrayList<>();

			if (!breakStmtVisited)
			{
				previousNodes.add(this.nodoActual);
			}

			previousNodes.add(switchNode);

			breakStmtVisited = this.visit(entry, cfg, previousNodes);
		}

		// Remove the current node to avoid adding duplicate transitions.
		switchControlNode.getExitNodes().remove(this.nodoActual);

		// Restore the actual exit depth.
		this.exitDepth = aux;

		this.exitDepth++;
	}

	/**
	 * Visits a {@link SwitchEntryStmt} and registers all the nodes into the {@link CFG}.
	 * @param switchEntryStatement The switch entry statement.
	 * @param cfg The control flow graph.
	 * @param previousNodes The previous nodes that can reach this case statement.
	 */
	public boolean visit(SwitchEntryStmt switchEntryStatement, CFG arg, List<String> previousNodes) {
		// If it is a case, add the case label. Otherwise add the default label.
		String switchLabel = switchEntryStatement.getLabel().isPresent()?
				"case " + switchEntryStatement.getLabel().get()
				: "default";

		// Create the edges from the previous node to the switch entry.
		String switchEntryNode = crearNodo(switchLabel);

		this.nodoActual = switchEntryNode;

		// Create the references to the previous nodes.
		for (String node : previousNodes)
		{
			this.nodoAnterior = node;

			this.crearArcos(arg);
		}

		this.nodoAnterior = switchEntryNode;

		// Set the flag of a visited break statement to false.
		this.breakExpressionVisited = false;

		// Store the switch statement control node so we can unstack all the child nodes.
		ControlNodeCFG switchControlNode = this.controlNodes.peek();

		// Visit the switch entry.
		super.visit(switchEntryStatement, arg);

		ControlNodeCFG controlNode = null;

		// Unstack all the control nodes minus the switch.
		for (int i = this.controlNodes.size() - 1; i >= 0; i--)
		{
			controlNode = this.controlNodes.get(i);

			if (controlNode.equals(switchControlNode))
			{
				break;
			}
		}

		// Add the last visited node to the exit nodes of the switch.
		// If a break statement was not visited, it should not be included. It should
		// direct to the next case.
		if (this.breakExpressionVisited)
		{
			switchControlNode.getExitNodes().add(this.nodoActual);
		}

		return this.breakExpressionVisited;
	}

	/**
	 * Visits a {@link BreakStmt} and registers it into the {@link CFG}.
	 * @param breakStmt The break statement to visit.
	 * @param cfg The control flow graph.
	 */
	public void visit(BreakStmt breakStmt, CFG cfg)
	{
		// Activate the flag to notify the switch entry visitor that a break statement was found.
		this.breakExpressionVisited = true;
	}

	/**
	 * Visits a {@link ThrowStmt} and registers it into the {@link CFG}.
	 * @param breakStmt The break statement to visit.
	 * @param cfg The control flow graph.
	 */
	public void visit(ThrowStmt throwsStmt, CFG cfg)
	{
		// Do nothing with the throw statement on the flow, just register it as a expression.
		this.visit(new ExpressionStmt(throwsStmt.getExpression()), cfg);
	}

	// Crear arcos
	private void crearArcos(CFG cfg)
	{
		añadirArcoSecuencialCFG(cfg);

		// Check if the control stack is empty.
		if (this.controlNodes.isEmpty())
		{
			return;
		}

		addDoWhileExitNode();

		// Check if any element from the control stack can be removed.
		if (exitDepth > 0)
		{
			addExitEdgesCFG(cfg);
		}
	}

	/**
	 * Adds the exit node to the do while control stack element.
	 */
	private void addDoWhileExitNode() {
		ControlNodeCFG lastControlNode = this.controlNodes.peek();

		// If the current control node is a Do-While with no exit code, this means that
		// this must be the first instruction of its body.
		if (lastControlNode != null
			&& lastControlNode.getType() == NodeType.DO
			&& lastControlNode.getExitNodes().isEmpty())
		{
			// Because the do-while statement needs to loop back to this instruction,
			// it must be stored for future reference.
			List<String> exitNodes = lastControlNode.getExitNodes();

			exitNodes.add(this.nodoActual);
		}
	}

	// Añade un arco desde el último nodo hasta el nodo actual (se le pasa como parametro)
	private void añadirArcoSecuencialCFG(CFG cfg)
	{
		System.out.println("NODO: " + nodoActual);

		String arco = nodoAnterior + "->" + nodoActual + ";";

		if (!cfg.arcos.contains(arco))
		{
			cfg.arcos.add(arco);
		}
	}

	/**
	 * Adds to the {@link CFG} the edges to the exit nodes of every {@link ControlNodeCFG}
	 * that can be removed.
	 * @param cfg The control flow graph.
	 */
	private void addExitEdgesCFG(CFG cfg) {
		String aux = this.nodoAnterior;

		while (this.exitDepth > 0)
		{
			ControlNodeCFG controlNode = this.controlNodes.pop();

			// Create an edge for each exit node of the control instruction.
			for (String exitNode : controlNode.getExitNodes())
			{
				this.nodoAnterior = exitNode;

				añadirArcoSecuencialCFG(cfg);
			}

			this.exitDepth--;
		}

		// Restore the node that existed before.
		this.nodoAnterior = aux;
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
