package jdepsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public final class MethodInfo {

	final private MethodDeclaration declaration;
	final private List<SimpleName> invocations;

	public MethodInfo(MethodDeclaration decl) {
		declaration = decl;
		invocations = new ArrayList<SimpleName>();
	}

	public MethodDeclaration getDeclaration() {
		return declaration;
	}

	public List<SimpleName> getInvocations() {
		return Collections.unmodifiableList(invocations);
	}

	public void addInvocation(MethodInvocation invocation) {
		if (!invocations.contains(invocation) && isLocalInvocation(invocation)) {
			invocations.add(invocation.getName());
		}
	}

	private boolean isLocalInvocation(MethodInvocation invocation) {
		return invocation.getExpression() == null;
	}

	@Override
	public String toString() {
		return String.format("[%s/%s/%d, %s]", declaration.getName(),
				declaration.isConstructor(), declaration.getModifiers(),
				printInvocations());
	}

	private Object printInvocations() {
		StringBuilder b = new StringBuilder();
		b.append("[");
		for (SimpleName i : invocations) {
			b.append(i).append(", ");
		}
		b.append("]");
		return b.toString();
	}

	public void addInvocations(List<SimpleName> roots) {
		invocations.addAll(roots);
	}
}
