/*
 *  Copyright (C) 2006  Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.history.DialogHistoryPageSite;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryCompareAdapter;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.spearce.egit.core.GitProvider;
import org.spearce.egit.core.internal.mapping.GitFileRevision;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.internal.actions.GitCompareRevisionAction;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository.StGitPatch;

public class GitHistoryPage extends HistoryPage implements IAdaptable,
		IHistoryCompareAdapter {

	private Composite localComposite;

	private TreeViewer viewer;

	private Tree tree;

	private IFileRevision[] fileRevisions;

	public GitHistoryPage(Object object) {
		setInput(object);
	}

	public boolean inputSet() {
		if (viewer != null)
			viewer.setInput(getInput());
		// TODO Auto-generated method stub
		return true;
	}

	public void createControl(Composite parent) {
		localComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		localComposite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.grabExcessVerticalSpace = true;
		localComposite.setLayoutData(data);

		createTree(localComposite);

		IHistoryPageSite parentSite = getHistoryPageSite();
		if (parentSite != null && parentSite instanceof DialogHistoryPageSite)
			parentSite.setSelectionProvider(viewer);

		final GitCompareRevisionAction compareAction = new GitCompareRevisionAction(
				"Compare");
		final GitCompareRevisionAction compareActionPrev = new GitCompareRevisionAction(
				"Show commit");
		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// update the current
				TreeItem[] selection = tree.getSelection();
				IFileRevision[] selection2 = new IFileRevision[selection.length];
				for (int i = 0; i < selection.length; ++i) {
					selection2[i] = (IFileRevision) selection[i].getData();
				}

				compareAction.setCurrentFileRevision(fileRevisions[0]);
				compareAction.selectionChanged(new StructuredSelection(
						selection2));
				IProject project = ((IResource) getInput()).getProject();
				GitProvider provider = (GitProvider)RepositoryProvider
						.getProvider(project);
				RepositoryMapping repositoryMapping = provider.getData().getRepositoryMapping(project);
				ObjectId parentId = (ObjectId)((GitFileRevision)selection2[0]).getCommit().getParentIds().get(0);
				try {
					if (selection2.length == 1) {
						Commit parent = repositoryMapping.getRepository().mapCommit(parentId);
						IFileRevision previous = new GitFileRevision(parent,
								((GitFileRevision)selection2[0]).getResource(),
								((GitFileRevision)selection2[0]).getCount()+1);
//						compareActionPrev.setCurrentFileRevision(selection2[0]);
						compareActionPrev.setCurrentFileRevision(null);
						compareActionPrev.selectionChanged(new StructuredSelection(new IFileRevision[] {selection2[0], previous}));
					} else {
						compareActionPrev.setCurrentFileRevision(null);
						compareActionPrev.selectionChanged(new StructuredSelection(new IFileRevision[0]));
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		compareAction.setPage(this);
		compareActionPrev.setPage(this);
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(tree);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr) {
				menuMgr.add(compareAction);
				menuMgr.add(compareActionPrev);
			}
		});
		menuMgr.setRemoveAllWhenShown(true);
		tree.setMenu(menu);

		GitHistoryResourceListener resourceListener = new GitHistoryResourceListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceListener, IResourceChangeEvent.POST_CHANGE);

	}

	class GitHistoryResourceListener implements IResourceChangeListener {

		public void resourceChanged(IResourceChangeEvent event) {
			System.out.println("resourceChanged(" + event + ")");
		}

	}

	class GitHistoryLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				int count = ((GitFileRevision) element).getCount();
				if (count < 0)
					return "";
				else
					if (count == 0)
						return "HEAD";
					else
						return "HEAD~"+count;
			}
			
			if (columnIndex == 1) {
				String rss = ((IFileRevision) element).getURI().toString();
				String rs = rss.substring(rss.length()-10);
				String id = ((IFileRevision) element).getContentIdentifier();
				if (appliedPatches!=null) {
					if (!id.equals("Workspace")) {
						StGitPatch patch = (StGitPatch) appliedPatches.get(new ObjectId(id));
						if (patch!=null)
							return patch.getName();
					}
				}
				if (id != null)
					if (id.length() > 9) // make sure "Workspace" is spelled out
						return id.substring(0, 7) + "..";
					else
						return id;
				else
					return id + "@.." + rs;
			}

			if (columnIndex == 2)
				return ""; // TAGS

			if (columnIndex == 3) {
				Date d = new Date(((IFileRevision) element).getTimestamp());
				if (d.getTime() == -1)
					return "";
				return d.toString();
			}
			if (columnIndex == 4)
				return ((IFileRevision) element).getAuthor();

			if (columnIndex == 5) {
				String comment = ((IFileRevision) element).getComment();
				if (comment == null)
					return null;
				int p = comment.indexOf('\n');
				if (p >= 0)
					return comment.substring(0, p);
				else
					return comment;
			}
			return Integer.toString(columnIndex);
		}
	};

	private void createTree(Composite composite) {
		tree = new Tree(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(data);
		tree.setData("HEAD");
		tree.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				try {
					TreeItem item = (TreeItem) event.item;
					Tree parent = item.getParent();
					if (parent == null) {
						item.setText(new String[] { "hej", "san" });
						item.setData("");
					} else {
						ITableLabelProvider p = (ITableLabelProvider) viewer
								.getLabelProvider();
						for (int i = 0; i < 6; ++i) {
							String text = p.getColumnText(
									fileRevisions[event.index], i);
							if (text != null)
								item.setText(i, text);
							else
								item.setText(i, "");
						}
						item.setData(fileRevisions[event.index]);
					}
				} catch (Throwable b) {
					b.printStackTrace();
				}
			}
		});
		TableLayout layout = new TableLayout();
		tree.setLayout(layout);

		viewer = new TreeViewer(tree, SWT.VIRTUAL | SWT.FULL_SELECTION);

		createColumns();

		viewer.setLabelProvider(new GitHistoryLabelProvider());

		viewer.setContentProvider(new GitHistoryContentProvider());

		viewer.setInput(getInput());
	}

	private Map appliedPatches;

	class GitHistoryContentProvider implements ITreeContentProvider,
			ILazyTreeContentProvider {

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput == null)
				return;
			System.out.println("inputChanged(" + viewer + "," + oldInput + ","
					+ newInput);
			IProject project = ((IResource) getInput()).getProject();
			RepositoryProvider provider = RepositoryProvider
					.getProvider(project);
			RepositoryMapping repositoryMapping = ((GitProvider)provider).getData().getRepositoryMapping(project);
			try {
				appliedPatches = null;
				appliedPatches = repositoryMapping.getRepository().getAppliedPatches();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			IFileHistoryProvider fileHistoryProvider = provider
					.getFileHistoryProvider();
			IFileHistory fileHistoryFor = fileHistoryProvider
					.getFileHistoryFor((IResource) getInput(),
							IFileHistoryProvider.SINGLE_LINE_OF_DESCENT, null/* monitor */);
			fileRevisions = fileHistoryFor.getFileRevisions();
			tree.removeAll();
			tree.setItemCount(fileRevisions.length);
			tree.setData(fileRevisions);
			tree.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
			System.out.println("inputchanged, invoking refresh");
			viewer.refresh();
		}

		public void dispose() {
			// TODO Auto-generated method stub
		}

		public Object[] getElements(Object inputElement) {
			System.out.println("getElements(" + inputElement + ")");
			return fileRevisions;
		}

		public boolean hasChildren(Object element) {
			// TODO Auto-generated method stub
			return false;
		}

		public Object getParent(Object element) {
			// System.out.println("getParent(" + element + ")");
			return null;
		}

		public Object[] getChildren(Object parentElement) {
			// System.out.println("getElements(" + parentElement + ")");
			return fileRevisions;
		}

		public void updateChildCount(Object element, int currentChildCount) {
			viewer.setChildCount(element, fileRevisions.length);
		}

		public void updateElement(Object parent, int index) {
			viewer.replace(parent, index, fileRevisions[index]);
		}
	}

	private void createColumns() {
		// X SelectionListener headerListener = getColumnListener(viewer);
		// count from head
		TreeColumn col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText("^");
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(15,
				true));

		// revision
		col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Revision);
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(15,
				true));

		// tags
		col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText("Tags");
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(15,
				true));
		// creation date
		col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_RevisionTime);
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(30,
				true));

		// author
		col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Author);
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(20,
				true));

		// comment
		col = new TreeColumn(tree, SWT.NONE);
		col.setResizable(true);
		col.setText(TeamUIMessages.GenericHistoryTableProvider_Comment);
		// X col.addSelectionListener(headerListener);
		((TableLayout) tree.getLayout()).addColumnData(new ColumnWeightData(35,
				true));
	}

	public Control getControl() {
		return localComposite;
	}

	public void setFocus() {
		localComposite.setFocus();
	}

	public String getDescription() {
		return "GIT History viewer";
	}

	public String getName() {
		return getInput().toString();
	}

	public boolean isValidInput(Object object) {
		// TODO Auto-generated method stub
		return true;
	}

	public void refresh() {
		// TODO Auto-generated method stub

	}

	public Object getAdapter(Class adapter) {
		if (adapter == IHistoryCompareAdapter.class) {
			return this;
		}
		return null;
	}

	public ICompareInput getCompareInput(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	public void prepareInput(ICompareInput input,
			CompareConfiguration configuration, IProgressMonitor monitor) {
		System.out.println("prepareInput()");
		// TODO Auto-generated method stub
	}

}
