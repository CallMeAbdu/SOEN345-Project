#!/usr/bin/env python3
"""
Local booking-confirmation mail relay for Android emulator demos.

Runs on host (default 127.0.0.1:8080) and accepts:
POST /api/send-booking-confirmation
Body JSON: {"to": "...", "subject": "...", "text": "..."}

Sends mail through Gmail SMTP using EMAIL_USER / EMAIL_APP_PASS from .env.
"""

import json
import os
import re
import smtplib
import ssl
from email.message import EmailMessage
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


EMAIL_PATTERN = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = value


def send_gmail(email_user: str, email_app_pass: str, to: str, subject: str, text: str) -> None:
    msg = EmailMessage()
    msg["From"] = email_user
    msg["To"] = to
    msg["Subject"] = subject
    msg.set_content(text)

    context = ssl.create_default_context()
    with smtplib.SMTP("smtp.gmail.com", 587, timeout=15) as smtp:
        smtp.ehlo()
        smtp.starttls(context=context)
        smtp.ehlo()
        smtp.login(email_user, email_app_pass)
        smtp.send_message(msg)


class RelayHandler(BaseHTTPRequestHandler):
    server_version = "BookingMailRelay/1.0"

    def do_POST(self) -> None:
        if self.path != "/api/send-booking-confirmation":
            self._json(404, {"error": "not_found"})
            return

        email_user = os.environ.get("EMAIL_USER", "").strip()
        email_app_pass = os.environ.get("EMAIL_APP_PASS", "").strip()
        if not email_user or not email_app_pass:
            self._json(500, {"error": "missing EMAIL_USER or EMAIL_APP_PASS"})
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(content_length)
        try:
            payload = json.loads(raw.decode("utf-8"))
        except Exception:
            self._json(400, {"error": "invalid_json"})
            return

        to = str(payload.get("to", "")).strip()
        subject = str(payload.get("subject", "")).strip()
        text = str(payload.get("text", "")).strip()
        if not EMAIL_PATTERN.match(to):
            self._json(400, {"error": "invalid_recipient"})
            return
        if not subject:
            self._json(400, {"error": "missing_subject"})
            return
        if not text:
            self._json(400, {"error": "missing_text"})
            return

        try:
            send_gmail(email_user, email_app_pass, to, subject, text)
            self._json(200, {"status": "sent"})
        except Exception as e:
            self._json(502, {"error": "smtp_failed", "detail": str(e)})

    def do_GET(self) -> None:
        if self.path == "/health":
            self._json(200, {"status": "ok"})
            return
        self._json(404, {"error": "not_found"})

    def log_message(self, fmt: str, *args) -> None:
        # Keep logs concise and parseable in terminal.
        print("[mail-relay] " + (fmt % args))

    def _json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    repo_root = Path(__file__).resolve().parents[1]
    load_env_file(repo_root / ".env")

    host = os.environ.get("MAIL_RELAY_HOST", "127.0.0.1").strip() or "127.0.0.1"
    port_raw = os.environ.get("MAIL_RELAY_PORT", "8080").strip() or "8080"
    port = int(port_raw)

    print(f"[mail-relay] starting on http://{host}:{port}")
    print("[mail-relay] endpoint: POST /api/send-booking-confirmation")
    server = ThreadingHTTPServer((host, port), RelayHandler)
    server.serve_forever()


if __name__ == "__main__":
    main()
