package com.foodguider.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.json.JSONObject;

import com.foodguider.util.Cmd;
import com.foodguider.util.pRes;

/**
 * 이 클래스에서는 Key값 대조를 통해 비정상적인 접속을 차단하고 서버의 트래픽을 조절함으로서 서버의 부하를 방지
 * 
 * 정상적인 접속을 한 유저에 한해 사진 검색을 허용
 * 
 * @author root
 *
 */
public final class Gate implements Runnable {
	private Socket clientSocket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	public Gate(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		if (!getStream())
			return;

		if (!keyMatching(getRequestKey()))
			return;

		if (serverIsBusy()) {
			sendServerStatus(Cmd.SERVER_IS_BUSY);
			return;
		}

		sendServerStatus(Cmd.SERVER_IS_STABLE);
		enterServer();
	}

	// 클라언트의 데이터 스트림을 받아옴
	private boolean getStream() {
		try {
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch (Exception e) {
			disconnect();
			pRes.log("Client data stream get failure");
			return false;
		}

		return true;
	}

	// 키 대조
	private boolean keyMatching(String requestKey) {
		if (!requestKey.equals(Cmd.SERVER_KEY)) {
			disconnect();
			pRes.log("[FATAL] Key matching failure");
			return false;
		}

		return true;
	}

	// 요청정보에서 서버 키 추출
	private String getRequestKey() {
		String requestKey = "DEFAULT";
		try {
			JSONObject json = new JSONObject((String) ois.readObject());
			requestKey = (String) json.getString("key");
		} catch (Exception e) {
			pRes.log("Key not found");
			e.printStackTrace();
		}
		
		return requestKey;
	}

	private boolean serverIsBusy() {
		return pRes.MAX_THREAD_COUNT - 5 <= pRes.threadCountAlerter.getActiveCount();
	}

	private void sendServerStatus(String serverStatus) {
		send("serverStatus", serverStatus);

		if (serverStatus.equals(Cmd.SERVER_IS_BUSY)) 
			disconnect();
	}

	private void enterServer() {
		pRes.log("접속  IP : " + clientSocket.getInetAddress().getHostAddress());
		pRes.serverThreadPool.execute(new RequestPic(oos, ois));
	}

	private void send(String request, String msg) {
		JSONObject json = new JSONObject();
		try {
			json.put(request, msg);
			oos.writeObject(json.toString());
		} catch (Exception e) {
		}
	}

	private void disconnect() {
		try {
			clientSocket.getOutputStream().close();
		} catch (Exception e) {
		}
	}
}