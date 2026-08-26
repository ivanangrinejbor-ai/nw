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
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipArchiver {

	private static final String DIRECTORY_LEVEL_UP = "../";
	private static final int ZIP_SLIP_BUFFER = 8192;
	private static final int COMPRESSION_LEVEL = 9;

	private static final String[] ROOT_JUNK_FILES = {
			"undo_code.xml", "code_undo.xml", "automatic_screenshot.png",
			"devicevariables.json", "devicelists.json"
	};
	private static final String[] ANYWHERE_JUNK_SUFFIXES = {".tmp"};
	private static final String[] ANYWHERE_JUNK_NAMES = {".ds_store", "thumbs.db", "desktop.ini"};
	private static final String[] STORED_EXTENSIONS = {
			"jpg", "jpeg", "png", "webp", "mp3", "m4a", "aac", "ogg", "oga", "glb"
	};

	private static final long MAX_UNCOMPRESSED_SIZE = 3072L * 1024 * 1024;
	private static final long MAX_ENTRY_SIZE = 200L * 1024 * 1024;
	private static final int MAX_COMPRESSION_RATIO = 100;
	private static final int MAX_ENTRY_COUNT = 10000;

	private static final long UNZIP_TIMEOUT_SECONDS = 600;
	private static final long ENTRY_TIMEOUT_SECONDS = 120;

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

	public void zipDedup(File archive, File[] files) throws IOException {
		java.security.MessageDigest md5;
		try {
			md5 = java.security.MessageDigest.getInstance("MD5");
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
		Map<String, String> seenHashes = new HashMap<>();
		List<String[]> duplicates = new ArrayList<>();

		archive.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(archive);
		ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
		try {
			zipOutputStream.setLevel(COMPRESSION_LEVEL);
			writeDedupEntriesToStream(zipOutputStream, Arrays.asList(files), "", md5, seenHashes, duplicates);
			if (!duplicates.isEmpty()) {
				String manifest = buildDedupManifest(duplicates);
				zipOutputStream.putNextEntry(new ZipEntry("dedup_manifest.json"));
				zipOutputStream.write(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				zipOutputStream.closeEntry();
			}
		} finally {
			zipOutputStream.close();
			fileOutputStream.close();
		}
	}

	private String buildDedupManifest(List<String[]> duplicates) throws IOException {
		try {
			org.json.JSONArray pairs = new org.json.JSONArray();
			for (String[] pair : duplicates) {
				pairs.put(new org.json.JSONArray().put(pair[0]).put(pair[1]));
			}
			return new org.json.JSONObject().put("deduplicated", pairs).toString();
		} catch (org.json.JSONException e) {
			throw new IOException(e);
		}
	}

	private void writeDedupEntriesToStream(ZipOutputStream zipOutputStream, List<File> files, String parentDir,
			java.security.MessageDigest md5, Map<String, String> seenHashes, List<String[]> duplicates) throws IOException {
		for (File file : files) {
			if (!file.exists()) {
				throw new FileNotFoundException("File: " + file.getAbsolutePath() + " does NOT exist.");
			}
			if (isJunkFile(file, parentDir)) {
				continue;
			}
			if (file.isDirectory()) {
				writeDedupEntriesToStream(zipOutputStream, Arrays.asList(file.listFiles()), parentDir
						+ file.getName() + "/", md5, seenHashes, duplicates);
				continue;
			}

			String relativePath = parentDir + file.getName();
			String hash = computeMd5(file, md5);
			String existingPath = seenHashes.get(hash);
			if (existingPath != null) {
				duplicates.add(new String[] {relativePath, existingPath});
				continue;
			}
			seenHashes.put(hash, relativePath);

			writeSingleEntry(zipOutputStream, file, relativePath);
		}
	}

	private void writeSingleEntry(ZipOutputStream zipOutputStream, File file, String entryName) throws IOException {
		ZipEntry zipEntry = new ZipEntry(entryName);
		boolean stored = shouldStoreUncompressed(file);
		if (stored) {
			zipEntry.setMethod(ZipEntry.STORED);
			zipEntry.setSize(file.length());
			zipEntry.setCrc(computeCrc32(file));
		}
		zipOutputStream.putNextEntry(zipEntry);

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

	private String computeMd5(File file, java.security.MessageDigest md5) throws IOException {
		md5.reset();
		try (FileInputStream in = new FileInputStream(file)) {
			byte[] b = new byte[Constants.BUFFER_8K];
			int len;
			while ((len = in.read(b)) != -1) {
				md5.update(b, 0, len);
			}
		}
		StringBuilder sb = new StringBuilder(32);
		for (byte x : md5.digest()) {
			sb.append(String.format("%02x", x));
		}
		return sb.toString();
	}

	private void writeZipEntriesToStream(ZipOutputStream zipOutputStream, List<File> files, String parentDir) throws IOException {
		for (File file : files) {
			if (!file.exists()) {
				throw new FileNotFoundException("File: " + file.getAbsolutePath() + " does NOT exist.");
			}

			if (isJunkFile(file, parentDir)) {
				continue;
			}

			if (file.isDirectory()) {
				writeZipEntriesToStream(zipOutputStream, Arrays.asList(file.listFiles()), parentDir
						+ file.getName() + "/");
				continue;
			}

			writeSingleEntry(zipOutputStream, file, parentDir + file.getName());
		}
	}

	private boolean isJunkFile(File file, String parentDir) {
		String name = file.getName();
		if (file.isDirectory()) {
			return false;
		}
		String lower = name.toLowerCase(java.util.Locale.ROOT);
		for (String suffix : ANYWHERE_JUNK_SUFFIXES) {
			if (lower.endsWith(suffix)) {
				return true;
			}
		}
		if (Arrays.asList(ANYWHERE_JUNK_NAMES).contains(lower)) {
			return true;
		}
		if (parentDir.isEmpty()) {
			for (String junk : ROOT_JUNK_FILES) {
				if (lower.equals(junk)) {
					return true;
				}
			}
			if (lower.equals("_recovery_autosave.rscene")) {
				return true;
			}
			if (lower.contains("_autosave") && lower.endsWith(".rscene")) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldStoreUncompressed(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		if (dot < 0 || dot == name.length() - 1) {
			return false;
		}
		String ext = name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
		return Arrays.asList(STORED_EXTENSIONS).contains(ext);
	}

	private long computeCrc32(File file) throws IOException {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		try (FileInputStream in = new FileInputStream(file)) {
			byte[] b = new byte[Constants.BUFFER_8K];
			int len;
			while ((len = in.read(b)) != -1) {
				crc.update(b, 0, len);
			}
		}
		return crc.getValue();
	}

	public void unzip(File archive, File dstDir) throws IOException {
  		InputStream inputStream = new FileInputStream(archive);
		unzip(inputStream, dstDir);
	}

	public void unzip(InputStream is, File dstDir) throws IOException {
		createDirIfNecessary(dstDir);
		String dstCanonical = dstDir.getCanonicalPath();

		try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
			ZipEntry zipEntry;
			int entryCount = 0;
			long totalUncompressedSize = 0;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				entryCount++;
				if (entryCount > MAX_ENTRY_COUNT) {
					throw new IOException("Zip bomb detected: too many entries (" + entryCount + " > " + MAX_ENTRY_COUNT + ")");
				}

				File zipEntryFile = new File(dstDir, zipEntry.getName());
				if (!zipEntryFile.getCanonicalPath().startsWith(dstCanonical + File.separator)) {
					continue;
				}
				if (zipEntry.isDirectory()) {
					createDirIfNecessary(zipEntryFile);
					continue;
				}

				zipEntryFile.getParentFile().mkdirs();

				long entrySize = 0;
				try (FileOutputStream fileOutputStream = new FileOutputStream(zipEntryFile)) {
					byte[] b = new byte[ZIP_SLIP_BUFFER];
					int len;
					while ((len = zipInputStream.read(b)) != -1) {
						fileOutputStream.write(b, 0, len);
						entrySize += len;
						if (entrySize > MAX_ENTRY_SIZE) {
							throw new IOException("Entry too large: " + zipEntry.getName()
									+ " (" + entrySize + " bytes exceeds " + MAX_ENTRY_SIZE + ")");
						}
					}
				}

				totalUncompressedSize += entrySize;
				if (totalUncompressedSize > MAX_UNCOMPRESSED_SIZE) {
					throw new IOException("Zip bomb detected: total uncompressed size exceeds "
							+ MAX_UNCOMPRESSED_SIZE + " bytes");
				}

				long compressedSize = zipEntry.getCompressedSize();
				if (compressedSize > 0 && entrySize > compressedSize * MAX_COMPRESSION_RATIO) {
					throw new IOException("Zip bomb detected: compression ratio " + (entrySize / compressedSize)
							+ ":1 exceeds limit of " + MAX_COMPRESSION_RATIO + ":1 for entry: " + zipEntry.getName());
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

		if (allEntries.size() > MAX_ENTRY_COUNT) {
			throw new IOException("Zip bomb detected: too many entries (" + allEntries.size() + " > " + MAX_ENTRY_COUNT + ")");
		}

		String dstCanonical = dstDir.getCanonicalPath();
		for (ZipEntry entry : allEntries) {
			File entryFile = new File(dstDir, entry.getName());
			if (!entryFile.getCanonicalPath().startsWith(dstCanonical + File.separator)) {
				continue;
			}
			if (entry.isDirectory()) {
				createDirIfNecessary(entryFile);
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
			java.util.concurrent.atomic.AtomicLong totalUncompressed = new java.util.concurrent.atomic.AtomicLong(0);

			List<Future<?>> futures = new ArrayList<>();
			for (ZipEntry entry : fileEntries) {
				futures.add(executor.submit(() -> {
					File zipEntryFile = new File(dstDir, entry.getName());
					try {
						if (!zipEntryFile.getCanonicalPath().startsWith(dstCanonical + File.separator)) {
							return;
						}
					} catch (IOException ioe) {
						return;
					}
					zipEntryFile.getParentFile().mkdirs();

					long entrySize = 0;
					try {
						synchronized (zipFile) {
							try (InputStream in = zipFile.getInputStream(entry);
								 FileOutputStream out = new FileOutputStream(zipEntryFile)) {
								byte[] b = new byte[Constants.BUFFER_8K];
								int len;
								while ((len = in.read(b)) != -1) {
									out.write(b, 0, len);
									entrySize += len;
									if (entrySize > MAX_ENTRY_SIZE) {
										throw new IOException("Entry too large: " + entry.getName()
												+ " (" + entrySize + " bytes exceeds " + MAX_ENTRY_SIZE + ")");
									}
								}
							}
						}
					} catch (IOException ioEx) {
						throw new RuntimeException("Failed to extract: " + entry.getName(), ioEx);
					}

					long compressedSize = entry.getCompressedSize();
					if (compressedSize > 0 && entrySize > compressedSize * MAX_COMPRESSION_RATIO) {
						throw new RuntimeException("Zip bomb detected: compression ratio "
								+ (entrySize / compressedSize) + ":1 exceeds limit for entry: " + entry.getName());
					}

					long totalSoFar = totalUncompressed.addAndGet(entrySize);
					if (totalSoFar > MAX_UNCOMPRESSED_SIZE) {
						throw new RuntimeException("Zip bomb detected: total uncompressed size exceeds " + MAX_UNCOMPRESSED_SIZE + " bytes");
					}

					int done = processed.incrementAndGet();
					if (listener != null) {
						int pct = done * 100 / totalFiles;
						listener.onProgress(pct, done, totalFiles, entry.getName());
					}
				}));
			}

			executor.shutdown();
			try {
				if (!executor.awaitTermination(UNZIP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
					executor.shutdownNow();
					if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
						throw new IOException("Unzip timed out after " + UNZIP_TIMEOUT_SECONDS + "s");
					}
				}
			} catch (InterruptedException intEx) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
				throw new IOException("Unzip was interrupted", intEx);
			}

			IOException firstError = null;
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				} catch (java.util.concurrent.CancellationException ce) {
					if (firstError == null) {
						firstError = new IOException("Unzip timed out on one or more entries");
					}
				} catch (java.util.concurrent.ExecutionException ee) {
					Throwable cause = ee.getCause();
					if (firstError == null) {
						firstError = (cause instanceof IOException)
							? (IOException) cause
							: new IOException("Unzip failed: " + cause.getMessage(), cause);
					}
				}
			}
			if (firstError != null) {
				throw firstError;
			}
		}
	}

	private void createDirIfNecessary(File dir) throws IOException {
		if (!dir.exists() && !dir.mkdir()) {
			throw new IOException("Could NOT create Dir: " + dir.getAbsolutePath());
		}
	}
}
