package model;

import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;

import nlogger.nlogger;

public class FileConvertModel {
	private Process process = null;
	private OpenOfficeConnection connection = null;
	private String command = "";

	/**
	 * 启动openoffice并连接到openoffice
	 * 
	 * @return
	 */
	public OpenOfficeConnection execOpenOffice() {
		String ip = GetFileUrl.getOpenOffice(0);
		String port = GetFileUrl.getOpenOffice(1);
		command = GetFileUrl.getOpenOfficeUrl() + "\\program\\soffice.exe -headless -accept=\"socket,host=" + ip
				+ ",port=" + port + ";urp;\"";
		return Connection(command, ip, port);
	}

	// 连接到openoffice
	private OpenOfficeConnection Connection(String command, String ip, String port) {
		try {
			process = Runtime.getRuntime().exec(command);
			connection = new SocketOpenOfficeConnection(ip, Integer.parseInt(port));
			connection.connect();
			if (!connection.isConnected()) {
				Connection(command, ip, port);
			}
		} catch (Exception e) {
			nlogger.logout(e);
			connection = null;
		}
		return connection;
	}

	/**
	 * 关闭连接
	 */
	public void close(OpenOfficeConnection connection) {
		if (connection.isConnected()) {
			connection.disconnect();
			process.destroy();
			connection = null;
		}
	}
}