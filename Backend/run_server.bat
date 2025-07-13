@echo off
echo ======================================
echo ...Starting Flask Attendance Server...
echo ======================================

REM Set working directory to current batch file location
cd /d %~dp0

python -m venv venv
call venv\Scripts\activate

REM Install required libraries
echo Installing dependencies from requirement.txt...
pip install -r requirement.txt

REM Start Flask server
echo Launching Flask server...
python backend\app.py

pause
