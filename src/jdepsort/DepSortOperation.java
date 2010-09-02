/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdepsort;

import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.util.CompilationUnitSorter;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.MembersOrderPreferenceCache;

import com.ibm.icu.text.Collator;

/**
 * Orders topologically methods in a compilation unit. A working copy must be
 * passed.
 */
public class DepSortOperation implements IWorkspaceRunnable {

	/**
	 * Default comparator for body declarations.
	 */
	public static class DefaultJavaElementComparator implements Comparator {

		private final Collator fCollator;
		private final MembersOrderPreferenceCache fMemberOrderCache;

		public DefaultJavaElementComparator() {
			fCollator = Collator.getInstance();
			fMemberOrderCache = JavaPlugin.getDefault()
					.getMemberOrderPreferenceCache();
		}

		/**
		 * This comparator follows the contract defined in
		 * CompilationUnitSorter.sort.
		 * 
		 * @see Comparator#compare(java.lang.Object, java.lang.Object)
		 * @see CompilationUnitSorter#sort(int,
		 *      org.eclipse.jdt.core.ICompilationUnit, int[],
		 *      java.util.Comparator, int,
		 *      org.eclipse.core.runtime.IProgressMonitor)
		 */
		public int compare(Object o1, Object o2) {
			BodyDeclaration bodyDeclaration1 = (BodyDeclaration) o1;
			BodyDeclaration bodyDeclaration2 = (BodyDeclaration) o2;

			if (bodyDeclaration1.getNodeType() == ASTNode.METHOD_DECLARATION) {
				return compareMethods(bodyDeclaration1, bodyDeclaration2);
			} else {
				return preserveRelativeOrder(bodyDeclaration1, bodyDeclaration2);
			}
		}

		private int compareMethods(BodyDeclaration bodyDeclaration1,
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
				cmp = this.fCollator.compare(
						buildSignature(param1.getType()),
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

		private int preserveRelativeOrder(BodyDeclaration bodyDeclaration1,
				BodyDeclaration bodyDeclaration2) {
			int value1 = ((Integer) bodyDeclaration1
					.getProperty(CompilationUnitSorter.RELATIVE_ORDER))
					.intValue();
			int value2 = ((Integer) bodyDeclaration2
					.getProperty(CompilationUnitSorter.RELATIVE_ORDER))
					.intValue();
			return value1 - value2;
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
	}

	private ICompilationUnit fCompilationUnit;
	private int[] fPositions;

	/**
	 * Creates the operation.
	 * 
	 * @param cu
	 *            The working copy of a compilation unit.
	 * @param positions
	 *            Positions to track or <code>null</code> if no positions should
	 *            be tracked.
	 */
	public DepSortOperation(ICompilationUnit cu, int[] positions) {
		fCompilationUnit = cu;
		fPositions = positions;
	}

	/**
	 * Runs the operation.
	 * 
	 * @param monitor
	 *            a monitor to use to report progress
	 * @throws CoreException
	 *             if the compilation unit could not be sorted. Reasons include:
	 *             <ul>
	 *             <li>The given compilation unit does not exist
	 *             (ELEMENT_DOES_NOT_EXIST)</li>
	 *             <li>The given compilation unit is not a working copy
	 *             (INVALID_ELEMENT_TYPES)</li>
	 *             <li>A <code>CoreException</code> occurred while accessing the
	 *             underlying resource
	 *             </ul>
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		CompilationUnitSorter.sort(AST.JLS3, fCompilationUnit, fPositions,
				new DefaultJavaElementComparator(), 0, monitor);
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}
