"""Печатает SQL для добавления пользователя. Пароль вводится скрыто, не записывается в файл."""

import base64, getpass, hashlib, secrets

login = input("Логин: ").strip()
if not login:
    raise SystemExit("Пустой логин")
password = getpass.getpass("Пароль: ")
if len(password) < 8:
    raise SystemExit("Нужно минимум 8 символов")
salt = secrets.token_bytes(16)
hash_value = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 120000)
print(
    "INSERT INTO app_users(login,salt,password_hash) VALUES ('%s','%s','%s');"
    % (
        login.replace("'", "''"),
        base64.b64encode(salt).decode(),
        base64.b64encode(hash_value).decode(),
    )
)
