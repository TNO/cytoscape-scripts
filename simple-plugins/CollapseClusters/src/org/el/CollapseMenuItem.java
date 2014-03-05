package org.el;

import java.awt.event.ActionEvent;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;

public class CollapseMenuItem extends AbstractCyAction {
	private final CyAppAdapter adapter;

	public CollapseMenuItem(CyAppAdapter adapter) {
		super("Collapse clusters",
				adapter.getCyApplicationManager(),
				"network",
				adapter.getCyNetworkViewManager());
		this.adapter = adapter;
		setPreferredMenu("Select");
	}

	public void actionPerformed(ActionEvent e) {
		CollapseClusterApp.groupClusters(adapter);
	}
}
