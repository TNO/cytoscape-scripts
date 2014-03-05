def net = cyAppAdapter.getCyApplicationManager().getCurrentNetwork();
def groupManager = cyAppAdapter.getCyGroupManager();
		
def groups = groupManager.getGroupSet(net);
for(g : groups) {
  groupManager.destroyGroup(g);
}
