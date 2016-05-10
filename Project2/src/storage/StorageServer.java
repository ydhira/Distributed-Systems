package storage;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

import common.*;
import conformance.rmi.TestInterface;
import rmi.*;
import naming.*;

/**
 * Storage server.
 * 
 * <p>
 * Storage servers respond to client file access requests. The files accessible
 * through a storage server are those accessible under a given directory of the
 * local filesystem.
 */
public class StorageServer implements Storage, Command {
	/**
	 * Creates a storage server, given a directory on the local filesystem.
	 * 
	 * @param root
	 *            Directory on the local filesystem. The contents of this
	 *            directory will be accessible through the storage server.
	 * @throws NullPointerException
	 *             If <code>root</code> is <code>null</code>.
	 */

	private Skeleton<Storage> storageSkeleton;
	private Skeleton<Command> commandSkeleton;
	private File root;

	public StorageServer(File root) {

		this.storageSkeleton = new Skeleton<Storage>(Storage.class, this);
		this.commandSkeleton = new Skeleton<Command>(Command.class, this);
		this.root = root;
	}

	/**
	 * Starts the storage server and registers it with the given naming server.
	 * 
	 * @param hostname
	 *            The externally-routable hostname of the local host on which
	 *            the storage server is running. This is used to ensure that the
	 *            stub which is provided to the naming server by the
	 *            <code>start</code> method carries the externally visible
	 *            hostname or address of this storage server.
	 * @param naming_server
	 *            Remote interface for the naming server with which the storage
	 *            server is to register.
	 * @throws UnknownHostException
	 *             If a stub cannot be created for the storage server because a
	 *             valid address has not been assigned.
	 * @throws FileNotFoundException
	 *             If the directory with which the server was created does not
	 *             exist or is in fact a file.
	 * @throws RMIException
	 *             If the storage server cannot be started, or if it cannot be
	 *             registered.
	 */
	public synchronized void start(String hostname, Registration naming_server)
			throws RMIException, UnknownHostException, FileNotFoundException {

		this.storageSkeleton.start();
		this.commandSkeleton.start();
		Path[] deleteFiles = naming_server.register(
				Stub.create(Storage.class, storageSkeleton),
				Stub.create(Command.class, commandSkeleton), Path.list(root));

		for (Path dPath : deleteFiles) {

			File f = dPath.toFile(root);
			File parentDir = f.getParentFile();

			// if the parentDir is null or its not a directory then say
			// FileNotFoundException
			if (parentDir == null || !parentDir.isDirectory()) {
				throw new FileNotFoundException();
			}

			// delete the file
			f.delete();

			// go through the parent of the file. until you hit the root
			while (!parentDir.equals(root)) {

				// list the children of the parent. If there are no children,
				// meaning the directory is empty then delete this useless
				// directory and get its parent to check if it now became empty
				// Make sure not to delete the root .
				// Else break.
				File[] child = parentDir.listFiles();
				if (child.length == 0) {
					File parentDir2 = parentDir.getParentFile();
					parentDir.delete();
					parentDir = parentDir2;
				}

				else {
					break;
				}

			}

		}

	}

	/**
	 * Stops the storage server.
	 * 
	 * <p>
	 * The server should not be restarted.
	 */
	public void stop() {
		try {
			this.commandSkeleton.stop();
			this.storageSkeleton.stop();
			stopped(null);
		} catch (Exception e) {
			stopped(e);
		}
	}

	/**
	 * Called when the storage server has shut down.
	 * 
	 * @param cause
	 *            The cause for the shutdown, if any, or <code>null</code> if
	 *            the server was shut down by the user's request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Storage.java.
	@Override
	public synchronized long size(Path file) throws FileNotFoundException {

		File f = file.toFile(root);

		// check it should exist
		if (!f.exists()) {
			throw new FileNotFoundException("File not present");
		}

		// if its a directory then wrong input
		if (f.isDirectory()) {
			throw new FileNotFoundException("Directory given");
		}

		// return the length of the File
		return f.length();

	}

	@Override
	public synchronized byte[] read(Path file, long offset, int length)
			throws FileNotFoundException, IOException {

		// get the file represented by the path
		File f = file.toFile(root);

		if (!f.exists()) {
			throw new FileNotFoundException("File not present");
		}

		if (f.isDirectory()) {
			throw new FileNotFoundException("Directory given");
		}

		if (length < 0 || offset < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (offset + length > f.length()) {
			throw new IndexOutOfBoundsException();
		}

		if (length == 0) {
			return new byte[0];
		}

		// get the input stream and read from it
		FileInputStream in = new FileInputStream(f);
		byte[] b = new byte[length];
		in.read(b, (int) offset, length);
		in.close();
		return b;

	}

	@Override
	public synchronized void write(Path file, long offset, byte[] data)
			throws FileNotFoundException, IOException {

		// get the file represented by the path
		File f = file.toFile(root);

		if (!f.exists()) {
			throw new FileNotFoundException("File not present");
		}

		if (f.isDirectory()) {
			throw new FileNotFoundException("Directory given");
		}

		if (offset < 0) {
			throw new IndexOutOfBoundsException();
		}

		if (data.length == 0) {
			return;
		}

		// get the output stream. position it correctly with respect tp the
		// offset you want. And write to it then
		FileOutputStream out = new FileOutputStream(f);
		FileChannel fC = out.getChannel();
		fC.position(offset);
		fC.write(ByteBuffer.wrap(data));
		out.close();
	}

	// The following methods are documented in Command.java.
	@Override
	public synchronized boolean create(Path file) {

		if (file.isRoot()) {
			return false;
		}

		// get the file represented by the path
		File f = file.toFile(root);

		if (f.exists()) {
			return false;
		}

		// get the parent of the file
		File parentDir = f.getParentFile();
		// if the parent doesnt exist then create the parent. mkdirs creates all
		// the above non existential files.

		if (!parentDir.isDirectory()) {
			delete(file.parent());
		}
		parentDir.mkdirs();

		// then create the file
		try {
			f.createNewFile();
		} catch (IOException e) {
			System.out.println(e);
		}
		return true;
	}

	@Override
	public synchronized boolean delete(Path path) {

		if (path.isRoot()) {
			return false;
		}

		// get the file represented by the path
		File f = path.toFile(root);
		if (!f.exists()) {
			return false;
		}

		// if f is a File then just delete it
		if (!f.isDirectory()) {
			return f.delete();
		}

		// otherwise call the helper function
		return deleteFile(f);
	}

	/**
	 * @param c
	 *            The file to be deleted
	 * @return true if the File is successfully deleted. False otherwise
	 */

	private boolean deleteFile(File c) {

		// if its file then delete it
		if (!c.isDirectory()) {
			return c.delete();
		}

		// else go though all the children of the directory. recurse on the
		// children
		File[] allChild = c.listFiles();
		for (File c2 : allChild) {

			if (!deleteFile(c2)) {
				return false;
			}
		}
		// then delete the input given
		return c.delete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see storage.Command#copy(common.Path, storage.Storage)
	 */
	public boolean copy(Path file, Storage server) throws RMIException,
			FileNotFoundException, IOException {

		if (file == null || server == null) {
			throw new NullPointerException("null arguments ");
		}

		File f = file.toFile(root);

		// if the file exists in this storage server. Delete it. Because it may
		// be invalid. We want to copy it all over again

		if (f.exists()) {
			delete(file);
		}

		if (f.isDirectory()) {
			throw new FileNotFoundException("path refers to a directory");
		}

		// Create the file here
		create(file);

		long fileSize = server.size(file);
		long offset = 0;

		// start copying the file. The whole size should not be copied
		// instantly. because the size can be huge. So we copy it as much as we
		// can in one round. until we are done copying the whole thing

		try {
			while (offset < fileSize) {
				int toRead = (int) Math.min(fileSize - offset,
						Integer.MAX_VALUE);
				byte[] data = server.read(file, offset, toRead);
				write(file, offset, data);
				offset += toRead;
			}
			return true;
		} catch (IOException e) {
			throw e;
		}

		catch (Exception e) {
			return false;
		}
	}

}
