package naming;


/**
 * @author hira.yasin This is an abstract class which is extended by the Leaf
 *         and a Branch. My tree is made up of Nodes.
 */
public abstract class Node {

	
	private MyLock m = new MyLock();
	private Branch parent;
	private String name;

	public Node(Branch b, String name) {
		
		if ((name.equals("") && b == null)) {
			throw new IllegalArgumentException("Not good arguments");
		}

		this.name = name;
		this.parent = b;

	}
	
	public MyLock getLock(){
		return this.m;
	}

	/**
	 * @return true if this is Directory. False otherwise
	 */
	public abstract boolean isDirectory();

}
