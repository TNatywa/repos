/* 2012-05-20 Version 2.0

Thanks John Reagan for this well-running code which repairs the original
obsolete code for Elliott's HostServer program. I've made a few additional
changes to John's code, so blame Elliott if something is not running.

-----------------------------------------------------------------------

Play with this code. Add your own comments to it before you turn it in.

-----------------------------------------------------------------------
NOTE: This is NOT a suggested implementation for your agent platform,
but rather a running example of something that might serve some of
your needs, or provide a way to start thinking about what YOU would like to do.
You may freely use this code as long as you improve it and write your own comments.

-----------------------------------------------------------------------

TO EXECUTE:

1. Start the HostServer in some shell. >> java HostServer

1. start a web browser and point it to http://localhost:1565. Enter some text and press
the submit button to simulate a state-maintained conversation.

2. start a second web browser, also pointed to http://localhost:1565 and do the same. Note
that the two agents do not interfere with one another.

3. To suggest to an agent that it migrate, enter the string "migrate"
in the text box and submit. The agent will migrate to a new port, but keep its old state.

During migration, stop at each step and view the source of the web page to see how the
server informs the client where it will be going in this stateless environment.

-----------------------------------------------------------------------------------

COMMENTS:

This is a simple framework for hosting agents that can migrate from
one server and port, to another server and port. For the example, the
server is always localhost, but the code would work the same on
different, and multiple, hosts.

State is implemented simply as an integer that is incremented. This represents the state
of some arbitrary conversation.

The example uses a standard, default, HostListener port of 1565.

-----------------------------------------------------------------------------------

DESIGN OVERVIEW

Here is the high-level design, more or less:

HOST SERVER
  Runs on some machine
  Port counter is just a global integer incrememented after each assignment
  Loop:
    Accept connection with a request for hosting
    Spawn an Agent Looper/Listener with the new, unique, port

AGENT LOOPER/LISTENER
  Make an initial state, or accept an existing state if this is a migration
  Get an available port from this host server
  Set the port number back to the client which now knows IP address and port of its
         new home.
  Loop:
    Accept connections from web client(s)
    Spawn an agent worker, and pass it the state and the parent socket blocked in this loop

AGENT WORKER
  If normal interaction, just update the state, and pretend to play the animal game
  (Migration should be decided autonomously by the agent, but we instigate it here with client)
  If Migration:
    Select a new host
    Send server a request for hosting, along with its state
    Get back a new port where it is now already living in its next incarnation
    Send HTML FORM to web client pointing to the new host/port.
    Wake up and kill the Parent AgentLooper/Listener by closing the socket
    Die

WEB CLIENT
  Just a standard web browser pointing to http://localhost:1565 to start.

  -------------------------------------------------------------------------------*/


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.*;
/**
 * HostServer Notes: This went pretty smoothly for me, although I did have to edit the HTML functions
 * to get an accurate content length so things would be compatible with browsers other than IE. I also modified
 * things to eliminate inaccurate state numbers based on fav.ico requests. If the string person wasnt found,
 * the requests was ignored
 */

/**
 * AgentWorker
 *
 * AgentWorker objects are created by AgentListeners and process requests made at the various
 * active ports occupied by agentlistener objects. They take a request and look for the string
 * migrate in that request(supplied from a get parameter via an html form). If migrate is found,
 * the worker finds the next availabel port and switches teh client to it.
 *
 * I made a small modification because my browser kept requesting fav.ico. So I verified that we receive
 * a person attribute before processing the request as valid(and incrementing agent state)
 *
 */

	// My Notes
	// Thanks to John Reagan for the original code

	// My modifications are below:
	// 		-Modified this to use Logger instead of System.out.print.ln. This way I can come back to the older log files
	//		-Created a new method to get line number which was included for my calls to logger




class AgentWorker extends Thread {

	Socket sock; //Create a socket called sock so we can connect
	agentHolder parentAgentHolder; //TODO: Come back to this after you see rest of code
	int localPort; //which port is being used
	Logger logger = Logger.getLogger("serverlog"); //declares a serverlog
	FileHandler logFile; //intializes a FileHandler for logs

	AgentWorker (Socket s, int prt, agentHolder ah) { //Create a constructor for the worker
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() {

		//initialize variables
		PrintStream out = null;
		BufferedReader in = null;
		//server is hardcoded in, if I decide to work on an distributed intelligent agent with a classmate I should change this to my ip
		String NewHost = "localhost";
		//port the main worker will run on
		int NewHostMainPort = 1565; //starting at port 1565
		String buf = ""; //Start out with nothing in the buffer
		int newPort; //initialize a new port. I think later in here new port will always be incremented by 1 from old port
		Socket clientSock; //initialize another socket for the client
		BufferedReader fromHostServer; //We will use this variable as our input
		PrintStream toHostServer; // we will use this variable as our output


		try {
			logFile = new FileHandler("/Users/tnatywa/repo/dia/logs/serverlog.txt"); //You'll need a filehandler to create a log.txt at the specified location i.e tnatywa/desktop/repos/435/threeAssignment/serverlog.txt
			logger.addHandler(logFile); //TODO: addhandler does what?
			SimpleFormatter formatter = new SimpleFormatter(); //declare a formatter
			logFile.setFormatter(formatter); //this just is formatting the filehandler. e.g. providing a timestamp TODO: double check what formatter is doing under the hood
			logger.info("Log is beginning"); // This will be the beginning of your log everytime a thread starts

		} catch (SecurityException e) { //TODO: figure out securityexception
			e.printStackTrace();//how can we log this instead?
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out = new PrintStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

			String inLine = in.readLine(); //TODO: can we do this better? read a line from the client
			//to allow for usage on non-ie browsers, I had to accurately determine the content
			//length and as a result need to build the html response so i can determine its length.
			StringBuilder htmlString = new StringBuilder(); //create a string builder for the HTML string

			logger.info(String.format("{%d}: %s", getLineNumber(), "Request line: " + inLine));

			if(inLine.indexOf("migrate") > -1) {
				//TODO: come back to this. the supplied request contains migrate, switch the user to a new port

				clientSock = new Socket(NewHost, NewHostMainPort); //create a new socket at port 1565
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				toHostServer = new PrintStream(clientSock.getOutputStream());
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]"); //Send a port for a client to connect to
				toHostServer.flush();

				//wait for the response and read a response until we find what should be a port
				for(;;) { //loop indefinitely
					buf = fromHostServer.readLine(); //getting valid port from host server
					if(buf.indexOf("[Port=") > -1) { //if you retrieved something valid then set it's port to whatever you received
						break; //port has now been set so break out of the loop
					}
				}

				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) ); //create a temporary buffer to store stuff in
				newPort = Integer.parseInt(tempbuf); //increment the next port here. Not really sure how this is incrementnig here. Will need to come back to it.
				//TODO: Logger
				logger.info(String.format("{%d}: %s", getLineNumber(),"newPort is: " + newPort));

				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine)); //send the html header along with some content in the lines below
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n"); //Migrate to the host server
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n");
				htmlString.append(AgentListener.sendHTMLsubmit());

				//TODO: Logger
				logger.info(String.format("{%d}: %s", getLineNumber(),"Killing parent listening loop."));
				ServerSocket ss = parentAgentHolder.sock; //intialize a a socket for the parentAgentHolder so that we can come back to it later if needed
				ss.close();//close the port here


			} else if(inLine.indexOf("person") > -1) {
				parentAgentHolder.agentState++;
				//Have a conversation with the state
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n");
				htmlString.append(AgentListener.sendHTMLsubmit());

			} else {
				//Invalid request- no person is specified
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine));
				htmlString.append("You have not entered a valid request!\n");
				htmlString.append(AgentListener.sendHTMLsubmit());


			}
			//output the html
			AgentListener.sendHTMLtoStream(htmlString.toString(), out);

			//close the socket
			sock.close();


		} catch (IOException ioe) {
			logger.info(String.format("{%d}: %s", getLineNumber(),ioe));
		}
	}

}

class agentHolder {
	//Will be holding the state of the agent
	ServerSocket sock;
	int agentState;
	agentHolder(ServerSocket s) { sock = s;}
}

 //AgentListener watchs individual ports and  sends a response when a connection to port 1565 occurs
class AgentListener extends Thread {
	Socket sock;
	int localPort;

	AgentListener(Socket As, int prt) {
		sock = As;
		localPort = prt;
	}
	int agentState = 0; //default state = 0

	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String NewHost = "localhost";
		logger.info(String.format("{%d}: %s", getLineNumber(),"In AgentListener Thread"));
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream());
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream()));

			buf = in.readLine(); //take input of the first line

			if(buf != null && buf.indexOf("[State=") > -1) { // If there is an active state, store the state for later
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State="))); //store the state to tempbuf
				agentState = Integer.parseInt(tempbuf); //Set agent state equal to the integer found in the state
				logger.info(String.format("{%d}: %s", getLineNumber(),"agentState is: " + agentState));

			}

			logger.info(String.format("{%d}: %s", getLineNumber(),buf));
			StringBuilder htmlResponse = new StringBuilder();//string builder to hold the html response
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf));
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n");
			htmlResponse.append("[Port="+localPort+"]<br/>\n");
			htmlResponse.append(sendHTMLsubmit());
			sendHTMLtoStream(htmlResponse.toString(), out);

			ServerSocket servsock = new ServerSocket(localPort,2);
			agentHolder agenthold = new agentHolder(servsock); //hold agent state in agenthold
			agenthold.agentState = agentState;

			while(true) {//classic wait for connection
				sock = servsock.accept();
				logger.info(String.format("{%d}: %s", getLineNumber(),"Got a connection to agent at port " + localPort));
				new AgentWorker(sock, localPort, agenthold).start(); //received a connection so create a new worker thread for it
			}

		} catch(IOException ioe) {
			logger.info(String.format("{%d}: %s", getLineNumber(),"Either connection failed, or just killed listener loop for agent at port " + localPort));
			logger.info(String.format("{%d}: %s", getLineNumber(),ioe));
		}
	}
	//send an html header. Similar to what we've done before on the webserver
	static String sendHTMLheader(int localPort, String NewHost, String inLine) {

		StringBuilder htmlString = new StringBuilder();

		htmlString.append("<html><head> </head><body>\n");
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n");
		htmlString.append("<h3>You sent: "+ inLine + "</h3>");
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:");
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n");

		return htmlString.toString();
	}
	//Could handle this one with one line of code but there is no reason to type more than we have to
	static String sendHTMLsubmit() {
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}
	//Sends back the response headers
	static void sendHTMLtoStream(String html, PrintStream out) {

		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length());
		out.println("Content-Type: text/html");
		out.println("");
		out.println(html);
	}



public static int getLineNumber() {
	//Used to put the line numbers inside your log files so if you hit an error you can identify where it occured in your log
	return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

}/**
 *
 * main hostserver class. this listens on port 1565 for requests. at each request,
 * increment NextPort and start a new listener on it. Assumes that all ports >3000
 * are free.
 */
public class HostServer {
	//we start listening on port 3001
	public static int NextPort = 3000;

	public static void main(String[] a) throws IOException {
		int q_len = 6;
		int port = 1565;
		Socket sock;
		ServerSocket servsock = new ServerSocket(port, q_len);
		logger.info(String.format("{%d}: %s", getLineNumber(),"Tim Natywa's DIA Master receiver started at port 1565."));
		logger.info(String.format("{%d}: %s", getLineNumber(),"Connect from 1 to 3 browsers using \"http://localhost:1565\"\n")); //TODO: what happens after the third browser?
		while(true) { //listen for a request
			//increment nextport! could be more sophisticated, but this will work for now. TODO: Figure out a better implementation
			NextPort = NextPort + 1;
			sock = servsock.accept();
			logger.info("Starting AgentListener at port " + NextPort);
			new AgentListener(sock, NextPort).start(); //start up an agent listener to receive new requests
		}

	}
}
