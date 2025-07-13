import sqlite3
import os
from datetime import datetime

DB_PATH = os.path.join(os.path.dirname(__file__), "attendance.db")

def reset_db():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print("✅ Old attendance.db deleted")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS attendance (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            emp_id TEXT NOT NULL,
            name TEXT NOT NULL,
            date TEXT NOT NULL,
            entry_time TEXT,
            exit_time TEXT
        )
    """)
    conn.commit()
    conn.close()

def insert_attendance(emp_id, name, timestamp, mode):
    date = timestamp.split(" ")[0]
    time = timestamp.split(" ")[1]

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    # Check if record exists for the day
    c.execute("SELECT id FROM attendance WHERE emp_id=? AND date=?", (emp_id, date))
    row = c.fetchone()

    if mode == "entry":
        if row:
            # Check if entry_time already exists
            c.execute("SELECT entry_time FROM attendance WHERE id=?", (row[0],))
            existing_entry = c.fetchone()[0]
            if existing_entry:
                print("[INFO] Entry already marked.")
                conn.close()
                return "duplicate"
            else:
                c.execute("UPDATE attendance SET entry_time=? WHERE id=?", (time, row[0]))
                conn.commit()
                conn.close()
                return "inserted"
        else:
            # Insert entry time
            c.execute("INSERT INTO attendance (emp_id, name, date, entry_time) VALUES (?, ?, ?, ?)",
                      (emp_id, name, date, time))
            conn.commit()
            conn.close()
            return "inserted"

    elif mode == "exit":
        if row:
            # Check if exit_time already exists
            c.execute("SELECT exit_time FROM attendance WHERE id=?", (row[0],))
            existing_exit = c.fetchone()[0]
            if existing_exit:
                print("[INFO] Exit already marked.")
                conn.close()
                return "duplicate"
            else:
                c.execute("UPDATE attendance SET exit_time=? WHERE id=?", (time, row[0]))
                conn.commit()
                conn.close()
                return "inserted"
        else:
            # Exit before entry — still log
            c.execute("INSERT INTO attendance (emp_id, name, date, exit_time) VALUES (?, ?, ?, ?)",
                      (emp_id, name, date, time))
            conn.commit()
            conn.close()
            return "inserted"


def get_attendance_logs():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT * FROM attendance")
    rows = c.fetchall()
    conn.close()
    return rows

# Reset and test
reset_db()
#init_db()
#insert_attendance("001", "Abhishek Kardam", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

