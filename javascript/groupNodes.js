// script javascript /home/thomas/code/cytoscape-scripts/javascript/groupNodes.js

var groupAttr = "cluster";
var ignore = "-1";
var minNodes = 5;

var net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
var table = net.getDefaultNodeTable();
var col = table.getColumn(groupAttr);

var groupManager = cyAppAdapter.getCyGroupManager();
var groupFactory = cyAppAdapter.getCyGroupFactory();
		
// Collect all unique values of group attributes
var groups = new java.util.ArrayList(
  new java.util.HashSet(col.getValues(java.lang.String))
);

for(var i = 0; i < groups.size(); i++) {
  var g = groups.get(i);
  
  // Check how many nodes have this attribute value
  var matching = table.getMatchingRows(groupAttr, g);
  var pk = table.getPrimaryKey().getName();
  var nodes = new java.util.ArrayList();
  for(var it = matching.iterator(); it.hasNext();) {
    var row = it.next();
    var nodeId = row.get(pk, java.lang.Long);
    nodes.add(net.getNode(nodeId));
  }
  // If group size > 1, collapse
  if(nodes.size() >= minNodes & !g.equals(ignore)) {
    println("Found " + nodes.size() + " nodes for " + groupAttr + " " + g);
    
    // Group nodes
		var cyGroup = groupFactory.createGroup(net, nodes, null, true);
		cyGroup.collapse(net);
  }
}

//Connect groups for which internal nodes share an edge
var cyGroups = new java.util.ArrayList(groupManager.getGroupSet(net));
for(var i = 0; i < (cyGroups.size()-1); i++) {
  var cg1 = cyGroups.get(i);
  var gn1 = cg1.getGroupNode();
  var nodes1 = cg1.getNodeList();
  var groupNbs = new java.util.HashSet();
  //Collect all neighbors of the nodes in the group
  for(var it = nodes1.iterator(); it.hasNext();) {
    var n = it.next();
    groupNbs.addAll(
      net.getNeighborList(n, "ANY")
    );
  }
  //For each other group, check if it contains any neighbors
  for(var j = i; j < cyGroups.size(); j++) {
    var cg2 = cyGroups.get(j);
    var gn2 = cg2.getGroupNode();
    var nodes2 = new java.util.HashSet(cg2.getNodeList());
    nodes2.retainAll(groupNbs);
    if(nodes2.size() > 0) {
      var edge = net.addEdge(gn1, gn2, false);
      net.getRow(edge).set("nrLinks", nodes2.size());
    }
  }
}
