package iter2rec.transformation.loop;

import iter2rec.transformation.Method;
import iter2rec.transformation.Sentence;
import iter2rec.transformation.variable.LoopVariables;
import iter2rec.transformation.variable.Variable;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.*;
import japa.parser.ast.stmt.*;
import japa.parser.ast.type.Type;

import java.util.*;

public abstract class Loop
{
	/********************************************************/
	/************************ Static ************************/
	/********************************************************/
	public static Loop createLoop(Method method, List<Statement> path, Statement loop)
	{
		Statement statement = loop;
		if (statement instanceof LabeledStmt)
			statement = ((LabeledStmt)statement).getStmt();

		if (statement instanceof ForStmt)
			return new For(method, path, loop);
		else if (statement instanceof WhileStmt)
			return new While(method, path, loop);
		else if (statement instanceof DoStmt)
			return new Do(method, path, loop);
		else if (statement instanceof ForeachStmt)
			return new Foreach(method, path, loop);
		throw new RuntimeException("Not contempled yet");
	}
	public static boolean isALoop(Statement statement)
	{
		if (statement instanceof LabeledStmt)
			statement = ((LabeledStmt)statement).getStmt();
		return (statement instanceof ForStmt || statement instanceof WhileStmt || statement instanceof DoStmt || statement instanceof ForeachStmt);
	}

	/********************************************************/
	/************************ Object ************************/
	/********************************************************/
	protected final Method method;
	protected final List<Statement> path;
	protected final Statement loop;
	protected final String name;
	protected LoopVariables loopVariables;
	protected Variable result;

	protected Loop(Method method, List<Statement> path, Statement loop)
	{
		this.method = method;
		this.path = path;
		this.loop = loop;
		// JJJ: Modificado para LTV
		this.name = "";
		// JJJ: Original:	this.name = this.method.getLoopName(loop);
	}

	protected List<Expression> getLoopDeclaredVariables()
	{
		return new LinkedList<Expression>();
	}
	protected List<Expression> getLoopVariables()
	{
		return new LinkedList<Expression>();
	}
	protected List<ImportDeclaration> getImports()
	{
		return new LinkedList<ImportDeclaration>();
	}
	protected void setLoopVariables(List<Variable> loopVariables)
	{
		this.loopVariables = new LoopVariables(loopVariables);
		this.result = this.loopVariables.getResult();
	}

	protected boolean isContained(Statement statement)
	{
		return (this.loop == statement || this.path.contains(statement));
	}
	protected boolean isContainedJosep(Statement statement)
	{
		return this.loop == statement;
	}

	/****************************/
	/******** Variables *********/
	/****************************/
	public List<Variable> getUsedVariables(MethodDeclaration method)
	{
		Variable.clearVariables();
		final List<Variable> declaredVariables = this.getDeclaredVariablesBeforeLoop(method);
		final List<Variable> usedVariables = this.getUsedVariables();

		return this.getVariablesIntersection(declaredVariables, usedVariables);
	}
	protected List<Variable> getDeclaredVariablesBeforeLoop(MethodDeclaration method)
	{
		final List<Variable> variableDeclarations = new LinkedList<Variable>();

		// Parameters of the method
		final List<Parameter> parameters = method.getParameters();
		if (parameters != null)
			for (Parameter parameter : parameters)
			{
				final VariableDeclaratorId variableDeclaratorId = parameter.getId();
				// FIXME: if a parameter is varargs, add 1 dimension of array to the type
				if (parameter.isVarArgs())
					variableDeclarations.add(Variable.createVariable(parameter.getModifiers(), parameter.getType(), variableDeclaratorId.getName(), variableDeclaratorId.getArrayCount() + 1));
				else
					variableDeclarations.add(Variable.createVariable(parameter.getModifiers(), parameter.getType(), variableDeclaratorId.getName(), variableDeclaratorId.getArrayCount()));
			}

		// Body of the method
		final BlockStmt methodBody = method.getBody();
		List<Statement> statements = methodBody.getStmts();

		//JJJ: Este codigo ha sido modificado para LTD
//		while (statements.size() > 0)
//			for (Statement statement : statements)
//			{
//				this.addVariableDeclarations(variableDeclarations, statement);
//				statements = statement == this.loop ? new LinkedList<Statement>() : this.getStatements(statement);
//			}
		//CGJ: LTD
		Deque<Statement> pathToLoop = new ArrayDeque<Statement>();
		if (!findLoop(statements, pathToLoop))
			throw new RuntimeException("Can't find the loop!");
		findVarsBeforeLoop(statements, pathToLoop, variableDeclarations);


		// Loop declarations
		final List<Expression> declaredVariables = this.getLoopDeclaredVariables();
		this.addVariableDeclarations(variableDeclarations, declaredVariables);

		return variableDeclarations;
	}

	protected void findVarsBeforeLoop(List<Statement> statements, Deque<Statement> deque, List<Variable> vars) {
		for (Statement s : statements) {
			// Go to inner statements
			if (s == deque.peekLast()) {
				deque.removeLast();
				if (s == loop)
					return;
				if (s instanceof BlockStmt) {
					findVarsBeforeLoop(((BlockStmt) s).getStmts(), deque, vars);
					return;
				}
				if (s instanceof TryStmt) {
					if (((TryStmt) s).getTryBlock() == deque.peekLast()) {
						findVarsBeforeLoop(((TryStmt) s).getTryBlock().getStmts(), deque, vars);
						return;
					}
					if (((TryStmt) s).getCatchs() != null)
						for (CatchClause cc : ((TryStmt) s).getCatchs())
							if (cc.getCatchBlock() == deque.peekLast()) {
								findVarsBeforeLoop(cc.getCatchBlock().getStmts(), deque, vars);
								return;
							}
					if (((TryStmt) s).getFinallyBlock() == deque.peekLast()) {
						findVarsBeforeLoop(((TryStmt) s).getFinallyBlock().getStmts(), deque, vars);
						return;
					}
					throw new RuntimeException("Can't find path through TryStmt");
				}
				if (s instanceof IfStmt) {
					if (((IfStmt) s).getThenStmt() == deque.peekLast()) {
						findVarsBeforeLoop(Collections.singletonList(((IfStmt) s).getThenStmt()), deque, vars);
						return;
					}
					if (((IfStmt) s).getElseStmt() == deque.peekLast()) {
						findVarsBeforeLoop(Collections.singletonList(((IfStmt) s).getElseStmt()), deque, vars);
						return;
					}
					throw new RuntimeException("Can't find path through IfStmt");
				}
				if (s instanceof SwitchStmt) {
					if (((SwitchStmt) s).getEntries() != null)
						for (SwitchEntryStmt entry : ((SwitchStmt) s).getEntries())
							if (entry == deque.peekLast()) {
								findVarsBeforeLoop(entry.getStmts(), deque, vars);
								return;
							}
					throw new RuntimeException("Can't find path through SwitchStmt");
				}
				if (s instanceof LabeledStmt) {
					findVarsBeforeLoop(Collections.singletonList(((LabeledStmt) s).getStmt()), deque, vars);
					return;
				}
			}
			// Read only variable declarations
			addVariableDeclarations(vars, s);
		}
	}

	protected boolean findLoop(List<Statement> statements, Deque<Statement> deque) {
		if (statements == null) return false;
		for (Statement s : statements) {
			if (s == loop) {
				deque.push(s);
				return true;
			}
			// If block statement, recurse inside each part
			if (s instanceof BlockStmt) {
				deque.push(s);
				if (findLoop(((BlockStmt) s).getStmts(), deque))
					return true;
				deque.pop();
			} else if (s instanceof TryStmt) {
				deque.push(s);
				if (((TryStmt) s).getTryBlock() != null) {
					deque.push(((TryStmt) s).getTryBlock());
					if (findLoop(((TryStmt) s).getTryBlock().getStmts(), deque))
						return true;
					deque.pop();
				}
				if (((TryStmt) s).getCatchs() != null)
					for (CatchClause cc : ((TryStmt) s).getCatchs())
						if (cc.getCatchBlock() != null && cc.getCatchBlock().getStmts() != null) {
							deque.push(cc.getCatchBlock());
							if (findLoop(cc.getCatchBlock().getStmts(), deque))
								return true;
							deque.pop();
						}
				if (((TryStmt) s).getFinallyBlock() != null) {
					deque.push(((TryStmt) s).getFinallyBlock());
					if (findLoop(((TryStmt) s).getFinallyBlock().getStmts(), deque))
						return true;
					deque.pop();
				}
				deque.pop();
			} else if (s instanceof IfStmt) {
				deque.push(s);
				if (((IfStmt) s).getThenStmt() != null) {
					if (findLoop(Collections.singletonList(((IfStmt) s).getThenStmt()), deque))
						return true;
				}
				if (((IfStmt) s).getElseStmt() != null) {
					if (findLoop(Collections.singletonList(((IfStmt) s).getElseStmt()), deque))
						return true;
				}
				deque.pop();
			} else if (s instanceof SwitchStmt) {
				deque.push(s);
				if (((SwitchStmt) s).getEntries() != null)
					for (SwitchEntryStmt entry : ((SwitchStmt) s).getEntries()) {
						deque.push(entry);
						if (entry.getStmts() != null && findLoop(entry.getStmts(), deque))
							return true;
						deque.pop();
					}
				deque.pop();
			} else if (s instanceof LabeledStmt) {
				deque.push(s);
				if (((LabeledStmt) s).getStmt() != null && findLoop(Collections.singletonList(((LabeledStmt) s).getStmt()), deque))
					return true;
				deque.pop();
			}
		}
		return false;
	}

	protected List<Statement> getStatements(Statement statement)
	{
		final List<Statement> statements = Sentence.getStatements(statement);
		if (statements.size() > 0)
			return statements;

		final List<Statement> bifurcationStatements = Sentence.getBifurcationStatements(statement);
		//JJJ: Este codigo ha sido comentado para LTD
		//final Iterator<Statement> iterator = bifurcationStatements.iterator();

		//while (iterator.hasNext())
		//	if (!this.isContained(iterator.next()))
		//		iterator.remove();

		return bifurcationStatements;
	}
	protected List<Variable> getUsedVariables()
	{
		int numHijoAVisitar = 0;
		final LinkedList<Statement> path = new LinkedList<Statement>();
		final List<Integer> bifurcaciones = new LinkedList<Integer>();

		final List<Variable> usedVariables = new LinkedList<Variable>();
		this.addVariables(usedVariables, this.getLoopVariables());
		Statement root = this.loop;
		Statement statement = root;

		do
		{
			final List<Statement> statements = Sentence.getAllStatements(statement);
			int numeroDeHijos = statements.size();

			// Evaluar nodo
			if (numHijoAVisitar == 0)
			{
				final List<Expression> expressions = this.getExpressions(statement);
				this.addVariables(usedVariables, expressions);
			}

			if (numHijoAVisitar == numeroDeHijos)
			{
				// Ir al padre
				// Si es la raiz terminar directamente
				if (statement == root)
					break;
				else
				{
					// Si no, volver al padre
					statement = path.removeLast();
					numHijoAVisitar = bifurcaciones.remove(bifurcaciones.size() - 1);
				}
			}
			else
			{
				// Ir al hijo
				path.add(statement);
				statement = statements.get(numHijoAVisitar);
				bifurcaciones.add(numHijoAVisitar + 1);
				numHijoAVisitar = 0;
			}
		}
		while (true);

		return usedVariables;
	}
	protected List<Variable> getVariablesIntersection(List<Variable> variables1, List<Variable> variables2)
	{
		final Iterator<Variable> iterator = variables1.iterator();

		while (iterator.hasNext())
		{
			boolean found = false;
			final Variable variable = iterator.next();

			for (Variable usedVariable : variables2)
				if (usedVariable.sameVariable(variable))
				{
					found = true;
					break;
				}
			if (!found)
				iterator.remove();
		}

		return variables1;
	}
	protected void addVariableDeclarations(List<Variable> variableDeclarations, Statement statement)
	{
		if (!(statement instanceof ExpressionStmt))
			return;

		final ExpressionStmt expressionStmt = (ExpressionStmt)statement;
		final Expression expression = expressionStmt.getExpression();
		this.addVariableDeclarations(variableDeclarations, expression);
	}
	protected void addVariableDeclarations(List<Variable> variableDeclarations, List<Expression> expressions)
	{
		for (Expression expression : expressions)
			this.addVariableDeclarations(variableDeclarations, expression);
	}
	protected void addVariableDeclarations(List<Variable> variableDeclarations, Expression expression)
	{
		if (expression instanceof VariableDeclarationExpr)
			variableDeclarations.addAll(this.getVariables((VariableDeclarationExpr)expression));
	}
	protected void addVariables(List<Variable> variables, List<Expression> expressions)
	{
		List<Expression> newExpressions = expressions;
		do
		{
			expressions = newExpressions;
			newExpressions = new LinkedList<Expression>();
			for (Expression insideExpression : expressions)
				if (insideExpression instanceof NameExpr)
				{
					final String name = ((NameExpr)insideExpression).getName();
					final Variable newVariable = Variable.createVariable(0, name);
					if (!variables.contains(newVariable))
						variables.add(newVariable);
				}
				else if (insideExpression instanceof VariableDeclarationExpr)
					variables.addAll(this.getVariables((VariableDeclarationExpr)insideExpression));
				else
					newExpressions.addAll(this.getExpressions(insideExpression));
		}
		while (newExpressions.size() > 0);
	}
	protected List<Variable> getVariables(VariableDeclarationExpr variableDeclarationExpr)
	{
		final List<Variable> variables = new LinkedList<Variable>();
		final int modifiers = variableDeclarationExpr.getModifiers();
		final Type type = variableDeclarationExpr.getType();

		for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVars())
		{
			final VariableDeclaratorId variableDeclaratorId = variableDeclarator.getId();
			variables.add(Variable.createVariable(modifiers, type, variableDeclaratorId.getName(), variableDeclaratorId.getArrayCount()));
		}

		return variables;
	}

	/****************************/
	/******** Expression ********/
	/****************************/
	protected List<Expression> getExpressions(Statement statement)
	{
		final List<Expression> expressions = new LinkedList<Expression>();

		if (statement instanceof AssertStmt)
		{
			final AssertStmt assertStmt = (AssertStmt)statement;
			expressions.add(assertStmt.getCheck());
			expressions.add(assertStmt.getMessage());
		}
		else if (statement instanceof BlockStmt) ;
		else if (statement instanceof BreakStmt) ;
		else if (statement instanceof ContinueStmt) ;
		else if (statement instanceof DoStmt)
		{
			final DoStmt doStmt = (DoStmt)statement;
			expressions.add(doStmt.getCondition());
		}
		else if (statement instanceof EmptyStmt) ;
		else if (statement instanceof ExplicitConstructorInvocationStmt)
		{
			final ExplicitConstructorInvocationStmt emptyStmt = (ExplicitConstructorInvocationStmt)statement;
			expressions.add(emptyStmt.getExpr());
		}
		else if (statement instanceof ExpressionStmt)
		{
			final ExpressionStmt expressionStmt = (ExpressionStmt)statement;
			expressions.add(expressionStmt.getExpression());
		}
		else if (statement instanceof ForeachStmt)
		{
			final ForeachStmt foreachStmt = (ForeachStmt)statement;
			expressions.add(foreachStmt.getIterable());
		}
		else if (statement instanceof ForStmt)
		{
			List<Expression> forExpressions;
			final ForStmt forStmt = (ForStmt)statement;
			forExpressions = forStmt.getInit();
			if (forExpressions != null)
				expressions.addAll(forExpressions);
			expressions.add(forStmt.getCompare());
			forExpressions = forStmt.getUpdate();
			if (forExpressions != null)
				expressions.addAll(forExpressions);
		}
		else if (statement instanceof IfStmt)
		{
			final IfStmt ifStmt = (IfStmt)statement;
			expressions.add(ifStmt.getCondition());
		}
		else if (statement instanceof LabeledStmt) ;
		else if (statement instanceof ReturnStmt)
		{
			final ReturnStmt returnStmt = (ReturnStmt)statement;
			expressions.add(returnStmt.getExpr());
		}
		else if (statement instanceof SwitchEntryStmt)
		{
			final SwitchEntryStmt switchEntryStmt = (SwitchEntryStmt)statement;
			expressions.add(switchEntryStmt.getLabel());
		}
		else if (statement instanceof SwitchStmt)
		{
			final SwitchStmt switchStmt = (SwitchStmt)statement;
			expressions.add(switchStmt.getSelector());
		}
		else if (statement instanceof SynchronizedStmt)
		{
			final SynchronizedStmt synchronizedStmt = (SynchronizedStmt)statement;
			expressions.add(synchronizedStmt.getExpr());
		}
		else if (statement instanceof ThrowStmt)
		{
			final ThrowStmt throwStmt = (ThrowStmt)statement;
			expressions.add(throwStmt.getExpr());
		}
		else if (statement instanceof TryStmt) ;
		else if (statement instanceof ThrowStmt) ;
		else if (statement instanceof WhileStmt)
		{
			final WhileStmt whileStmt = (WhileStmt)statement;
			expressions.add(whileStmt.getCondition());
		}

		return expressions;
	}
	protected List<Expression> getExpressions(Expression expression)
	{
		final List<Expression> expressions = new LinkedList<Expression>();

		if (expression instanceof AnnotationExpr)
		{
			final AnnotationExpr annotationExpr = (AnnotationExpr)expression;
			expressions.add(annotationExpr.getName());
		}
		else if (expression instanceof ArrayAccessExpr)
		{
			final ArrayAccessExpr arrayAccessExpr = (ArrayAccessExpr)expression;
			expressions.add(arrayAccessExpr.getName());
			expressions.add(arrayAccessExpr.getIndex());
		}
		else if (expression instanceof ArrayCreationExpr)
		{
			final ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr)expression;
			expressions.add(arrayCreationExpr.getInitializer());
			expressions.addAll(arrayCreationExpr.getDimensions());
		}
		else if (expression instanceof ArrayInitializerExpr)
		{
			final ArrayInitializerExpr arrayCreationExpr = (ArrayInitializerExpr)expression;
			expressions.addAll(arrayCreationExpr.getValues());
		}
		else if (expression instanceof AssignExpr)
		{
			final AssignExpr assignExpr = (AssignExpr)expression;
			expressions.add(assignExpr.getTarget());
			expressions.add(assignExpr.getValue());
		}
		else if (expression instanceof BinaryExpr)
		{
			final BinaryExpr binaryExpr = (BinaryExpr)expression;
			expressions.add(binaryExpr.getLeft());
			expressions.add(binaryExpr.getRight());
		}
		else if (expression instanceof CastExpr)
		{
			final CastExpr castExpr = (CastExpr)expression;
			expressions.add(castExpr.getExpr());
		}
		else if (expression instanceof ClassExpr) ;
		else if (expression instanceof ConditionalExpr)
		{
			final ConditionalExpr conditionalExpr = (ConditionalExpr)expression;
			expressions.add(conditionalExpr.getCondition());
			expressions.add(conditionalExpr.getThenExpr());
			expressions.add(conditionalExpr.getElseExpr());
		}
		else if (expression instanceof EnclosedExpr)
		{
			final EnclosedExpr enclosedExpr = (EnclosedExpr)expression;
			expressions.add(enclosedExpr.getInner());
		}
		else if (expression instanceof FieldAccessExpr)
		{
			final FieldAccessExpr fieldAccessExpr = (FieldAccessExpr)expression;
			expressions.add(fieldAccessExpr.getScope());
		}
		else if (expression instanceof InstanceOfExpr)
		{
			final InstanceOfExpr instanceOfExpr = (InstanceOfExpr)expression;
			expressions.add(instanceOfExpr.getExpr());
		}
		else if (expression instanceof LiteralExpr) ;
		else if (expression instanceof MarkerAnnotationExpr)
		{
			final MarkerAnnotationExpr markerAnnotationExpr = (MarkerAnnotationExpr)expression;
			expressions.add(markerAnnotationExpr.getName());
		}
		else if (expression instanceof MethodCallExpr)
		{
			final MethodCallExpr methodCallExpr = (MethodCallExpr)expression;
			expressions.add(methodCallExpr.getScope());
			List<Expression> args = methodCallExpr.getArgs();
			if (args != null)
				expressions.addAll(args);
		}
		else if (expression instanceof NameExpr) ;
		else if (expression instanceof NormalAnnotationExpr)
		{
			final NormalAnnotationExpr normalAnnotationExpr = (NormalAnnotationExpr)expression;
			expressions.add(normalAnnotationExpr.getName());
		}
		else if (expression instanceof ObjectCreationExpr)
		{
			final ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr)expression;
			expressions.add(objectCreationExpr.getScope());
			// FIXME: the argument list may be null!!!
			if (objectCreationExpr.getArgs() != null)
				expressions.addAll(objectCreationExpr.getArgs());
		}
		else if (expression instanceof QualifiedNameExpr)
		{
			final QualifiedNameExpr qualifiedNameExpr = (QualifiedNameExpr)expression;
			expressions.add(qualifiedNameExpr.getQualifier());
		}
		else if (expression instanceof SingleMemberAnnotationExpr)
		{
			final SingleMemberAnnotationExpr singleMemberAnnotationExpr = (SingleMemberAnnotationExpr)expression;
			expressions.add(singleMemberAnnotationExpr.getMemberValue());
			expressions.add(singleMemberAnnotationExpr.getName());
		}
		else if (expression instanceof SuperExpr)
		{
			final SuperExpr superExpr = (SuperExpr)expression;
			expressions.add(superExpr.getClassExpr());
		}
		else if (expression instanceof ThisExpr)
		{
			final ThisExpr thisExpr = (ThisExpr)expression;
			expressions.add(thisExpr.getClassExpr());
		}
		else if (expression instanceof UnaryExpr)
		{
			final UnaryExpr unaryExpr = (UnaryExpr)expression;
			expressions.add(unaryExpr.getExpr());
		}
		else if (expression instanceof VariableDeclarationExpr)
		{
			final VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr)expression;
			expressions.addAll(variableDeclarationExpr.getAnnotations());
		}

		return expressions;
	}

	/****************************/
	/********* Transform ********/
	/****************************/
	protected abstract Expression getCondition();
	protected abstract List<Statement> getStatements();

	public List<Method> transformLoop()
	{
		// Information of the loop
		final MethodDeclaration method = this.method.getMethod();
		final CompilationUnit cu = this.method.getCompilationUnit();
		final TypeDeclaration type = this.method.getType();
		final List<Variable> loopVariables = this.getUsedVariables(method);
		final int modifiers = this.getModifiers();
		final String typeName = ModifierSet.hasModifier(modifiers, ModifierSet.STATIC) ? type.getName() : null;

		// Replace loop
		this.setLoopVariables(loopVariables);
		this.replaceLoop(this.createRecursiveCaller(typeName));
		final List<MethodDeclaration> recursiveMethods = this.createRecursiveMethods(typeName);

		// Add imports
		final List<ImportDeclaration> newImports = this.getImports();
		if (newImports.size() > 0)
		{
			final List<ImportDeclaration> imports = cu.getImports() == null ? new LinkedList<ImportDeclaration>() : cu.getImports();
			newImports.removeAll(imports);
			imports.addAll(newImports);
			cu.setImports(imports);
		}

		// Add methods
		final List<Method> methods = new LinkedList<Method>();
		for (MethodDeclaration recursiveMethod : recursiveMethods)
		{
			recursiveMethod.setModifiers(modifiers);
			type.getMembers().add(recursiveMethod);
			methods.add(new Method(cu, type, recursiveMethod));
		}

		return methods;
	}
	protected int getModifiers()
	{
		int modifiers = this.method.getMethod().getModifiers();

		if (ModifierSet.hasModifier(modifiers, ModifierSet.PUBLIC))
		{
			modifiers = ModifierSet.removeModifier(modifiers, ModifierSet.PUBLIC);
			modifiers = ModifierSet.addModifier(modifiers, ModifierSet.PRIVATE);
		}

		return modifiers;
	}
	protected void replaceLoop(BlockStmt block)
	{
		final Statement parent = this.path.get(this.path.size() - 1);

		if (parent instanceof BlockStmt)
		{
			final BlockStmt blockStmt = ((BlockStmt) parent);
			final List<Statement> statements = blockStmt.getStmts();
			int index = statements.indexOf(this.loop);
			statements.set(index, block);
		}
		else if (parent instanceof IfStmt)
		{
			final IfStmt ifStmt = ((IfStmt) parent);
			if (ifStmt.getThenStmt() == this.loop)
				ifStmt.setThenStmt(block);
			else if (ifStmt.getElseStmt() == this.loop)
				ifStmt.setElseStmt(block);
		}
		else
			throw new RuntimeException("Not contempled yet");
	}
	protected BlockStmt createRecursiveCaller(String typeName)
	{
		// Block
		final BlockStmt blockStmt = new BlockStmt();
		final List<Statement> blockStmts = new LinkedList<Statement>();
		final ExpressionStmt methodCallStmt = new ExpressionStmt();
		final MethodCallExpr methodCall = this.getRecursiveCallExpr(typeName);

		blockStmt.setStmts(blockStmts);
		blockStmts.add(methodCallStmt);

		if (this.result != null)
		{
			// Object[] result = loop(...)
			if (this.loopVariables.hasBridgeVariable())
			{
				methodCallStmt.setExpression(this.result.getDeclarationExpr(methodCall));
				// ... = (cast)result[0]
				for (Statement statement : this.loopVariables.getCastings(this.result))
					blockStmts.add(statement);
			}
			else
				methodCallStmt.setExpression(this.result.getAssignationExpr(methodCall));
		}
		else
			methodCallStmt.setExpression(methodCall);

		return blockStmt;
	}
	protected List<MethodDeclaration> createRecursiveMethods(String typeName)
	{
		final List<MethodDeclaration> methods = new LinkedList<MethodDeclaration>();

		methods.add(this.createRecursiveMethod(typeName));

		return methods;
	}
	protected MethodDeclaration createRecursiveMethod(String typeName)
	{
		final MethodDeclaration method = new MethodDeclaration();
		method.setName(this.name);
		method.setType(this.loopVariables.getReturnType());

		// Parameters
		final List<Parameter> parameters = new LinkedList<Parameter>();

		method.setParameters(parameters);
		for (Parameter parameter : this.loopVariables.getParameters())
			parameters.add(parameter);

		// Body
		final BlockStmt bodyBlock = new BlockStmt();
		final List<Statement> blockStmts = this.getStatements();

		method.setBody(bodyBlock);
		bodyBlock.setStmts(blockStmts);

		// if (coondition)
		// return loop(...)
		final IfStmt ifStmt = new IfStmt();

		blockStmts.add(ifStmt);
		ifStmt.setCondition(this.getCondition());
		ifStmt.setThenStmt(new ReturnStmt(this.getRecursiveCallExpr(typeName)));

		// return new Object[]{}
		blockStmts.add(new ReturnStmt(this.loopVariables.getReturnExpr()));

		return method;
	}
	protected MethodCallExpr getRecursiveCallExpr(String typeName)
	{
		final MethodCallExpr methodCall = new MethodCallExpr();

		if (typeName != null)
			methodCall.setScope(new NameExpr(typeName));
		methodCall.setName(this.name);
		methodCall.setArgs(this.loopVariables.getArgs());

		return methodCall;
	}
}