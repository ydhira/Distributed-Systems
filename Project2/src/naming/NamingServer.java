package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import rmi.*;
import common.*;
import storage.*;

/**
 * Naming server.
 * 
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The
 * naming server maintains the filesystem directory tree. It does not store any
 * file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server which
 * hosts the file's contents.
 * 
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of
 * their existence. Clients use the <code>Service</code> interface to perform
 * most filesystem operations. The documentation accompanying these interfaces
 * provides details on the methods supported.
 * 
 * <p>
 * Stubs for accessing the naming server must typically be created by directly
 * specifying the remote network address. To make this possible, the client and
 * registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration {

	private Skeleton<Registration> registrationS;
	private Skeleton<Service> serviceS;
	private Branch root;
	private boolean start = false;
	private ArrayList<Storage> allStorage;
	private ArrayList<Command> allCommand;

	/**
	 * Creates the naming server object.
	 * 
	 * <p>
	 * The naming server is not started.
	 */
	public NamingServer() {
		this.registrationS = new Skeleton<Registration>(Registration.class,
				this, new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
		this.serviceS = new Skeleton<Service>(Service.class, this,
				new InetSocketAddress(NamingStubs.SERVICE_PORT));
		this.root = new Branch(null, "/");
		allStorage = new ArrayList<Storage>();
		allCommand = new ArrayList<Command>();

	}

	/**
	 * Starts the naming server.
	 * 
	 * <p>
	 * After this method is called, it is possible to access the client and
	 * registration interfaces of the naming server remotely.
	 * 
	 * @throws RMIException
	 *             If either of the two skeletons, for the client or
	 *             registration server interfaces, could not be started. The
	 *             user should not attempt to start the server again if an
	 *             exception occurs.
	 */
	public synchronized void start() throws RMIException {
		if (start) {
			return;
		}
		try {
			this.registrationS.start();
			this.serviceS.start();
		}

		catch (Exception e) {
			throw new RMIException(
					"Could not start registration or service skeleton");
		}

		this.start = true;
	}

	/**
	 * Stops the naming server.
	 * 
	 * <p>
	 * This method waits for both the client and registration interface
	 * skeletons to stop. It attempts to interrupt as many of the threads that
	 * are executing naming server code as possible. After this method is
	 * called, the naming server is no longer accessible remotely. The naming
	 * server should not be restarted.
	 */
	public void stop() {
		if (!start) {
			return;
		}

		try {
			this.registrationS.stop();
			this.serviceS.stop();
			this.start = false;
			stopped(null);
		}

		catch (Exception e) {
			stopped(e);
			this.start = false;
		}

	}

	/**
	 * Indicates that the server has completely shut down.
	 * 
	 * <p>
	 * This method should be overridden for error reporting and application exit
	 * purposes. The default implementation does nothing.
	 * 
	 * @param cause
	 *            The cause for the shutdown, or <code>null</code> if the
	 *            shutdown was by explicit user request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Service.java.
	@Override
	public boolean isDirectory(Path path) throws FileNotFoundException {
		if (path.isRoot()) {
			return true;
		}

		return isDirectoryHelp(path);
	}

	/**
	 * @param path
	 *            The object to be checked.
	 * @return true if the object is a directory, false if it is a file.
	 * @throws FileNotFoundException
	 *             true if the object is a directory, false if it is a file.
	 */
	private boolean isDirectoryHelp(Path path) throws FileNotFoundException {

		Branch f = root;
		Iterator<String> i = path.iterator();

		if (path.isRoot()) {
			return true;
		}

		// checks until the path has a next element

		while (i.hasNext()) {
			String name = i.next();
			Node child = f.children.get(name);

			// if f does not contain a File of name 'name' then return
			// FileNotFoundException
			if (child == null) {
				throw new FileNotFoundException("File does not exist");
			}

			// if path still has something then check if the Node the current
			// Node is a branch. if not then return FileNotFoundException
			if (i.hasNext()) {

				if (child instanceof Leaf) {
					throw new FileNotFoundException("File does not exist");
				}

				f = (Branch) child;
			}
			// If this is the end of the path, then call the isDirectory
			// function in the Leaf or the Branch
			else {

				return child.isDirectory();
			}
		}
		return false;
	}

	@Override
	public String[] list(Path directory) throws FileNotFoundException {

		Branch f = root;
		Iterator<String> i = directory.iterator();
		String[] result = null;

		// if the given file is the root then simply lists its children.
		if (directory.isRoot()) {
			Set<String> directory_listing = root.children.keySet();
			result = new String[directory_listing.size()];
			int j = 0;
			for (String a : directory_listing) {
				result[j] = a;
				j++;
			}
		}

		// go through every element of the path
		else {
			while (i.hasNext()) {

				String name = i.next();
				Node child = f.children.get(name);

				if (child == null) {
					throw new FileNotFoundException("Directory does not exist");
				}

				// meaning it should be a directory
				if (i.hasNext()) {
					if (child instanceof Leaf) {
						throw new FileNotFoundException(
								"Directory does not exist");
					}

					else {
						f = (Branch) child;
					}
				}
				// End of the path
				else {
					// if its a leaf then wrong path, because we need a
					// directory
					if (child instanceof Leaf) {
						throw new FileNotFoundException(
								"Directory does not exist");
					}
					// go through all the names of the File inside directory and
					// add to a list
					else {

						Branch childa = (Branch) child;
						Set<String> directory_listing = childa.children
								.keySet();
						result = new String[directory_listing.size()];
						int j = 0;
						for (String a : directory_listing) {
							result[j] = a;
							j++;
						}
					}
				}
			}
		}
		return result;

	}

	@Override
	public boolean createFile(Path file) throws RMIException,
			FileNotFoundException {

		Branch f = root;
		Iterator<String> i = file.iterator();

		// go through every element in the path
		while (i.hasNext()) {

			String name = i.next();
			Node child = f.children.get(name);

			if (child == null) {
				// if child is null and path doesnot have anything else. then
				// this is where i need to create the file
				if (!i.hasNext()) {

					if (f.children.containsKey(name)) {
						return false;
					}

					Random rn = new Random();
					int index = rn.nextInt(this.allCommand.size());

					// create the Leaf

					Leaf newLeaf = new Leaf(f, name, allStorage.get(index),
							allCommand.get(index));

					// call a random storage server to create a file

					try {
						Command c = this.allCommand.get(index);
						Storage s = this.allStorage.get(index);
						c.create(file);
						f.children.put(name, newLeaf);
						newLeaf.putCommad(c);
						newLeaf.putStorage(s);

						return true;
					} catch (Exception e) {
						throw new RMIException("network error");
					}
				} else {
					throw new FileNotFoundException("File does not exist");
				}
			}

			// path still has something else
			if (i.hasNext()) {
				if (child instanceof Leaf) {
					throw new FileNotFoundException("File does not exist");
				}
				// change the parent
				f = (Branch) child;
			}
		}

		return false;
	}

	@Override
	public boolean createDirectory(Path directory)
			throws FileNotFoundException, RMIException {

		Branch f = root;
		Iterator<String> i = directory.iterator();

		if (directory.isRoot()) {
			return false;
		}

		// go through every element in the path

		while (i.hasNext()) {

			String name = i.next();
			Node child = f.children.get(name);

			if (child == null) {
				if (!i.hasNext()) {
					// if the child is null and path has ended then this where i
					// need to create the directory.

					Branch newBranch = new Branch(f, name);

					if (f.children.containsKey(name)) {
						return false;
					}

					f.children.put(name, newBranch);
					return true;
				} else {
					throw new FileNotFoundException("Directory does not exist");
				}

			}
			// path still has something else
			if (i.hasNext()) {

				if (child instanceof Leaf) {
					throw new FileNotFoundException("Directory does not exist");
				}
				// change the parent
				f = (Branch) child;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see naming.Service#delete(common.Path)
	 */
	@Override
	public boolean delete(Path path) throws FileNotFoundException, RMIException {

		if (path.isRoot()) {
			return false;
		}

		Branch f = root;
		Iterator<String> i = path.iterator();

		// go through every element in the path

		while (i.hasNext()) {
			String name = i.next();
			Node child = f.children.get(name);

			if (child == null) {
				throw new FileNotFoundException("File does not exist");
			}

			if (i.hasNext()) {
				if (child instanceof Leaf) {
					throw new FileNotFoundException("Parent is a file");

				}
				f = (Branch) child;
			}

			// path ended here. So i need to delete this File
			else {

				// if its a Leaf then i get all the command stubs that the file
				// is stored in. and ask all the command stubs to delete this
				// file

				if (child instanceof Leaf) {
					Leaf child2 = (Leaf) child;
					ArrayList<Command> leafCommands = child2.getallCommands();
					try {

						for (Command c : leafCommands) {
							if (!c.delete(path)) {
								return false;
							}
						}

						f.children.remove(name);

					} catch (RMIException e) {
						throw new RMIException("Command Stub creating problem");
					}
				} else {

					// if its a directory. then recursively delete all the files
					// that are in the directory. Collect the command stubs of
					// every file that is in the list and delete the files from
					// the command stubs

					ArrayList<Command> allCommandDir = new ArrayList<Command>();

					deleteHelp(allCommandDir, child);

					for (Command c : allCommandDir) {
						c.delete(path);
					}

					f.children.remove(name);
					return true;

				}
			}
		}
		return true;
	}

	/**
	 * @param allCommandDir
	 *            collects all the command stubs of the files in this list
	 * @param child
	 *            Node to be checked
	 */
	private void deleteHelp(ArrayList<Command> allCommandDir, Node child) {

		// if a Leaf. then we get all the command stubs leaf is stored in.
		if (child instanceof Leaf) {
			ArrayList<Command> allLeafC = ((Leaf) child).getallCommands();
			for (Command c : allLeafC) {
				if (!allCommandDir.contains(c)) {
					allCommandDir.add(c);
				}
			}
		}

		// if a branch then we recurse on all of its children

		else {
			Branch d = (Branch) child;
			Collection<Node> children = d.children.values();

			for (Node c : children) {
				deleteHelp(allCommandDir, c);
			}
		}
	}

	@Override
	public Storage getStorage(Path file) throws FileNotFoundException {

		Branch f = root;
		Iterator<String> i = file.iterator();
		Storage sStub = null;

		// go through every element in the path
		while (i.hasNext()) {

			String name = i.next();
			Node child = f.children.get(name);

			if (child == null) {
				throw new FileNotFoundException("File does not exist");
			}
			// path still has something else
			if (i.hasNext()) {

				if (child instanceof Leaf) {
					throw new FileNotFoundException("File does not exist");
				}
				f = (Branch) child;
			}
			// path is finished. if its a a Directory then this is an error. If
			// its a leaf then get the storage stub of it and return

			else {

				if (child instanceof Branch) {
					throw new FileNotFoundException("Path points to a Branch");
				}

				Leaf thisFile = (Leaf) child;
				sStub = thisFile.getStorage();
			}
		}

		return sStub;
	}

	// The method register is documented in Registration.java.

	@Override
	public Path[] register(Storage client_stub, Command command_stub,
			Path[] files) throws RMIException {

		ArrayList<Path> fileNotNeed = new ArrayList<Path>();

		if (client_stub == null || command_stub == null || files == null) {
			throw new NullPointerException("the arguments are null");
		}

		if (this.allCommand.contains(command_stub)
				|| this.allStorage.contains(client_stub)) {
			throw new IllegalStateException("already registered");
		}

		// add the storage and command stub to the respective lists
		allCommand.add(command_stub);
		allStorage.add(client_stub);

		Node child = null;
		for (Path file : files) {

			if (!file.isRoot()) {

				Branch f = root;
				Iterator<String> i = file.iterator();

				// go through every element in the path

				while (i.hasNext()) {
					String name = i.next();
					child = f.children.get(name);

					if (child == null) {

						// not the end of the path. The element I am at should
						// be a directory. create a new Branch and add to the
						// tree

						if (i.hasNext()) {
							Branch newB = new Branch(f, name);
							f.children.put(name, newB);
							f = newB;
						}

						// end of the path. this is a file. Create a new Leaf.
						// add to the tree.

						else {

							Leaf newF = new Leaf(f, name);
							newF.putStorage(client_stub);
							newF.putCommad(command_stub);
							f.children.put(name, newF);
						}

					} else {
						// end of the path and I still found something of the
						// File name. Duplicate occurrence. need to delete it
						if (!i.hasNext()) {
							fileNotNeed.add(file);
						}

						// change the parent and go to the while loop again

						else {
							f = (Branch) child;
						}
					}
				}
			}
		}

		Path[] deletepath = new Path[fileNotNeed.size()];
		int j = 0;
		for (Path i : fileNotNeed) {
			deletepath[j] = i;
			j++;
		}

		return deletepath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see naming.Service#lock(common.Path, boolean)
	 */
	@Override
	public void lock(Path path, boolean exclusive) throws RMIException,
			FileNotFoundException {

		if (path == null) {
			throw new NullPointerException("null pointers");
		}

		Branch f = root;

		if (path.getName().equals("/")) {
			if (exclusive) {
				f.getLock().getExclusive();
			} else {
				f.getLock().getShared();
			}
			return;
		}

		f.getLock().getShared();
		Iterator<String> i = path.iterator();

		// go through every element in the path
		while (i.hasNext()) {

			String name = i.next();
			Node child = f.children.get(name);

			if (child == null) {
				throw new FileNotFoundException("File does not exist");
			}

			// if path still has something then check if the current
			// Node is a branch. if not then return FileNotFoundException.
			// IF yes then give a shared lock to the directory

			if (i.hasNext()) {

				if (child instanceof Leaf) {
					throw new FileNotFoundException("File does not exist");
				}

				f = (Branch) child;
				f.getLock().getShared();
			}

			// the path ends
			else {
				Node end = (Node) child;

				// Exclusive request

				if (exclusive) {

					// Exclusive for File: Get all the command stubs File is
					// stored in. Delete the file in every storage server except
					// for one. Then reset the number_of_resedrs of the file to
					// zero. Give the exclusive lock

					if (end instanceof Leaf) {
						Leaf end2 = (Leaf) end;
						ArrayList<Command> leafCommands = end2.getallCommands();
						for (int j = 1; j < leafCommands.size(); j++) {
							leafCommands.get(j).delete(path);
						}
						end2.setNum_of_readers(0);
					}
					end.getLock().getExclusive();
				}

				// Shared Request

				else {

					// Shared for File: Check if the File has been requested for
					// reading more than 20 times. If it is then replicate it to
					// any of the Storage servers that it is not present in
					// before. Give the shared lock

					if (end instanceof Leaf) {

						Leaf end2 = (Leaf) end;
						end2.setNum_of_readers(end2.getNum_of_readers() + 1);

						if (end2.getNum_of_readers() > 20 && (end2.getallCommands().size() < this.allCommand.size())) {

							ArrayList<Command> leafCommands = end2
									.getallCommands();
							ArrayList<Storage> leafStorages = end2
									.getallStorages();

							int index = 0;
							for (Command c : this.allCommand) {
								if (!leafCommands.contains(c)) {
									try {

										Storage s = this.allStorage.get(index);
										c.copy(path, leafStorages.get(0));
										end2.setNum_of_readers(0);
										end2.putCommad(c);
										end2.putStorage(s);

									} catch (IOException e) {
										e.printStackTrace();
									}
									break;
								}
								index++;
							}
						}
					}
					end.getLock().getShared();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see naming.Service#unlock(common.Path, boolean)
	 */
	@Override
	public void unlock(Path path, boolean exclusive) throws RMIException {

		if (path == null) {
			throw new NullPointerException("null pointers");
		}

		Branch f = root;
		Iterator<String> i = path.iterator();

		// Unlocking to the root

		if (path.getName().equals("/")) {

			if (exclusive) {
				f.getLock().releaseExcluive();
			} else {

				f.getLock().releaseShared();
			}

			return;
		}
		f.getLock().releaseShared();

		// go through every element in the path
		while (i.hasNext()) {

			String name = i.next();
			Node child = f.children.get(name); // this is a Node of the String
												// name

			if (child == null) {
				throw new IllegalArgumentException("child is null");
			}

			// if there is still something in the path. then the current Node is
			// a Branch and we can release the share lock on it

			if (i.hasNext()) {

				f = (Branch) child;
				f.getLock().releaseShared();
			}

			// Given path ends. If the lock was exclusive we release the
			// exclusive lock on it. If it was shared then we release the shared
			// lock.

			else {
				Node end = (Node) child;
				if (exclusive) {
					end.getLock().releaseExcluive();
				}

				else {
					end.getLock().releaseShared();
				}
			}
		}

	}

}
