package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Book;
import models.Question;
import models.Quiz;
import services.AnswerService;
import services.BookService;
import services.QuizService;
import services.QuestionService;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class QuizController {

    @FXML
    private ListView<Quiz> quizList;
    @FXML
    private Button addQuizBtn;
    @FXML
    private TextField searchField;            // NEW
    @FXML
    private ComboBox<String> sortComboBox;
    private QuizService quizService = new QuizService();

    @FXML
    public void initialize() {
        // Setup sort combo
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Pinned First", "Title A-Z", "Title Z-A", "Newest", "Oldest"
        ));
        sortComboBox.getSelectionModel().selectFirst();

        // Add quiz button
        addQuizBtn.setOnAction(e -> openQuizForm(null));

        // Refresh whenever search text changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshQuizList());

        // Refresh whenever sort changes
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> refreshQuizList());

        refreshQuizList();
    }


    // ------------------- SUBMIT QUIZ -------------------
    @FXML
    private void submitQuiz(Quiz quiz) {
        // Use quiz.getCategory() as the keyword
        String topic = quiz.getCategory();

        BookService bookService = new BookService();
        List<Book> books = bookService.getBooksByCategory(topic);
        showBookRecommendation(books);
    }

    private int calculateScore() {
        return 7; // Replace with real logic
    }

    private void showBookRecommendation(List<Book> books) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BookRecommendation.fxml"));
            VBox root = loader.load();

            BookRecommendationController controller = loader.getController();
            controller.setBooks(books);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Book Recommendations");
            stage.show();

// Set the popup size here
            stage.setWidth(700);   // width
            stage.setHeight(500);  // height
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Failed to load book recommendation window.");
        }
    }

    // ------------------- REFRESH QUIZ LIST -------------------
    private void refreshQuizList() {
        List<Quiz> quizzes = new ArrayList<>(quizService.getAllQuizzes()); // mutable copy

        // --- FILTER BY SEARCH ---
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        if (!search.isEmpty()) {
            quizzes = new ArrayList<>(quizzes.stream()
                    .filter(q -> q.getTitle().toLowerCase().contains(search)
                            || q.getCategory().toLowerCase().contains(search))
                    .toList());
        }

        // --- SORT ---
        String sort = sortComboBox.getSelectionModel().getSelectedItem();
        if (sort != null) {
            switch (sort) {
                case "Pinned First" -> quizzes.sort((q1, q2) -> Boolean.compare(q2.isPinned(), q1.isPinned()));
                case "Title A-Z" -> quizzes.sort((q1, q2) -> q1.getTitle().compareToIgnoreCase(q2.getTitle()));
                case "Title Z-A" -> quizzes.sort((q1, q2) -> q2.getTitle().compareToIgnoreCase(q1.getTitle()));
                case "Newest" -> quizzes.sort((q1, q2) -> Integer.compare(q2.getId(), q1.getId()));
                case "Oldest" -> quizzes.sort((q1, q2) -> Integer.compare(q1.getId(), q2.getId()));
            }
        }

        quizList.setItems(FXCollections.observableArrayList(quizzes));

        quizList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Quiz quiz, boolean empty) {
                super.updateItem(quiz, empty);
                if (empty || quiz == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label titleLabel = new Label(
                            (quiz.isPinned() ? "📌 " : "") +
                                    quiz.getTitle() + " (" +
                                    quizService.getQuestionsByQuiz(quiz.getId()).size() + " questions)"
                    );
                    titleLabel.setMaxWidth(Double.MAX_VALUE);

                    Button editBtn = new Button("Edit");
                    Button manageBtn = new Button("Manage Questions");
                    Button takeBtn = new Button("Take Quiz");
                    Button deleteBtn = new Button("Delete");
                    Button pinBtn = new Button(quiz.isPinned() ? "Unpin" : "Pin");

                    // Pin toggle
                    pinBtn.setOnAction(e -> {
                        quiz.setPinned(!quiz.isPinned());
                        quizService.updateQuiz(quiz);
                        refreshQuizList();
                    });

                    editBtn.setOnAction(e -> openQuizForm(quiz));
                    manageBtn.setOnAction(e -> openQuestionManagement(quiz));
                    takeBtn.setOnAction(e -> takeQuiz(quiz));
                    deleteBtn.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setHeaderText("Delete this quiz?");
                        confirm.setContentText(quiz.getTitle());
                        confirm.showAndWait().ifPresent(resp -> {
                            if (resp == ButtonType.OK) {
                                quizService.deleteQuiz(quiz.getId());
                                refreshQuizList();
                            }
                        });
                    });

                    HBox hBox = new HBox(10, titleLabel, editBtn, manageBtn, takeBtn, pinBtn, deleteBtn);
                    HBox.setHgrow(titleLabel, Priority.ALWAYS);
                    setGraphic(hBox);
                }
            }
        });
    }

    // ------------------- QUESTION MANAGEMENT -------------------
    private void openQuestionManagement(Quiz quiz) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Questions for " + quiz.getTitle());
        VBox vbox = new VBox(10);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(600, 400);

        // Fix: use a Runnable array to refresh dynamically
        final Runnable[] refreshQuestions = new Runnable[1];

        refreshQuestions[0] = () -> {
            vbox.getChildren().clear();
            List<Question> questions = quizService.getQuestionsByQuiz(quiz.getId());

            for (Question q : questions) {
                Label qLabel = new Label(q.getQuestionText());
                Button editBtn = new Button("Edit");
                Button deleteBtn = new Button("Delete");

                editBtn.setOnAction(e -> editQuestionDialog(q, quiz));
                deleteBtn.setOnAction(e -> {
                    quizService.deleteQuestion(q.getId());
                    refreshQuestions[0].run();
                });

                HBox hBox = new HBox(10, qLabel, editBtn, deleteBtn);
                vbox.getChildren().add(hBox);
            }

            Button addNew = new Button("Add Question");
            addNew.setOnAction(e -> {
                addQuestionDialog(quiz);
                refreshQuestions[0].run();
            });
            vbox.getChildren().add(addNew);
        };

        refreshQuestions[0].run();
        dialog.showAndWait();
        refreshQuizList(); // update quiz list with new question counts
    }

    private void addQuestionDialog(Quiz quiz) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Add Question");

        TextField qText = new TextField();
        TextField a = new TextField();
        TextField b = new TextField();
        TextField c = new TextField();
        TextField d = new TextField();

        qText.setPromptText("Question Text");
        a.setPromptText("Option A");
        b.setPromptText("Option B");
        c.setPromptText("Option C");
        d.setPromptText("Option D");

        VBox box = new VBox(10,
                new Label("Question:"), qText,
                new Label("Option A:"), a,
                new Label("Option B:"), b,
                new Label("Option C:"), c,
                new Label("Option D:"), d
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(500, 400);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (qText.getText().trim().isEmpty() ||
                        a.getText().trim().isEmpty() ||
                        b.getText().trim().isEmpty() ||
                        c.getText().trim().isEmpty() ||
                        d.getText().trim().isEmpty()) {
                    showWarning("All fields must be filled!");
                    return null;
                }

                // Save to DB
                quizService.addQuestion(quiz.getId(),
                        qText.getText().trim(),
                        a.getText().trim(),
                        b.getText().trim(),
                        c.getText().trim(),
                        d.getText().trim());

                return null;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void editQuestionDialog(Question q, Quiz quiz) {
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle("Edit Question");

        TextField qText = new TextField(q.getQuestionText());
        TextField a = new TextField(q.getOptionA());
        TextField b = new TextField(q.getOptionB());
        TextField c = new TextField(q.getOptionC());
        TextField d = new TextField(q.getOptionD());

        VBox box = new VBox(10,
                new Label("Question:"), qText,
                new Label("Option A:"), a,
                new Label("Option B:"), b,
                new Label("Option C:"), c,
                new Label("Option D:"), d
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefSize(500, 400);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (qText.getText().trim().isEmpty() ||
                        a.getText().trim().isEmpty() ||
                        b.getText().trim().isEmpty() ||
                        c.getText().trim().isEmpty() ||
                        d.getText().trim().isEmpty()) {
                    showWarning("All fields must be filled!");
                    return null;
                }

                q.setQuestionText(qText.getText().trim());
                q.setOptionA(a.getText().trim());
                q.setOptionB(b.getText().trim());
                q.setOptionC(c.getText().trim());
                q.setOptionD(d.getText().trim());

                quizService.updateQuestion(q);
                return q;
            }
            return null;
        });

        dialog.showAndWait();
        openQuestionManagement(quiz);
    }

    // ------------------- TAKE QUIZ -------------------
    private void takeQuiz(Quiz quiz) {
        List<Question> questions = quizService.getQuestionsByQuiz(quiz.getId());
        AnswerService answerService = new AnswerService();

        for (Question q : questions) {
            ToggleGroup group = new ToggleGroup();
            RadioButton a = new RadioButton(q.getOptionA());
            RadioButton b = new RadioButton(q.getOptionB());
            RadioButton c = new RadioButton(q.getOptionC());
            RadioButton d = new RadioButton(q.getOptionD());

            a.setToggleGroup(group);
            b.setToggleGroup(group);
            c.setToggleGroup(group);
            d.setToggleGroup(group);

            VBox box = new VBox(10, new Label(q.getQuestionText()), a, b, c, d);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Answer Question");
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.getDialogPane().setPrefSize(500, 400);
            dialog.showAndWait();

            if (group.getSelectedToggle() == null) {
                showWarning("You must select an answer before continuing!");
                return;
            }

            String selected = ((RadioButton) group.getSelectedToggle()).getText();
            answerService.saveAnswer(quiz.getId(), q.getId(), selected);
        }

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setHeaderText("Answers Submitted");
        done.showAndWait();

        submitQuiz(quiz);
    }

    // ------------------- HELPERS -------------------
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(message);
        alert.showAndWait();
    }
    private void openQuizForm(Quiz quiz) {
        Dialog<Quiz> dialog = new Dialog<>();
        dialog.setTitle(quiz == null ? "Create Quiz" : "Edit Quiz");

        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");

        if (quiz != null) {
            titleField.setText(quiz.getTitle());
            categoryField.setText(quiz.getCategory());
            descArea.setText(quiz.getDescription());
        }

        VBox box = new VBox(10,
                new Label("Title:"), titleField,
                new Label("Category:"), categoryField,
                new Label("Description:"), descArea
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (titleField.getText().isBlank() || categoryField.getText().isBlank()) {
                    showWarning("Title and Category cannot be empty!");
                    return null;
                }

                if (quiz == null) {
                    return quizService.create(titleField.getText(), descArea.getText(), categoryField.getText(),false);
                } else {
                    quiz.setTitle(titleField.getText());
                    quiz.setCategory(categoryField.getText());
                    quiz.setDescription(descArea.getText());
                    quizService.updateQuiz(quiz);
                    return quiz;
                }
            }
            return null;
        });

        dialog.showAndWait();
        refreshQuizList();



    }
}