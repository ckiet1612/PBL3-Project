package com.pbl3.project.pbl3_project;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final com.pbl3.project.pbl3_project.service.AuthService authService;

    public StageInitializer(com.pbl3.project.pbl3_project.service.AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        showLoginScene(stage);
        stage.setTitle("Sales Management System");
        stage.show();
    }

    private void showLoginScene(Stage stage) {
        // UI Layout
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF;"); // White Background

        // Title
        Label titleLabel = new Label("Sales Management System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        // Error Label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        // Input Fields
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-border-color: #CCCCCC; -fx-border-radius: 5; -fx-background-radius: 5;");
        usernameField.setMaxWidth(300);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-border-color: #CCCCCC; -fx-border-radius: 5; -fx-background-radius: 5;");
        passwordField.setMaxWidth(300);

        // Login Button
        Button loginButton = new Button("LOGIN");
        loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        loginButton.setMinWidth(300);
        loginButton.setPadding(new Insets(10));
        loginButton.setDefaultButton(true); // Enable Enter key triggering
        
        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            var user = authService.login(username, password);
            
            if (user != null) {
                showDashboardScene(stage, user);
            } else {
                errorLabel.setText("Invalid username or password!");
            }
        });

        root.getChildren().addAll(titleLabel, errorLabel, usernameField, passwordField, loginButton);

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
    }

    private void showDashboardScene(Stage stage, com.pbl3.project.pbl3_project.entity.User user) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label welcomeLabel = new Label("Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
        welcomeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Button logoutButton = new Button("LOGOUT");
        logoutButton.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutButton.setOnAction(e -> showLoginScene(stage));

        root.getChildren().addAll(welcomeLabel, logoutButton);
        
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}
