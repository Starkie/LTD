package transformador;

import japa.parser.ast.Node;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

import java.util.LinkedList;
import java.util.List;

public class Visitador_1 extends ModifierVisitorAdapter<Object>
{
	
	// Cambia los nombres de todos los métodos añadiendo un 2 al final
	public Node visit(MethodDeclaration method, Object args)
	{
		// Añade un 2 al nombre del método
		method.setName(method.getName() + "2");

		// Continúa visitando el cuerpo del método y luego lo devuelve
		return super.visit(method, args);
	}

	// Reemplaza el cuerpo de los bucles "while" que solo tenga una instrucción,
	// por un bloque con esa única instrucción
	public Node visit(WhileStmt whileStmt, Object args)
	{
		// Cogemos el cuerpo del bucle
		Statement body = whileStmt.getBody();
		
		// Si el cuerpo del bucle es un bloque ignoramos este while y visitamos su cuerpo
		if (body instanceof BlockStmt)
			return super.visit(whileStmt, args);

		// Creamos un bloque de instrucciones 
		BlockStmt newBody = new BlockStmt();
		List<Statement> newBodyStmts = new LinkedList<Statement>();

		// Añadimos el cuerpo del bucle (una única instrucción) al bloque
		newBodyStmts.add(body);
		newBody.setStmts(newBodyStmts);
		
		// El nuevo cuerpo del bucle es el bloque creado
		whileStmt.setBody(newBody);

		// Seguimos visitando el cuerpo del bucle
		return super.visit(whileStmt, args);
	}
}