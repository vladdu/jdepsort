package jdepsort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.util.CompilationUnitSorter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;
import org.eclipse.jdt.ui.SharedASTProvider;

import com.ibm.icu.text.Collator;

public class TopoSorter implements Comparator<MethodInfo> {
	private final static boolean DEBUG = true;

	private final Collator fCollator;
	private final MembersOrderPreferenceCache fMemberOrderCache;

	public TopoSorter() {
		fCollator = Collator.getInstance();
		fMemberOrderCache = JavaPlugin.getDefault()
				.getMemberOrderPreferenceCache();
	}

	public static List<BodyDeclaration> getSortedMethods(
			ICompilationUnit fCompilationUnit) {
		CompilationUnit ast = createAst(fCompilationUnit);
		return sortMethods(getMethodsWithDependencies(ast));
	}

	private static List<MethodInfo> getMethodsWithDependencies(
			final CompilationUnit ast) {
		final List<MethodInfo> result = new ArrayList<MethodInfo>();
		final List<SimpleName> roots = new ArrayList<SimpleName>();

		ast.accept(new ASTVisitor() {
			MethodInfo crt = null;

			@Override
			public boolean visit(MethodDeclaration node) {
				crt = new MethodInfo(node);
				result.add(crt);
				if ((node.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC) {
					roots.add(node.getName());
				}
				return super.visit(node);
			}

			@Override
			public void endVisit(MethodDeclaration node) {
				crt = null;
				super.endVisit(node);
			}

			@Override
			public boolean visit(MethodInvocation node) {
				if (crt != null) {
					crt.addInvocation(node);
				}
				return super.visit(node);
			}
		});

		MethodInfo root = new MethodInfo(ast.getAST().newMethodDeclaration());
		root.addInvocations(roots);
		result.add(root);

		if (DEBUG) {
			System.out.println("All methods:");
			for (int i = 0; i < result.size(); i++) {
				System.out.println("> " + result.get(i));
			}
			System.out.println("------------");
		}

		return result;
	}

	private static List<BodyDeclaration> sortMethods(List<MethodInfo> methods) {
		Collections.sort(methods, new TopoSorter());

		List<BodyDeclaration> result = new ArrayList<BodyDeclaration>();
		for (MethodInfo d : methods) {
			result.add(d.getDeclaration());
		}
		return result;
	}

	public int compare(MethodInfo m1, MethodInfo m2) {
		MethodDeclaration method1 = m1.getDeclaration();
		MethodDeclaration method2 = m2.getDeclaration();

		if (fMemberOrderCache.isSortByVisibility()) {
			int vis = fMemberOrderCache.getVisibilityIndex(method1
					.getModifiers())
					- fMemberOrderCache.getVisibilityIndex(method2
							.getModifiers());
			if (vis != 0) {
				return vis;
			}
		}

		String name1 = method1.getName().getIdentifier();
		String name2 = method2.getName().getIdentifier();

		// method declarations (constructors) are sorted by name
		int cmp = this.fCollator.compare(name1, name2);
		if (cmp != 0) {
			return cmp;
		}

		// if names equal, sort by parameter types
		List parameters1 = method1.parameters();
		List parameters2 = method2.parameters();
		int length1 = parameters1.size();
		int length2 = parameters2.size();

		int len = Math.min(length1, length2);
		for (int i = 0; i < len; i++) {
			SingleVariableDeclaration param1 = (SingleVariableDeclaration) parameters1
					.get(i);
			SingleVariableDeclaration param2 = (SingleVariableDeclaration) parameters2
					.get(i);
			cmp = this.fCollator.compare(buildSignature(param1.getType()),
					buildSignature(param2.getType()));
			if (cmp != 0) {
				return cmp;
			}
		}
		if (length1 != length2) {
			return length1 - length2;
		}
		return preserveRelativeOrder(method1, method2);
	}

	private int compareNames(BodyDeclaration bodyDeclaration1,
			BodyDeclaration bodyDeclaration2, String name1, String name2) {
		int cmp = this.fCollator.compare(name1, name2);
		if (cmp != 0) {
			return cmp;
		}
		return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
	}

	private String buildSignature(Type type) {
		return ASTNodes.asString(type);
	}

	public static int preserveRelativeOrder(BodyDeclaration bodyDeclaration1,
			BodyDeclaration bodyDeclaration2) {
		int value1 = ((Integer) bodyDeclaration1
				.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
		int value2 = ((Integer) bodyDeclaration2
				.getProperty(CompilationUnitSorter.RELATIVE_ORDER)).intValue();
		return value1 - value2;
	}

	private static CompilationUnit createAst(ICompilationUnit unit) {
		CompilationUnit ast = SharedASTProvider.getAST(unit,
				SharedASTProvider.WAIT_NO, new NullProgressMonitor());
		return ast;
	}
}
