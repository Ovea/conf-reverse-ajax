import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public final class ChatCometStreamingServlet30 extends HttpServlet {

    final Queue<AsyncContext> asyncContexts = new ConcurrentLinkedQueue<AsyncContext>();
    final String boundary = "ABCDEFGHIJKLMNOPQRST"; // generated

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String paramUser = req.getParameter("user");
        if (paramUser != null) {
            req.getSession().setAttribute("user", paramUser);
        }

        String message = req.getParameter("msg");
        String user = (String) req.getSession().getAttribute("user");
        if (message != null && user != null) {
            try {
                String msg = new JSONArray().put(new JSONObject()
                        .put("from", user + " (" + req.getRemoteHost() + ":" + req.getRemotePort() + ")")
                        .put("at", System.currentTimeMillis())
                        .put("msg", message))
                        .toString();

                for (AsyncContext asyncContext : asyncContexts) {

                    HttpServletResponse peer = (HttpServletResponse) asyncContext.getResponse();
                    peer.getOutputStream().println("Content-Type: application/json");
                    peer.getOutputStream().println();
                    peer.getOutputStream().println(msg);
                    peer.getOutputStream().println("--" + boundary);
                    peer.flushBuffer();

                }
                resp.setStatus(HttpServletResponse.SC_OK);
            } catch (JSONException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String user = (String) req.getSession().getAttribute("user");
        if (user == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No user in session");

        } else {
            AsyncContext asyncContext = req.startAsync();
            asyncContext.setTimeout(0);

            resp.setContentType("multipart/x-mixed-replace;boundary=\"" + boundary + "\"");
            resp.setHeader("Connection", "keep-alive");
            resp.getOutputStream().print("--" + boundary);
            resp.flushBuffer();

            asyncContexts.offer(asyncContext);
        }
    }
}
