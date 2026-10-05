"""Дополнительные проверки JSON и диапазонов. Нужны демонстрационные данные."""

from smoke import Client

a = Client()
a.login("student1")
options = a.call("references")
assert options["coordinates"] and options["persons"], "Нужны справочные записи"
for body in [
    {"x": None, "y": 1},
    {"x": 1, "y": "NaN"},
    {"x": 1, "y": "Infinity"},
    {"x": 1, "y": 2.1},
]:
    a.call("references/coordinates", "POST", body, 400)
base = {
    "name": "edge",
    "coordinatesId": options["coordinates"][0]["id"],
    "budget": 1,
    "totalBoxOffice": 1,
    "mpaaRating": "PG",
    "directorId": options["persons"][0]["id"],
    "length": 1,
}
for key, val in [
    ("oscarsCount", 9223372036854775808),
    ("length", 1.5),
    ("budget", "NaN"),
    ("budget", "Infinity"),
    ("mpaaRating", "BAD"),
    ("directorId", 999999999),
    ("name", "   "),
]:
    a.call("movies", "POST", dict(base, **{key: val}), 400)
a.call("movies/999999999", expected=404)
print("PASS: 12 проверок ошибочного ввода и отсутствующего ID")
