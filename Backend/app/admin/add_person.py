import cv2
import os
import time
import subprocess
from admin_auth import verify_admin

def create_dataset(name, save_path="Data", samples=2):
    if not os.path.exists(save_path):
        os.makedirs(save_path)

    person_dir = os.path.join(save_path, name)
    os.makedirs(person_dir, exist_ok=True)

    cap = cv2.VideoCapture(0)
    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
    
    count = 0
    print(f"[INFO] Capturing {samples} images for {name}. Look at the camera...")

    while count < samples:
        ret, frame = cap.read()
        if not ret:
            break

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(gray, 1.3, 5)

        for (x, y, w, h) in faces:
            face_img = gray[y:y+h, x:x+w]
            img_path = os.path.join(person_dir, f"{count}.jpg")
            cv2.imwrite(img_path, face_img)
            count += 1

            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            cv2.putText(frame, f"{count}/{samples}", (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 0, 0), 2)

        cv2.imshow("Capturing Face", frame)
        if cv2.waitKey(1) & 0xFF == ord("q"):
            break

    cap.release()
    cv2.destroyAllWindows()
    print(f"[DONE] Collected {count} face samples for {name}")

if __name__ == "__main__":
    if verify_admin():
        name = input("Enter name of person to add: ").strip()
        if name:
            create_dataset(name)
            subprocess.run(["python", "train_model.py"])
            print("Training model to update the model.")
