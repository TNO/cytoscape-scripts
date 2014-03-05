package org.el;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.AbstractCySwingApp;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

public class CollapseClusterApp extends AbstractCySwingApp {
	private static final int MIN_NODES = 2;
	private static final String CLUSTER_ATTR = "cluster";
	private static final String[] IGNORE = new String[] { "-1" };
	
	public CollapseClusterApp(CySwingAppAdapter adapter) {
		super(adapter);
		adapter.getCySwingApplication().addAction(new CollapseMenuItem(adapter));
		adapter.getCySwingApplication().addAction(new ResetGroupsMenuItem(adapter));
	}

	public static void resetGroups(CyAppAdapter cyAppAdapter) {
		CyNetwork net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
		CyGroupManager groupManager = cyAppAdapter.getCyGroupManager();

		Set<CyGroup> groups = groupManager.getGroupSet(net);
		//Expand groups
		for(CyGroup g : groups) {
			g.expand(net);
		}
		//Remove groups
		for(CyGroup g : groups) {
			groupManager.destroyGroup(g);
		}
	}

	public static void groupClusters(CyAppAdapter cyAppAdapter) {
		CyNetwork net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
		CyTable table = net.getDefaultNetworkTable();

		String clusterAttr = CLUSTER_ATTR;
		CyColumn clCol = table.getColumn("CollapseCluster.clusterAttr");
		if(clCol != null) {
			List<String> clVal = clCol.getValues(String.class);
			if(clVal.size() > 0) clusterAttr = clCol.getValues(String.class).get(0);
		}

		int minNodes = MIN_NODES;
		CyColumn  mCol = table.getColumn("CollapseCluster.minNodes");
		if(mCol != null) {
			List<Long> mVal = mCol.getValues(Long.class);
			if(mVal.size() > 0) minNodes = mVal.get(0).intValue();
		}

		String[] ignore = IGNORE;
		CyColumn iCol = table.getColumn("CollapseCluster.ignore");
		if(iCol != null) {
			List iVal = iCol.getValues(List.class).get(0);
			if(iVal.size() > 0) ignore = new ArrayList<String>(iVal).toArray(new String[0]);
		}
		
		System.out.println("clusterAttr: " + clusterAttr);
		System.out.println("minNodes: " + minNodes);
		System.out.println("ignore: " + Arrays.asList(ignore));
		
		groupClusters(cyAppAdapter, clusterAttr, minNodes, ignore);
	}

	/**
	 * Group clusters into group nodes and add edges between clusters based on the number
	 * of edges between nodes within the different groups.
	 * @param cyAppAdapter The CyAppAdapter for current cytoscape app
	 * @param clusterAttr The attribute that defines the clusters
	 * @param minNodes Minimum size of the cluster in order to collapse into group node
	 * @param ignore Clusters to ignore (never collapse into group node)
	 */
	public static void groupClusters(CyAppAdapter cyAppAdapter, String clusterAttr, int minNodes, String[] ignore) {
		//		def groupAttr = "cluster";
		//		def ignore = "-1";
		//		def minNodes = 5;

		CyNetwork net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
		CyTable table = net.getDefaultNodeTable();
		CyColumn col = table.getColumn(clusterAttr);

		CyGroupManager groupManager = cyAppAdapter.getCyGroupManager();
		CyGroupFactory groupFactory = cyAppAdapter.getCyGroupFactory();

		System.out.println(table.getColumn("groupName"));
		if(table.getColumn("groupName") == null) {
			table.createColumn("groupName", String.class, false);
		}
		
		// Collect all unique values of group attributes
		List<String> groups = new ArrayList<String>(
				new HashSet<String>(col.getValues(String.class))
				);

		for(String g : groups) {
			// Check how many nodes have this attribute value
			Collection<CyRow> matching = table.getMatchingRows(clusterAttr, g);
			String pk = table.getPrimaryKey().getName();
			List<CyNode> nodes = new ArrayList<CyNode>();
			for(CyRow row : matching) {
				Long nodeId = row.get(pk, Long.class);
				nodes.add(net.getNode(nodeId));
			}
			// If cluster size > 1, group
			if(nodes.size() >= minNodes & Arrays.binarySearch(ignore, g) < 0) {
				System.out.println("Found " + nodes.size() + " nodes for " + clusterAttr + " " + g);

				// Group nodes
				CyGroup cg = groupFactory.createGroup(net, nodes, null, true);
				table.getRow(cg.getGroupNode().getSUID()).set("groupName", g);
			}
		}

		//Connect groups for which internal nodes share an edge
		List<CyGroup> cyGroups = new ArrayList<CyGroup>(groupManager.getGroupSet(net));
		int[][] groupLinkCount = new int[cyGroups.size()][cyGroups.size()];
		
		for(int i = 0; i < (cyGroups.size()-1); i++) {
			CyGroup cg1 = cyGroups.get(i);
			List<CyNode> nodes1 = cg1.getNodeList();
			Set<CyNode> groupNbs = new HashSet<CyNode>();
			//Collect all neighbors of the nodes in the group
			for(CyNode n : nodes1) {
				groupNbs.addAll(
						net.getNeighborList(n, CyEdge.Type.ANY)
				);
			}
			//For each other group, check if it contains any neighbors
			for(int j = i + 1; j < cyGroups.size(); j++) {
				CyGroup cg2 = cyGroups.get(j);
				Set<CyNode> nodes2 = new HashSet<CyNode>(cg2.getNodeList());
				nodes2.retainAll(groupNbs);
				groupLinkCount[i][j] = nodes2.size();
			}
		}
		
		//Collapse the groups
		for(CyGroup cg : cyGroups) {
			cg.collapse(net);
		}
		
		//Add group edges
		CyTable edgeTable = net.getDefaultEdgeTable();
		CyColumn nrCol = edgeTable.getColumn("nrClusterLinks");
		if(nrCol == null) {
			edgeTable.createColumn("nrClusterLinks", Integer.class, false, 0);
		}
		
		for(int i = 0; i < (cyGroups.size()-1); i++) {
			CyGroup cg1 = cyGroups.get(i);
			CyNode gn1 = cg1.getGroupNode();
			for(int j = i + 1; j < cyGroups.size(); j++) {
				CyGroup cg2 = cyGroups.get(j);
				CyNode gn2 = cg2.getGroupNode();
				if(groupLinkCount[i][j] > 0) {
					List<CyEdge> edgeList = net.getConnectingEdgeList(gn1, gn2, CyEdge.Type.ANY);
					CyEdge edge = edgeList.get(0);
					net.getRow(edge).set("nrClusterLinks", groupLinkCount[i][j]);
				}
			}
		}
	}
}
