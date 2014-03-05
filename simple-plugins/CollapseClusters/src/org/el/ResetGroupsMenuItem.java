package org.el;

import java.awt.event.ActionEvent;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;

public class ResetGroupsMenuItem extends AbstractCyAction {
	private final CyAppAdapter adapter;

	public ResetGroupsMenuItem(CyAppAdapter adapter) {
		super("Reset groups",
				adapter.getCyApplicationManager(),
				"network",
				adapter.getCyNetworkViewManager());
		this.adapter = adapter;
		setPreferredMenu("Select");
	}

	public void actionPerformed(ActionEvent e) {
		CollapseClusterApp.resetGroups(adapter);
	}
}
