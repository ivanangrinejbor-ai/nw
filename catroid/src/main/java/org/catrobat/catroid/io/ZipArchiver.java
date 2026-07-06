/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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

package org.catrobat.catroid.io;

import org.catrobat.catroid.common.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipArchiver {

	private static final String DIRECTORY_LEVEL_UP = "../";
	private static final int COMPRESSION_LEVEL = 9;

	public void zip(File archive, File[] files) throws IOException {
		archive.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(archive);
		ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
		try {
			zipOutputStream.setLevel(COMPRESSION_LEVEL);
			writeZipEntriesToStream(zipOutputStream, Arrays.asList(files), "");
		} finally {
			zipOutputStream.close();
			fileOutputStream.close();
		}
	}

	private void writeZipEntriesToStream(ZipOutputStream zipOutputStream, List<File> files, String parentDir) throws IOException {
		for (File file : files) {
			if (!file.exists()) {
				throw new FileNotFoundException("File: " + file.getAbsolutePath() + " does NOT exist.");
			}

			if (file.isDirectory()) {
				writeZipEntriesToStream(zipOutputStream, Arrays.asList(file.listFiles()), parentDir
						+ file.getName() + "/");
				continue;
			}

			zipOutputStream.putNextEntry(new ZipEntry(parentDir + file.getName()));

			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				byte[] b = new byte[Constants.BUFFER_8K];
				int len;
				while ((len = fileInputStream.read(b)) != -1) {
					zipOutputStream.write(b, 0, len);
				}
			} finally {
				zipOutputStream.closeEntry();
			}
		}
	}

	public void unzip(File archive, File dstDir) throws IOException {
  		InputStream inputStream = new FileInputStream(archive);
		unzip(inputStream, dstDir);
	}

	public void unzip(InputStream is, File dstDir) throws IOException {
		createDirIfNecessary(dstDir);

		try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				if (zipEntry.getName().contains(DIRECTORY_LEVEL_UP)) {
					continue;
				}
				if (zipEntry.isDirectory()) {
					createDirIfNecessary(new File(dstDir, zipEntry.getName()));
					continue;
				}

				File zipEntryFile = new File(dstDir, zipEntry.getName());
				zipEntryFile.getParentFile().mkdirs();

				try (FileOutputStream fileOutputStream = new FileOutputStream(zipEntryFile)) {
					byte[] b = new byte[Constants.BUFFER_8K];
					int len;
					while ((len = zipInputStream.read(b)) != -1) {
						fileOutputStream.write(b, 0, len);
					}
				}
			}
		}
	}

	public interface UnzipProgressListener {
		void onProgress(int percent, int filesDone, int totalFiles, String currentFile);
	}

	public void unzip(File archive, File dstDir, UnzipProgressListener listener) throws IOException {
		createDirIfNecessary(dstDir);

		try (ZipFile zipFile = new ZipFile(archive)) {
			List<ZipEntry> allEntries = new ArrayList<>();
			java.util.Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements()) allEntries.add(e.nextElement());
			List<ZipEntry> fileEntries = new ArrayList<>();

			for (ZipEntry entry : allEntries) {
				if (entry.getName().contains(DIRECTORY_LEVEL_UP)) {
					continue;
				}
				if (entry.isDirectory()) {
					createDirIfNecessary(new File(dstDir, entry.getName()));
					continue;
				}
				fileEntries.add(entry);
			}

			int totalFiles = fileEntries.size();
			if (totalFiles == 0) return;

			int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
			ExecutorService executor = Executors.newFixedThreadPool(threads, r -> {
				Thread t = new Thread(r, "unzip-worker");
				t.setDaemon(true);
				return t;
			});

			AtomicInteger processed = new AtomicInteger(0);

			for (ZipEntry entry : fileEntries) {
				executor.submit(() -> {
					File zipEntryFile = new File(dstDir, entry.getName());
					zipEntryFile.getParentFile().mkdirs();

					try (InputStream in = zipFile.getInputStream(entry);
						 FileOutputStream out = new FileOutputStream(zipEntryFile)) {
						byte[] b = new byte[Constants.BUFFER_8K];
						int len;
						while ((len = in.read(b)) != -1) {
							out.write(b, 0, len);
						}
					} catch (IOException ioEx) {
						throw new RuntimeException("Failed to extract: " + entry.getName(), ioEx);
					}

					int done = processed.incrementAndGet();
					if (listener != null) {
						int pct = done * 100 / totalFiles;
						listener.onProgress(pct, done, totalFiles, entry.getName());
					}
				});
			}

			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException intEx) {
				Thread.currentThread().interrupt();
				throw new IOException("Unzip was interrupted", intEx);
			}
		}
	}

	private void createDirIfNecessary(File dir) throws IOException {
		if (!dir.exists() && !dir.mkdir()) {
			throw new IOException("Could NOT create Dir: " + dir.getAbsolutePath());
		}
	}
}
