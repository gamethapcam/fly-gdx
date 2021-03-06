package de.fau.cs.mad.fly.HttpClient;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class PostUserService {
    
    public PostUserService(FlyHttpResponseListener listener) {
        this.listener = listener;
    }
    
    private final FlyHttpResponseListener listener;

	public static class UserRecord {
		public int id;
		public String secretKey;
	}
    
    public void execute(String player) {
        String subURL = "/users";
        String userData = "{ \"user\": { \"name\": \"" + player + "\" } }";
        HttpRequest post = new HttpRequest(HttpMethods.POST);
        post.setHeader("Content-Type", "application/json");
        post.setUrl(RemoteServices.getServerURL() + subURL);
        post.setContent(userData);
        
        Gdx.app.log("PostUserService", "sending:" + userData);
        RemoteServices.sendHttpRequest(post, new HttpResponseListener() {
            
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                
                HttpStatus status = httpResponse.getStatus();
                if (status.getStatusCode() == HttpStatus.SC_OK || status.getStatusCode() == HttpStatus.SC_CREATED) {
                    JsonReader reader = new JsonReader();
                    JsonValue val = reader.parse(httpResponse.getResultAsStream());
					UserRecord record = new UserRecord();
                    record.id = val.getInt("id");
					record.secretKey = val.getString("secret_key");
                    Gdx.app.log("PostUserService", "get fly id from server:" + record.id);
                    listener.successful(record);
                } else {
                    Gdx.app.log("PostUserService", "new fly id server return code: " + String.valueOf(status.getStatusCode()));
                    listener.failed(String.valueOf(status.getStatusCode()));
                }
            }
            
            @Override
            public void failed(Throwable t) {
                Gdx.app.log("PostUserService", "new fly id server failed msg" + t.getMessage());
                listener.failed(t.getLocalizedMessage());
            }
            
            @Override
            public void cancelled() {
                listener.cancelled();
            }
        });
    }
}
