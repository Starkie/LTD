package grafos;

import java.lang.annotation.Inherited;
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
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import grafos.nodes.ControlNode;
import grafos.nodes.ControlNodeType;


public class Visitador extends VoidVisitorAdapter<CFG>
{
	/********************************************************/
	/********************** Atributos ***********************/
	/********************************************************/

	// Usamos un contador para numerar las instrucciones
	int contador=1;
	String nodoAnterior = "Start";
	String nodoActual = "";

	Stack<ControlNode> controlNodes = new Stack<ControlNode>();

	int exitDepth = 0;

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
		String ifNode = crearNodo("if " + ifStmt.getCondition());

		// Create the arcs with the previous node.
		this.nodoActual = ifNode;

		crearArcos(cfg);

		// Create the arcs to the 'if' child nodes.
		this.nodoAnterior = ifNode;

		ControlNode ifControlNode = new ControlNode(ControlNodeType.IF,  ifNode);
		this.controlNodes.push(ifControlNode);

		// First visit the 'then' statement, that will always be present.
		super.visit(convertirEnBloque(ifStmt.getThenStmt()), cfg);

		// If it is present, also visit the 'else' statement.
		Optional<Statement> elseStmt = ifStmt.getElseStmt();

		if (elseStmt.isPresent())
		{
			// Replace the exit node with a reference to the last node in the 'then' branch.
			// This way, both branches will converge in the instruction after the if statement.
			ifControlNode.setExitNode(this.nodoAnterior);

			this.nodoAnterior = ifNode;

			super.visit(convertirEnBloque(elseStmt.get()), cfg);
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
		String whileNode = crearNodo("while " + whileStmt.getCondition());

		ControlNode whileControlNode = new ControlNode(ControlNodeType.WHILE,  whileNode);
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
		
		String forNode = crearNodo("for " + forStmt.getCompare().get());
		
		ControlNode forControlNode = new ControlNode(ControlNodeType.FOR,  forNode);
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
		ControlNode doWhileControlNode = new ControlNode(ControlNodeType.DO,  null);
		this.controlNodes.push(doWhileControlNode);
		
		// Create the arcs to the 'do' statement child nodes.
		// The first iteration is executed unconditionally.
		super.visit(convertirEnBloque(doStmt.getBody()), cfg);
		
		String doWhileNode = crearNodo("while " + doStmt.getCondition());
		
		// Create the arcs with the previous node.
		this.nodoActual = doWhileNode;
		
		crearArcos(cfg);
		
		// The while statement is the node to continue the CFG analysis.
		this.nodoAnterior = doWhileNode;
		
		// Create the edge to loop to the first instruction of the body.
		this.nodoActual = doWhileControlNode.getExitNode();

		crearArcos(cfg);

		// Remove the while statement from the control nodes, since it is not needed anymore.
		this.controlNodes.pop();
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
		ControlNode lastControlNode = this.controlNodes.peek();
		
		// If the current control node is a Do-While with no exit code, this means that
		// this must be the first instruction of its body.
		if (lastControlNode != null
			&& lastControlNode.getType() == ControlNodeType.DO
			&& lastControlNode.getExitNode() == null)
		{
			// Because the do-while statement needs to loop back to this instruction,
			// it must be stored for future reference.
			lastControlNode.setExitNode(this.nodoActual);
		}
	}

	// Añade un arco desde el último nodo hasta el nodo actual (se le pasa como parametro)
	private void añadirArcoSecuencialCFG(CFG cfg)
	{
		System.out.println("NODO: " + nodoActual);

		String arco = nodoAnterior + "->" + nodoActual + ";";
		cfg.arcos.add(arco);
	}

	/**
	 * Adds to the {@link CFG} the edges to the exit nodes of every {@link ControlNode}
	 * that can be removed.
	 * @param cfg The control flow graph.
	 */
	private void addExitEdgesCFG(CFG cfg) {
		String aux = this.nodoAnterior;

		while (this.exitDepth > 0)
		{
			// Workaround for the case where an if-else statement is nested inside the 'then'
			// of another if statement.
			// The edge should be created after the outer if has exited. Otherwise, the exit edge
			// from the inner if will point to the else statement of the outer one.
			if (this.controlNodes.size() > 1
				&& exitDepth == 1
				&& this.controlNodes.peek().getType() == ControlNodeType.IF
				&& this.controlNodes.get(1).getType() == ControlNodeType.IF)
			{
				break;
			}

			ControlNode controlNode = this.controlNodes.pop();

			// Create an edge the exit node of the control instruction.
			this.nodoAnterior = controlNode.getExitNode();

			añadirArcoSecuencialCFG(cfg);

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
