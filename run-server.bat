@echo off
setlocal
cd /d "%~dp0"
echo Starting MohanMart and H2 Console on port 8082...
"C:\tools\jdk17\jdk-17.0.12+7\bin\java.exe" @jvm_options.txt com.mohan.mohanmart.TomcatServer 8080
