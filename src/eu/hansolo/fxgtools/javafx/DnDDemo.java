package eu.hansolo.fxgtools.javafx;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


/**
 * Created by
 * User: hansolo
 * Date: 15.05.12
 * Time: 16:57
 */
public class DnDDemo extends Application {
    /**
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {

        Group root = new Group();
        Scene scene = new Scene(root, 800, 600, Color.BLACK);
        stage.setScene(scene);
        initSceneDragAndDrop(scene);
        stage.show();
    }

    private void initSceneDragAndDrop(Scene scene) {
        scene.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles() || db.hasUrl()) {
                    event.acceptTransferModes(TransferMode.ANY);
                }
                System.out.println("DRAG_OVER " + db.hasFiles());
                event.consume();
            }
        });

        scene.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                System.out.println("DRAG_DROPPED");
                Dragboard db = event.getDragboard();
                String url = null;
                if (db.hasFiles()) {
                    url = db.getFiles().get(0).toURI().toString();
                } else if (db.hasUrl()) {
                    url = db.getUrl();
                }
                event.setDropCompleted(url != null);
                event.consume();
            }
        });

    }
}
