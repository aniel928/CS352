import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class HTTP1Server{    
	
	//Initialize variables to be seen in all parts of the program and last the life of the program (Global/Static)
	static Socket newClient = null;
	static String filename = null;
	static PrintWriter socketOut = null;
	static BufferedReader socketIn = null;
	static int status = -1;
	static List<Thread> newThreads = Collections.synchronizedList(new ArrayList<Thread>(5));			
	static ArrayList<String> msgList = new ArrayList<String>();
	static String returnmsg = null;
	static String escapeChar[][] = {{"!","%21"},{"*","%2A"},{"'","%27"},{"(","%28"},{")","%29"},{";","%3B"},{":","%3A"},{"@","%40"},{"&","%26"},{"=","%3D"},{"+","%2B"},{"$","%24"},{",","%2C"},{"/","%2F"},{"?","%3F"},{"#","%23"},{"[","%5B"},{"]","%5D"},{" ","%20"}};
	static String url = null;
	static int contentLength = 0;
	static String portno = null;
	static String from = null;
	static String userAgent = null;
	
	//takes extension, goes through switch case to find Content-Type
	static String findFileType(String ext){
		String type = null;
	
		switch(ext){
			case ".c":
			case ".c++":
			case ".cc":
			case ".com":
			case ".conf":
			case ".cxx":
			case ".def":
			case ".f":
			case ".f90":
			case ".for":
			case ".g":
			case ".h":
			case ".hh":
			case ".idc":
			case ".jav":
			case ".java":
			case ".list":
			case ".log":
			case ".lst":
			case ".m":
			case ".mar":
			case ".pl":
			case ".sdml":
			case ".text":
			case ".txt":
				type = "text/plain";
				break;
			case ".acgi":
			case ".htm":
			case ".html":
			case ".htmls":
			case ".htx":
			case ".shtml":
				type = "text/html";
				break;
			case ".gif":
				type = "image/gif";
				break;
			case ".jfif":
			case ".jfif-tbnl":
			case ".jpe":
			case ".jpeg":
			case ".jpg":
				type = "image/jpeg";
				break;
			case ".png":
			case ".x-png":
				type = "image/png";
				break;
			case ".pdf":
				type = "application/pdf";
				break;
			case ".gz":
			case ".gzip":
				type = "application/x-gzip";
				break;
			case ".zip":
				type = "application/zip";
				break;
			default:
				type = "application/octet-stream";
		}
		return type;
	}
	
	//Error Handling (method to generate all responses except 200 OK)
	static String getResponse(int responseNumber){
		returnmsg = null;
		switch(responseNumber){
			case 204:
				returnmsg = "HTTP/1.0 204 No Content";
				break;
			case 304: 
				returnmsg = ("HTTP/1.0 304 Not Modified" + '\r' + '\n');
				returnmsg += "Expires: Sat, 21 Jul 2018 11:00:00 GMT";
				break;
			case 400:
				returnmsg = "HTTP/1.0 400 Bad Request";
				break;
			case 403:
				returnmsg = "HTTP/1.0 403 Forbidden";
				break;
			case 404:
				returnmsg = "HTTP/1.0 404 Not Found";
				break;
			case 405:
				returnmsg = "HTTP/1.0 405 Method Not Allowed";
				break;
			case 408:
				returnmsg = "HTTP/1.0 408 Request Timeout";
				break;
			case 411:
				returnmsg = "HTTP/1.0 411 Length Required";
				break;
			case 418:
				returnmsg = "HTTP/1.0 418 I'm a teapot";
				break;
			case 500:
				returnmsg = "HTTP/1.0 500 Internal Server Error";
				break;
			case 501:
				returnmsg = "HTTP/1.0 501 Not Implemented";
				break;
			case 503:
				returnmsg = "HTTP/1.0 503 Service Unavailable";
				break;
			case 505:
				returnmsg = "HTTP/1.0 505 HTTP Version Not Supported";
				break;
			default:
				returnmsg = "HTTP/1.0 500 Internal Server Error";
				break;
		}
		return returnmsg;
	}
	
	//Check for valid command then run that command
	static String executeCommand(String command, String pathname){
		returnmsg = null;
		switch(command){
			case "GET":
				returnmsg = getCommand(pathname);
				break;
			case "POST":
				returnmsg = postCommand(pathname);
				break;
			case "HEAD":
				returnmsg = headCommand(pathname);
				break;
			case "PUT":
				System.out.println("PUT not implemented");
				returnmsg = getResponse(501);		
				break;
			case "DELETE":
				System.out.println("DELETE not implemented");
				returnmsg = getResponse(501);		
				break;
			case "LINK":
				System.out.println("LINK not implemented");
				returnmsg = getResponse(501);		
				break;
			case "UNLINK":
				System.out.println("UNLINK not implemented");
				returnmsg = getResponse(501);		
				break;
			default:
				System.out.println("Bad format");
				returnmsg = getResponse(400);		
		}
		return returnmsg;

	}

	//check for valid file stuff	
	static File validFile(String pathname){
		File userFile = null;
		userFile = new File (pathname);

		if(!userFile.exists()){
			System.out.println("No such file.");
			userFile = null;
			status = 404;
			return userFile;
		}
		if(!userFile.canRead()){
			userFile = null;
			status = 403;			
			return userFile;
		}
		return userFile;
	}
	
	//get and return extension of filename
	static String getExtension(String pathname){
		int start = -1;
		start = pathname.lastIndexOf(".");	
		String ext = pathname.substring(start);
		return ext;
	}
	
	//when GET is given.	
	static String getCommand(String pathname){
		//Set up vars
		BufferedReader fileText = null;
		returnmsg = null;
		
		//get file and ext
		String ext = getExtension(pathname);
		File userFile = validFile(pathname);	
		
		//if null then not valid file, return with proper code.
		if(userFile == null){
			returnmsg = getResponse(status);
			return returnmsg;
		}
		
		//if text of some sort, print using text
		if(findFileType(ext).equals("text/plain") || findFileType(ext).equals("text/html")){
			try{
				int nextLine;
				fileText = new BufferedReader(new FileReader(userFile));
				
				returnmsg = (headCommand(pathname) + '\r' + '\n' + '\r' + '\n');
				
				while ((nextLine = fileText.read()) != -1) {
					returnmsg += (char)nextLine;
				}
				
				 fileText.close();	
			} 
			catch(IOException e) {
				System.err.println(e.getMessage());
				returnmsg = getResponse(403);
				return returnmsg;
			}
		}
		//otherwise print using bytes
		else{
			try{
				returnmsg = (headCommand(pathname) + '\r' + '\n' + '\r' + '\n');
				socketOut.print(returnmsg);
				socketOut.flush();
				System.err.println(returnmsg);
				returnmsg = null;
				
				byte fileContent[] = new byte[(int)userFile.length()];
				FileInputStream fstream = new FileInputStream(userFile);
				fstream.read(fileContent);
		
				DataOutputStream ds = new DataOutputStream(newClient.getOutputStream());
				ds.write(fileContent, 0, fileContent.length);
				
				fstream.close();	 
				return null;
			}
			catch(IOException e) {
				System.err.println(e.getMessage());
				returnmsg = getResponse(403);
				return returnmsg;
			}
		}
		return returnmsg;			
	}
	    
	//when POST is given.
	static String postCommand(String pathname){
		//call get for now
		String ext = getExtension(pathname);
		File userFile = validFile(pathname);
		returnmsg = null;
		
		if(!ext.equals(".cgi")){
			returnmsg = getResponse(405);
		}
		else{
			if(userFile == null){
				returnmsg = getResponse(status);
			}
			else{
				if(!userFile.canExecute()){
					returnmsg = getResponse(403);
				}
				else{
					if(url.isEmpty()){
						returnmsg = getResponse(204);
						System.out.println("It was null.");
					}
					else{
	
						//Scanner scan = new Scanner(System.in);
						PrintWriter out = null;
						try {
							out = new PrintWriter("./result.html");
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						}
						ProcessBuilder builder = new ProcessBuilder(pathname);//Runtime.getRuntime().exec(pathname));

						Map<String, String> env = builder.environment();
						env.put("CONTENT_LENGTH", contentLength+"");
						env.put("SCRIPT_NAME", filename);
						try {
							env.put("SERVER_NAME", ""+InetAddress.getLocalHost().getHostName());
						} catch (UnknownHostException e) {
							
							e.printStackTrace();
						}
						env.put("SERVER_PORT",portno);
						if(from != null){	
							env.put("HTTP_FROM", from);
						}
						if(userAgent != null){
							env.put("HTTP_USER_AGENT", userAgent);
						}
						String urlDecoded= null;
						try {
							urlDecoded = java.net.URLDecoder.decode(url, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						env.put("CONTENT_LENGTH", urlDecoded.length()+"");
						System.out.print("url: "+url);
						System.out.print("urlDecoded: "+urlDecoded);
						
						builder.redirectErrorStream(true);
						
						Process proc = null;
						
						try{
							proc = builder.start();
						}
						catch(IOException e){
							System.err.print(e.getMessage());
						}
						BufferedReader procIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						BufferedWriter procOut = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));		
					

						String stuff = null;											
							try{
								//procOut.write(line);
								procOut.write(urlDecoded, 0, urlDecoded.length());
								procOut.flush();	
								stuff = (procIn.readLine())+'\r'+'\n';
								while(procIn.ready()){
									stuff += (procIn.readLine()+'\r'+'\n');
								}
								if(stuff.equals('\n')){
									returnmsg = getResponse(204);
									return returnmsg;
								}
								else{
									System.out.println("stuff = "+stuff);
									out.println(stuff);
									out.close();
									
								}
	//							procOut.write(url);
							}
							catch(IOException e){
							
							}
					//	}			
						
					}
				}
			}
		}
		System.out.println("return msg: "+returnmsg);
		if(returnmsg == null){
			 returnmsg = getCommand("./result.html");
		}
		return returnmsg;
	}
	
	//when HEAD is given.
	static String headCommand(String pathname){
		returnmsg = null;		
		
		//get file and ext
		File userFile = validFile(pathname);
		String ext = getExtension(pathname);
        
        //set up format for lastMod
        SimpleDateFormat lastMod = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        lastMod.setTimeZone(TimeZone.getTimeZone("GMT"));

		//if null then not valid file, return proper error code		
		if(userFile == null){
			returnmsg = getResponse(status);
			return returnmsg;
		}
		
 	        returnmsg  = "HTTP/1.0 200 OK\r\n";
			returnmsg += "Content-Type: " + findFileType(ext) + '\r'+'\n';
			returnmsg += "Content-Length: " + userFile.length() + '\r'+ '\n';
			returnmsg += "Last-Modified: " + lastMod.format(userFile.lastModified()) + '\r' + '\n';
			returnmsg += "Content-Encoding: identity" + '\r' + '\n';
			returnmsg += "Allow: GET, POST, HEAD" + '\r' + '\n';
			returnmsg += "Expires: Sat, 21 Jul 2018 11:00:00 GMT";//some date in future
				 
			return returnmsg;
	}	
	
	//parses incoming message from server.
	static String[] parseMessage(String msg){
		String[] msgArray = null;

		//Split message into array.
		msgArray = msg.split(" ");

		//if not 3 parts, then not good format.
		if(msgArray.length != 3){
			System.err.println("Wrong number of parameters");
			status = 400;
			return null;
		}
		
		//make sure command is uppercase
		int i = 0;
		while(i < msgArray[0].length()){
			if(!Character.isUpperCase(msgArray[0].charAt(i))){
				System.err.println("lower case");
				status = 400;
				return null;
			}
			else{
				i++;
			}
		}
		
		//make sure file starts with '/'
		if ((msgArray[1]).charAt(0)!='/'){
			System.err.println("Missing slash");
			status = 400;
			return null;	
		}
		
		//make sure you have 8 characters in HTTP portion (HTTP/#.#)
		if((msgArray[2]).length() != 8){
			System.err.println("Missing HTTP/X.X");
			status = 400;
			return null;
		}
		
		//make sure first 5 characters are HTTP/
		if(!(msgArray[2]).substring(0,5).equals("HTTP/")){
			System.err.println("Missing HTTP/X.X");
			status = 400;
			return null;
		}
		
		//Make sure the 6th, 7th, 8th characters are digit, '.', digit.
		if(Character.isDigit(msgArray[2].charAt(5)) && msgArray[2].charAt(6) == '.' && Character.isDigit(msgArray[2].charAt(7))){
			//if 0, then just return, we take all numbers in second digit
			if(msgArray[2].charAt(5) == '0'){
				return msgArray;
			}	
			//if 1, then make sure the second digit is zero and return
			else if((msgArray[2].charAt(5) == '1') && (msgArray[2].charAt(7) == '0')){
				return msgArray;
			}
			//all other formats not supported.
			else{
				System.err.println("Version not supported");
				status = 505;
				return null;
			}
		}
		//not digit, then '.', then digit, so return bad format
		else{
			System.err.println("Bad Format (not #.#)");
			status = 400;
			return null;
		}
		
	}
	
	static Date findLMD(ArrayList<String> messageList){
		Date modSince = null;
		for(int i=0; i < messageList.size(); i++){
			if((messageList.get(i)).length() > 18){	
				//check to make sure that the first 18 characters == If-Modified-Since, else ignore.
				if((messageList.get(i)).substring(0,18).equals("If-Modified-Since:")) {
					//do stuff to figure out date and put into proper format.
					String modDate = (msgList.get(i)).substring(19);
//					System.out.println(modDate);
					modSince = null;
					try{
						modSince = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").parse(modDate);		
					}catch(ParseException e){
						System.err.println("Not an HTTP date, so ignore");
						modSince = new Date(1);
					}		
//					System.out.println(modSince);
				}	
			}
		}
		return modSince;
	}

	static int findCL(ArrayList<String> messageList){
		int length = -1;
		for(int i=0; i < messageList.size(); i++){
			if((messageList.get(i)).length() > 15){	
				if((messageList.get(i)).substring(0,15).equals("Content-Length:")){
					String len = (msgList.get(i)).substring(16);
					try{
						length = Integer.parseInt(len);		
					}catch(NumberFormatException e){
					
					}
				}
			}
		}
		return length;
	}
	
	static String findCT(ArrayList<String> messageList){
		String type = null;
		for(int i=0; i < messageList.size(); i++){
			if((messageList.get(i)).length() > 13){
				if((messageList.get(i)).substring(0,13).equals("Content-Type:")){
					type = (msgList.get(i)).substring(14);
				}
			}
		}
		return type;
	}

	static String findUA(ArrayList<String> messageList){
		String UA = null;
		for(int i=0; i < messageList.size(); i++){
			if((messageList.get(i)).length() > 11){
				if((messageList.get(i)).substring(0,11).equals("User-Agent:")){
					UA = (msgList.get(i)).substring(12);
				}
			}
		}
		return UA;
	}
	
	static String findFrom(ArrayList<String> messageList){
		String fr = null;
		for(int i=0; i < messageList.size(); i++){
			if((messageList.get(i)).length() > 5){
				if((messageList.get(i)).substring(0,5).equals("From:")){
					fr = (msgList.get(i)).substring(6);
				}
			}
		}
		return fr;	
	}
	//closes all sockets and streams.
	static void closeItAll(){
		try{
			socketOut.close();
			socketIn.close();
			newClient.close();
		}
		catch(IOException ex ){
			System.err.println("Error closing out of streams and sockets.");
			System.err.println(ex.getMessage());
//			System.err.println("thread count -- " + newThreads.size());
//	    	newThreads.remove(Thread.currentThread());
		 	return;
		}
	}
	
	//Main Method
    public static void main(String[] args){
		//Check arguments and assign into variable    
    	if(args.length != 1){
    		System.err.println("Invalid number of arguments.  Please enter port number.");
    		return;
    	}
    	portno = args[0];
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
//			System.err.println("Start new thread: " + newThreads.size());
//			System.out.println("Ready for new connections...");
			try{
				//Blocking call waiting for a connection
				newClient = newSocket.accept();
			}
			catch(IOException e){
				System.err.println("Error accepting new connections.");
			   	System.err.println(e.getMessage());
			   	return;
			}
			if(newThreads.size() < 50){
				//Spawns thread and hands off work in order to accept another connection ASAP
				Thread newThread = new Thread(new ClientThread());
				newThreads.add(newThread);
				newThread.start(); //calls "run" method defined below.
			}
			else{
				try{
					socketOut = new PrintWriter(newClient.getOutputStream(), true);
				}
				catch(IOException e){//if streams fail.
					System.err.println("Error setting up IO streams. " + e.getMessage());
//		    		System.err.println(e.getMessage());
		    		socketOut.print(getResponse(500));//+'\r'+'\n');
					socketOut.flush();
		    		return;
				}
				returnmsg = (getResponse(503));//+'\r');// + '\r' + '\n');
				socketOut.print(returnmsg);
				socketOut.flush();
				socketOut.flush();
				socketOut.flush();
				System.out.println(returnmsg);
				closeItAll();
			}
			
		}
    }
     
	//Create new class to handle thread
	static class ClientThread extends Thread{
	//Must overwrite this method "run". This is the code that the thread is going to run
	    public void run(){
	    	//Initializing variables
			String msg = null;
			String msg2 = null;
			returnmsg = null;
			String[] messageArray = null;
			Date modSince = null;
			String pathname = null;
			String contentType = null;
			
			//Set up streams
			try{			
				socketOut = new PrintWriter(newClient.getOutputStream());
				socketIn = new BufferedReader(new InputStreamReader(newClient.getInputStream()));	
			}
			catch(IOException e){//if streams fail.
//				System.err.println("Error setting up IO streams." + e.getMessage());
				returnmsg = getResponse(500);// + '\r';
		    	socketOut.print(returnmsg);
				socketOut.flush();
				socketOut.flush();
				socketOut.flush();
				System.out.println(returnmsg);
//				System.out.println("thread count -- " + newThreads.size());
				closeItAll();
		    	newThreads.remove(Thread.currentThread());
			 	return;
			}
			
			//read in line from server - this is where timeout might occur.
			try{
				newClient.setSoTimeout(3000);
				msg = socketIn.readLine();
				while(socketIn.ready()){
					msg2 = socketIn.readLine();
					if(!msg2.equals('\r' + '\n')){
						msgList.add(msg2);
					}
				}
			}
			catch(IOException e){//connection timed out 
//				System.err.println("Connection to client timed out.");
//		    	System.err.println(e.getMessage());
		    	returnmsg = (getResponse(408));//+'\r');//+'\n');
		    	socketOut.print(returnmsg);
				socketOut.flush();
				socketOut.flush();
				socketOut.flush();				
				System.err.println(returnmsg);
//		    	System.err.println("TIMEOUT");
				closeItAll();
//				System.out.println("thread count -- " + newThreads.size());
		    	newThreads.remove(Thread.currentThread());
			 	return;
			}		
			System.out.println(msg);
			for(int i=0; i < msgList.size(); i++){
				System.out.println("i " + msgList.get(i));
			}	
			//Parsing the message to check for Format
			messageArray = parseMessage(msg);
			
			//if something was wrong with the message provided, returns null, pull status.
			if(messageArray == null){
				returnmsg = getResponse(status);
			
				System.out.println("about to print it");
	
				socketOut.print(returnmsg);// + '\r' + '\n');
				socketOut.flush();
				socketOut.flush();
				socketOut.flush();
				System.out.println(returnmsg);				
				closeItAll();	
//				System.out.println("thread count: " + newThreads.size());
    			newThreads.remove(Thread.currentThread());
			 	return;
			}

			//pull file out of array and make pathname.		 
			pathname = "./doc_root/" + messageArray[1];
			filename = messageArray[1];
			
			modSince = findLMD(msgList);
			contentLength = findCL(msgList);
			contentType = findCT(msgList);
			from = findFrom(msgList);
			userAgent = findUA(msgList);

			url = msgList.get(msgList.size()-1);
			System.out.println("url: " + url);
			
			msgList.clear();
			
			System.out.println("LMD: " + modSince + "CL: " + contentLength);
			
			
			//If-Modified-since only applies to "GET" according to RFC
			//So if receive on anything else ignore.
			if(messageArray[0].equals("GET")){
				//need to check if empty to avoid system hanging
				if(modSince != null){
					File userFile = new File(pathname);
					Date lastMod = new Date(userFile.lastModified());		
					
					//check to see if it has not been modified since and return 304 code
					if(lastMod.compareTo(modSince) < 0) {
						returnmsg = getResponse(304);// + '\r' + '\n';
					}
					//if modified since, check for valid command and execute
					else{			
						returnmsg = executeCommand(messageArray[0],pathname);
					}
				}
				//if msg2 is empty, then just continue on
				else{			
					returnmsg = executeCommand(messageArray[0], pathname);
				}
			}
			//if not "GET" or "HEAD, continue on 
			else{			
				if(messageArray[0].equals("POST") && contentLength == -1){
					returnmsg = getResponse(411);
				}
				else if(messageArray[0].equals("POST") && (contentType == null) || (!(contentType.equals("application/x-www-form-urlencoded")))) {
					returnmsg = getResponse(500);
				}
				else{
				//check for valid command and execute
					returnmsg = executeCommand(messageArray[0], pathname);
				}
			}
			System.out.println("Here: " + returnmsg);
			//If there is a returnmsg, print it (Otherwise it was bytes and it was already sent)
			if(returnmsg != null){		
				socketOut.print(returnmsg);// + '\r' + '\n');
				socketOut.flush();
				socketOut.flush();
				socketOut.flush();
				System.out.println(returnmsg);
			}
					
			closeItAll();
			
			//wait for 1/4 second (1000 milliseconds = 1 second, so 250 milliseconds = .25 seconds or 1/4 second.
			try{
				Thread.sleep(250);
			}
			catch(InterruptedException e){
				System.err.println(e.getMessage());
//				System.out.println("thread count: " + newThreads.size());
		    	newThreads.remove(Thread.currentThread());
			 	return;
			}

			//return to exit thread.		
//			System.out.println("thread count: " + newThreads.size());	
	    	newThreads.remove(Thread.currentThread());
		 	return;
	    }

	
	}


}
