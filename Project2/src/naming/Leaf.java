package naming;

import java.util.ArrayList;
import java.util.Random;

import storage.Command;
import storage.Storage;

/**
 * @author hira.yasin
 * 
 *         This is part of the tree. Leaf represents a File in my tree.
 */
public class Leaf extends Node {

	private ArrayList<Storage> storage = new ArrayList<Storage>();
	private ArrayList<Command> command = new ArrayList<Command>();
	private int num_of_readers = 0;
	private int cons = 0;

	public Leaf(Branch b, String name) {
		super(b, name);
	}

	/**
	 * @param b
	 *            The parent branch
	 * @param name
	 *            The name of the file
	 * @param storage
	 *            The storage stub of the server this file is stored in
	 * @param command
	 *            The command stub of the server this file is stored in
	 */
	public Leaf(Branch b, String name, Storage storage, Command command) {
		super(b, name);
		this.storage.add(storage);
		this.command.add(command);
	}

	/**
	 * @return the list of all the commands the File is stored in
	 */
	public ArrayList<Command> getallCommands() {
		return this.command;
	}

	/**
	 * @return the list of all the storages the File is stored in
	 */
	public ArrayList<Storage> getallStorages() {
		return this.storage;
	}

	/**
	 * @param s
	 *            removes storage s from the list
	 */
	public void removeStorage(Storage s) {
		this.storage.remove(s);
	}

	/**
	 * @param c
	 *            removes command c from the list
	 */
	public void removeCommand(Command c) {
		this.command.remove(c);
	}

	/**
	 * @return Any storage stub this File is stored in
	 */
	public Storage getStorage() {

		return this.storage.get(this.cons);
	}

	/**
	 * @return Any command stub this File is stored in
	 */
	public Command getCommand() {

		return this.command.get(this.cons);
	}

	/**
	 * @param s
	 *            The storage stub of the server this file is stored in
	 */
	public void putStorage(Storage s) {
		this.storage.add(s);
	}

	/**
	 * @param c
	 *            The command stub of the server this file is stored in
	 */
	public void putCommad(Command c) {
		this.command.add(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see distributedSystems.P1.naming.Node#isDirectory()
	 */
	public boolean isDirectory() {
		return false;
	}

	/**
	 * @return the num_of_readers
	 */
	public int getNum_of_readers() {
		return this.num_of_readers;
	}

	/**
	 * @param num_of_readers
	 *            sets the num_of_readers to num_of_readers
	 */
	public void setNum_of_readers(int num_of_readers) {
		this.num_of_readers = num_of_readers;
	}

}
