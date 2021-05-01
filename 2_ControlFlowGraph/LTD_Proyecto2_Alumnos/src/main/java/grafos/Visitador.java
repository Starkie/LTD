package grafos;

import java.lang.annotation.Inherited;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
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
	 * Visits a {@link IfStmt} and registers all the nodes in the {@link CFG}.
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
	 * Visits a {@link WhileStmt} and registers all the nodes in the {@link CFG}.
	 * @param whileStmt The while statement to visit.
	 * @param cfg The control flow graph.
	 */
	@Override
	public void visit(WhileStmt whileStmt, CFG cfg) {
		String whileNode = crearNodo("while " + whileStmt.getCondition());

		// Create the arcs with the previous node.
		this.nodoActual = whileNode;

		crearArcos(cfg);

		this.nodoAnterior = whileNode;

		ControlNode whileControlNode = new ControlNode(ControlNodeType.WHILE,  whileNode);
		this.controlNodes.push(whileControlNode);

		// Create the arcs to the 'while' child nodes.
		super.visit(convertirEnBloque(whileStmt.getBody()), cfg);

		// Create the arc from the last node of the body to the while statement.
		this.nodoActual = whileNode;

		crearArcos(cfg);

		// Remove the while statement from the control nodes, since it is not needed anymore.
		this.controlNodes.pop();

		// The while statement is the node to continue the CFG analysis.
		this.nodoAnterior = whileNode;
	}
	
	/**
	 * Visits a {@link DoStmt} and registers all the nodes in the {@link CFG}.
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
		while (this.exitDepth > 0)
		{
			ControlNode controlNode = this.controlNodes.pop();

			// Create an edge the exit node of the control instruction.
			this.nodoAnterior = controlNode.getExitNode();

			añadirArcoSecuencialCFG(cfg);

			this.exitDepth--;
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
