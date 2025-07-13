from flask import Flask, request, jsonify
from datetime import datetime
import base64
import os
import cv2
import numpy as np
import shutil
import subprocess
import threading
from database import init_db, insert_attendance, get_attendance_logs


app = Flask(__name__)
init_db()

# Config
BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # backend/
ROOT_DIR = os.path.abspath(os.path.join(BASE_DIR, '..'))  # project_root/

DATASET_PATH = os.path.join(ROOT_DIR, "Data")
TRAIN_SCRIPT_PATH = os.path.join(ROOT_DIR, "app", "trainer", "train_model.py")
MODEL_PATH = os.path.join(ROOT_DIR, "app", "trainer", "face_model.yml")
LABELS_PATH = os.path.join(ROOT_DIR, "app", "trainer", "labels.npy")

PASSCODE_FILE = os.path.join(BASE_DIR, "passcode.txt")

def load_passcode():
    if os.path.exists(PASSCODE_FILE):
        with open(PASSCODE_FILE, "r") as f:
            return f.read().strip()
    return "admin123"

def save_passcode(new_pass):
    with open(PASSCODE_FILE, "w") as f:
        f.write(new_pass.strip())

CURRENT_PASS = load_passcode()
 

# Load model and label mappings
def load_model():
    if not os.path.exists(MODEL_PATH) or not os.path.exists(LABELS_PATH):
        raise FileNotFoundError("Trained model or labels not found. Please add at least one person.")

    recognizer = cv2.face.LBPHFaceRecognizer_create()
    print("[DEBUG] Loading model from:", MODEL_PATH)
    recognizer.read(MODEL_PATH)
    label_ids = np.load(LABELS_PATH, allow_pickle=True).item()
    print("[DEBUG] Loaded labels:", label_ids)
    id_to_name = {v: k for k, v in label_ids.items()}
    return recognizer, id_to_name

def save_face_image(name, face_img):
    person_dir = os.path.join(DATASET_PATH, name)
    if not os.path.exists(person_dir):
        print(f"[INFO] Skipping image save: directory for {name} not found.")
        return

    # Limit max images per person
    MAX_IMAGES = 20
    existing_images = sorted(
        [f for f in os.listdir(person_dir) if f.endswith(('.jpg', '.jpeg', '.png'))]
    )

    # Delete oldest image if limit exceeded
    if len(existing_images) >= MAX_IMAGES:
        oldest = sorted(existing_images)[0]
        os.remove(os.path.join(person_dir, oldest))

    # Save the new face image
    new_index = len(os.listdir(person_dir))
    img_path = os.path.join(person_dir, f"{new_index}.jpg")
    cv2.imwrite(img_path, face_img)
    print(f"[INFO] Saved new image to {img_path}")

# Log attendance
def mark_attendance(name, mode):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if "[" in name and "]" in name:
        emp_id = name.split("[")[-1].split("]")[0].strip()
        person_name = name.split("[")[0].strip()
    else:
        emp_id = "N/A"
        person_name = name

    try:
        status = insert_attendance(emp_id, person_name, timestamp, mode)
        if status == "duplicate":
            return "duplicate"
        print(f"[ATTENDANCE LOGGED] {person_name} ({emp_id}) {mode} at {timestamp}")
        return timestamp
    except Exception as e:
        print(f"[ERROR] Failed to insert into DB: {e}")
        return None

def retrain_model_in_background():
    def run():
        print("[INFO] Starting background model retraining...")
        result = subprocess.run(["python", TRAIN_SCRIPT_PATH], capture_output=True, text=True)
        print("[TRAIN OUTPUT]", result.stdout)
        print("[TRAIN ERRORS]", result.stderr)
        print("[INFO] Model retraining finished.")

    threading.Thread(target=run).start()


@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    if data.get("passcode") == load_passcode():
        return jsonify({"success": True, "message": "Login successful"})
    return jsonify({"success": False, "message": "Invalid passcode"}), 401

@app.route("/attendance", methods=["GET"])
def get_attendance():
    try:
        records = get_attendance_logs()
        result = [
            {
                "id": row[0],
                "emp_id": row[1],
                "name": row[2],
                "date": row[3],
                "entry_time": row[4],
                "exit_time": row[5]
            }
            for row in records
        ]
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/recognize", methods=["POST"])
def recognize_face():
    try:
        img_data = request.json.get("image_base64")
        mode = request.json.get("mode", "entry")
        if not img_data:
            return jsonify({"error": "Missing image_base64"}), 400

        # Decode base64 image
        img_bytes = base64.b64decode(img_data)
        nparr = np.frombuffer(img_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            return jsonify({"error": "Failed to decode image"}), 400

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=3)

        if len(faces) == 0:
            print("[INFO] No faces detected")
            return jsonify({"name": "Unknown", "timestamp": None})

        try:
            recognizer, id_to_name = load_model()
        except FileNotFoundError as e:
            print(f"[MODEL ERROR] {e}")
            return jsonify({"error": str(e)}), 500


        for (x, y, w, h) in faces:
            roi_gray = gray[y:y+h, x:x+w]
            roi_color = frame[y:y+h, x:x+w]
            try:
                id_, confidence = recognizer.predict(roi_gray)
                print(f"[DEBUG] Prediction: ID={id_}, Confidence={confidence}")
                name = id_to_name.get(id_, "Unknown")
            except cv2.error as e:
                print(f"[ERROR] Prediction error: {e}")
                return jsonify({"error": "Face recognition error"}), 500

            if confidence > 50 and name != "Unknown":
                save_face_image(name, roi_color)
                timestamp = mark_attendance(name, mode)
                if timestamp == "duplicate":
                    return jsonify({"name": name, "timestamp": None, "status": "duplicate"})
                return jsonify({"name": name, "timestamp": timestamp, "status": "ok"})
        return jsonify({"name": "Unknown", "timestamp": None, "status": "low_confidence"})

    except Exception as e:
        print(f"[SERVER ERROR] {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/check_person_exists", methods=["POST"])
def check_person_exists():
    data = request.get_json()
    if data.get("passcode") != load_passcode():
        return jsonify({"error": "Unauthorized"}), 401

    emp_id = data.get("emp_id")
    if not emp_id:
        return jsonify({"error": "Missing emp_id"}), 400

    # Look for any folder ending with [emp_id]
    for dirname in os.listdir(DATASET_PATH):
        if f"[{emp_id}]" in dirname:
            return jsonify({"exists": True})
    return jsonify({"exists": False})


@app.route("/add_person", methods=["POST"])
def add_person():
    passcode = request.form.get("passcode")
    if passcode != load_passcode():
        return jsonify({"error": "Unauthorized"}), 401

    name = request.form.get("name")
    if not name:
        return jsonify({"error": "Missing name"}), 400

    files = request.files.getlist("images")
    if not files:
        return jsonify({"error": "No images provided"}), 400

    person_dir = os.path.join(DATASET_PATH, name)
    if os.path.exists(person_dir):
        return jsonify({
            "error": f"Person '{name}' already exists. Use a different ID or remove the person first."
        }), 409  # Conflict

    os.makedirs(person_dir)

    for idx, file in enumerate(files):
        file.save(os.path.join(person_dir, f"{idx}.jpg"))

    retrain_model_in_background()
    return jsonify({"message": f"{name} added and model retrained"})

@app.route("/remove_person", methods=["POST"])
def remove_person():
    data = request.get_json()
    if data.get("passcode") != load_passcode():
        return jsonify({"error": "Unauthorized"}), 401

    name = data.get("name")
    if not name:
        return jsonify({"error": "Missing name"}), 400

    person_dir = os.path.join(DATASET_PATH, name)
    if os.path.exists(person_dir):
        shutil.rmtree(person_dir)
        retrain_model_in_background()
        return jsonify({"message": f"{name} removed and model retrained"})
    
    return jsonify({"error": f"Person '{name}' not found in database"}), 404

@app.route("/change_passcode", methods=["POST"])
def change_passcode():
    data = request.get_json()
    old_pass = data.get("old_passcode")
    new_pass = data.get("new_passcode")

    global CURRENT_PASS
    if old_pass != CURRENT_PASS:
        return jsonify({"success": False, "message": "Old passcode incorrect"}), 401

    if not new_pass or len(new_pass.strip()) < 4:
        return jsonify({"success": False, "message": "New passcode must be at least 4 characters"}), 400

    CURRENT_PASS = new_pass.strip()
    save_passcode(CURRENT_PASS)
    return jsonify({"success": True, "message": "Passcode changed successfully"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
