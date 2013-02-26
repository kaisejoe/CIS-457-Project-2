import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import javax.activation.*;

public class HttpServer1234 {
    private static int port = 8080;
    public static void main(String args[]) throws Exception{
        /* Reads in the Args from the project call
         *  Will read in all args, does not matter the order as
         *  long as each call is preceded by the proper - identifier
         * Will set the log file, doc root, and port properly
         * if no port, port is preset to 8080.
         */
  	String docroot = System.getProperty("user.dir"); //set default directory
		String logfile = "", argLog="";
		for(int x = 0; x < args.length; x++){
            argLog+=args[x]+" ";
			switch(args[x]){
				case"-p":{
					x++;
					port = Integer.parseInt(args[x]);
                    argLog+=port+" ";
					break;
				}case"-docroot":{
					x++;
					docroot = args[x];
                    argLog+=docroot+" ";
					break;
				}case"-logfile":{
					x++;
					logfile = args[x];
                    argLog+=logfile+" ";
					System.out.println("Log:" + logfile);
					break;
				}default:{
					System.out.println("Error reading args");
				}
			}
		}
		ServerSocket listener = new ServerSocket(port);
		System.out.println("HTML server is running on port " + port + ".");
        
		while(true){
			Socket s = listener.accept();
			Clienthandler c = new Clienthandler(s, docroot, logfile, argLog);
            
			Thread t = new Thread(c);
			t.start();
		}
	}
}

class Clienthandler implements Runnable{
	Socket connection;
    String line, mimeType, date, lastMod, ifMod, root, error;
	private static String log;
    private static ArrayList<String> mod = new ArrayList<String>();
	File file;
	StringTokenizer st;
	byte[] fileBytes;
    boolean boolModSince = true;
	BufferedReader clientRequest;
	DataOutputStream clientReply;
    
	static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    
	boolean valid = false;
    
    
	Clienthandler(Socket s, String docroot, String logfile, String pArgLog){
        root = docroot;
        //System.out.println("root is: " + root);
        try{
            connection = s;
			connection.setKeepAlive(false);
            connection.setSoTimeout(500);
        }
        catch(Exception e){
            System.out.println("Timeout error: "+e);
        }
		log = logfile;
        writeToLog(pArgLog+"\r\n",log);
	}
    
	public void run(){
		try{
            System.out.println("The directory is: "+root);
			ifMod = "";
            error = "";
			clientRequest = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			clientReply = new DataOutputStream(connection.getOutputStream());
			line = clientRequest.readLine();
            System.out.println("Original line****: ");
            writeToLog(line+"\r\n",log);
			System.out.println(line); /////////////////
            valid = line.startsWith("GET");
            
			System.out.println(valid); ////////////////
            
			if(!valid) sendNotImplemented();
            
			if(!parseRequest()) sendNotFound();
            else{
                
                mimeType = URLConnection.guessContentTypeFromName(file.getName());
                System.out.println(mimeType);////////////
                
                fileBytes = Files.readAllBytes(file.toPath());
                lastMod = getLastModified();
                date = getServerTime();
                
                if(!boolModSince){
                    sendNotModified();
                /*System.out.println("ifMod is: "+ifMod);
                if(!ifMod.equals("")){
                    if(dateFormat.parse(lastMod).after(dateFormat.parse(ifMod))){
                        sendNotModified();
                    }*/
                }else{
                    System.out.println(mimeType + "  " + lastMod + " " + date);
                    sendValidResponse();
                }
                
                /*if(dateFormat.parse(lastMod).after(dateFormat.parse(ifMod))){
                 sendNotModified();
                 }else{
                 System.out.println(fileBytes.length);////////////
                 System.out.println(lastMod); ///////////////
                 date = getServerTime();
                 System.out.println(date);/////////////////
                 System.out.println(mimeType + "  " + lastMod + " " + date);
                 sendValidResponse();
                 
                 //connection.close();*/
                //}
            }
        }catch(Exception e){
            error =e.toString();
            System.out.println("\nError running:   "+e);
        } finally{
            if(error.equals("java.net.SocketTimeoutException: Read timed out")){
                try{
                    connection.close();
                    if(connection.isClosed()) System.out.println("The connection has been closed.");
                }
                catch(Exception f){
                    System.out.println("Could not time out correctly because: "+f);
                }
            }
            error="";
        }
    }
    
    private synchronized void sendNotImplemented(){
        try{
            date = getServerTime();
            
            clientReply.writeBytes("HTTP/1.1 501 Not Implemented\r\n");
            writeToLog("HTTP/1.1 501 Not Implemented\r\n", log);
            clientReply.writeBytes("Date: " + date + "\r\n");
            writeToLog("Date: " + date + "\r\n", log);
            clientReply.writeBytes("Connection: close\r\n");
            writeToLog("Connection: close\r\n", log);
            clientReply.writeBytes("\r\n");
            writeToLog("\r\n", log);
            clientReply.flush();
            connection.close();
        }catch(Exception e){
            System.out.println("Error sending 501.");
        }
    }
    
    private synchronized void sendNotFound(){
        try{
            //clientReply.writeBytes("HTTP/1.1 404 Not Found\r\n");
            writeToLog("HTTP/1.1 404 Not Found\r\n", log);
            /*clientReply.writeBytes("Date: " + date + "\r\n");
             writeToLog("Date: " + date + "\r\n", log);
             clientReply.writeBytes("Connection: close\r\n");
             writeToLog("Connection: close\r\n", log);
             clientReply.writeBytes("\r\n");
             writeToLog("\r\n", log);
             clientReply.writeBytes("404 File Not Found\r\n");
             writeToLog("404 File Not Found\r\n", log);
             clientReply.writeBytes("The server was unable to find the file requested.");
             writeToLog("The server was unable to find the file requested.", log);*/
            file = new File("404.html");
            fileBytes = Files.readAllBytes(file.toPath());
            clientReply.writeBytes("\r\n");
            writeToLog("\r\n", log);
            for(int i = 0; i < fileBytes.length; i++){
                clientReply.write(fileBytes[i]);
            }
            clientReply.flush();
            connection.close();
        }catch(Exception e){
            System.out.println("Error sending 404.");
        }
    }
    
    private synchronized void sendNotModified(){
        try{
            date = getServerTime();
            clientReply.writeBytes("HTTP/1.1 304 Not Modified\r\n");
            writeToLog("HTTP/1.1 304 Not Modified\r\n", log);
            clientReply.writeBytes("Date: " + date + "\r\n");
            writeToLog("Date: " + date + "\r\n", log);
            if(connection.getKeepAlive()){
                clientReply.writeBytes("Connection: keep-alive\r\n");
                writeToLog("Connection: keep-alive\r\n", log);
            }else{
                clientReply.writeBytes("Connection: close\r\n");
                writeToLog("Connection: close\r\n", log);
            }
            clientReply.writeBytes("\r\n");
            writeToLog("\r\n", log);
            clientReply.flush();
            connection.close();
        }catch(Exception e){
            System.out.println("Error sending 501.");
        }
    }
    
    private synchronized void sendValidResponse(){
        try{
            date = getServerTime();
            
            clientReply.writeBytes("HTTP/1.1 200 OK\r\n");
            writeToLog("HTTP/1.1 200 OK\r\n", log);
            clientReply.writeBytes("Content-Length: " + fileBytes.length + "\r\n");
            writeToLog("Content-Length: " + fileBytes.length + "\r\n", log);
            clientReply.writeBytes("Content-Type: " + mimeType + "\r\n");
            writeToLog("Content-Type: " + mimeType + "\r\n", log);
            clientReply.writeBytes("Last-Modified: " + lastMod + "\r\n");
            writeToLog("Last-Modified: " + lastMod + "\r\n", log);
            clientReply.writeBytes("Date: " + date + "\r\n");
            writeToLog("Date: " + date + "\r\n", log);
            if(connection.getKeepAlive()){
                clientReply.writeBytes("Connection: keep-alive\r\n");
                writeToLog("Connection: keep-alive\r\n", log);
            }else{
                clientReply.writeBytes("Connection: close\r\n");
                writeToLog("Connection: close\r\n", log);
            }
            clientReply.writeBytes("\r\n");
            writeToLog("\r\n", log);
            
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
            line = line.substring(3).trim();
            System.out.println("Trimmed line****: ");
            System.out.println(line); /////////
            st = new StringTokenizer(line);
            temp = st.nextToken();
            //Security. Checking to make sure the client doesn't go into a folder.
            temp = temp.substring(temp.lastIndexOf("/"));
            System.out.println("The file is: "+root+temp);
            file = new File(temp.substring(1));
            System.out.println(mod.toString());
            if(mod.contains(temp+" "+file.lastModified())){
                System.out.println("NOT MODIFIED");
                boolModSince=false;
                //sendNotModified();
            }else{
                mod.add(temp+" "+file.lastModified());
                System.out.println("mod after: "+mod.toString());
                System.out.println(file.toString()); /////////
                line = clientRequest.readLine();
                //while(line != null){
                while(st.hasMoreTokens()){
                    //st = new StringTokenizer(line);
                    temp = st.nextToken();
                    if(temp.equals("If-Modified-Since:")){
                        ifMod = st.nextToken();
                    }else if(temp.equals("Connection:")){
                        if(st.nextToken().equals("keep-alive")){
                            connection.setSoTimeout(20000);
                        }else{
                            connection.setSoTimeout(0);
                        }
                    }
                    //line = clientRequest.readLine();
                }
            }
        }catch(Exception e){
            System.out.println("\nError parsing request:   "+e);
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
    private static synchronized void writeToLog(String text, String logfile){
        if(logfile.equals("")){
            System.out.println("No Log File Specified.");
        }else{
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
                out.println(text);
                out.close();
            } catch (IOException e) {
                System.out.println("Error Writing to Log.");
                e.printStackTrace();
            }
        }
    }
}
