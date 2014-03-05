var net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
var groupManager = cyAppAdapter.getCyGroupManager();
		
var groups = groupManager.getGroupSet(net);
for(var it = groups.iterator(); it.hasNext();) {
  var g = it.next();
  groupManager.destroyGroup(g);
}
