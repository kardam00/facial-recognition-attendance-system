# admin_auth.py

def verify_admin():
    correct_passcode = "admin123"
    input_passcode = input("Enter admin passcode: ")
    if input_passcode == correct_passcode:
        print("[AUTH] Access granted.")
        return True
    else:
        print("[AUTH] Access denied.")
        return False
