package org.catrobat.catroid.formulaeditor;

import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

public final class ExternalIpFetcher {

	private static final String TAG = ExternalIpFetcher.class.getSimpleName();
	private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;
	private static final long FAILURE_TTL_MILLIS = 30 * 1000L;

	private static String cachedIp = "Unknown";
	private static long cachedAt = 0L;

	private ExternalIpFetcher() {
	}

	public static synchronized String getExternalIp() {
		long now = System.currentTimeMillis();
		try {
			if (!cachedIp.equals("Unknown") && now - cachedAt < CACHE_TTL_MILLIS) {
				return cachedIp;
			}
			if (cachedIp.equals("Unknown") && now - cachedAt < FAILURE_TTL_MILLIS) {
				return cachedIp;
			}
			if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
				return cachedIp;
			}
		} catch (Exception e) {
			return "Unknown";
		}
		String result = "Unknown";
		HttpURLConnection connection = null;
		java.util.Scanner scanner = null;
		try {
			connection = (HttpURLConnection) new URL("https://api.ipify.org").openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			int code = connection.getResponseCode();
			if (code == 200) {
				java.io.InputStream input = connection.getInputStream();
				scanner = new java.util.Scanner(input, "UTF-8").useDelimiter("\\A");
				String body = scanner.hasNext() ? scanner.next() : "";
				if (body != null && !body.trim().isEmpty()) {
					result = body.trim();
				}
			}
		} catch (Throwable e) {
			Log.e(TAG, "Failed to fetch external IP", e);
		} finally {
			if (scanner != null) {
				try { scanner.close(); } catch (Throwable ignored) {}
			}
			if (connection != null) {
				try { connection.disconnect(); } catch (Throwable ignored) {}
			}
		}
		try {
			cachedIp = result;
			cachedAt = now;
		} catch (Throwable ignored) {}
		return result;
	}
}