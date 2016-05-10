package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.*;
import java.lang.reflect.Proxy.*;

/**
 * RMI stub factory.
 * 
 * <p>
 * RMI stubs hide network communication with the remote server and provide a
 * simple object-like interface to their users. This class provides methods for
 * creating stub objects dynamically, when given pre-defined interfaces.
 * 
 * <p>
 * The network address of the remote server is set when a stub is created, and
 * may not be modified afterwards. Two stubs are equal if they implement the
 * same interface and carry the same remote server address - and would therefore
 * connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub {
	/**
	 * Creates a stub, given a skeleton with an assigned address.
	 * 
	 * <p>
	 * The stub is assigned the address of the skeleton. The skeleton must
	 * either have been created with a fixed address, or else it must have
	 * already been started.
	 * 
	 * <p>
	 * This method should be used when the stub is created together with the
	 * skeleton. The stub may then be transmitted over the network to enable
	 * communication with the skeleton.
	 * 
	 * @param c
	 *            A <code>Class</code> object representing the interface
	 *            implemented by the remote object.
	 * @param skeleton
	 *            The skeleton whose network address is to be used.
	 * @return The stub created.
	 * @throws IllegalStateException
	 *             If the skeleton has not been assigned an address by the user
	 *             and has not yet been started.
	 * @throws UnknownHostException
	 *             When the skeleton address is a wildcard and a port is
	 *             assigned, but no address can be found for the local host.
	 * @throws NullPointerException
	 *             If any argument is <code>null</code>.
	 * @throws Error
	 *             If <code>c</code> does not represent a remote interface - an
	 *             interface in which each method is marked as throwing
	 *             <code>RMIException</code>, or if an object implementing this
	 *             interface cannot be dynamically created.
	 */

	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> c, Skeleton<T> skeleton)
			throws UnknownHostException {

		InetSocketAddress address = skeleton.getAddress();

		if (c == null || skeleton == null) {
			throw new NullPointerException(
					"interface and skeleton should not be null for stub creation");
		}

		else if (address == null && !(skeleton.isRunning())) {
			throw new IllegalStateException("address of the skeleton is null");
		}

		else if (address.isUnresolved()) {
			throw new UnknownHostException();
		}

		// checks if all the methods in the interface throw RMIException
		Method[] allMethods = c.getDeclaredMethods();
		int count = 0;

		for (Method m : allMethods) {
			Class[] exceptions = m.getExceptionTypes();
			for (Class e : exceptions) {
				if (e.getSimpleName().equals("RMIException")) {
					count++;
				}
			}

			if (count == 0) {
				throw new Error("not all methods are RMI Exceptions");
			}

			count = 0;
		}

		// create the proxyHandler with the skeleton's address.
		ProxyHandler p = new ProxyHandler(address);

		return (T) java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(),
				new Class[] { c }, p);

	}

	/**
	 * Creates a stub, given a skeleton with an assigned address and a hostname
	 * which overrides the skeleton's hostname.
	 * 
	 * <p>
	 * The stub is assigned the port of the skeleton and the given hostname. The
	 * skeleton must either have been started with a fixed port, or else it must
	 * have been started to receive a system-assigned port, for this method to
	 * succeed.
	 * 
	 * <p>
	 * This method should be used when the stub is created together with the
	 * skeleton, but firewalls or private networks prevent the system from
	 * automatically assigning a valid externally-routable address to the
	 * skeleton. In this case, the creator of the stub has the option of
	 * obtaining an externally-routable address by other means, and specifying
	 * this hostname to this method.
	 * 
	 * @param c
	 *            A <code>Class</code> object representing the interface
	 *            implemented by the remote object.
	 * @param skeleton
	 *            The skeleton whose port is to be used.
	 * @param hostname
	 *            The hostname with which the stub will be created.
	 * @return The stub created.
	 * @throws IllegalStateException
	 *             If the skeleton has not been assigned a port.
	 * @throws NullPointerException
	 *             If any argument is <code>null</code>.
	 * @throws Error
	 *             If <code>c</code> does not represent a remote interface - an
	 *             interface in which each method is marked as throwing
	 *             <code>RMIException</code>, or if an object implementing this
	 *             interface cannot be dynamically created.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> c, Skeleton<T> skeleton, String hostname) {

		if (c == null || skeleton == null || hostname == null) {
			throw new NullPointerException(
					"interface and skeleton should not be null for stub creation");
		}

		InetSocketAddress address = new InetSocketAddress(hostname, skeleton
				.getAddress().getPort());

		// checks if all the methods in the interface throw RMIException
		Method[] allMethods = c.getDeclaredMethods();
		int count = 0;

		for (Method m : allMethods) {
			Class[] exceptions = m.getExceptionTypes();
			for (Class e : exceptions) {
				if (e.getSimpleName().equals("RMIException")) {
					count++;
				}
			}

			if (count == 0) {
				throw new Error("not all methods are RMI Exceptions");
			}

			count = 0;
		}

		ProxyHandler p = new ProxyHandler(address);

		return (T) java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(),
				new Class[] { c }, p);

	}

	/**
	 * Creates a stub, given the address of a remote server.
	 * 
	 * <p>
	 * This method should be used primarily when bootstrapping RMI. In this
	 * case, the server is already running on a remote host but there is not
	 * necessarily a direct way to obtain an associated stub.
	 * 
	 * @param c
	 *            A <code>Class</code> object representing the interface
	 *            implemented by the remote object.
	 * @param address
	 *            The network address of the remote skeleton.
	 * @return The stub created.
	 * @throws NullPointerException
	 *             If any argument is <code>null</code>.
	 * @throws Error
	 *             If <code>c</code> does not represent a remote interface - an
	 *             interface in which each method is marked as throwing
	 *             <code>RMIException</code>, or if an object implementing this
	 *             interface cannot be dynamically created.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> c, InetSocketAddress address) {
		if (c == null || address == null) {
			throw new NullPointerException(
					"interface and address should not be null for stub creation");
		}

		// checks if all the methods in the interface throw RMIException
		Method[] allMethods = c.getDeclaredMethods();
		int count = 0;

		for (Method m : allMethods) {
			Class[] exceptions = m.getExceptionTypes();
			for (Class e : exceptions) {
				if (e.getSimpleName().equals("RMIException")) {
					count++;
				}
			}

			if (count == 0) {
				throw new Error("not all methods are RMI Exceptions");
			}

			count = 0;
		}

		ProxyHandler p = new ProxyHandler(address);

		return (T) java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(),
				new Class[] { c }, p);

	}

	public static class ProxyHandler implements InvocationHandler, Serializable {

		private InetSocketAddress serverAddress;

		/**
		 * instantiates the proxy handler with the given address.
		 * 
		 * @param serverAddress
		 *            the address for the proxyHandler
		 */
		public ProxyHandler(InetSocketAddress serverAddress) {
			this.serverAddress = serverAddress;

		}

		/**
		 * @return the address of the proxyHandler
		 */
		public InetSocketAddress getAddress() {
			return this.serverAddress;
		}

		/**
		 * @param arg0
		 *            The object
		 * @param arg1
		 *            The method
		 * @param arg2
		 *            arguments for the method
		 * @return true if the argument is equal to the this Proxy and the
		 *         proxyHandlers have the same address. false otherwise
		 */
		public boolean equals(Object arg0, Method arg1, Object[] arg2) {

			if (arg2.length != 1) {
				return false;
			}

			Object argument = arg2[0];

			if (argument == null) {
				return false;
			}

			if (!(java.lang.reflect.Proxy.isProxyClass(argument.getClass()))) {
				return false;
			}

			if (!arg0.getClass().equals(argument.getClass())) {
				return false;
			}

			InvocationHandler h = java.lang.reflect.Proxy
					.getInvocationHandler(argument);

			if (!(h instanceof ProxyHandler)) {
				return false;
			}

			ProxyHandler h2 = (ProxyHandler) h;

			if (!(this.serverAddress.equals(h2.getAddress()))) {
				return false;
			}

			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return serverAddress.hashCode();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return serverAddress.toString();
		}

		@Override
		public Object invoke(Object arg0, Method arg1, Object[] arg2)
				throws Throwable {

			Object result = null;

			// checks if the method given is the local method

			if (arg1.getName().equals("equals")) {
				return equals(arg0, arg1, arg2);
			}

			else if (arg1.getName().equals("hashCode")) {
				return hashCode();
			}

			else if (arg1.getName().equals("toString")) {
				return toString();
			}

			// remote method. Marshals the arguments and the methods. Unmarshals
			// the result back. and returns it back to the client
			else {
				Socket clientSocket = new Socket();
				try {
					clientSocket.connect(this.serverAddress);

					ObjectOutputStream out = new ObjectOutputStream(
							clientSocket.getOutputStream());
					out.flush();
					ObjectInputStream in = new ObjectInputStream(
							clientSocket.getInputStream());

					out.writeObject(arg1.getName());
					out.writeObject(arg1.getParameterTypes());
					out.writeObject(arg2);

					result = in.readObject();

					// if during the process, an error is seen then throws an
					// RMIException
				} catch (Exception e) {
					throw new RMIException(e);
				} finally {
					clientSocket.close();

				}
				// if the result is an error itself, meaning the skeleton threw
				// an error, then tells the client that an error is seen
				if (result instanceof Throwable) {
					throw (Throwable) result;
				}
			}

			return result;
		}

	}
}
