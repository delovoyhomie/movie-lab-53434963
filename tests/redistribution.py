"""Проверка HTTP-переноса наград. Только тестовая база с ACTION/COMEDY.
Запуск: python3 tests/redistribution.py http://localhost:8080/movie-lab
"""

from smoke import Client

c = Client()
c.login("student1")
old = c.call("movies?size=100")["items"]
donors = [m for m in old if m["genre"] == "ACTION"]
targets = sorted([m for m in old if m["genre"] == "COMEDY"], key=lambda m: m["id"])
assert donors and targets, "Нужны фильмы жанров ACTION и COMEDY"
s = sum(m["oscarsCount"] or 0 for m in donors)
q, r = divmod(s, len(targets))
assert (
    c.call("special/redistribute", "POST", {"source": "ACTION", "target": "COMEDY"})["transferred"]
    == s
)
for index, m in enumerate(targets):
    assert (c.call("movies/" + str(m["id"]))["oscarsCount"] or 0) == (m["oscarsCount"] or 0) + q + (
        index < r
    )
for m in donors:
    assert c.call("movies/" + str(m["id"]))["oscarsCount"] is None
# Восстановить награды учебных данных после проверки.
for m in donors + targets:
    current = c.call("movies/" + str(m["id"]))
    body = {
        k: m[k]
        for k in (
            "name",
            "budget",
            "totalBoxOffice",
            "mpaaRating",
            "length",
            "oscarsCount",
            "goldenPalmCount",
            "tagline",
            "genre",
        )
    }
    body.update(version=current["version"])
    for role in ("coordinates", "director", "screenwriter", "operator"):
        body[role + "Id"] = m[role]["id"] if m[role] else None
    c.call("movies/" + str(m["id"]), "PUT", body)
print("PASS: перераспределение через HTTP и бизнес-сервис; исходные награды восстановлены")
