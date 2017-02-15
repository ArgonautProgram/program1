/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import java.nio.file.*;
import java.text.SimpleDateFormat;

public class WebWorker implements Runnable
{

private Socket socket;
private String newString = "";

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      writeHTTPHeader(os,typeCheck(newString),checkStatus(newString));
      writeContent(os);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   String [] stringArray;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
     try {
       while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
	      String readget = line.substring(0, 3);
         System.err.println("Request line: ("+line+")");
         if(line.substring(0,3).equals("GET")) {
           stringArray = line.split(" ");
           newString = stringArray[1];
           newString = newString.substring(1);
         } 		
         if (line.length()==0) break;
     } 
     catch (Exception e) {
       System.err.println("Request error: "+e);
       break;
     }
   }
   return;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String newline) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));

   os.write(newline.getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: THIS IS NOT A DRILL\n".getBytes());
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
   
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{
  DateFormat day = new SimpleDateFormat("MM/dd/yyyy");
  Date today = new Date();

  if((typeCheck(newString)).equals("text/html")){

    try {
      InputStream istream = new FileInputStream(newString);
      BufferedReader in = new BufferedReader(new InputStreamReader(istream));
      String str;
      while ((str = in.readLine()) != null) {
        if(str.contains("<cs371date>"))
          str = str.replace("<cs371date>", day.format(today));
        if(str.contains("<cs371server>"))
          str = str.replace("<cs371server>", "Christian's Server!");
        os.write(str.getBytes());
      }
      in.close();
    } 
    catch (IOException e) {
      os.write("<html><head></head><body>HTTP/1.1 404 Not Found\r\n Content-type: text/html\r\n\r\n".getBytes());
      os.write(newString.getBytes());
      os.write(" not found</body></html>\n".getBytes());
      os.close();
   }
 }//end html if statement    
   
 else {   
   try {
     FileInputStream in = null;
     try {
       in = new FileInputStream(newString);     
       int c;
       while ((c = in.read()) != -1) {
         os.write(c);
       }
     } 
     finally {
       if(in != null) {
         in.close();
       }
     }
   }
   catch (FileNotFoundException e) {
     os.write("<html><head></head><body>HTTP/1.1 404 Not Found\r\n Content-type: text/html\r\n\r\n".getBytes());
     os.write(newString.getBytes());
     os.write(" not found</body></html>\n".getBytes());
   }   
 }//end else
}
 
private String checkStatus(String statusString){
  try {
    InputStream istream = new FileInputStream(statusString);
    return "HTTP/1.1 200 OK\n";
  }
  catch(FileNotFoundException error){
    return "HTTP/1.1 404 Not Found\n";
  }
}

private String typeCheck (String inFile){
   
  String extension = inFile.substring(inFile.lastIndexOf('.'));   
  extension.toLowerCase();

  if(extension.equals(".html"))
    return "text/html";
  else if(extension.equals(".gif"))
    return "image/gif";
  else if(extension.equals(".jpeg"))
    return "image/jpeg";
  else if(extension.equals(".png"))
    return "image/png";
   
  else if(extension.equals(".ico"))
    return "image/x-icon";
      
  else
    return "error";
}

} // end class
