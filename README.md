# 👁️ Facial Recognition Attendance System

A fully functional attendance system powered by **facial recognition**, designed with a **Kotlin Android app frontend** and a **Python Flask backend**. This project enables secure, real-time attendance logging using face recognition, and includes admin features like adding/removing personnel and changing passcodes — all from the Android interface.

---

## 📱 Android App (Frontend)

### 🔧 Tech Stack
- **Language:** Kotlin
- **Camera:** CameraX API
- **Networking:** Retrofit (REST API)
- **Storage:** SharedPreferences
- **Design:** XML-based layouts

### 🔍 App Features

- 🔑 **Admin Login** using a local passcode
- ➕ **Add Person** (captures face and sends to backend)
- ➖ **Remove Person** from recognition database
- 📷 **Face Recognition** using device camera
- 📅 **Attendance Logging** on face match
- 🧾 **View Attendance Logs**
- ⚙️ **Settings** for:
  - Changing admin passcode
  - Updating server IP address

---

### 🚀 App Navigation

| Screen                     | Functionality                                              |
|---------------------------|------------------------------------------------------------|
| `LoginActivity`           | Enter admin passcode to access admin dashboard             |
| `AdminDashboardActivity`  | Navigate to Add, Remove, Recognize, View Logs, Settings    |
| `AddPersonActivity`       | Capture and send new face data to the server               |
| `RemovePersonActivity`    | Remove an existing person from database                    |
| `RecognitionActivity`     | Start live facial recognition                              |
| `AttendanceLogActivity`   | View stored attendance records                             |
| `SettingsActivity`        | Change passcode and server IP                              |
| `ChangeServerActivity`    | Change Flask server IP address                             |
| `ChangePasscodeActivity`  | Update the local admin passcode                            |

📁 All screens are located under:  
`Frontend/app/src/main/java/com/example/attendance/`

📱 UI Layouts:  
`Frontend/app/src/main/res/layout/`

---

## 🔌 Flask Server (Backend)

### 🔧 Tech Stack
- **Language:** Python 3.x
- **Framework:** Flask
- **Face Recognition:** OpenCV + LBPH
- **Database:** SQLite
- **Data Storage:** Local face image folder structure

---

### 🧠 Backend Features

- API endpoints for:
  - `/add_person`
  - `/remove_person`
  - `/recognize`
- Automatic model training on new person addition/removal
- Logs attendance with date/time and recognized ID
- Stores admin passcode securely (basic text for demo purposes)

---

### 🗂 Backend Structure

| Folder/File                | Description                                               |
|---------------------------|-----------------------------------------------------------|
| `app/admin/`              | Scripts for adding/removing persons, admin auth          |
| `app/trainer/`            | Model training and label encoding logic                  |
| `app/recognize.py`        | Facial recognition logic using camera input              |
| `backend/app.py`          | Flask server with API routes                             |
| `backend/database.py`     | SQLite DB functions for attendance                       |
| `backend/passcode.txt`    | Stores current admin passcode (changeable via app)       |
| `Data/`                   | Stores captured images in labeled folders                |
| `requirement.txt`         | Python dependencies                                      |
| `run_server.bat`          | Windows batch script to start server                     |

---

## ⚙️ Setup Instructions

### ✅ Backend Setup (Python)

The backend comes with an auto-setup script (run_server.bat) that creates a virtual environment, installs all dependencies, and launches the Flask server.

```bash
# In Command Prompt or PowerShell
cd Backend
run_server.bat
```
- This will:
  - Create a Python virtual environment (if not already present)
  - Install all required packages from requirement.txt
  - Launch the Flask server at:
  - http://localhost:5000/ (or your local IP for mobile access)

## 📱 Android App Setup (Kotlin)

1. Open `Frontend/` in Android Studio.
2. Set the server IP address:
   - Either hardcoded in `PrefsHelper.kt`, or
   - Through the app via **Settings > Change Server IP**.
3. Build and run the app on an Android device or emulator.
4. Ensure the Flask backend server is accessible from the Android device (same network or via public IP if remote).

---

## 🔐 Security Notes

- The `passcode.txt` file contains a dummy passcode by default.
- You can change the passcode via the app at **Settings > Change Passcode**.
- Facial data is stored **locally** in the `Data/` directory — **no cloud storage** is used.
- Attendance logs are stored securely in `attendance.db`.

---

## 📸 Example Flow

1. Admin logs in using the passcode.
2. Adds a person by entering a name and capturing their face (`AddPersonActivity`).
3. The backend saves the face data and retrains the model.
4. Later, the admin opens `RecognitionActivity`:
   - Camera detects a face
   - Backend matches it
   - Attendance is marked automatically

---

## 🧪 Testing

- You can test API endpoints using **Postman**, **Curl**, or directly from the app.
- Attendance logs:
  - Can be viewed in the app (`AttendanceLogActivity`)
  - Or manually accessed from `attendance.db` in the backend

---

## 📂 Project Structure Overview
```bash
facial-recognition-attendance-system/
├── Backend/
│   ├── app/                  # Recognition and training logic
│   ├── backend/              # Flask app and DB
│   ├── Data/                 # Captured face images (runtime)
│   ├── requirement.txt       # Python dependencies
│   └── run_server.bat        # Auto setup and server launcher
├── Frontend/
│   ├── app/                  # Android Kotlin code & UI layouts
│   ├── gradle/               # Gradle wrapper config
│   ├── gradlew, settings.gradle.kts, etc.
└── README.md                 # Project documentation
```

---

## 🙌 Credits

Developed by **Abhishek Kardam**  
📧 Contact: [kardamabhishek78@gmail.com]

