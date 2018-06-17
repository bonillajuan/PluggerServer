package pluggerserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pluggerserver.ChangeRequest;
import pluggerserver.ClientSession;

public class PluggerServer implements Runnable {

	public static void main(String[] args){
		try{
			InetAddress localhost = InetAddress.getLocalHost();
			int port = 9090;
			System.out.println("STARTING SERVER ON HOST: "+localhost.toString()+", port: "+port);
			new Thread(new PluggerServer(localhost, port)).start();
		}catch(Throwable t){
			t.printStackTrace();
		}
	}

	Selector selector;
	private InetAddress hostAddress;
	private int port;
	private ServerSocketChannel serverChannel;
	public Connection connectionDB;
	static Map<SelectionKey, ClientSession> clientMap = new HashMap<>();
	public Map<SocketChannel,byte[]> dataTracking = new HashMap<SocketChannel, byte[]>();

	// A list of PendingChange instances
	public List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();


	public PluggerServer(InetAddress hostAddress, int port){
		this.hostAddress = hostAddress;
		this.port = port;
		try {
			selector = this.initSelector();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.connectionDB = connectToDB();
		System.out.println("Server address: "+hostAddress);
		System.out.println("Server port: "+port);
		System.out.println("In attesa di connessioni...");
	}

	@Override
	public void run() {

		while (true) {

			/*try {
				// Process any pending changes
				synchronized (this.pendingChanges) {
					Iterator<ChangeRequest> changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
						}
					}
					this.pendingChanges.clear();
				}*/


				// Wait for an event one of the registered channels
			try {
				selector.select();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("SELECTING KEY...");

			// Iterate over the set of keys for which events are available
			Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = (SelectionKey) selectedKeys.next();
				selectedKeys.remove();

				if (!key.isValid()) {
					System.out.println("KEY IS NOT VALID.");
					ClientSession clientSession = clientMap.get(key);
					clientSession.disconnect();
					continue;
				}

				// Check what event is available and deal with it
				if (key.isAcceptable()) {
					System.out.println("KEY ACCEPTABLE");
					try {
						this.accept(key);
					} catch (IOException e) {
						System.out.println("ACCEPT ERROR.");
						ClientSession clientSession = clientMap.get(key);
						clientSession.disconnect();
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if (key.isReadable()) {
					System.out.println("KEY READABLE");
					this.handleCommand(key);
				}
				else if (key.isWritable()) {
					System.out.println("KEY WRITABLE");
					this.write(key);
				}
			}
		}
	}

	private void write(SelectionKey key){
		System.out.println("SERVER WRITE METHOD.");
        SocketChannel channel = (SocketChannel) key.channel();
        /**
         * The hashmap contains the object SockenChannel along with the information in it to be written.
         * In this example, we send the "Hello from server" String and also an echo back to the client.
         * This is what the hashmap is for, to keep track of the messages to be written and their socketChannels.
         */
        byte[] data = dataTracking.get(channel);
        dataTracking.remove(channel);

        // Something to notice here is that reads and writes in NIO go directly to the channel and in form of
        // a buffer.
        String toClient = new String(data);
        System.out.println("SERVER WRITING TO CLIENT: "+toClient);
        System.out.println("SERVER RESPONSE LENGTH: "+toClient.length());
        try {
			channel.write(ByteBuffer.wrap(data));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Since we wrote, then we should register to read next, since that is the most logical thing
        // to happen next. YOU DO NOT HAVE TO DO THIS. But I am doing it for the purpose of the example
                // Usually if you register once for a read/write/connect/accept, you never have to register again for that unless you
                // register for none (0). Like it said, I am doing it here for the purpose of the example. The same goes for all others.
        System.out.println("SETTING KEY READY TO READ.");
        key.interestOps(SelectionKey.OP_READ);

    }
    // Nothing special, just closing our selector and socket.
    private void closeConnection(){
        System.out.println("Closing server down...");
        if (selector != null){
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();
                System.out.println("SERVER TERMINATED.");
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	public void handleCommand(SelectionKey key){
		System.out.println("SERVER HANDLE COMMAND METHOD TRIGGERED.");
		ClientSession clientSession = clientMap.get(key);
		clientSession.handleCommand();
	}

	public void send(SelectionKey key, byte[] data){
		System.out.println("SERVER SEND METHOD TRIGGERED.");
        SocketChannel socketChannel = (SocketChannel) key.channel();
        dataTracking.put(socketChannel, data);
        key.interestOps(SelectionKey.OP_WRITE);
    }

	private void accept(SelectionKey key) throws IOException {
		System.out.println("SERVER ACCEPT METHOD TRIGGERED.");

		// For an accept to be pending the channel must be a server socket channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);

		System.out.println("New connection accepted.");

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		SelectionKey readKey = socketChannel.register(this.selector, SelectionKey.OP_READ);
		ClientSession clientSession = null;
		try {
			clientSession = new ClientSession(this, readKey, socketChannel, connectionDB);
			System.out.println("New client session created.");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientMap.put(readKey, clientSession);
		System.out.println("New client ip=" + socketChannel.getRemoteAddress() + ", online clients=" + clientMap.size());
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
		serverChannel.socket().bind(isa);

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		return socketSelector;
	}

	 public Connection connectToDB(){
		 	System.out.println("CONNECT TO DB METHOD ACTIVE.");
	        //Oggetti del Database
	        Connection connessioneDB = null;

	        //Driver Database
	        try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        //Connessione al Database
	        //connessioneDB = DriverManager.getConnection("jdbc:mysql://plugger.zapto.org/Sql1188281_1?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "Esterno", "Pluggare87821");

	        String user="root";
	        String pass="";
	        String DBURL="jdbc:mysql://localhost/plugger";
	        try {
				connessioneDB = DriverManager.getConnection(DBURL,user,pass);
				System.out.println("CONNECTION TO DB SUCCESSFUL.");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("CONNECTION TO DB FAILED.");
				closeConnection();
			}
	        return connessioneDB;
	    }


}
