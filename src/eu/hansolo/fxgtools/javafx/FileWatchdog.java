package eu.hansolo.fxgtools.javafx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;


/**
 * Created by
 * User: hansolo
 * Date: 27.02.12
 * Time: 13:38
 */
public class FileWatchdog implements Runnable {
    private File         folder;
    private File         file;
    private WatchService watchService;
    private boolean      listening;

    public FileWatchdog(final File FOLDER, final File FILE) {
        folder = FOLDER;
        file = FILE;
    }

    private void listenForChanges(final File FOLDER, final File FILE) throws IOException {
        Path path = FOLDER.toPath();
        if (FOLDER.isDirectory()) {
            watchService = path.getFileSystem().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            WatchKey watch = null;
            while (true) {
                System.out.println("Watching directory: " + FOLDER.getPath());
                try {
                    watch = watchService.take();
                } catch (InterruptedException ex) {
                    System.err.println("Interrupted");
                }
                List<WatchEvent<?>> events = watch.pollEvents();
                watch.reset();
                for (WatchEvent<?> event : events) {
                    WatchEvent.Kind<Path> kind = (WatchEvent.Kind<Path>) event.kind();
                    Path context = (Path) event.context();
                    if (kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                        System.out.println("OVERFLOW");
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        System.out.println("Created: " + context.getFileName());
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        System.out.println("Deleted: " + context.getFileName());
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        if (context.toString().equals(FILE.getName())) {
                            System.out.println("Given FILE was updated");
                        } else {
                            System.out.println("Modified: " + context.getFileName());
                        }
                    }
                }
            }
        } else {
            System.err.println("Not a FOLDER. Will exit.");
        }
    }

    @Override
    public void run() {
        listening = true;
        while(listening) {
            try {
                System.out.println("Listening on: " + folder);
                listenForChanges(folder, file);
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }

        try {
            watchService.close();
        } catch (IOException e) {

        }
    }
}
