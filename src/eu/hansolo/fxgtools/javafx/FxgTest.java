package eu.hansolo.fxgtools.javafx;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * User: han.solo at muenster.de
 * Date: 02.09.11
 * Time: 09:22
 */
public class FxgTest extends Application
{
    private File                folder       = new File("/Volumes/Macintosh HD/Users/hansolo/Desktop/InSync/Java Apps/FXG Converter/fxg files");
    private File                file         = new File(folder + System.getProperty("file.separator") + "LcdFX.fxg");
    private Map<String, Group>  groups       = new HashMap<>();
    private Group               dropZone     = new Group();
    private int                 width        = 200;
    private int                 height       = 200;
    private StackPane           stackPane;
    private WatchService        watchService;
    private Thread              fileWatcher;

    @Override
    public void start(Stage stage) {
        drawDropZone(width, height, groups);
        final DataFormat customFormat = new DataFormat("helloworld.custom");

        final File folder = new File("/Volumes/Macintosh HD/Users/hansolo/Desktop/InSync/Java Apps/FXG Converter/fxg files");
        final File file = new File(folder + System.getProperty("file.separator") + "LcdFX.fxg");

        fileWatcher = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listenForChanges(folder, file);
                } catch(ClosedWatchServiceException exception) {
                    System.out.println("WatchService closed");
                } catch (IOException exception) {
                   System.out.println(exception);
                }
            }
        });
        fileWatcher.start();
        stackPane = new StackPane();

        stackPane.getChildren().add(dropZone);
        //convert(file.getAbsolutePath());

        Scene scene = new Scene(stackPane, width, height);
        initSceneDragAndDrop(scene);
        stage.setTitle("FXG -> JavaFX (live)");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws IOException {
        watchService.close();
        fileWatcher.interrupt();
    }

    private void listenForChanges(final File FOLDER, final File FILE) throws IOException, ClosedWatchServiceException {
        java.nio.file.Path path = FOLDER.toPath();
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
                    java.nio.file.Path context = (java.nio.file.Path) event.context();
                    if (kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                        System.out.println("OVERFLOW");
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        System.out.println("Created: " + context.getFileName());
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        System.out.println("Deleted: " + context.getFileName());
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        if (context.toString().equals(FILE.getName())) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    convert(FILE.getAbsolutePath());
                                }
                            });
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

    private void convert(final String FILE_NAME) {
            FxgFxParser parser = new FxgFxParser();
        groups.clear();
        groups = parser.parse(FILE_NAME, width, height, true);
        drawDropZone(width, height, groups);
    }

    private void drawDropZone(final int WIDTH, final int HEIGHT, final Map<String, Group> FXG_GROUPS) {
        final double SIZE = WIDTH <= HEIGHT ? WIDTH : HEIGHT;

        dropZone.getChildren().clear();

        if (FXG_GROUPS.isEmpty()) {
            final Color COLOR = Color.rgb(165, 165, 165);
            final javafx.scene.shape.Path ARROW = new javafx.scene.shape.Path();
            ARROW.setFillRule(FillRule.EVEN_ODD);
            ARROW.getElements().add(new MoveTo(SIZE * 0.4, SIZE * 0.3));
            ARROW.getElements().add(new LineTo(SIZE * 0.6142857142857143, SIZE * 0.3));
            ARROW.getElements().add(new LineTo(SIZE * 0.6142857142857143, SIZE * 0.5285714285714286));
            ARROW.getElements().add(new LineTo(SIZE * 0.7428571428571429, SIZE * 0.5285714285714286));
            ARROW.getElements().add(new LineTo(SIZE * 0.5142857142857142, SIZE * 0.7142857142857143));
            ARROW.getElements().add(new LineTo(SIZE * 0.2714285714285714, SIZE * 0.5285714285714286));
            ARROW.getElements().add(new LineTo(SIZE * 0.4, SIZE * 0.5285714285714286));
            ARROW.getElements().add(new LineTo(SIZE * 0.4, SIZE * 0.3));
            ARROW.getElements().add(new ClosePath());
            ARROW.setFill(COLOR);
            ARROW.setStroke(null);

            final Rectangle TOPLEFTRECT = new Rectangle(SIZE * 0.2571428571428571, SIZE * 0.02857142857142857, SIZE * 0.2, SIZE * 0.04285714285714286);
            TOPLEFTRECT.setFill(COLOR);
            TOPLEFTRECT.setStroke(null);

            final Rectangle TOPRIGHTRECT = new Rectangle(SIZE * 0.5428571428571428, SIZE * 0.02857142857142857, SIZE * 0.2, SIZE * 0.04285714285714286);
            TOPRIGHTRECT.setFill(COLOR);
            TOPRIGHTRECT.setStroke(null);

            final Rectangle UPPERRIGHTRECT = new Rectangle(SIZE * 0.9285714285714286, SIZE * 0.24285714285714285, SIZE * 0.04285714285714286, SIZE * 0.2);
            UPPERRIGHTRECT.setFill(COLOR);
            UPPERRIGHTRECT.setStroke(null);

            final Rectangle LOWERRIGHTRECT = new Rectangle(SIZE * 0.9285714285714286, SIZE * 0.5428571428571428, SIZE * 0.04285714285714286, SIZE * 0.2);
            LOWERRIGHTRECT.setFill(COLOR);
            LOWERRIGHTRECT.setStroke(null);

            final Rectangle BOTTOMRIGHTRECT = new Rectangle(SIZE * 0.5571428571428572, SIZE * 0.9285714285714286, SIZE * 0.2, SIZE * 0.04285714285714286);
            BOTTOMRIGHTRECT.setFill(COLOR);
            BOTTOMRIGHTRECT.setStroke(null);

            final Rectangle BOTTOMLEFTRECT = new Rectangle(SIZE * 0.2571428571428571, SIZE * 0.9285714285714286, SIZE * 0.2, SIZE * 0.04285714285714286);
            BOTTOMLEFTRECT.setFill(COLOR);
            BOTTOMLEFTRECT.setStroke(null);

            final Rectangle LOWERLEFTRECT = new Rectangle(SIZE * 0.02857142857142857, SIZE * 0.5571428571428572, SIZE * 0.04285714285714286, SIZE * 0.2);
            LOWERLEFTRECT.setFill(COLOR);
            LOWERLEFTRECT.setStroke(null);

            final Rectangle UPPERLEFTRECT = new Rectangle(SIZE * 0.02857142857142857, SIZE * 0.2571428571428571, SIZE * 0.04285714285714286, SIZE * 0.2);
            UPPERLEFTRECT.setFill(COLOR);
            UPPERLEFTRECT.setStroke(null);

            final Path CORNER1 = new Path();
            CORNER1.setFillRule(FillRule.EVEN_ODD);
            CORNER1.getElements().add(new MoveTo(SIZE * 0.07142857142857142, SIZE * 0.14285714285714285));
            CORNER1.getElements().add(new CubicCurveTo(SIZE * 0.07142857142857142, SIZE * 0.1, SIZE * 0.1, SIZE * 0.07142857142857142, SIZE * 0.14285714285714285, SIZE * 0.07142857142857142));
            CORNER1.getElements().add(new CubicCurveTo(SIZE * 0.14285714285714285, SIZE * 0.07142857142857142, SIZE * 0.14285714285714285, SIZE * 0.02857142857142857, SIZE * 0.14285714285714285, SIZE * 0.02857142857142857));
            CORNER1.getElements().add(new CubicCurveTo(SIZE * 0.08571428571428572, SIZE * 0.02857142857142857, SIZE * 0.02857142857142857, SIZE * 0.08571428571428572, SIZE * 0.02857142857142857, SIZE * 0.14285714285714285));
            CORNER1.getElements().add(new CubicCurveTo(SIZE * 0.02857142857142857, SIZE * 0.14285714285714285, SIZE * 0.07142857142857142, SIZE * 0.14285714285714285, SIZE * 0.07142857142857142, SIZE * 0.14285714285714285));
            CORNER1.getElements().add(new ClosePath());
            CORNER1.setFill(COLOR);
            CORNER1.setStroke(null);

            final Path CORNER2 = new Path();
            CORNER2.setFillRule(FillRule.EVEN_ODD);
            CORNER2.getElements().add(new MoveTo(SIZE * 0.9285714285714286, SIZE * 0.14285714285714285));
            CORNER2.getElements().add(new CubicCurveTo(SIZE * 0.9285714285714286, SIZE * 0.1, SIZE * 0.9, SIZE * 0.07142857142857142, SIZE * 0.8571428571428571, SIZE * 0.07142857142857142));
            CORNER2.getElements().add(new CubicCurveTo(SIZE * 0.8571428571428571, SIZE * 0.07142857142857142, SIZE * 0.8571428571428571, SIZE * 0.02857142857142857, SIZE * 0.8571428571428571, SIZE * 0.02857142857142857));
            CORNER2.getElements().add(new CubicCurveTo(SIZE * 0.9142857142857143, SIZE * 0.02857142857142857, SIZE * 0.9714285714285714, SIZE * 0.08571428571428572, SIZE * 0.9714285714285714, SIZE * 0.14285714285714285));
            CORNER2.getElements().add(new CubicCurveTo(SIZE * 0.9714285714285714, SIZE * 0.14285714285714285, SIZE * 0.9285714285714286, SIZE * 0.14285714285714285, SIZE * 0.9285714285714286, SIZE * 0.14285714285714285));
            CORNER2.getElements().add(new ClosePath());
            CORNER2.setFill(COLOR);
            CORNER2.setStroke(null);

            final Path CORNER3 = new Path();
            CORNER3.setFillRule(FillRule.EVEN_ODD);
            CORNER3.getElements().add(new MoveTo(SIZE * 0.07142857142857142, SIZE * 0.8571428571428571));
            CORNER3.getElements().add(new CubicCurveTo(SIZE * 0.07142857142857142, SIZE * 0.9, SIZE * 0.1, SIZE * 0.9285714285714286, SIZE * 0.14285714285714285, SIZE * 0.9285714285714286));
            CORNER3.getElements().add(new CubicCurveTo(SIZE * 0.14285714285714285, SIZE * 0.9285714285714286, SIZE * 0.14285714285714285, SIZE * 0.9714285714285714, SIZE * 0.14285714285714285, SIZE * 0.9714285714285714));
            CORNER3.getElements().add(new CubicCurveTo(SIZE * 0.08571428571428572, SIZE * 0.9714285714285714, SIZE * 0.02857142857142857, SIZE * 0.9142857142857143, SIZE * 0.02857142857142857, SIZE * 0.8571428571428571));
            CORNER3.getElements().add(new CubicCurveTo(SIZE * 0.02857142857142857, SIZE * 0.8571428571428571, SIZE * 0.07142857142857142, SIZE * 0.8571428571428571, SIZE * 0.07142857142857142, SIZE * 0.8571428571428571));
            CORNER3.getElements().add(new ClosePath());
            CORNER3.setFill(COLOR);
            CORNER3.setStroke(null);

            final Path CORNER4 = new Path();
            CORNER4.setFillRule(FillRule.EVEN_ODD);
            CORNER4.getElements().add(new MoveTo(SIZE * 0.9285714285714286, SIZE * 0.8571428571428571));
            CORNER4.getElements().add(new CubicCurveTo(SIZE * 0.9285714285714286, SIZE * 0.9, SIZE * 0.9, SIZE * 0.9285714285714286, SIZE * 0.8571428571428571, SIZE * 0.9285714285714286));
            CORNER4.getElements().add(new CubicCurveTo(SIZE * 0.8571428571428571, SIZE * 0.9285714285714286, SIZE * 0.8571428571428571, SIZE * 0.9714285714285714, SIZE * 0.8571428571428571, SIZE * 0.9714285714285714));
            CORNER4.getElements().add(new CubicCurveTo(SIZE * 0.9142857142857143, SIZE * 0.9714285714285714, SIZE * 0.9714285714285714, SIZE * 0.9142857142857143, SIZE * 0.9714285714285714, SIZE * 0.8571428571428571));
            CORNER4.getElements().add(new CubicCurveTo(SIZE * 0.9714285714285714, SIZE * 0.8571428571428571, SIZE * 0.9285714285714286, SIZE * 0.8571428571428571, SIZE * 0.9285714285714286, SIZE * 0.8571428571428571));
            CORNER4.getElements().add(new ClosePath());
            CORNER4.setFill(COLOR);
            CORNER4.setStroke(null);

            dropZone.getChildren().addAll(ARROW,
                                          TOPLEFTRECT,
                                          TOPRIGHTRECT,
                                          UPPERLEFTRECT,
                                          UPPERRIGHTRECT,
                                          BOTTOMLEFTRECT,
                                          BOTTOMRIGHTRECT,
                                          LOWERLEFTRECT,
                                          LOWERRIGHTRECT,
                                          CORNER1,
                                          CORNER2,
                                          CORNER3,
                                          CORNER4);
        } else {
            dropZone.getChildren().addAll(groups.values());
        }
    }


    private void initSceneDragAndDrop(Scene scene) {
        dropZone.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                System.out.println("DragOver");
                Dragboard dragboard = dragEvent.getDragboard();
                if (dragboard.hasFiles() || dragboard.hasUrl()) {
                    dragEvent.acceptTransferModes(TransferMode.ANY);
                }
                dragEvent.consume();
            }
        });
        dropZone.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                System.out.println("DragEntered");
            }
        });
        dropZone.setOnDragDone(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                System.out.println("DragDone");
            }
        });
        dropZone.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("DragDetected");
            }
        });

        dropZone.setOnMouseDragReleased(new EventHandler<MouseDragEvent>() {
            @Override
            public void handle(MouseDragEvent mouseDragEvent) {
                System.out.println("MouseDragReleased");
        }
        });
        dropZone.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent dragEvent) {
                System.out.println("DragDropped");
                Dragboard dragboard = dragEvent.getDragboard();
                String url = null;
                if (dragboard.hasFiles()) {
                    url = dragboard.getFiles().get(0).toURI().toString();
                } else if (dragboard.hasUrl()) {
                    url = dragboard.getUrl();
                }
                if (url != null) {
                    System.out.println(url);
                    convert(url.toString());
                }
                dragEvent.setDropCompleted(url != null);
                dragEvent.consume();
            }
        });
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
