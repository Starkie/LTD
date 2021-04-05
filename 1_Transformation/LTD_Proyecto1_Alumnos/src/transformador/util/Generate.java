package transformador.util;

//import com.sun.istack.internal.Nullable;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.util.ArrayList;
import java.util.Collections;

import com.sun.istack.Nullable;

import static transformador.util.Transform.int2e;
import static transformador.util.Transform.string2e;

/** Helper class that creates statements */
public class Generate {
	/** Generates a {@link VariableDeclarationExpr} with a single variable and no initializer
	 * @see #varDecl(Type, String, Expression, Type) */
	public static VariableDeclarationExpr varDecl(Type t, String name) {
		return varDecl(t, name, null);
	}

	/** Generates a {@link VariableDeclarationExpr} with a single variable and an optional initializer
	 * @see #varDecl(Type, String, Expression, Type) */
	public static VariableDeclarationExpr varDecl(Type t, String name, @Nullable Expression init) {
		return varDecl(t, name, init, null);
	}

	/** Generates a {@link VariableDeclarationExpr} with a single variable and an optional initializer, which is
	 * optionally casted to the a given {@link Type}
 	 * @param t      The type of the variable.
	 * @param name   The name of the variable.
	 * @param init   The {@link Expression} to which the variable will be initialized (null for no initialization).
	 * @param castTo The {@link Type} to which the init {@link Expression} will be casted (null for no cast).
	 * @return The expression declaration such variable.
	 */
	public static VariableDeclarationExpr varDecl(Type t, String name, @Nullable Expression init, @Nullable Type castTo) {
		VariableDeclarationExpr expr = new VariableDeclarationExpr(t, new ArrayList<VariableDeclarator>());
		if (castTo != null && init != null)
			init = new CastExpr(castTo, init);
		expr.getVars().add(new VariableDeclarator(new VariableDeclaratorId(name), init));
		return expr;
	}

	/**
	 * Generates an {@link ArrayAccessExpr}, to retrieve the value of a position of such array.
	 * @param name  The name of the array to be accessed.
	 * @param index The position from which the value should be retrieved.
	 * @return The resulting expression.
	 */
	public static ArrayAccessExpr accessArr(String name, int index) {
		return new ArrayAccessExpr(new NameExpr(name), int2e(index));
	}

	/**
	 * Generates a {@link IfStmt} which checks a {@link Throwable} object against a {@link Type}. If it matches, then
	 * the exception is casted to the type and thrown.
	 * @param e The {@link Expression} used to access the {@link Throwable} object.
	 * @param t The {@link Type} to be checked against and then casted.
	 * @return The resulting statement.
	 */
	public static IfStmt ifInstanceOfThenThrow(Expression e, Type t) {
		return new IfStmt(
				new InstanceOfExpr(e, t),
				new ThrowStmt(new CastExpr(t, e)),
				null
		);
	}

	/**
	 * Creates the code to create and throw a {@link RuntimeException}. Mainly used to add warnings on points of failure
	 * on the library that may appear in the execution of the transformed code.
	 * @param message Text that explains the error to the user.
	 * @return A {@link ThrowStmt} of the form {@code throw new RuntimeException("message");}.
	 */
	public static Statement runtimeException(String message) {
		return new ThrowStmt(new ObjectCreationExpr(null,
				new ClassOrInterfaceType("RuntimeException"),
				Collections.singletonList(string2e(message))));
	}

	/**
	 * Generates a catch clause from its components.
	 * @param type   The type of exception to be caught (a class name).
	 * @param var    The name for the variable where the throwable object will be stored.
	 * @param action The statements that should be executed were this clause to catch an exception.
	 * @return The resulting catch clause, ready to be added to a try statement.
	 */
	public static CatchClause catchClause(String type, String var, BlockStmt action) {
		return new CatchClause(new Parameter(new ClassOrInterfaceType(type), new VariableDeclaratorId(var)), action);
	}
}
