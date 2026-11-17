# Suggested Repository Structure — Habit Tracker

```text
habit-tracker/
  backend/
    src/main/java/com/example/habits/
      config/
      controller/
      dto/
      domain/
      exception/
      repository/
      security/
      service/
    src/main/resources/db/migration/
    src/test/java/
    pom.xml
  frontend/
    src/
      app/
      components/
      features/
        auth/
        habits/
        calendar/
        analytics/
        settings/
      hooks/
      lib/api/
      styles/
      types/
    public/
    package.json
  docs/
    screenshots/
    diagrams/
  .github/workflows/
  .gitignore
  .env.example
  README.md
```

Keep frontend and backend independently runnable during development. Add shared generated API types only if generation is configured and maintained.
