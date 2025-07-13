import os
import shutil
import subprocess
from admin_auth import verify_admin

def remove_person(name, dataset_path="Data"):
    person_dir = os.path.join(dataset_path, name)
    if os.path.exists(person_dir):
        shutil.rmtree(person_dir)
        print(f"[INFO] Removed {name} from dataset.")
        subprocess.run(["python", "train_model.py"])
        print("Training model to update the model.")
    else:
        print("[ERROR] Person not found in dataset.")

if __name__ == "__main__":
    if verify_admin():
        name = input("Enter name of person to remove: ").strip()
        if name:
            remove_person(name)
