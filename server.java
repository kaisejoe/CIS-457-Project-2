import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Vector;

public class Server{
  /*
   * Match Port Number on Server and Client
	 */
	public static void main(String args[]) throws Exception{
		int port = 8080;
		ServerSocket listenSocket = new ServerSocket(port);
		while(true){
			Socket s = listenSocket.accept();
			Clienthandler c = new Clienthandler (s);
			Thread t = new Thread (c);
			t.start();
		}
	}
}
// User Class, Holds Socket and Name for single User
class User{
	private String name;
	private Socket s;
	public User(Socket ps, String pname){
		name = pname;
		s = ps;
	}
	String getName(){
		return name;
	}
	void setName(String str){
		name = str;
	}
	Socket getSocket(){
		return s;
	}
	void setSocket(Socket ps){
		s = ps;
	}
	
}
//User Info is static vector holding all User's Info
class UserInfo{
    private static Vector<User> userInfo = new Vector<User>();
    
    public static Vector<User> getData() {
        return userInfo;
    }
    public static void addData(User user) {
        userInfo.add(user);
    }
    public static void removeData(User user){
        userInfo.remove(user);
    }
}
//Thread for each User
class Clienthandler implements Runnable{
	User user;
	Socket connectionSocket;
	Boolean valid = true;
	ArrayList<User> sendTo = new ArrayList<User>();
	Clienthandler(Socket s){
		connectionSocket = s;
	}
	public void run(){
		try{
			boolean open = true;
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			
			//OutStream for User
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			
			//OutStream to send to Friends
			DataOutputStream outToFriend;
			String clientID = "";
			
			//Read in Client User ID
			while(valid){
				clientID = inFromClient.readLine();
				if(UserInfo.getData().isEmpty()){
					valid=false;
					break;
				}
				if(checkUser(clientID)){
					System.out.println("#3");
					System.out.println("ID already used");
					outToClient.writeBytes("ID already taken. Enter new ID: "+'\n');
					outToClient.flush();
				}else{
                    valid = false;
                }
			}
			//Create User and add to UserInfo
			valid=true;
			user = new User(connectionSocket, clientID);
			UserInfo.addData(user);
			String friend;
			System.out.println("User added: "+clientID);
			outToClient.writeBytes("Welcome to YOCOChat, " + clientID+"! You Oughta Chat Often! \n");
            outToClient.writeBytes("Please chat responsibly. Enjoy your stay! \n");
            
			outToClient.flush();
			
			//Loop for all commands and Messages
			while(open){
				outToClient.writeBytes("Enter your command or _Help for list of commands: "+'\n');
				outToClient.flush();
				String clientMessage = inFromClient.readLine();
				System.out.println("received: "+clientMessage);
				
				//Exit Command
				if(clientMessage.equals("_Exit")){
					outToClient.writeBytes("Thank you for using YOCOChat!");
					outToClient.flush();
					UserInfo.removeData(user);
					open=false;
					
				}
				
				//Close Server Command
				else if(clientMessage.equals("_Close Server")){
					outToClient.writeBytes("The server has been closed.");
					outToClient.flush();
					UserInfo.removeData(user);
					connectionSocket.close();
					System.exit(0);
					open=false;
				}
				
				//List Command
				else if(clientMessage.equals("_List")){
					String usersString ="";
					for(int i =0; i < UserInfo.getData().size(); i++){
						usersString += (UserInfo.getData().get(i).getName().toString() + " ");
					}
					outToClient.writeBytes("The Available users are: " + usersString + '\n');
					outToClient.flush();
				}
				
				//Help Command
				else if(clientMessage.equals("_Help")){
					outToClient.writeBytes("To send a message to one or multiple users, enter each user's name followed by the return key.\n");
					outToClient.flush();
					outToClient.writeBytes("_List - Lists all connected Users \n");
					outToClient.flush();
					outToClient.writeBytes("_Close Server - Closes the server \n");
					outToClient.flush();
					outToClient.writeBytes("_Exit - exits from the IM client \n");
					outToClient.flush();
					outToClient.writeBytes("_Single - sends message to single user \n");
					outToClient.flush();
					outToClient.writeBytes("_Multi - sends message to multiple users \n");
					outToClient.flush();
					outToClient.writeBytes("_All - sends message to all users\n");
					outToClient.flush();
				}
				
				//Send to Single User
				else if(clientMessage.equals("_Single")){
					outToClient.writeBytes("Enter user to send to: \n");
					outToClient.flush();
					boolean found = false;
                    while(found == false){
                        friend = inFromClient.readLine();
                        User temp, temp2;
                        for(int i = 0; i < UserInfo.getData().size(); i++){
                            temp = UserInfo.getData().get(i);
                            if(friend.equals("_Cancel")){
                                found = true;
                            }else if(temp.getName().equals(friend)){
                                temp2 = new User(temp.getSocket(), friend);
                                outToClient.writeBytes("Enter your message: \n");
                                outToClient.flush();
                                clientMessage = inFromClient.readLine();
                                outToClient.writeBytes("To (" + temp.getName() + "): " + clientMessage + '\n');
                                outToClient.flush();
                                outToFriend = new DataOutputStream(temp2.getSocket().getOutputStream());
                                outToFriend.writeBytes("From (" + clientID + "): " + clientMessage + '\n');
                                outToFriend.flush();
                                found = true;
                            }
                        }
                        if(found == false){
                            outToClient.writeBytes("Invalid Username - Re-enter Username (_Cancel to cancel): ");
                            outToClient.flush();
                        }
                    }
					
					
                    //Send to Multiple Users
				}else if(clientMessage.equals("_Multi")){
                    sendTo = new ArrayList<User>();
					outToClient.writeBytes("Enter users to message (_Done when finished): \n");
					outToClient.flush();
					friend = inFromClient.readLine();
					User temp, temp2;
                    String validUsers="";
                    int counter = 0, counter2 = 0;
                    if(!friend.equals("_Cancel")){
                        while(!friend.equals("_Done")){
                            counter++;
                            for(int i = 0; i < UserInfo.getData().size(); i++){
                                temp = UserInfo.getData().get(i);
                                if(temp.getName().equals(friend)){
                                    temp2 = new User(temp.getSocket(), friend);
                                    sendTo.add(temp2);
                                    counter2++;
                                }
                            }
                            friend = inFromClient.readLine();
                        }
                        if(counter>counter2){
							outToClient.writeBytes("There was an invalid Username. Your message will be sent to all valid recipients. ");
							outToClient.flush();
                        }
                        outToClient.writeBytes("Enter your message:\n");
                        outToClient.flush();
                        clientMessage = inFromClient.readLine();
                        for(int i = 0; i < sendTo.size(); i++){
                            outToFriend = new DataOutputStream(sendTo.get(i).getSocket().getOutputStream());
                            outToFriend.writeBytes("From (" + clientID + "): " + clientMessage + '\n');
                            outToFriend.flush();
                            validUsers+=" " + UserInfo.getData().get(i).getName().toString();
                        }
                        outToClient.writeBytes("To (" + validUsers + " ): " + clientMessage + '\n');
                        outToClient.flush();
                    } else {
                        outToClient.writeBytes("Operation cancelled." + '\n');
                        outToClient.flush();
                    }
				}
                
				//Send to all Users
				else if(clientMessage.equals("_All")){
					outToClient.writeBytes("Enter Message to send to all YOCOChatters: \n ");
					outToClient.flush();
					clientMessage = inFromClient.readLine();
                    outToClient.writeBytes("To All: " + clientMessage + '\n');
					for(int i = 0; i < UserInfo.getData().size(); i++){
						outToFriend = new DataOutputStream(UserInfo.getData().get(i).getSocket().getOutputStream());
						outToFriend.writeBytes("From (" + clientID + "): " + clientMessage + '\n');
						outToFriend.flush();
					}
				}
				
				//Invalid Command, Loops back and asks for new command
				else
					outToClient.writeBytes("That is not a valid command."+'\n');
                outToClient.flush();
			}
		}
		catch(Exception e){
			System.out.println("Got an exception\n" + e.getMessage());
            System.exit(0);
		}
	}
	
	//Checks Username
	boolean checkUser(String pUser){
		boolean result = false;
		for(int i=0; i<UserInfo.getData().size(); i++){	
			User tempName = UserInfo.getData().get(i);
			if(tempName.getName().equals(pUser)){
				result = true;
				break;
			}
		}
		return result;
	}
}
