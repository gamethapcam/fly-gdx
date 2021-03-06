package de.fau.cs.mad.fly.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StreamUtils;
import de.fau.cs.mad.fly.profile.PlayerProfileManager;

public class NetHttpsJavaImpl {

	static class HttpClientResponse implements HttpResponse {
		private HttpsURLConnection connection;
		private HttpStatus status;

		public HttpClientResponse(HttpsURLConnection connection) throws IOException {
			this.connection = connection;
			try {
				this.status = new HttpStatus(connection.getResponseCode());
			} catch (IOException e) {
				this.status = new HttpStatus(-1);
			}
		}

		@Override
		public byte[] getResult() {
			InputStream input = getInputStream();
			try {
				return StreamUtils.copyStreamToByteArray(input, connection.getContentLength());
			} catch (IOException e) {
				return StreamUtils.EMPTY_BYTES;
			} finally {
				StreamUtils.closeQuietly(input);
			}
		}

		@Override
		public String getResultAsString() {
			InputStream input = getInputStream();
			try {
				return StreamUtils.copyStreamToString(input, connection.getContentLength());
			} catch (IOException e) {
				return "";
			} finally {
				StreamUtils.closeQuietly(input);
			}
		}

		@Override
		public InputStream getResultAsStream() {
			return getInputStream();
		}

		@Override
		public HttpStatus getStatus() {
			return status;
		}

		@Override
		public String getHeader(String name) {
			return connection.getHeaderField(name);
		}

		@Override
		public Map<String, List<String>> getHeaders() {
			return connection.getHeaderFields();
		}

		private InputStream getInputStream() {
			try {
				return connection.getInputStream();
			} catch (IOException e) {
				return connection.getErrorStream();
			}
		}
	}

	private final ExecutorService executorService;
	final ObjectMap<HttpRequest, HttpsURLConnection> connections;
	final ObjectMap<HttpRequest, HttpResponseListener> listeners;
	final Lock lock;

	public NetHttpsJavaImpl() {
		executorService = Executors.newCachedThreadPool();
		connections = new ObjectMap<HttpRequest, HttpsURLConnection>();
		listeners = new ObjectMap<HttpRequest, HttpResponseListener>();
		lock = new ReentrantLock();
	}

	public void sendHttpRequest(final HttpRequest httpRequest, final HttpResponseListener httpResponseListener) {
		if (httpRequest.getUrl() == null) {
			httpResponseListener.failed(new GdxRuntimeException("can't process a HTTP request without URL set"));
			return;
		}

		try {
			final String method = httpRequest.getMethod();
			URL url;

			String token = System.getenv("FLY_SERVER_ACCESS_TOKEN");
			if ( token == null )
				switch ( Gdx.app.getType() ) {
					case Desktop:
						token = "ef78b5edd1e46531cc815b2cf7107bc5e6f7564c0b6cf253be2306b2edc53484";
						break;
					case Android:
						token = "d96706409d2aaeb167944124041677b2073a2073c5e5a545e421097faee2ea38";
						break;
					case iOS:
						token = "798a943f9e113f365ee8c546eff290b453801aac36e1087ab615fc920bc1db4a";
						break;
				}

			String tokenString = "Token token=\"" + token + "\"";
			String secretKey = PlayerProfileManager.getInstance().getCurrentPlayerProfile().getSecretKey();
			if ( secretKey != null )
				tokenString += "; secret_key=\"" + secretKey + "\"";
			httpRequest.setHeader("Authorization", tokenString);

			if (method.equalsIgnoreCase(HttpMethods.GET)) {
				String queryString = "";
				String value = httpRequest.getContent();
				if (value != null && !value.isEmpty())
					queryString = "?" + value;
				url = new URL(httpRequest.getUrl() + queryString);
			} else {
				url = new URL(httpRequest.getUrl());
			}

//			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
//				@Override
//				public boolean verify(String s, SSLSession sslSession) {
//					return true;
//				}
//			});

			final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			// should be enabled to upload data.
			final boolean doingOutPut = method.equalsIgnoreCase(HttpMethods.POST) || method.equalsIgnoreCase(HttpMethods.PUT);
			connection.setDoOutput(doingOutPut);
			connection.setDoInput(true);
			connection.setRequestMethod(method);


			connection.setSSLSocketFactory(RemoteServices.getSSLSocketFactory());
			HttpsURLConnection.setFollowRedirects(httpRequest.getFollowRedirects());

			lock.lock();
			connections.put(httpRequest, connection);
			listeners.put(httpRequest, httpResponseListener);
			lock.unlock();

			// Headers get set regardless of the method
			for (Map.Entry<String, String> header : httpRequest.getHeaders().entrySet())
				connection.addRequestProperty(header.getKey(), header.getValue());

			// Set Timeouts
			connection.setConnectTimeout(httpRequest.getTimeOut());
			connection.setReadTimeout(httpRequest.getTimeOut());

			executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						// Set the content for POST and PUT (GET has the
						// information embedded in the URL)
						if (doingOutPut) {
							// we probably need to use the content as stream
							// here instead of using it as a string.
							String contentAsString = httpRequest.getContent();
							if (contentAsString != null) {
								OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
								try {
									writer.write(contentAsString);
								} finally {
									StreamUtils.closeQuietly(writer);
								}
							} else {
								InputStream contentAsStream = httpRequest.getContentStream();
								if (contentAsStream != null) {
									OutputStream os = connection.getOutputStream();
									try {
										StreamUtils.copyStream(contentAsStream, os);
									} finally {
										StreamUtils.closeQuietly(os);
									}
								}
							}
						}

						connection.connect();

						final HttpClientResponse clientResponse = new HttpClientResponse(connection);
						try {
							lock.lock();
							HttpResponseListener listener = listeners.get(httpRequest);

							if (listener != null) {
								listener.handleHttpResponse(clientResponse);
								listeners.remove(httpRequest);
							}

							connections.remove(httpRequest);
						} finally {
							connection.disconnect();
							lock.unlock();
						}
					} catch (final Exception e) {
						Gdx.app.log("NetHttpsJavaImpl", e.toString());
						e.printStackTrace();
						connection.disconnect();
						lock.lock();
						try {
							httpResponseListener.failed(e);
						} finally {
							connections.remove(httpRequest);
							listeners.remove(httpRequest);
							lock.unlock();
						}
					}
				}
			});

		} catch (Exception e) {
			Gdx.app.log("NetHttpsJavaImpl", e.toString());
			e.printStackTrace();
			lock.lock();
			try {
				httpResponseListener.failed(e);
			} finally {
				connections.remove(httpRequest);
				listeners.remove(httpRequest);
				lock.unlock();
			}
			return;
		}
	}

	public void cancelHttpRequest(HttpRequest httpRequest) {
		try {
			lock.lock();
			HttpResponseListener httpResponseListener = listeners.get(httpRequest);

			if (httpResponseListener != null) {
				httpResponseListener.cancelled();
				connections.remove(httpRequest);
				listeners.remove(httpRequest);
			}
		} finally {
			lock.unlock();
		}
	}

}
