---
name: Feature Request
about: Propose a new feature or enhancement for MohanMart
title: "[FEAT] "
labels: enhancement
---

## Spec Reference
Which requirement (F1–F8, O1–O4, or Section) does this feature address?

## Proposed Solution
Describe the controller, service, DAO, database migration, and JSP/API changes.

## Acceptance Criteria
- [ ] All SQL queries use parameterized `PreparedStatement` in try-with-resources
- [ ] All JSP output escaped with `<c:out>`
- [ ] JUnit 5 + Mockito unit/integration tests added and green (`mvn -B clean verify`)
- [ ] `CHANGELOG.md` updated
