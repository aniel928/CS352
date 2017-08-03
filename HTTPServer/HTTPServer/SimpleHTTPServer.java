import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class SimpleHTTPServer{    
	
	//Initialize variables to be seen in all parts of the program and last the life of the program (Global/Static)
	static Socket newClient = null;
	static String filename = null;
	static String pathname = null;
	static PrintWriter socketOut = null;
	static BufferedReader socketIn = null;
					
	
	//Error Handling (method to generate all responses except 200 OK)
	static String getResponse(int responseNumber){
		String returnmsg = null;
		switch(responseNumber){
			case 400:
				returnmsg = "400 Bad Request";
				break;
			case 404:
				returnmsg = "404 Not Found";
				break;
			case 501:
				returnmsg = "501 Not Implemented";
				break;
			case 408:
				returnmsg = "408 Request Timeout";
				break;
			default:
				returnmsg = "500 Internal Error";
				break;
		}
		return returnmsg;
	}
	
	//Check for valid command
	static void checkCommand(String command){
		String returnmsg = null;
		switch(command){
			case "GET":
				getCommand();
				break;
			default:
				System.out.println("Bad command given.");
				returnmsg = getResponse(501);		
				socketOut.println(returnmsg);
		}
		return;

	}
	
	//when GET is given.	
	static void getCommand(){
		File userFile = null;
		BufferedReader fileText = null;
		String returnmsg = null;
		
		//"Open" File
		userFile = new File(pathname);
		//Check to see if file exists (regardless of read access), if not then 404.
		if(userFile.exists()){
			//Read File into String:
			try{
				String nextLine;
				fileText = new BufferedReader(new FileReader(userFile));
				socketOut.println("200 OK\n");
				socketOut.flush();
				
				while ((nextLine = fileText.readLine()) != null) {
					System.out.println(nextLine);
					socketOut.println(nextLine);
					socketOut.flush();
				}
					
				 fileText.close();
					
			} 
			catch(IOException e) {
				System.out.println(e.getMessage());
				returnmsg = getResponse(500);
				socketOut.println(returnmsg);
				socketOut.flush();
				return;
			}

			socketOut.flush();
			return;			
			
		}
		else{
			System.out.println("No such file.");
			returnmsg = getResponse(404);
			socketOut.println(returnmsg);
			socketOut.flush();
			return;
		}
	}
	    
	/*static void postCommand(){
		//implement down the line
	}*/
	
	/*static void headCommand(){
		//implement down the line
	}*/
	
	
	//Main Method
    public static void main(String[] args){
		//Check arguments and assign into variable    
    	if(args.length != 1){
    		System.err.println("Invalid number of arguments.  Please enter port number.");
    		return;
    	}
    	int port;
		try{
		    port = Integer.parseInt(args[0]);
	    }
	    catch(Exception e){
	    	System.err.println("Please provide an integer port number as input.");
	    	System.err.println(e.getMessage());
	    	return;
	    }
	    
	    //Construct ServerSocket
	    ServerSocket newSocket = null;
		try{
			newSocket = new ServerSocket(port);
		}
		catch(IOException e){		
			System.err.println("Error creating new server socket.");
	    	System.err.println(e.getMessage());
			return;
		}
		//Keep socket open with infinite loop
		while(true){
			System.out.println("Ready for new connections...");
			try{
				//Blocking call waiting for a connection
				newClient = newSocket.accept();
	
				try{
					newClient.setSoTimeout(3000);
				}
				catch(SocketException e){
					System.out.println("Failed to set timeout");
					System.out.println(e.getMessage());
					return;
				}
			}
			catch(IOException e){
					System.err.println("Error accepting new connections.");
			    	System.err.println(e.getMessage());
			    	return;
			}
			
			//Spawns thread and hands off work in order to accept another connection ASAP
			ClientThread newThread = new ClientThread();
			newThread.start(); //calls "run" method defined below.
			
		}
    }
     


	//Create new class to handle thread
	static class ClientThread extends Thread{
		//Must overwrite this method "ruN". This is the code that the thread is going to run
	    public void run(){
	    	//Initializing variables
			String msg = null;
			String returnmsg = null;
			String command = null;
			
			//Set up streams
			try{			
				socketOut = new PrintWriter(newClient.getOutputStream(), true);
				socketIn = new BufferedReader(new InputStreamReader(newClient.getInputStream()));	
			}
			catch(IOException e){//if streams fail.
				System.err.println("Error setting up IO streams.");
		    	System.err.println(e.getMessage());
		    	socketOut.println(getResponse(500));
		    	return;
			}
			//read in line from server - this is where timeout might occur.
			try{
				msg = socketIn.readLine();
			}
			catch(IOException e){//connection timed out 
				System.err.println("XXConnection to client timed out.");
		    	System.err.println(e.getMessage());
		    	socketOut.println(getResponse(408));
		    	return;
			}
			//Parsing the message to check for Format
			int i = 0;

			//Check that first char is not white space
			try{
				if(msg.charAt(i) == ' '){
					returnmsg = getResponse(400);
					socketOut.println(returnmsg);
					socketOut.flush();
					return;
				}
			}
			catch(Exception e){//if something goes wrong (such as null string), send Bad Format
				System.err.println("Issue with incoming message.");
				System.err.println(e.getMessage());
				returnmsg = getResponse(400);
				socketOut.println(returnmsg);
				return;
			}
			//Check that all characters are upper case before the only white space
			try{
				while(i < msg.length() && msg.charAt(i) != ' '){
					if(!Character.isUpperCase(msg.charAt(i))){ //How to check for upper case
						System.out.println("Does not start with upper case command");
						returnmsg = getResponse(400);
						socketOut.println(returnmsg);
						socketOut.flush();
						return;
					}
					else{
						i++;
					}
				}
			}
			catch(Exception e){//catching possibility of falling off end of string.
				System.err.println("Issue with incoming message.");
				System.err.println(e.getMessage());
				returnmsg = getResponse(400);
				socketOut.println(returnmsg);
				return;
			}
			//Throw error if there is no space in all of message
			if(i == msg.length()){
				System.out.println("No space in message anywhere");
				returnmsg = getResponse(400);
				socketOut.println(returnmsg);
				socketOut.flush();
				return;
			}
			//Well formatted command, save into var called command
			command = msg.substring(0,i); //i is at white space right now, index 3
			i++; //Increment past current spot (White Space)
			
			//Check for '/'
			try{
				if(msg.charAt(i) != '/'){ //Should be pointing at a '/'
					System.out.println("no backslash after space");
					returnmsg = getResponse(400);
					socketOut.println(returnmsg);
					socketOut.flush();
					return;			
				} 	
			}
			catch(Exception e){//catch bad formats (like "GET " with no filename)
				System.err.println("Issue with incoming message.");
				System.err.println(e.getMessage());
				returnmsg = getResponse(400);
				socketOut.println(returnmsg);
				return;
			}
			i++; //Increment past current spot ('/') , should now be pointing at fileName
			filename = msg.substring(i);
			pathname = "./doc_root/"+filename;

			//Check for spaces inside of filename (filename should be unbroken text)
			while(i < msg.length()){ //Cannot have spaces in the fileName
				if(msg.charAt(i) == ' '){
					System.out.println("Space in filename");
					returnmsg = getResponse(400);
					socketOut.println(returnmsg);
					socketOut.flush();
					return;
				}
				else{
					i++;
				}
			}			
			
			//Check for valid command, in this case 'GET'
			checkCommand(command);//in this method after finding valid command, other methods are called to do the work.
			
			//close out of streams and socket.			
			try{
				socketOut.close();
				socketIn.close();
				newClient.close();
			}
			catch(IOException e){
				System.err.println("Error closing out of streams and sockets.");
				System.err.println(e.getMessage());
		    	return;
			}
			
			//wait for 1/4 second (1000 milliseconds = 1 second, so 250 milliseconds = .25 seconds or 1/4 second.
			try{
				Thread.sleep(250);
			}
			catch(InterruptedException e){
				System.err.println(e.getMessage());
				return;
			}

			//return to exit thread.			
		    return;
	    }
	}

    
}