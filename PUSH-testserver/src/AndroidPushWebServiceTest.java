import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AndroidPushWebServiceTest {
        public static void main(String[] args) {
                // String projectId = "51018446040";
                String apiKey = "AIzaSyCpZTWx1GM4AUihfWC2PX3pZAIjqarWUyc";
                String registrationId = "APA91bESscGf4v8WSh0el-9mldojm6QjU5IAHQ7zRvYMSHk9xNT0dqnWaNkrxnqZ9s7FefzjK_PHIJbVeKwyWjfjtz75zqruPLV6KAb-qzZTeFovRvfzpLwMOzz5h9jdCMDczaIlqOAgmJC5pvhH1OOFp7fHnqoiXw";
                JSONObject obj = getJSON(registrationId);
                sendMessage(obj, apiKey);
        }

        public static JSONObject getJSON(String registrationId) {
                JSONObject obj = new JSONObject();
                obj.put("collapse_key", "1");
                obj.put("time_to_live", 3);
                obj.put("delay_while_idle", true);
                JSONObject data = new JSONObject();
                data.put("message", "A minimum payment of $XX.XX for your account ending in XXXX is due mm/dd/yy.");
                data.put("A", "206");
                data.put("type", "B");
                data.put("alertid", "16");
                obj.put("data", data);
                JSONArray ids = new JSONArray();
                ids.add(registrationId);
                obj.put("registration_ids", ids);
                return obj;
        }

        public static void sendMessage(JSONObject obj, String apiKey) {
                System.out.println(obj.toJSONString());
                byte[] postData = obj.toJSONString().getBytes();
                try {
                        URL url = new URL("https://android.googleapis.com/gcm/send");
                        HttpsURLConnection.setDefaultHostnameVerifier(new CustomizedHostnameVerifier());
                        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("webproxy.ssmb.com", 8080));
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                        conn.setDoOutput(true);
                        conn.setUseCaches(false);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("Authorization", "key=" + apiKey);
                        OutputStream out = conn.getOutputStream();
                        out.write(postData);
                        out.close();

                        int responseCode = conn.getResponseCode();
                        switch (responseCode) {
                        case 200:
                                processResponse(conn);
                                break;
                        case 400:
                                System.out.println("JASON object could not be parsed");
                                processErrorResponse(conn);
                                break;
                        case 401:
                                System.out.println("Error authenticating sender account");
                                processErrorResponse(conn);
                                break;
                        case 500:
                                System.out.println("Internal error in the GCM server");
                                processErrorResponse(conn);
                                break;
                        case 503:
                                System.out.println("Server is temporarily unavailable");
                                processErrorResponse(conn);
                                break;
                        default:
                                System.out.println("Unknown Error.");

                        }
                } catch (Exception e) {
                        System.out.println("Exception thrown\n" + e.getMessage());
                        e.printStackTrace();
                }
        }

        public static void processErrorResponse(HttpsURLConnection conn) {
                InputStream is = null;
                try {
                        is = conn.getErrorStream();
                        BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(is));
                        String line = null;
                        System.out.print("Complete Error Response: ");
                        while ((line = reader.readLine()) != null) {
                                System.out.println(line);
                        }

                } catch (IOException e) {
                        e.printStackTrace();
                } finally {
                        if (is != null) {
                                try {
                                        is.close();
                                } catch (IOException e) {
                                } finally {
                                        is = null;
                                }
                        }
                }
        }

        public static void processResponse(HttpsURLConnection conn) {
                InputStream is = null;
                byte[] response;

                try {
                        is = conn.getInputStream();
                        response = new byte[is.available()];
                        BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(is));
                        String line = null;
                        System.out.print("Complete Response: ");
                        StringBuffer responseDesc = new StringBuffer();
                        while ((line = reader.readLine()) != null) {
                                responseDesc.append(line);
                        }
                        System.out.println(responseDesc);
                        // retrieve JSON object from Response
                        JSONParser parser = new JSONParser();
                        JSONObject temp1 = (JSONObject) parser.parse(responseDesc.toString());
                        Iterator<String> it = temp1.keySet().iterator();
                        while (it.hasNext()) {
                                String key = it.next();
                                Object value = temp1.get(key);

                                if ("results".equalsIgnoreCase(key)){
                                        JSONArray resultsArray = (JSONArray) value;
                                        for (Object temp:resultsArray) {
                                                System.out.println("[[" + temp.getClass() + "]]");
                                        }
                                }
                                System.out.println("KEY[" + key +"]VALUE[" + value + "]");
                        }
/*                      System.out.println("JSON: " + temp1);
                        System.out.println("toString: " + temp1.toString());
                        System.out.println("toJSONString: " + temp1.toJSONString());
*/                      /*is.read(response);
                        JSONParser parser = new JSONParser();
                        String temp = new String(response);
                        System.out.println("Response: " + temp);
                    JSONObject temp1 = (JSONObject) parser.parse(temp);
                    System.out.println(temp1.toJSONString());*/

                        /*if (responseDesc.toString().contains("QuotaExceeded")) {

                        } else if (responseDesc.toString().contains("DeviceQuotaExceeded")) {

                        } else if (responseDesc.toString().contains("InvalidRegistration")) {

                        } else if (responseDesc.toString().contains("NotRegistered")) {

                        } else if (responseDesc.toString().contains("MessageTooBig")) {

                        } else if (responseDesc.toString().contains("MissingCollapseKey")) {

                        } else if (responseDesc.toString().contains("MismatchSenderId")) {

                        } else {

                        }*/

                } catch (IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                        e.printStackTrace();
                } catch (ParseException e) {
                        System.out.println("ParseException: " + e.getMessage());
                        e.printStackTrace();
                } finally {
                        if (is != null) {
                                try {
                                        is.close();
                                } catch (IOException e) {
                                } finally {
                                        is = null;
                                }
                        }
                }
        }

        private static class CustomizedHostnameVerifier implements HostnameVerifier {
                public boolean verify(String hostname, SSLSession session) {
                        return true;
                }
        }
}