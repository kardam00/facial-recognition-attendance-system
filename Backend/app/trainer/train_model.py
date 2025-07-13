import cv2
import os
import numpy as np

def train_model():
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # trainer/
    dataset_path = os.path.abspath(os.path.join(BASE_DIR, '..', '..', 'Data'))
    model_save_path = os.path.join(BASE_DIR, "face_model.yml")
    labels_save_path = os.path.join(BASE_DIR, "labels.npy")

    print("[DEBUG] Dataset path:", dataset_path)
    if not os.path.exists(dataset_path):
        print("[ERROR] Dataset folder not found.")
        return
    print("[DEBUG] Subdirs found:", os.listdir(dataset_path))

    recognizer = cv2.face.LBPHFaceRecognizer_create()
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

    label_ids = {}
    x_train = []
    y_labels = []
    current_id = 0

    total_faces = 0

    for root, dirs, files in os.walk(dataset_path):
        for file in files:
            if file.endswith(('jpg', 'jpeg', 'png')):
                path = os.path.join(root, file)
                label = os.path.basename(root)

                if label not in label_ids:
                    label_ids[label] = current_id
                    current_id += 1

                id_ = label_ids[label]
                image = cv2.imread(path, cv2.IMREAD_GRAYSCALE)

                if image is None:
                    print(f"[SKIP] Could not read image: {path}")
                    continue

                print(f"[DEBUG] Reading {path}, shape: {image.shape}")
                faces = face_cascade.detectMultiScale(image, scaleFactor=1.05, minNeighbors=3)

                if len(faces) == 0:
                    print(f"[SKIP] No face in {path}")
                    continue

                for (x, y, w, h) in faces:
                    roi = image[y:y+h, x:x+w]
                    x_train.append(roi)
                    y_labels.append(id_)
                    total_faces += 1

    if not x_train:
        print("[ERROR] No valid face data. Training aborted.")

        if os.path.exists(model_save_path):
            os.remove(model_save_path)
        if os.path.exists(labels_save_path):
            os.remove(labels_save_path)
        return

    recognizer.train(x_train, np.array(y_labels))
    recognizer.save(model_save_path)
    np.save(labels_save_path, label_ids)

    print(f"[SUCCESS] Trained on {total_faces} faces, {len(label_ids)} persons.")

if __name__ == "__main__":
    train_model()
