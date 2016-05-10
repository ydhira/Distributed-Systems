package naming;

import java.util.HashMap;

/**
 * @author hira.yasin This is part of the tree. Branch represents a Directory in
 *         my tree.
 */
public class Branch extends Node {

	// these are the children of the current Branch. String is the name of the
	// File and Node is either a Leaf if its a File or Branch it its a directory

	HashMap<String, Node> children;

	public Branch(Branch b, String name) {
		super(b, name);
		this.children = new HashMap<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see distributedSystems.P1.naming.Node#isDirectory()
	 */
	public boolean isDirectory() {
		return true;
	}
	
}