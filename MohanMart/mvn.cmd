@echo off
if exist "C:\tools\maven\apache-maven-3.9.7\bin\mvn.cmd" (
    "C:\tools\maven\apache-maven-3.9.7\bin\mvn.cmd" %*
) else if exist "D:\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" (
    "D:\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" %*
) else (
    mvn %*
)
