package transformador;

import japa.parser.ast.Node;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.visitor.ModifierVisitorAdapter;

public class Visitador_2 extends ModifierVisitorAdapter<Object>
{
	// Reemplaza la condición de los bucles "while" por "true"
	public Node visit(WhileStmt whileStmt, Object args)
	{
		// Creamos el literal "true"
		BooleanLiteralExpr condition = new BooleanLiteralExpr(true);
		
		// Establecemos la condición del while
		whileStmt.setCondition(condition);
		
		return super.visit(whileStmt, args);
	}
}