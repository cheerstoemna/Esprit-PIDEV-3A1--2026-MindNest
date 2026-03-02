package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import models.Question;
import models.Quiz;
import services.AnswerService;
import services.QuizService;

import java.util.List;

public class QuizController {

    @FXML
    private ListView<Quiz> quizList;
    @FXML
    private Button addQuizBtn;

    private QuizService quizService = new QuizService();

    @FXML
    public void initialize() {
        addQuizBtn.setOnAction(e -> openQuizForm(null));
        refreshQuizList();
    }

    // ------------------- REFRESH QUIZ LIST -------------------
    private void refreshQuizList() {
        List<Quiz> quizzes = quizService.getAllQuizzes();
        quizList.setItems(FXCollections.observableArrayList(quizzes));

        quizList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Quiz quiz, boolean empty) {
                super.updateItem(quiz, empty);
                if (empty || quiz == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label titleLabel = new Label(quiz.getTitle() + " (" + quizService.getQuestionsByQuiz(quiz.getId()).size() + " questions)");
                    titleLabel.setMaxWidth(Double.MAX_VALUE);

                    Button editBtn = new Button("Edit");
                    Button manageBtn = new Button("Manage Questions");
                    Button takeBtn = new Button("Take Quiz");
                    Button deleteBtn = new Button("Delete");

                    editBtn.setOnAction(e -> openQuizForm(quiz));
                    manageBtn.setOnAction(e -> openQuestionManagement(quiz));
                    takeBtn.setOnAction(e -> takeQuiz(quiz));
                    deleteBtn.setOnAction(e -> {
                        quizService.deleteQuiz(quiz.getId());
                        refreshQuizList();
                    });

                    HBox hBox = new HBox(10, titleLabel, editBtn, manageBtn, takeBtn, deleteBtn);
                    HBox.setHgrow(titleLabel, Priority.ALWAYS);
                    setGraphic(hBox);
                }
            }
        });
    }

    // ------------------- CREATE / EDIT QUIZ -------------------
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
                if (quiz == null) {
                    return quizService.create(titleField.getText(), descArea.getText(), categoryField.getText());
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

    // ------------------- QUESTION MANAGEMENT -------------------
    private void openQuestionManagement(Quiz quiz) {
        List<Question> questions = quizService.getQuestionsByQuiz(quiz.getId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Questions for " + quiz.getTitle());
        VBox vbox = new VBox(10);
        dialog.getDialogPane().setPrefSize(500, 400);  // width x height


        for (Question q : questions) {
            Label qLabel = new Label(q.getQuestionText());
            Button editBtn = new Button("Edit");
            Button deleteBtn = new Button("Delete");

            editBtn.setOnAction(e -> editQuestionDialog(q, quiz));
            deleteBtn.setOnAction(e -> {
                quizService.deleteQuestion(q.getId());
                dialog.close();
                openQuestionManagement(quiz);
            });

            HBox hBox = new HBox(10, qLabel, editBtn, deleteBtn);
            vbox.getChildren().add(hBox);
        }

        Button addNew = new Button("Add Question");
        addNew.setOnAction(e -> {
            addQuestionDialog(quiz);
            dialog.close();
            openQuestionManagement(quiz);
        });
        vbox.getChildren().add(addNew);

        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void addQuestionDialog(Quiz quiz) {
        TextInputDialog qDialog = new TextInputDialog();
        qDialog.setHeaderText("Enter Question Text");
        qDialog.showAndWait().ifPresent(questionText -> {
            TextInputDialog a = new TextInputDialog(); a.setHeaderText("Option A");
            TextInputDialog b = new TextInputDialog(); b.setHeaderText("Option B");
            TextInputDialog c = new TextInputDialog(); c.setHeaderText("Option C");
            TextInputDialog d = new TextInputDialog(); d.setHeaderText("Option D");

            String optA = a.showAndWait().orElse("");
            String optB = b.showAndWait().orElse("");
            String optC = c.showAndWait().orElse("");
            String optD = d.showAndWait().orElse("");

            quizService.addQuestion(quiz.getId(), questionText, optA, optB, optC, optD);
            refreshQuizList();
        });
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
        dialog.getDialogPane().setPrefSize(500, 400);  // width x height

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                q.setQuestionText(qText.getText());
                q.setOptionA(a.getText());
                q.setOptionB(b.getText());
                q.setOptionC(c.getText());
                q.setOptionD(d.getText());
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

            a.setToggleGroup(group); b.setToggleGroup(group);
            c.setToggleGroup(group); d.setToggleGroup(group);

            VBox box = new VBox(10, new Label(q.getQuestionText()), a, b, c, d);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Answer Question");
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.showAndWait();
            dialog.getDialogPane().setPrefSize(500, 400);  // width x height


            if (group.getSelectedToggle() != null) {
                String selected = ((RadioButton) group.getSelectedToggle()).getText();
                answerService.saveAnswer(quiz.getId(), q.getId(), selected);
            }
        }

        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.setHeaderText("Answers Submitted");
        done.showAndWait();
    }
}
