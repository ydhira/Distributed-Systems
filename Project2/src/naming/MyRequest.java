package naming;

/**
 * @author hira.yasin
 *
 *         This class represents a request by the client. It can be a read
 *         request in which case the exclusive will be false. or it can be a
 *         write request in which case the exclusive will be true.
 */
public class MyRequest {

	boolean exclusive;
	int num_of_readers;
	boolean isMyTurn;

	public MyRequest(boolean b) {
		this.exclusive = b;
		this.isMyTurn = false;
		this.num_of_readers = 0;
	}

}
