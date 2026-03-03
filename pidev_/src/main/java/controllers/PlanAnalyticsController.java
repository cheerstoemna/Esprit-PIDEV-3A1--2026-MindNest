package controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import models.CoachingPlan;
import services.ExerciseProgressService;
import services.ExerciseService;
import utils.AppState;

import java.util.ArrayList;
import java.util.List;

public class PlanAnalyticsController {

    @FXML private Button backBtn;

    @FXML private Label titleLabel;
    @FXML private Label planNameLabel;
    @FXML private Label completionLabel;

    @FXML private Label emptyLabel;

    @FXML private PieChart pieChart;

    @FXML private Label nsCountLabel;
    @FXML private Label ipCountLabel;
    @FXML private Label coCountLabel;
    @FXML private Label skCountLabel;

    @FXML private Label nsPctLabel;
    @FXML private Label ipPctLabel;
    @FXML private Label coPctLabel;
    @FXML private Label skPctLabel;

    @FXML private Label hintLabel;

    private AnchorPane contentArea;
    private List<Node> previousContent;

    private CoachingPlan plan;
    private int userId;

    private final ExerciseProgressService progressService = new ExerciseProgressService();
    private final ExerciseService exerciseService = new ExerciseService();

    public void setContext(AnchorPane contentArea) { this.contentArea = contentArea; }
    public void setPreviousContent(List<Node> previousContent) { this.previousContent = previousContent; }

    public void setUserId(int userId) { this.userId = userId; }

    public void setPlan(CoachingPlan plan) {
        this.plan = plan;
        applyLanguage();
        refresh();
    }

    private void applyLanguage() {
        boolean fr = "fr".equals(AppState.getCoachingLang());

        if (titleLabel != null) titleLabel.setText(fr ? "Analytique" : "Analytics");
        if (backBtn != null) backBtn.setText(fr ? "← Retour" : "← Back");
        if (hintLabel != null) hintLabel.setText(fr
                ? "Astuce : modifiez les statuts des exercices pour voir l'analytique changer."
                : "Tip: Update exercise statuses to see analytics change."
        );
    }

    private void refresh() {
        if (plan == null) return;

        if (planNameLabel != null) planNameLabel.setText(plan.getTitle() == null ? "" : plan.getTitle());

        // if plan has no exercises, show empty state
        int totalExercises = exerciseService.getExercisesByPlan(plan.getPlanId()).size();
        if (totalExercises == 0) {
            if (emptyLabel != null) { emptyLabel.setVisible(true); emptyLabel.setManaged(true); }
            if (pieChart != null) { pieChart.setVisible(false); pieChart.setManaged(false); }
            if (completionLabel != null) completionLabel.setText(("fr".equals(AppState.getCoachingLang()) ? "Avancement: " : "Completion: ") + "0%");
            return;
        } else {
            if (emptyLabel != null) { emptyLabel.setVisible(false); emptyLabel.setManaged(false); }
            if (pieChart != null) { pieChart.setVisible(true); pieChart.setManaged(true); }
        }

        // breakdown: [ns, ip, co, sk]
        int[] b = progressService.getBreakdownForPlan(plan.getPlanId(), userId);
        int ns = b[0], ip = b[1], co = b[2], sk = b[3];
        int total = ns + ip + co + sk;
        if (total <= 0) total = totalExercises; // safety

        // completion %
        int pct = (int) Math.round((co * 100.0) / Math.max(1, total));
        boolean fr = "fr".equals(AppState.getCoachingLang());
        if (completionLabel != null) completionLabel.setText((fr ? "Avancement: " : "Completion: ") + pct + "%");

        // counts
        if (nsCountLabel != null) nsCountLabel.setText(String.valueOf(ns));
        if (ipCountLabel != null) ipCountLabel.setText(String.valueOf(ip));
        if (coCountLabel != null) coCountLabel.setText(String.valueOf(co));
        if (skCountLabel != null) skCountLabel.setText(String.valueOf(sk));

        // % labels
        if (nsPctLabel != null) nsPctLabel.setText(percent(ns, total));
        if (ipPctLabel != null) ipPctLabel.setText(percent(ip, total));
        if (coPctLabel != null) coPctLabel.setText(percent(co, total));
        if (skPctLabel != null) skPctLabel.setText(percent(sk, total));

        // pie chart
        if (pieChart != null) {
            pieChart.getData().clear();
            pieChart.setLabelsVisible(false); // cleaner; legend/grid gives details
            pieChart.getData().add(new PieChart.Data(fr ? "Pas commencé" : "Not Started", ns));
            pieChart.getData().add(new PieChart.Data(fr ? "En cours" : "In Progress", ip));
            pieChart.getData().add(new PieChart.Data(fr ? "Terminé" : "Completed", co));
            pieChart.getData().add(new PieChart.Data(fr ? "Ignoré" : "Skipped", sk));
        }
    }

    private String percent(int part, int total) {
        int pct = (int) Math.round((part * 100.0) / Math.max(1, total));
        return pct + "%";
    }

    @FXML
    private void goBack() {
        if (contentArea != null && previousContent != null) {
            contentArea.getChildren().setAll(previousContent);
        }
    }
}