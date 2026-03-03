package controllers;

import db.DBConnection;
import models.UserRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UserDashboardController implements Initializable {

    // ── Header ────────────────────────────────────────────────────────
    @FXML private Label subtitleLabel;

    // ── Stat cards ────────────────────────────────────────────────────
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAdminsLabel;
    @FXML private Label totalTherapistsLabel;
    @FXML private Label totalPatientsLabel;

    // ── Charts ────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  roleBarChart;
    @FXML private PieChart                  genderPieChart;
    @FXML private LineChart<String, Number> registrationLineChart;
    @FXML private BarChart<String, Number>  ageBarChart;

    // ── Table ─────────────────────────────────────────────────────────
    @FXML private TableView<UserRow>         usersTable;
    @FXML private TableColumn<UserRow, Integer> colId;
    @FXML private TableColumn<UserRow, String>  colName;
    @FXML private TableColumn<UserRow, String>  colEmail;
    @FXML private TableColumn<UserRow, String>  colRole;
    @FXML private TableColumn<UserRow, String>  colGender;
    @FXML private TableColumn<UserRow, Integer> colAge;
    @FXML private TableColumn<UserRow, String>  colStatus;

    // ── Filters ───────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label            tableFooterLabel;

    // ── Internal data ─────────────────────────────────────────────────
    private ObservableList<UserRow> allUsers = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        loadAllData();
    }

    // ─────────────────────────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────────────────────────

    private void loadAllData() {
        allUsers.clear();

        // Counters for stat cards
        int[] counts = {0, 0, 0, 0}; // total, admin, therapist, patient

        // Maps for charts
        Map<String, Integer> roleMap    = new LinkedHashMap<>();
        Map<String, Integer> genderMap  = new LinkedHashMap<>();
        Map<String, Integer> monthMap   = new LinkedHashMap<>();
        Map<String, Integer> ageMap     = new LinkedHashMap<>();

        roleMap.put("ADMIN", 0);
        roleMap.put("THERAPIST", 0);
        roleMap.put("PATIENT", 0);

        String sql = "SELECT id, name, email, role, gender, age, status, created_at FROM users ORDER BY id ASC";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String role   = rs.getString("role")   != null ? rs.getString("role").toUpperCase()   : "PATIENT";
                String gender = rs.getString("gender") != null ? rs.getString("gender").toUpperCase() : "OTHER";
                int    age    = rs.getInt("age");
                int    status = rs.getInt("status");

                // Build row
                allUsers.add(new UserRow(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    role,
                    rs.getString("gender"),
                    age,
                    status
                ));

                // Stat card counts
                counts[0]++;
                if ("ADMIN".equals(role))     counts[1]++;
                if ("THERAPIST".equals(role)) counts[2]++;
                if ("PATIENT".equals(role))   counts[3]++;

                // Role chart
                roleMap.merge(role, 1, Integer::sum);

                // Gender chart
                genderMap.merge(gender, 1, Integer::sum);

                // Registration month chart
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    String month = ts.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("MMM yyyy"));
                    monthMap.merge(month, 1, Integer::sum);
                }

                // Age group chart
                String ageGroup = getAgeGroup(age);
                ageMap.merge(ageGroup, 1, Integer::sum);
            }

        } catch (Exception e) {
            // created_at column might not exist — retry without it
            loadAllDataFallback(counts, roleMap, genderMap, ageMap);
        }

        // Update UI
        updateStatCards(counts);
        buildRoleChart(roleMap);
        buildGenderChart(genderMap);
        buildRegistrationChart(monthMap);
        buildAgeChart(ageMap);
        updateTableFooter();

        subtitleLabel.setText("Last updated: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
    }

    /** Fallback if created_at column doesn't exist */
    private void loadAllDataFallback(int[] counts, Map<String,Integer> roleMap,
                                     Map<String,Integer> genderMap, Map<String,Integer> ageMap) {
        allUsers.clear();
        String sql = "SELECT id, name, email, role, gender, age, status FROM users ORDER BY id ASC";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String role   = rs.getString("role")   != null ? rs.getString("role").toUpperCase()   : "PATIENT";
                String gender = rs.getString("gender") != null ? rs.getString("gender").toUpperCase() : "OTHER";
                int    age    = rs.getInt("age");
                int    status = rs.getInt("status");

                allUsers.add(new UserRow(
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("email"), role,
                    rs.getString("gender"), age, status
                ));

                counts[0]++;
                if ("ADMIN".equals(role))     counts[1]++;
                if ("THERAPIST".equals(role)) counts[2]++;
                if ("PATIENT".equals(role))   counts[3]++;

                roleMap.merge(role, 1, Integer::sum);
                genderMap.merge(gender, 1, Integer::sum);
                ageMap.merge(getAgeGroup(age), 1, Integer::sum);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        buildRegistrationChart(new LinkedHashMap<>()); // empty — no date data
    }

    // ─────────────────────────────────────────────────────────────────
    // CHART BUILDERS
    // ─────────────────────────────────────────────────────────────────

    private void buildRoleChart(Map<String, Integer> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Users");
        data.forEach((role, count) ->
            series.getData().add(new XYChart.Data<>(role, count)));
        roleBarChart.getData().setAll(series);
        roleBarChart.setLegendVisible(false);
    }

    private void buildGenderChart(Map<String, Integer> data) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        data.forEach((gender, count) ->
            pieData.add(new PieChart.Data(gender + " (" + count + ")", count)));
        genderPieChart.setData(pieData);
    }

    private void buildRegistrationChart(Map<String, Integer> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Registrations");
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No date data", 0));
        } else {
            data.forEach((month, count) ->
                series.getData().add(new XYChart.Data<>(month, count)));
        }
        registrationLineChart.getData().setAll(series);
        registrationLineChart.setLegendVisible(false);
    }

    private void buildAgeChart(Map<String, Integer> data) {
        // Sort age groups in natural order
        List<String> orderedKeys = Arrays.asList(
            "Under 18", "18-25", "26-35", "36-45", "46-60", "60+"
        );
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Users");
        orderedKeys.forEach(key -> {
            if (data.containsKey(key))
                series.getData().add(new XYChart.Data<>(key, data.get(key)));
        });
        ageBarChart.getData().setAll(series);
        ageBarChart.setLegendVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────
    // TABLE SETUP
    // ─────────────────────────────────────────────────────────────────

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Color-code status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    setStyle("Active".equals(item)
                        ? "-fx-text-fill: #2ecc71; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });

        // Color-code role column
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    setStyle(switch (item) {
                        case "ADMIN"     -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                        case "THERAPIST" -> "-fx-text-fill: #3498db; -fx-font-weight: bold;";
                        default          -> "-fx-text-fill: #2ecc71; -fx-font-weight: bold;";
                    });
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // FILTER & SORT SETUP
    // ─────────────────────────────────────────────────────────────────

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList(
            "All Roles", "ADMIN", "THERAPIST", "PATIENT"
        ));
        roleFilter.setValue("All Roles");

        sortCombo.setItems(FXCollections.observableArrayList(
            "Name A→Z", "Name Z→A", "Age ↑", "Age ↓", "Role"
        ));

        // Wire up listeners — all trigger applyFilters()
        searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
        roleFilter.valueProperty().addListener((o, ov, nv) -> applyFilters());
        sortCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void applyFilters() {
        String search  = searchField.getText().trim().toLowerCase();
        String role    = roleFilter.getValue();
        String sort    = sortCombo.getValue();

        FilteredList<UserRow> filtered = new FilteredList<>(allUsers, row -> {
            // Search filter
            boolean matchSearch = search.isEmpty()
                || row.getName().toLowerCase().contains(search)
                || row.getEmail().toLowerCase().contains(search);

            // Role filter
            boolean matchRole = "All Roles".equals(role) || role == null
                || row.getRole().equalsIgnoreCase(role);

            return matchSearch && matchRole;
        });

        SortedList<UserRow> sorted = new SortedList<>(filtered);

        if (sort != null) {
            sorted.setComparator(switch (sort) {
                case "Name A→Z" -> Comparator.comparing(UserRow::getName);
                case "Name Z→A" -> Comparator.comparing(UserRow::getName).reversed();
                case "Age ↑"    -> Comparator.comparingInt(UserRow::getAge);
                case "Age ↓"    -> Comparator.comparingInt(UserRow::getAge).reversed();
                case "Role"     -> Comparator.comparing(UserRow::getRole);
                default         -> Comparator.comparingInt(UserRow::getId);
            });
        }

        usersTable.setItems(sorted);
        updateTableFooter();
    }

    private void updateTableFooter() {
        int showing = usersTable.getItems() != null ? usersTable.getItems().size() : 0;
        int total   = allUsers.size();
        tableFooterLabel.setText("Showing " + showing + " of " + total + " users");
    }

    // ─────────────────────────────────────────────────────────────────
    // STAT CARDS
    // ─────────────────────────────────────────────────────────────────

    private void updateStatCards(int[] counts) {
        totalUsersLabel.setText(String.valueOf(counts[0]));
        totalAdminsLabel.setText(String.valueOf(counts[1]));
        totalTherapistsLabel.setText(String.valueOf(counts[2]));
        totalPatientsLabel.setText(String.valueOf(counts[3]));
    }

    // ─────────────────────────────────────────────────────────────────
    // CSV EXPORT
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleExportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Users CSV");
        chooser.setInitialFileName("mindnest_users_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showSaveDialog(usersTable.getScene().getWindow());
        if (file == null) return;

        // Export the currently visible (filtered/sorted) rows
        ObservableList<UserRow> rows = usersTable.getItems() != null
            ? FXCollections.observableArrayList(usersTable.getItems())
            : allUsers;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header
            pw.println("ID,Name,Email,Role,Gender,Age,Status");

            // Rows
            for (UserRow row : rows) {
                pw.printf("%d,\"%s\",\"%s\",%s,%s,%d,%s%n",
                    row.getId(),
                    row.getName().replace("\"", "\"\""),
                    row.getEmail().replace("\"", "\"\""),
                    row.getRole(),
                    row.getGender(),
                    row.getAge(),
                    row.getStatus()
                );
            }

            // Show success alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("✔ Exported " + rows.size() + " users to:\n" + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                "Failed to export: " + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────

    private String getAgeGroup(int age) {
        if (age < 18)        return "Under 18";
        else if (age <= 25)  return "18-25";
        else if (age <= 35)  return "26-35";
        else if (age <= 45)  return "36-45";
        else if (age <= 60)  return "46-60";
        else                 return "60+";
    }
}
