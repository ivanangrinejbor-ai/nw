/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.web;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.catrobat.catroid.R;

import java.util.Locale;

public class MediaGalleryWebViewClient extends WebViewClient {
	private AlertDialog webViewLoadingDialog;
	private WebClientCallback callback;

	public interface WebClientCallback {
		void finish();
	}

	public MediaGalleryWebViewClient(WebClientCallback callback) {
		super();
		this.callback = callback;
	}

	@Override
	public void onPageStarted(WebView view, String urlClient, Bitmap favicon) {
		if (webViewLoadingDialog == null && !urlClient.matches("https://share.catrob.at/pocketcode/")) {
			showWebViewLoadingDialog(view.getContext());
		} else {
			callback.finish();
		}
	}

	private void showWebViewLoadingDialog(android.content.Context context) {
		ProgressBar progressBar = new ProgressBar(context, null, android.R.style.Widget_ProgressBar_Small);
		progressBar.setIndeterminate(true);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		progressBar.setLayoutParams(params);

		AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.WebViewLoadingCircle);
		builder.setView(progressBar);
		builder.setCancelable(true);

		webViewLoadingDialog = builder.create();
		webViewLoadingDialog.setCanceledOnTouchOutside(false);
		webViewLoadingDialog.show();
	}

	private void dismissWebViewLoadingDialog() {
		if (webViewLoadingDialog != null) {
			webViewLoadingDialog.dismiss();
			webViewLoadingDialog = null;
		}
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		dismissWebViewLoadingDialog();
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		Uri uri = Uri.parse(url);
		String scheme = uri.getScheme();
		if (scheme == null) {
			return true;
		}
		scheme = scheme.toLowerCase(Locale.US);
		if (!scheme.equals("http") && !scheme.equals("https")) {
			return true;
		}
		String host = uri.getHost();
		if (host == null) {
			return true;
		}
		host = host.toLowerCase(Locale.US);
		boolean allowedHost = host.equals("share.catrob.at") || host.endsWith(".share.catrob.at")
				|| host.equals("catrob.at") || host.endsWith(".catrob.at");
		return !allowedHost;
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		callback.finish();
	}
}
