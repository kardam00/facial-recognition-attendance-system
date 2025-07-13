import os
import cv2
import time
import numpy as np
import pandas as pd
from datetime import datetime


BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # trainer/
ROOT_DIR = os.path.abspath(os.path.join(BASE_DIR, '..', '..'))  # project_root/

dataset_path = os.path.join(ROOT_DIR, "Data")
model_path = os.path.join(BASE_DIR, "face_model.yml")
labels_path = os.path.join(BASE_DIR, "labels.npy")

# Load model and labels
recognizer = cv2.face.LBPHFaceRecognizer_create()
recognizer.read(model_path)
label_ids = np.load(labels_path, allow_pickle=True).item()
id_to_name = {v: k for k, v in label_ids.items()}

face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

attendance_log = set()

def mark_attendance(name):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    log_entry = {"Name": name, "Timestamp": timestamp}
    file_path = "attendance.csv"

    # Same day login
    try:
        if os.path.exists(file_path):
            df = pd.read_csv(file_path)
            df = pd.concat([df, pd.DataFrame([log_entry])], ignore_index=True)
        else:
            df = pd.DataFrame([log_entry])

        df.to_csv(file_path, index=False)
        print(f"[ATTENDANCE LOGGED] {name} at {timestamp}")
    except Exception as e:
        print(f"[ERROR] Could not log attendance: {e}")

# Open webcam
cap = cv2.VideoCapture(0)

start_time = time.time()
timeout = 10  # seconds
recognized = False

while True:
    ret, frame = cap.read()
    if not ret:
        break

    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=3)
    print("[DEBUG] Faces detected:", faces)

    for (x, y, w, h) in faces:
        roi_gray = gray[y:y+h, x:x+w]

        id_, confidence = recognizer.predict(roi_gray)
        print(f"[DEBUG] Predicted ID: {id_}, Confidence: {confidence}")
        name = id_to_name.get(id_, "Unknown")

        if confidence < 80:
            mark_attendance(name)
            recognized = True
        else:
            name = "Unknown"

        # Draw rectangle and label
        cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
        cv2.putText(frame, f"{name} {int(confidence)}", (x, y-10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)

    cv2.imshow("LBPH Face Recognition", frame)

    # Exit conditions
    if recognized or (time.time() - start_time > timeout):
        break

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
