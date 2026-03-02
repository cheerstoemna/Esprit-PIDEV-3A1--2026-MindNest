Jdbc3A1 • Therapy Module (Client + Admin) — Ready to run

Run with Maven:
- Client:
  mvn clean javafx:run
- Admin:
  mvn clean javafx:run -Djavafx.args="admin"

Run in IntelliJ:
- Run test.MainFX (client) OR add program arg "admin" (admin)
- Alternative entry point: test.Launcher

DB:
- Uses your utils/MyDB connection settings.
- Needs tables: TherapySession, SessionFeedback.
