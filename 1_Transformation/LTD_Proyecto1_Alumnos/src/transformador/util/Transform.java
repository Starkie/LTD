package transformador.util;

import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.Type;

import java.util.LinkedList;
import java.util.List;

/** Helper class that transforms nodes into other elements. */
public class Transform {
	/** Converts an {@link Expression} into a {@link Statement} ({@link ExpressionStmt}). */
	public static Statement e2s(Expression e) {
		return new ExpressionStmt(e);
	}

	/** Given a {@link Type}, it obtains its object type if it was primitive, else returns the same object. */
	public static Type getWrapper(Type type)
	{
		if (!(type instanceof PrimitiveType))
			return type;

		PrimitiveType primitiveType = (PrimitiveType) type;
		String wrapperName = primitiveType.getType().name();

		if (wrapperName.equals("Int"))
			wrapperName = "Integer";
		else if (wrapperName.equals("Char"))
			wrapperName = "Character";

		return new ClassOrInterfaceType(wrapperName);
	}

	/** Given a {@link Statement}, it wraps it in a {@link BlockStmt} if it isn't one already. */
	public static BlockStmt blockWrapper(Statement statement)
	{
		if (statement == null) return null;
		if (statement instanceof BlockStmt)
			return (BlockStmt) statement;

		BlockStmt block = new BlockStmt();
		List<Statement> blockStmts = new LinkedList<Statement>();
		blockStmts.add(statement);

		block.setStmts(blockStmts);

		return block;
	}

	/** Generates an {@link IntegerLiteralExpr} from an {@code int} */
	public static Expression int2e(int i) {
		return new IntegerLiteralExpr(String.valueOf(i));
	}

	/** Generates a {@link StringLiteralExpr} from a {@link String} object */
	public static Expression string2e(String s) {
		return new StringLiteralExpr(s);
	}

	/** Generates a {@link BooleanLiteralExpr} from a {@code bool} object */
	public static Expression boolean2e(boolean b) {
		return new BooleanLiteralExpr(b);
	}
}
