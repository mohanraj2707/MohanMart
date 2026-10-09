## Summary
Briefly describe the changes introduced in this Pull Request and the Capstone Spec section addressed.

## Verification & Quality Checklist
- [ ] `mvn -B clean verify` passes with 0 failures
- [ ] All SQL queries use `PreparedStatement` inside try-with-resources
- [ ] All JSP outputs are escaped via `<c:out>` / `fn:escapeXml`
- [ ] CSRF protection and `AuthFilter` RBAC verified on state-changing routes
- [ ] No secrets, API keys, or plaintext passwords committed
- [ ] `CHANGELOG.md` updated
