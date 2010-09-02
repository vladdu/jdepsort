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
import org.eclipse.jdt.core.util.CompilationUnitSorter;

/**
 * Orders topologically methods in a compilation unit. A working copy must be
 * passed.
 */
public class DepSortOperation implements IWorkspaceRunnable {

	/**
	 * Default comparator for body declarations.
	 */
	public static class DefaultJavaElementComparator implements Comparator {

		private final List fSortedMethods;

		public DefaultJavaElementComparator(List sortedMethods) {
			fSortedMethods = sortedMethods;
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
				return TopoSorter.preserveRelativeOrder(bodyDeclaration1,
						bodyDeclaration2);
			}
		}

		private int compareMethods(BodyDeclaration bodyDeclaration1,
				BodyDeclaration bodyDeclaration2) {
			return fSortedMethods.indexOf(bodyDeclaration1)
					- fSortedMethods.indexOf(bodyDeclaration2);
		}

	}

	private ICompilationUnit fCompilationUnit;
	private int[] fPositions;
	private List fSortedMethods;

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

		fSortedMethods = TopoSorter.getSortedMethods(fCompilationUnit);
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
				new DefaultJavaElementComparator(fSortedMethods), 0, monitor);
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

}
