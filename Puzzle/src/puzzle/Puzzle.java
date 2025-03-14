package puzzle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author sarar
 */
public class Puzzle extends Application {
    
     @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/puzzle/view/FXMLPuzzle.fxml"));
        primaryStage.setTitle("Problema do 8-Puzzle");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}
