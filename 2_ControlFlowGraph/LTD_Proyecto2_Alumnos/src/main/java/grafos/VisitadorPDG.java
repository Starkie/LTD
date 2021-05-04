package grafos;

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
