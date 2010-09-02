package jdepsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public final class MethodInfo {

	final private MethodDeclaration declaration;
	final private List<MethodInvocation> invocations;

	public MethodInfo(MethodDeclaration decl) {
		declaration = decl;
		invocations = new ArrayList<MethodInvocation>();
	}

	public MethodDeclaration getDeclaration() {
		return declaration;
	}

	public List<MethodInvocation> getInvocations() {
		return Collections.unmodifiableList(invocations);
	}

	public void addInvocation(MethodInvocation invocation) {
		if (!invocations.contains(invocation) && isLocalInvocation(invocation)) {
			invocations.add(invocation);
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
		for (MethodInvocation i : invocations) {
			b.append(i.getName()).append(", ");
		}
		b.append("]");
		return b.toString();
	}
}
