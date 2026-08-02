package org.catrobat.catroid.ui.sceneeditor;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.io.XstreamSerializer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serializes project writes made by the UI 2.0 editors. */
public final class ProjectSaveCoordinator {

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "ui2-project-save");
		thread.setDaemon(true);
		return thread;
	});

	private ProjectSaveCoordinator() {
	}

	public static void saveAsync(Project project) {
		if (project == null) return;
		EXECUTOR.execute(() -> saveNow(project));
	}

	public static boolean saveBlocking(Project project) {
		if (project == null) return false;
		try {
			EXECUTOR.submit(() -> saveNow(project)).get();
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (ExecutionException e) {
			return false;
		}
	}

	private static void saveNow(Project project) {
		try {
			XstreamSerializer.getInstance().saveProject(project);
		} catch (Exception ignored) {
		}
	}
}
