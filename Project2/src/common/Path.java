package common;

import java.io.*;
import java.util.*;

/**
 * Distributed filesystem paths.
 * 
 * <p>
 * Objects of type <code>Path</code> are used by all filesystem interfaces. Path
 * objects are immutable.
 * 
 * <p>
 * The string representation of paths is a forward-slash-delimeted sequence of
 * path components. The root directory is represented as a single forward slash.
 * 
 * <p>
 * The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
 * not permitted within path components. The forward slash is the delimeter, and
 * the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable, Comparable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private ArrayList<String> component = new ArrayList<String>();

	/** Creates a new path which represents the root directory. */

	public Path() {
		this.setName("/");
	}

	public ArrayList<String> getComponent() {
		return this.component;
	}

	/**
	 * Creates a new path by appending the given component to an existing path.
	 * 
	 * @param path
	 *            The existing path.
	 * @param component
	 *            The new component.
	 * @throws IllegalArgumentException
	 *             If <code>component</code> includes the separator, a colon, or
	 *             <code>component</code> is the empty string.
	 */
	public Path(Path path, String component) {
		if (component.isEmpty() || component.contains("/")
				|| component.contains(":")) {
			throw new IllegalArgumentException("component is not a path");
		}

		if (!(path.getName().equals("/"))) {
			this.setName(path.getName() + "/" + component);
		}

		else {
			this.setName(path.getName() + component);
		}

		String[] comp = this.getName().split("/");

		for (int i = 0; i < comp.length; i++) {
			if (comp[i].equals("")) {
				continue;
			}

			this.component.add(comp[i]);
		}
	}

	/**
	 * Creates a new path from a path string.
	 * 
	 * <p>
	 * The string is a sequence of components delimited with forward slashes.
	 * Empty components are dropped. The string must begin with a forward slash.
	 * 
	 * @param path
	 *            The path string.
	 * @throws IllegalArgumentException
	 *             If the path string does not begin with a forward slash, or if
	 *             the path contains a colon character.
	 */
	public Path(String path) {

		if (path == "") {
			throw new IllegalArgumentException("path is empty");
		}

		if (path.charAt(0) != '/' || path.contains(":")) {
			throw new IllegalArgumentException("component is not a path");
		}

		this.setName(path);

		for (int i = 0; i < path.length(); i++) {

			this.setName(getName().replaceAll("//", "/"));
		}

		for (int i = this.getName().length() - 1; i >= 0; i--) {
			if (!(this.getName().equals("/")) && this.getName().endsWith("/")) {
				this.setName(this.getName().substring(0,
						this.getName().length() - 1));
			}
		}

		String[] comp = this.getName().split("/");

		for (int i = 0; i < comp.length; i++) {
			if (comp[i].equals("")) {
				continue;
			}

			this.component.add(comp[i]);
		}
	}

	private class myIterator implements Iterator<String> {

		private Iterator<String> i;

		public myIterator(Iterator<String> i) {
			this.i = i;
		}

		@Override
		public boolean hasNext() {
			return this.i.hasNext();
		}

		@Override
		public String next() {
			return this.i.next();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Returns an iterator over the components of the path.
	 * 
	 * <p>
	 * The iterator cannot be used to modify the path object - the
	 * <code>remove</code> method is not supported.
	 * 
	 * @return The iterator.
	 */
	@Override
	public Iterator<String> iterator() {

		return new myIterator(this.component.iterator());
	}

	/**
	 * Lists the paths of all files in a directory tree on the local filesystem.
	 * 
	 * @param directory
	 *            The root directory of the directory tree.
	 * @return An array of relative paths, one for each file in the directory
	 *         tree.
	 * @throws FileNotFoundException
	 *             If the root directory does not exist.
	 * @throws IllegalArgumentException
	 *             If <code>directory</code> exists but does not refer to a
	 *             directory.
	 */
	public static Path[] list(File directory) throws FileNotFoundException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException(
					"the param has to be a directory");
		}

		ArrayList<Path> result = new ArrayList<Path>();

		ArrayList<String> allStringPaths = listHelp(directory, "",
				new ArrayList<String>());

		for (String p : allStringPaths) {
			Path np = new Path(p);
			result.add(np);
		}

		int length = result.size();
		Path[] result2 = new Path[length];

		for (int i = 0; i < length; i++) {
			result2[i] = result.get(i);

		}

		return result2;
	}

	/**
	 * @param dir
	 *            Directory we want to look into
	 * @param name
	 *            name we want to accumulate the names into
	 * @param allNames
	 *            array list of all the names we found so far. Is built
	 *            recursively
	 * @return allNames. array list of all the names we found so far
	 */
	public static ArrayList<String> listHelp(File dir, String name,
			ArrayList<String> allNames) {
		if (dir.isFile()) {
			allNames.add(name);
		} else {
			for (File f : dir.listFiles()) {
				listHelp(f, name + "/" + f.getName(), allNames);
			}
		}
		return allNames;
	}

	/**
	 * Determines whether the path represents the root directory.
	 * 
	 * @return <code>true</code> if the path does represent the root directory,
	 *         and <code>false</code> if it does not.
	 */
	public boolean isRoot() {
		if (this.getName().equals("/")) {
			return true;
		}

		return false;
	}

	/**
	 * Returns the path to the parent of this path.
	 * 
	 * @throws IllegalArgumentException
	 *             If the path represents the root directory, and therefore has
	 *             no parent.
	 */
	public Path parent() {
		if (isRoot()) {
			throw new IllegalArgumentException("root has no parent");
		}

		String result = "/";
		for (int i = 0; i < this.component.size() - 1; i++) {
			result += this.component.get(i) + "/";
		}

		Path p = new Path(result);
		return p;
	}

	/**
	 * Returns the last component in the path.
	 * 
	 * @throws IllegalArgumentException
	 *             If the path represents the root directory, and therefore has
	 *             no last component.
	 */
	public String last() {
		if (isRoot()) {
			throw new IllegalArgumentException("root has no last part");
		}

		return this.component.get(this.component.size() - 1);
	}

	/**
	 * Determines if the given path is a subpath of this path.
	 * 
	 * <p>
	 * The other path is a subpath of this path if is a prefix of this path.
	 * Note that by this definition, each path is a subpath of itself.
	 * 
	 * @param other
	 *            The path to be tested.
	 * @return <code>true</code> If and only if the other path is a subpath of
	 *         this path.
	 */
	public boolean isSubpath(Path other) {
		if (this.getName().contains(other.getName())
				&& other.getName().length() <= this.getName().length()) {
			return true;
		}

		return false;

	}

	/**
	 * Converts the path to <code>File</code> object.
	 * 
	 * @param root
	 *            The resulting <code>File</code> object is created relative to
	 *            this directory.
	 * @return The <code>File</code> object.
	 */
	public File toFile(File root) {
		File f = null;
		try {
			String name = root.getCanonicalPath() + this.getName();
			f = new File(name);
		} catch (IOException e) {

			e.printStackTrace();
		}
		return f;

	}

	/**
	 * Compares two paths for equality.
	 * 
	 * <p>
	 * Two paths are equal if they share all the same components.
	 * 
	 * @param other
	 *            The other path.
	 * @return <code>true</code> if and only if the two paths are equal.
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Path)) {
			return false;
		}
		Path otherPath = (Path) other;

		return this.getName().equals(otherPath.getName());

	}

	/** Returns the hash code of the path. */
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	/**
	 * Converts the path to a string.
	 * 
	 * <p>
	 * The string may later be used as an argument to the
	 * <code>Path(String)</code> constructor.
	 * 
	 * @return The string representation of the path.
	 */
	@Override
	public String toString() {
		return this.getName();
	}

	// TODO: check this. Might be wrong
	@Override
	public int compareTo(Object arg0) {
		if (arg0 == null) {
			throw new NullPointerException();
		}

		Path p = null;
		try {
			p = (Path) arg0;
		} catch (Exception e) {
			throw e;
		}

		if (p.equals(this)) {
			return 0;
		}

		ArrayList<String> thisList = this.component;
		ArrayList<String> inputList = p.component;

		if (thisList.size() == 0 && inputList.size() == 0) {
			return 0;
		}

		if (thisList.size() == 0 && inputList.size() != 0) {
			return -1;
		}

		if (thisList.size() != 0 && inputList.size() == 0) {
			return 1;
		}

		else {
			int i = 0;
			for (String s : inputList) {
				if (s != thisList.get(i)) {
					if (inputList.size() > thisList.size()) {
						return -1;
					}
					return 1;
				}
				i++;
			}

			if (thisList.size() == 0 && inputList.size() == 0) {
				return 0;
			}
			return 1;
		}
	}

	/**
	 * @return The name of the path
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            sets the name of this Path to given name
	 */
	public void setName(String name) {
		this.name = name;
	}
}
