package org.catrobat.catroid.ui.sceneeditor;

import android.util.Log;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.io.XstreamSerializer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ProjectSaveCoordinator {

	private static final String TAG = "ProjectSaveCoordinator";

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "ui2-project-save");
		thread.setDaemon(true);
		return thread;
	});

	private static final Object LOCK = new Object();
	private static volatile Project pendingProject;
	private static boolean workerRunning;

	private ProjectSaveCoordinator() {
	}

	public static void saveAsync(Project project) {
		if (project == null) {
			return;
		}
		boolean startWorker;
		synchronized (LOCK) {
			pendingProject = project;
			startWorker = !workerRunning;
			if (startWorker) {
				workerRunning = true;
			}
		}
		if (startWorker) {
			EXECUTOR.execute(ProjectSaveCoordinator::runWorker);
		}
	}

	public static boolean saveBlocking(Project project) {
		if (project == null) {
			return false;
		}
		try {
			Future<Boolean> future = EXECUTOR.submit(() -> {
				synchronized (LOCK) {
					pendingProject = null;
				}
				return saveNow(project);
			});
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (ExecutionException e) {
			return false;
		}
	}

	private static void runWorker() {
		while (true) {
			Project toSave;
			synchronized (LOCK) {
				toSave = pendingProject;
				pendingProject = null;
			}
			if (toSave == null) {
				break;
			}
			saveNow(toSave);
		}
		synchronized (LOCK) {
			if (pendingProject != null) {
				if (workerRunning) {
					EXECUTOR.execute(ProjectSaveCoordinator::runWorker);
				}
				return;
			}
			workerRunning = false;
		}
	}

	private static boolean saveNow(Project project) {
		try {
			XstreamSerializer.getInstance().saveProject(project);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Failed to save project " + project.getName(), e);
			return false;
		}
	}
}