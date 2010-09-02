package jdepsort;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class DepSortActionDelegate implements IWorkbenchWindowActionDelegate {

	private DepSortAction my_action;

	public void run(IAction action) {
		my_action.run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
		my_action = null;
	}

	public void init(IWorkbenchWindow window) {
		CompilationUnitEditor editor = (CompilationUnitEditor) window
				.getActivePage().getActiveEditor();
		my_action = new DepSortAction(editor);
	}

}
