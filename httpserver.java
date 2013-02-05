import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Vector;

public class Server{
	public static void main(String args[]) throws Exception{
		int port = 8080;
		ServerSocket listenSocket = new ServerSocket(port);
		System.out.println("HTTP server started on port " + port + ".");
		while(true){			
			Socket s = listenSocket.accept();
			Clienthandler c = new Clienthandler (s);
			Thread t = new Thread (c);
			t.start();
		}
	}
}

class Clienthandler implements Runnable{
	
	Socket connectionSocket;
	
	Clienthandler(Socket s){
		connectionSocket = s;
	}
	
	void run(){
		try{
	
		} catch (SocketTimeoutException e) {
			sendPacket(.....);
		}
	}
	

	returntype sendPacket(int statusCode,  , something content){
		outToClient.
	}

}
