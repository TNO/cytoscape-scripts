// script groovy /home/thomas/code/cytoscape-scripts/groovy/groupNodes.groovy

import java.util.*;
import org.cytoscape.*;


def groupAttr = "cluster";
def ignore = "-1";
def minNodes = 5;

def net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
def table = net.getDefaultNodeTable();
def col = table.getColumn(groupAttr);

def groupManager = cyAppAdapter.getCyGroupManager();
def groupFactory = cyAppAdapter.getCyGroupFactory();
		
// Collect all unique values of group attributes
def groups = new ArrayList(
  new HashSet(col.getValues(String.class))
);

for(g : groups) {
  // Check how many nodes have this attribute value
  def matching = table.getMatchingRows(groupAttr, g);
  def pk = table.getPrimaryKey().getName();
  def nodes = new ArrayList();
  for(row : matching) {
    def nodeId = row.get(pk, java.lang.Long);
    nodes.add(net.getNode(nodeId));
  }
  // If group size > 1, collapse
  if(nodes.size() >= minNodes & !g.equals(ignore)) {
    println("Found " + nodes.size() + " nodes for " + groupAttr + " " + g);
    
    // Group nodes
		def cyGroup = groupFactory.createGroup(net, nodes, null, true);
		cyGroup.collapse(net);
  }
}

//Connect groups for which internal nodes share an edge
def cyGroups = new ArrayList(groupManager.getGroupSet(net));
for(cg1 : cyGroups) {
  def gn1 = cg1.getGroupNode();
  def nodes1 = cg1.getNodeList();
  def groupNbs = HashSet();
  //Collect all neighbors of the nodes in the group
  for(n : nodes1) {
    groupNbs.addAll(
      net.getNeighborList(n, "ANY")
    );
  }
  //For each other group, check if it contains any neighbors
  for(cg2 : cyGroups) {
    def gn2 = cg2.getGroupNode();
    def nodes2 = new HashSet(cg2.getNodeList());
    nodes2.retainAll(groupNbs);
    if(nodes2.size() > 0) {
      def edge = net.addEdge(gn1, gn2, false);
      net.getRow(edge).set("nrLinks", nodes2.size());
    }
  }
}
