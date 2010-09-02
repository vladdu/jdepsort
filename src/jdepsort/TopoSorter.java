package jdepsort;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.util.CompilationUnitSorter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;

import com.ibm.icu.text.Collator;

public class TopoSorter {
	private final Collator fCollator;
	private final MembersOrderPreferenceCache fMemberOrderCache;

	public TopoSorter() {
		fCollator = Collator.getInstance();
		fMemberOrderCache = JavaPlugin.getDefault()
				.getMemberOrderPreferenceCache();
	}

	public static List getSortedMethods(ICompilationUnit fCompilationUnit) {
		return null;
	}

	private int compare(BodyDeclaration bodyDeclaration1,
			BodyDeclaration bodyDeclaration2) {

		MethodDeclaration method1 = (MethodDeclaration) bodyDeclaration1;
		MethodDeclaration method2 = (MethodDeclaration) bodyDeclaration2;

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
		return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
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
}
