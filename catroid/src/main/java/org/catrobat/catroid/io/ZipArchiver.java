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

	// Zip bomb protection limits
	private static final long MAX_UNCOMPRESSED_SIZE = 3072L * 1024 * 1024; // 3 GB
	private static final long MAX_ENTRY_SIZE = 200L * 1024 * 1024; // 200 MB per entry (was 50 MB, bumped for large .glb 3D models)
	private static final int MAX_COMPRESSION_RATIO = 100; // 100:1
	private static final int MAX_ENTRY_COUNT = 10000;

	// Timeout for unzip operation
	private static final long UNZIP_TIMEOUT_SECONDS = 600; // 10 minutes total
	private static final long ENTRY_TIMEOUT_SECONDS = 120; // 2 minutes per entry

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

				// Check compression ratio: if compressed size is known and very small but output is large
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
						// TOCTOU check on canonical path before extraction to prevent Zip Slip
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

					// Compression ratio check
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
