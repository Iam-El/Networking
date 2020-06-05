
/*
 * 
 * 
 * Name: Elsy Mariate Fernandes
 * UTA ID: 1001602253
 * FileName: Webserver.java
 * How to test:
 * Type "http://127.0.0.1:5555/gaia.cs.umass.edu/wireshark-labs/alice.txt" to the address field of the web browser.
 * The browser will show the HTTP response.
 * The console will show the HTTP Request and HTTP Response.
 * 
 * Type "http://127.0.0.1:5555/gaia.cs.umass.edu/wireshark-labs/alice.aaa" to the address filed of the web browser.
 * Browser will show file not found error.
 * The console will show  the response code with File not found error.
 * 
 * Before you type the Url to the address field clear the cache to test if the response is directed from the server or stored cache.
 *
 *
*/

package WebProxyServer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Webserver {

	final static String CRLF = "\r\n";
	HashMap<String, String> map = new HashMap<>(); // define hash map for cache server to store key value pair

	@SuppressWarnings("resource")
	Webserver() throws Exception {
		ServerSocket myServer = new ServerSocket(5555); // Establish a server socket
		System.out.println("Server Created"); // Creates server
		Socket clientSocket; // define a client socket
		while (true) {
			clientSocket = myServer.accept(); // Accept the client request
			new ProcessClient(clientSocket); // Initialize a process for the client request
		}
	}

	public static void main(String[] args) throws Exception {
		new Webserver(); // Main function for Method
	}

	class ProcessClient extends Thread // Thread which handles the client request
	{
		Socket Client;

		ProcessClient(Socket clientSocket) throws Exception // Constructor for the Class
		{
			try {
				// Listening for a TCP connection request.
				Client = clientSocket; // Requested client socket
				System.out.println("------------------START REQUEST------------------\n");
				System.out.println("Handling request from the client:" + Client.getPort()); // Handling the request from
																							// the client with thread id
				DataInputStream input = new DataInputStream(Client.getInputStream());
				DataOutputStream output = new DataOutputStream(Client.getOutputStream());
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(input));
				char buffer[] = new char[2048];

				int len = bufferRead.read(buffer, 0, 2048); // Read http request
				String request = Webserver.charArrayToString(buffer, len); // Convert character array to string
				if (!request.isEmpty()) {
					System.out.println("HTTP Request:"); // Display the HTTP Request from the web browser
					System.out.println(request);
					String requestUrl = this.getRequestUrl(request);
					if (requestUrl.indexOf("favicon") != -1) {
						this.closeConnection(input, output, bufferRead, clientSocket);
					} else {
						System.out.println("Request url: " + requestUrl + "\n"); // Print requested Url

						// Create HTTP Response
						if (this.isCacheEmpty() || !this.isResponseCached(requestUrl)) { // Check if the requested URL
																							// has
																							// been cached
							try {
								System.out.println("This is the first time object is requested from the client \n");
								String returnedResponse = this.sendGet(requestUrl); // Call sendGet method to display
																					// the
																					// content of requested object
								this.storeIntoCache(requestUrl, returnedResponse); // Store the response in a cache
								this.sendHttpResponse(output, returnedResponse); // Display the response
							} catch (FileNotFoundException e) {
								String responseStatus = "HTTP/1.1 404 File not Found"; // Store response status if file
																						// not
																						// found
								String requestedResponse = "File Not Found \n " + e.getLocalizedMessage()
										+ " was not found on the server"; // If the file not fund displays this message
								String httpResponse = this.formatHttpResponse(requestedResponse, responseStatus);
								this.storeIntoCache(requestUrl, httpResponse); // Store response in a cache
								this.sendHttpResponse(output, httpResponse); // Display the response if the object
																				// requested
																				// for the first time
								output.close();
								Client.close();
							}
						}

						else {
							// If the object requested is not for the first time retrieve the response from
							// a cache
							System.out.println("This is a response from Cache \n");

							String cachedResponse = this.retrieveFromCache(requestUrl);
							this.sendHttpResponse(output, cachedResponse); // Prints the cached response
						}
					}
					this.closeConnection(input, output, bufferRead, Client);
				} else {
					this.closeConnection(input, output, bufferRead, Client);
				}

				System.out.println("------------------END REQUEST------------------\n");

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		/*
		 * Method to get the request url
		 */
		public String getRequestUrl(String request) {
			int start = request.indexOf("GET"); // Get the start index within the string
			int end = request.indexOf(" HTTP/"); // Get the end index within the string
			String requestUrl = request.substring(start + 4, end + 1);

			// Check if the request url starts with the http protocol, if not append http:/
			// to it
			if (!requestUrl.startsWith("http:/")) {
				requestUrl = "http:/" + requestUrl;
			}
			return requestUrl;
		}

		/*
		 * Helper method to close the socket connection
		 */
		public void closeConnection(DataInputStream input, DataOutputStream output, BufferedReader bufferRead,
				Socket Client) throws IOException {
			output.close();
			input.close();
			bufferRead.close();
			Client.close();
		}

		/*
		 * Helper method to see if the response for the get request is already cached
		 */
		public boolean isResponseCached(String key) {
			return map.containsKey(key);
		}

		/*
		 * Helper method to store the response into cache
		 */
		public void storeIntoCache(String key, String value) {
			map.put(key, value);
		}

		/*
		 * Helper method to check if the cache is empty
		 */
		public boolean isCacheEmpty() {
			return map.isEmpty();
		}

		/*
		 * Helper method to retrieve the requested url response from cache
		 */
		public String retrieveFromCache(String key) {
			return map.get(key);
		}

		/*
		 * Helper method to generate the http response from the header fields of the get
		 * request
		 */
		public String generateResponseFromResponseHeaders(String response, Map<String, List<String>> headerFields,
				String responseMessage) {
			List<String> server = headerFields.get("Server");
			List<String> lastModified = headerFields.get("Last-Modified");
			List<String> keepAlive = headerFields.get("Keep-Alive");
			List<String> contentType = headerFields.get("Content-Type");
			List<String> acceptRanges = headerFields.get("Accept-Ranges");
			List<String> date = headerFields.get("Date");

			String httpResponse = responseMessage + " Date: " + date.get(0) + "\r\n" + "Server:" + server.get(0)
					+ "\r\n" + "Last-Modified: " + lastModified.get(0) + "\r\n" + "Accept-Ranges: "
					+ acceptRanges.get(0) + "\r\n" + "Content-Length: " + response.length() + "\r\n" + "Keep-Alive:"
					+ keepAlive.get(0) + "\r\n" + "Connection: Keep-Alive\r\n" + "Content-Type:" + contentType.get(0)
					+ "\r\n\r\n";

			return httpResponse + response;

		}

		/*
		 * Helper method to generate the response in case of 404 file not found error
		 */
		public String formatHttpResponse(String response, String responseStatus) {
			long time = System.currentTimeMillis();
			Date date = new Date(time);

			String httpResponse = responseStatus + " Date: " + date + "\r\n"
					+ "Server: Apache/2.4.6 (CentOS) OpenSSL/1.0.2k-fips PHP/5.4.16 mod_perl/2.0.10 Perl/v5.16.3\r\n"
					+ "Last-Modified: Sat, 21 Aug 2004 14:21:11 GMT\r\n" + "ETag: \"2524a-3e22aba3a03c0\"\r\n"
					+ "Accept-Ranges: bytes\r\n" + "Content-Length: " + response.length() + "\r\n"
					+ "Keep-Alive: timeout=5, max=100\r\n" + "Connection: Keep-Alive\r\n"
					+ "Content-Type: text/plain; charset=UTF-8\r\n" + "\r\n" + response;
			return httpResponse;

		}

		/*
		 * Helper method to send the HTTP response to the client.
		 */
		public void sendHttpResponse(DataOutputStream output, String httpResponse) throws Exception {
			output.writeBytes(httpResponse);
			output.flush();
			System.out.println("HTTP Response:");
			System.out.println(httpResponse);
		}

		/*
		 * Helper method to sent the Get response from the Web proxy server to the
		 * request server in the url.
		 */

		public String sendGet(String url) throws Exception {

			URL newUrlObject = new URL(url);
			// Create a http connection//
			HttpURLConnection newHttpConnection = (HttpURLConnection) newUrlObject.openConnection();
			// Request message
			newHttpConnection.setRequestMethod("GET");
			int responseCode = newHttpConnection.getResponseCode(); // Get the response code
			Map<String, List<String>> headerFields = newHttpConnection.getHeaderFields();

			System.out.println("HTTP Request from Web proxy server to: " + url); // Send get request to URL
			System.out.println("Response Code  : " + responseCode);
			BufferedReader input = new BufferedReader(new InputStreamReader(newHttpConnection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = input.readLine()) != null) {

				response.append(inputLine + '\n');
			}
			input.close();
			String responseString = response.toString();
			return this.generateResponseFromResponseHeaders(responseString, headerFields, "HTTP/1.1 200 OK\r\n");
		}
	}

	/*
	 * Method to convert character array to string
	 */
	public static String charArrayToString(char[] charArray, int len) {
		String result = "";
		for (int i = 0; i < len; i++) {
			result += charArray[i];
		}
		return result;
	}
}
