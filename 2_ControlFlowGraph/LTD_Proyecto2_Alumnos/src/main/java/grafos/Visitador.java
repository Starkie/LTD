package grafos;

import java.lang.annotation.Inherited;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
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
			List<String> exitNodes = ifControlNode.getExitNodes();
			exitNodes.set(0, this.nodoAnterior);

			this.nodoAnterior = ifNode;

			super.visit(convertirEnBloque(elseStmt.get()), cfg);
		}
		
		// Indicate that this control instruction can be removed from the stack.
		this.exitDepth++;
	}
	

	// Crear arcos
	private void crearArcos(CFG cfg)
	{
		añadirArcoSecuencialCFG(cfg);
		
		// Check if any instruction can be exited from the stack.
		if (exitDepth > 0)
		{
			addExitEdgesCFG(cfg);
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

			// Create an edge for each exit node of the control instruction.
			for (String exitNode : controlNode.getExitNodes())
			{
				this.nodoAnterior = exitNode;
	
				añadirArcoSecuencialCFG(cfg);
			}

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
