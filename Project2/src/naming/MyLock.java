package naming;

import java.util.LinkedList;

/**
 * @author hira.yasin
 * 
 *         This class is used to describe the different types of Lock we can
 *         have. And the functionalities we can do with the locks.
 *
 */
public class MyLock {

	private LinkedList<MyRequest> queue = new LinkedList<MyRequest>();

	/**
	 * adds a non-exclusive request (read request) into the queue. does so by
	 * checking if we have a read request at the end of the queue. if we do,
	 * then does not add the new read request. because the 2 read requests can
	 * be done at the same time.
	 */
	public synchronized void getShared() {

		MyRequest m = null;

		if (queue.size() > 0) {

			// if there was a read request in the queue already then don't add a
			// new read request. Just add 1 to the number_of_readers to the read
			// request already in the queue.

			if (!queue.getLast().exclusive) {
				m = queue.getLast();
				m.num_of_readers++;
			}

			// Else just add a new read_request
			else {
				m = new MyRequest(false);
				queue.add(m);
			}

			// and then wait for your turn.
			while (!m.isMyTurn) {
				try {
					wait();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// if the queue is empty then add a new Read request. and it is your
		// turn. No waiting
		else {

			m = new MyRequest(false);
			m.isMyTurn = true;
			queue.add(m);
		}
	}

	/**
	 * adds a new exclusive request(write request) into the queue. This does not
	 * care what is at the end of the queue.
	 */
	public synchronized void getExclusive() {

		MyRequest m = null;

		if (queue.size() > 0) {

			// add a new write request to the queue.

			m = new MyRequest(true);
			queue.add(m);

			// Wait for your turn
			while (!m.isMyTurn) {
				try {
					wait();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// If the queue is empty. Add a new request. Your turn. No waiting
		else {

			m = new MyRequest(true);
			m.isMyTurn = true;
			queue.add(m);
		}
	}

	/**
	 * Releases a lock for the shared request
	 */
	public synchronized void releaseShared() {

		MyRequest m = queue.getFirst();

		int n = m.num_of_readers;

		// Get the first request in the queue. If the number_of_readers is zero.
		// Then remove the first request. Get the next element in the queue and
		// turn its turn t true. and NotifyAll the waits.

		if (n == 0) {

			queue.removeFirst();
			if (queue.size() > 0) {

				MyRequest m2 = queue.getFirst();
				m2.isMyTurn = true;
				notifyAll();
			}
		}

		// if the number_of_readers != 0 meaning there are still readers. then
		// just decrement the number_of_readers by 1.

		else {
			m.num_of_readers--;
		}
	}

	/**
	 * Releases a lock for the exclusive request
	 */
	public synchronized void releaseExcluive() {
		queue.removeFirst();
		if (queue.size() > 0) {
			MyRequest m2 = queue.getFirst();
			m2.isMyTurn = true;
			notifyAll();
		}
	}
}
