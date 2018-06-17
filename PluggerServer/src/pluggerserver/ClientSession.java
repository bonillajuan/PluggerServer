package pluggerserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mysql.jdbc.PreparedStatement;

import pluggerserver.Brano;

public class ClientSession {

	SelectionKey selKey;
	SocketChannel socketChannel;
	ByteBuffer readBuffer;
	PluggerServer pluggerServer;
	Connection connectionDB;

	int idUser;
	String username;

    public static final String CONNECTED = "connected";
    public static final String DISCONNECTED = "disconnected";
    public String status;
    public String staticTempDir = "C:/Users/Juan/Music/MusicaPlugger/_tmp";
    public String staticDir = "C:/Users/Juan/Music/MusicaPlugger/";

    public OutputStream outputStream;
    public InputStream inputStream;

    public ObjectOutputStream outObjectToClient;
	public ObjectInputStream inObjectFromClient;


	ClientSession(PluggerServer server, SelectionKey selKey, SocketChannel channel, Connection connection) throws Throwable {
		this.pluggerServer = server;
		this.selKey = selKey;
		this.socketChannel = (SocketChannel) channel.configureBlocking(false); // asynchronous/non-blocking
		this.connectionDB = connection;
		readBuffer = ByteBuffer.allocate(8*1024); // 8 KByte capacity
		initiateConnection();
		setStatus(CONNECTED);
	}

	public void initiateConnection(){
        //inFromUser = new BufferedReader(new InputStreamReader(System.in));
        try {
        	outputStream = socketChannel.socket().getOutputStream();
        	inputStream = socketChannel.socket().getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public void handleCommand() {

		System.out.println("WAITING FOR A COMMAND...");

			int amount_read = -1;

			String response = readFromClient();
			amount_read = response.length();

			if (amount_read == -1){
				System.out.println("END OF STREAM, CLOSING CONNECTION.");
			}

			if(amount_read>0){

				switch(response){
					case "UPLOAD":
						System.out.println("CASE UPLOAD DETECTED.");
						receiveFile();
						System.out.println("CASE UPLOAD TERMINATED.");
						break;
					case "LOGIN":
	                    System.out.println("CASE LOGIN DETECTED.");
	                    login();
	                    System.out.println("CASE LOGIN TERMINATED.");
	                    break;
					case "UPDATE_HOMEPAGE":
						System.out.println("CASE INITIALIZE_SESSION DETECTED.");
						updateHomepage();
						System.out.println("CASE INITIALIZE_SESSION TERMINATED.");
						break;
					case "SELECT_FILE":
						System.out.println("CASE SELECT_FILE DETECTED.");
						selectFile();
						System.out.println("CASE SELECT_FILE TERMINATED.");
						break;
					default:
						System.out.println("ERRORE SWITCH COMMANDO.");
						System.out.println("CLIENT SENT ERROR: "+response);
						break;
				}
			}

			if (amount_read < 1)
				return; // if zero
	}

	public void setIdUser(int id){
    	this.idUser = id;
    	System.out.println("ID USER: "+idUser);
    }

    public int getIdUser(){
    	return this.idUser;
    }

    public void setUsername(String username){
    	this.username = username;
    }

    public String getUsername(){
    	return this.username;
    }

	private void updateHomepage() {
		//Oggetti del Database
        java.sql.Statement interrogazioneSQL = null;
        PreparedStatement comandoSQL = null;
        ResultSet risultatoSQL = null;

        try {
            //Preparazione Query al Database
            interrogazioneSQL = connectionDB.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            risultatoSQL = interrogazioneSQL.executeQuery("SELECT percorsoFIle FROM brano");
        } catch (SQLException ex) {
            Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            int Nbrani = 0;
            while(risultatoSQL.next()){
                Nbrani++;
            }
            risultatoSQL.beforeFirst();
            writeToClient("CASE_UPDATE_HOMEPAGE/SENDING_NBRANI," + String.valueOf(Nbrani));
            System.out.println("QUANTITA' BRANI = " + Nbrani);
            while(risultatoSQL.next()){
                String PercorsoBrano = staticDir + risultatoSQL.getString("percorsoFile");
                System.out.println("PERCORSO BRANO = " + PercorsoBrano);
                Brano brano = new Brano(new File(PercorsoBrano));
                System.out.println("Brano Creato = " + brano.toString());
                ByteArrayOutputStream byteArrOutStream = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream objOutStream = new ObjectOutputStream(byteArrOutStream);
                    objOutStream.writeObject(brano);
                    objOutStream.flush();
                    int wrote = socketChannel.write(ByteBuffer.wrap(byteArrOutStream.toByteArray()));
                    System.out.println("Grandezza Oggetto Spedito = " + wrote);

                } catch (IOException ex) {
                    Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
                }


            }

                String CheckMusic = readFromClient();
                List<String> splitValues = Arrays.asList(CheckMusic.split(","));
                if(splitValues.get(0).equals("BRANI RICEVUTI") && splitValues.get(1).equals(String.valueOf(Nbrani))){
                    writeToClient("CASE_UPDATE_HOMEPAGE/TRANSFER_SUCCESSFUL");
                    System.out.println("TASFERIMENTO COMPLETATO");
                }

        } catch (SQLException ex) {
            Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
        }

	}

	private void selectFile(){

	}

	public void login(){

        //Invia a Client conferma per l'invio delle credenziali
        writeToClient("CASE_LOGIN/SEND_VALUES");

        //Oggetti del Database
        java.sql.Statement interrogazioneSQL = null;
        //PreparedStatement comandoSQL = null;
        ResultSet risultatoSQL = null;

        String response = readFromClient();

        List<String> splitValues = Arrays.asList(response.split(","));

        System.out.println("Login in corso...");

            //Preparazione Query al Databas
			try {
				interrogazioneSQL = connectionDB.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


            //SE è una Mail
            if(splitValues.get(0).contains("@")){
                //Query con Mail
                try {
					risultatoSQL = interrogazioneSQL.executeQuery("Select * FROM utente WHERE email = '" + splitValues.get(0) + "' AND psw = '" + splitValues.get(1) + "'");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }else{  //SE non c'� non � una Mail
                //Query con Nome
                try {
					risultatoSQL = interrogazioneSQL.executeQuery("Select * FROM utente WHERE username = '" + splitValues.get(0) + "' AND psw = '" + splitValues.get(1) + "'");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }


            //SE i dati vengono trovati nel Database (Login avvenuto con successo)
            try {
				if(risultatoSQL.next()){
				    System.out.println("Accesso Avvenuto");
                    setIdUser(risultatoSQL.getInt("utente.id_utente"));
                    setUsername(risultatoSQL.getString("utente.username"));
				    String result = "CASE_LOGIN/VALUES_CONFIRMED,"+getIdUser()+","+getUsername();
				    pluggerServer.send(selKey, result.getBytes());
				}else{ //SE l'accesso viene negato
				    //Da sostituire con risposta a Client
				    System.out.println("ACCESSO Negato!");
				    String result = "CASE_LOGIN/VALUES_INVALID";
				    pluggerServer.send(selKey, result.getBytes());
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public String readFromClient(){
		int read = 0;
		readBuffer.clear();

		System.out.println("STATUS: "+getStatus());
		while(getStatus().contentEquals(CONNECTED) && read==0){
			try {
				read = socketChannel.read(readBuffer);
			} catch (IOException e) {
				disconnect();
				e.printStackTrace();
			}
		}

		if(getStatus().contentEquals(DISCONNECTED)){
			System.out.println("CANNOT READ FROM CLIENT.");
		}

		if(read==-1){
			disconnect();
		}
		System.out.println("BYTES READ:" +read);
		String response = bufferToString(readBuffer);
		System.out.println("CLIENT SENT: "+response);
		return response;
	}

	public void writeToClient(String str){
		byte[] responseArr = str.getBytes();
		//readBuffer.flip();
		int wrote;
		try {
			wrote = socketChannel.write(ByteBuffer.wrap(responseArr));
			System.out.println("WROTE: "+wrote);
			System.out.println("RESPONDING TO CLIENT: "+new String(responseArr));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			disconnect();
			e.printStackTrace();
		}
	}

	public void receiveFile(){

		String status = "CASE_UPLOAD/SEND_LENGTH";
		writeToClient(status);

		String properties = readFromClient();
		long fileLength = Long.parseLong(properties);

		writeToClient("CASE_UPLOAD/SEND_BRANO");

		Brano brano = null;
		try {
			int ricevuto = 0;
			int contatore = 0;
			System.out.println("RICEVENDO BRANO...");
			while(ricevuto==0){
				readBuffer.clear();
				ricevuto = socketChannel.read(readBuffer);
				if(ricevuto>0){
					System.out.println("DATA READ: "+ricevuto);
					contatore += ricevuto;
				}
			}
			System.out.println("TOTALE: "+contatore);
			ByteArrayInputStream byteArrInStream = new ByteArrayInputStream(readBuffer.array());
			ObjectInputStream objInStream = new ObjectInputStream(byteArrInStream);
			brano = (Brano) objInStream.readObject();
			System.out.println("BRANO RICEVUTO: "+brano.toString());
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String checkingDir = "CASE_UPLOAD/CREATING_DIRECTORIES";
		writeToClient(checkingDir);

		//List<String> splitDetails = Arrays.asList(response.split(","));

		//long fileLength = Long.parseLong(splitDetails.get(0));

		//String staticTempDir = "C:/Users/Juan/Music/MusicaPlugger/_tmp";
		//String staticDir = "C:/Users/Juan/Music/MusicaPlugger";
		String artistDir = brano.getArtista();
		String albumDir = brano.getAlbum();
		String outputFile = brano.getTitolo();

		Path staticTempPath = Paths.get(staticTempDir);
		//Path staticPath = Paths.get(staticDir);
		//Path artistPath = Paths.get(staticDir, artistDir);
		Path albumPath = Paths.get(staticDir, artistDir, albumDir);
		Path pathFile = Paths.get(staticDir, artistDir, albumDir, outputFile+".mp3");
		System.out.println("NEW OUTPUT FILE: "+pathFile.toString());
		FileChannel fileChannel = null;

		//Path tempStaticPath = Files.createTempDirectory(staticPath, "tmp");
		Path tempArtistPath = null;
		Path tempAlbumPath;
		Path tempFile = null;

		try {
			tempArtistPath = Files.createTempDirectory(staticTempPath, artistDir);
			tempAlbumPath = Files.createTempDirectory(tempArtistPath, albumDir);
			tempFile = Files.createTempFile(tempAlbumPath, outputFile, ".mp3");
			System.out.println("PATH TEMP FILE: "+tempFile.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(tempFile.toFile().exists()==true){
				try {
					fileChannel = FileChannel.open(tempFile, EnumSet.of(StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("TEMP DIRECTORIES AND FILE READY.");
				String confirm = "CASE_UPLOAD/READY";
				writeToClient(confirm);
		}else{
			String errorDir = "CASE_UPLOAD/ERROR_DIRECTORIES";
			writeToClient(errorDir);
		}

		int res = 0;
		int counter = 0;

		do{
			readBuffer.clear();
			try {
				res = socketChannel.read(readBuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(res>0){
				System.out.println("DATA READ: "+res);
				counter += res;
				readBuffer.flip();
				try {
					fileChannel.write(readBuffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("COUNTER: "+counter);
				System.out.println("FILE LENGTH: "+fileLength);
			}
		}while(counter != fileLength);

		try {
			fileChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("TOTAL READ: "+counter);

		String confirmFile = "CASE_UPLOAD/TRANSFER_TERMINATED,"+counter;
		writeToClient(confirmFile);

		String handleConfirm = readFromClient();
		switch(handleConfirm){
			case "TRANSFER_ERROR":
				System.out.println("TRANSFER ERROR.");
				tempFile.toFile().delete();
				deleteDirectory(tempArtistPath.toFile());
				String error = "CASE_UPLOAD/TRANSFER_CANCELED";
				pluggerServer.send(selKey, error.getBytes());
				break;
			case "TRANSFER_COMPLETE":
				System.out.println("TRANSFER COMPLETE.");
				if(createDirectory(albumPath)==true){
					Path finalPath = null;
					try {
						finalPath = Files.move(tempFile, pathFile, StandardCopyOption.REPLACE_EXISTING);
						System.out.println("FINAL FILE PATH: "+finalPath.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//tempFile.toFile().renameTo(pathFile.toFile());
				deleteDirectory(tempArtistPath.toFile());
				String success = "CASE_UPLOAD/TRANSFER_SUCCESSFUL";
				pluggerServer.send(selKey, success.getBytes());
				break;
		}
	}

	public void deleteDirectory(File file){

		for (File childFile : file.listFiles()) {

			if (childFile.isDirectory()) {
				deleteDirectory(childFile);
			} else {
				System.out.println("CHILD FILE: "+childFile.getPath().toString());
				/*if (!childFile.delete()) {
					throw new IOException();
				}*/
			}
		}

		if (!file.delete()) {
			System.out.println("ERROR DELETING FILE/DIRECTORY");
			try {
				throw new IOException();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean createDirectory(Path path){
		boolean exists = false;
		if(!Files.exists(path)){
			try{
				Files.createDirectories(path);
				exists = Files.exists(path);
				if(exists==true){
					System.out.println("DIRECTORY CREATED: "+path.toString());
				}else{
					System.out.println("ERROR WHILE CREATING DIRECTORY.");
				}
			}catch(IOException e){
				e.printStackTrace();
			}
		}else{
			System.out.println("DIRECTORY ALREADY EXISTS.");
			exists = true;
		}
		return exists;
	}

	public synchronized void disconnect(){
		System.out.println("DISCONNECT METHOD ACTIVE.");

		if(getStatus().contentEquals(CONNECTED)){
			PluggerServer.clientMap.remove(selKey);
			if (selKey != null)
				selKey.cancel();

			if (socketChannel == null)
				return;

			try {
				System.out.println("Client disconnected: " + (InetSocketAddress) socketChannel.getRemoteAddress());
				System.out.println("Online clients: " + PluggerServer.clientMap.size());
				socketChannel.close();
				setStatus(DISCONNECTED);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("CLIENT ALREADY DISCONNECTED.");
		}

	}

	public void setStatus(String newStatus){
		status = newStatus;
		System.out.println("USER ID: "+getIdUser()+", NEW STATUS: "+status);
	}

	public String getStatus(){
		return status;
	}

	public String bufferToString(ByteBuffer buffer) {
		buffer.flip();
		byte[] bytes = new byte [buffer.remaining()];
		System.out.println("BYTES LENGTH: "+bytes.length);
		buffer.get(bytes);
		String converted = new String(bytes);
		System.out.println("CONVERTED LENGHT: "+converted.length());
		return converted;
	}

}
