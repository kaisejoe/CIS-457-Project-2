import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import javax.activation.*;

public class HttpServer {
  private static final int port = 8080;
  public static void main(String args[]) throws Exception{
		ServerSocket listener = new ServerSocket(port);
		System.out.println("HTML server is running on port " + port + ".");

		while(true){
			Socket s = listener.accept();
			Clienthandler c = new Clienthandler(s);

			Thread t = new Thread(c);
			t.start();
		}
	}
}

class Clienthandler implements Runnable{
	Socket connection;
	String line;
	String mimeType;
	String date;
	String lastMod;
	String ifMod;
	File file;
	StringTokenizer st;
	byte[] fileBytes;
	BufferedReader clientRequest;
	DataOutputStream clientReply;

	static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			
	boolean valid = false;

	Clienthandler(Socket s){
		connection = s;

		try{
			connection.setKeepAlive(false);
		}catch(Exception e){
			System.out.println("Error at socket creation");
		}
	}

	public void run(){
		try{
			boolean open = true;
			ifMod = "";
			clientRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			clientReply = new DataOutputStream(connection.getOutputStream());
			line = clientRequest.readLine();
			System.out.println(line); /////////////////
			valid = line.startsWith("GET");
			System.out.println(valid); ////////////////

			if(!valid) sendNotImplemented();

			if(!parseRequest()) sendNotFound();


			mimeType = URLConnection.guessContentTypeFromName(file.getName());
			System.out.println(mimeType);////////////
			
			fileBytes = Files.readAllBytes(file.toPath());
			lastMod = getLastModified();
			
			if(dateFormat.parse(lastMod).after(dateFormat.parse(ifMod))){
				sendNotModified();
			}else{
				System.out.println(fileBytes.length);////////////
				System.out.println(lastMod); ///////////////
				date = getServerTime();
				System.out.println(date);/////////////////
				System.out.println(mimeType + "  " + lastMod + " " + date);
				sendValidResponse();

				connection.close();
			}
		}catch(Exception e){
			System.out.println("Error running.");
		}
	}

	private synchronized void sendNotImplemented(){
		try{
			clientReply.writeBytes("HTTP/1.1 501 Not Implemented\r\n");
			clientReply.writeBytes("Date: " + date + "\r\n");
			clientReply.writeBytes("Connection: close\r\n");
			clientReply.writeBytes("\r\n");
			clientReply.flush();
			connection.close();
		}catch(Exception e){
			System.out.println("Error sending 501.");
		}
	}

	private synchronized void sendNotFound(){
		try{
			clientReply.writeBytes("HTTP/1.1 404 Not Found\r\n");
			clientReply.writeBytes("Date: " + date + "\r\n");
			clientReply.writeBytes("Connection: close\r\n");
			clientReply.writeBytes("\r\n");
			clientReply.writeBytes("404 File Not Found\n\n");
			clientReply.writeBytes("The server was unable to find the file requested.");
			clientReply.flush();
			connection.close();
		}catch(Exception e){
			System.out.println("Error sending 404.");
		}
	}
	
	private synchronized void sendNotModified(){
		try{
			clientReply.writeBytes("HTTP/1.1 304 Not Modified\r\n");
			clientReply.writeBytes("Date: " + date + "\r\n");
			if(connection.getKeepAlive()){
				clientReply.writeBytes("Connection: keep-alive\r\n");
			}else{
				clientReply.writeBytes("Connection: close\r\n");
			}
			clientReply.writeBytes("\r\n");
			clientReply.flush();
			connection.close();
		}catch(Exception e){
			System.out.println("Error sending 501.");
		}
	}

	private synchronized void sendValidResponse(){
		try{
			clientReply.writeBytes("HTTP/1.1 200 OK\r\n");
			clientReply.writeBytes("Content-Length: " + fileBytes.length + "\r\n");
			clientReply.writeBytes("Content-Type: " + mimeType + "\r\n");
			clientReply.writeBytes("Last-Modified: " + lastMod + "\r\n");
			clientReply.writeBytes("Date: " + date + "\r\n");
			if(connection.getKeepAlive()){
				clientReply.writeBytes("Connection: keep-alive\r\n");
			}else{
				clientReply.writeBytes("Connection: close\r\n");
			}
			clientReply.writeBytes("\r\n");

			for(int i = 0; i < fileBytes.length; i++){
				clientReply.write(fileBytes[i]);
			}
			clientReply.flush();
			connection.close();
		}catch(Exception e){
			System.out.println("Error sending 200.");
		}
	}

	private boolean parseRequest(){
		String temp;
		try{
			line = line.substring(3);
			System.out.println(line); /////////
			st = new StringTokenizer(line);
			temp = st.nextToken();
			file = new File(temp.substring(1));
			System.out.println(file.toString()); /////////
			line = clientRequest.readLine();
			while(line != null){
				st = new StringTokenizer(line);
				temp = st.nextToken();
				if(temp.equals("If-Modified-Since:")){
					ifMod = st.nextToken();
				}else if(temp.equals("Connection:")){
					if(st.nextToken().equals("keep-alive")){
						connection.setSoTimeout(20000);
					}
				}
				line = clientRequest.readLine();
			}
		}catch(Exception e){
			System.out.println("Error parsing request.");
		}

		return file.exists();
	}

	private String getLastModified(){
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(file.lastModified());
	}

	private String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}
}
