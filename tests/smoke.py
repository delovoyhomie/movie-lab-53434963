#!/usr/bin/env python3
"""Интеграционный тест запущенного сервера. Только отдельная тестовая база!
python3 tests/smoke.py http://localhost:8080/movie-lab
Использует двух демонстрационных пользователей; создаёт/удаляет тестовые данные.
"""

import json, sys, urllib.request, urllib.error, http.cookiejar, uuid

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/movie-lab").rstrip(
    "/"
) + "/api/"
checks = 0


class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )
        self.token = None

    def call(self, path, method="GET", body=None, expected=200):
        global checks
        headers = {"Content-Type": "application/json"}
        if self.token:
            headers["X-CSRF-Token"] = self.token
        req = urllib.request.Request(
            BASE + path,
            data=None if body is None else json.dumps(body).encode(),
            headers=headers,
            method=method,
        )
        try:
            with self.opener.open(req, timeout=15) as res:
                code = res.status
                raw = res.read()
        except urllib.error.HTTPError as e:
            code = e.code
            raw = e.read()
        try:
            value = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            raise AssertionError((path, code, raw.decode()[:1500]))
        assert code == expected, (path, code, value, expected)
        checks += 1
        return value

    def login(self, name):
        self.token = self.call("auth/login", "POST", {"login": name, "password": "student123"})[
            "csrf"
        ]


def main():
    a, b = Client(), Client()
    a.call("movies", expected=401)
    a.login("student1")
    b.login("student2")
    # Проверка CSRF
    saved = a.token
    a.token = "wrong"
    a.call("movies", "POST", {}, 403)
    a.token = saved
    a.call("references")
    for kind in ("persons", "locations", "coordinates"):
        a.call("references/" + kind)
    prefix = "TEST-" + uuid.uuid4().hex[:8]
    revision = b.call("movies/revision")["revision"]
    location = a.call("references/locations", "POST", {"x": 1, "y": 2, "z": 3, "name": prefix})
    person = a.call(
        "references/persons",
        "POST",
        {
            "name": prefix,
            "eyeColor": "RED",
            "hairColor": "WHITE",
            "height": 180,
            "locationId": location["id"],
        },
    )
    coord = a.call("references/coordinates", "POST", {"x": 4, "y": 2})
    coord = a.call(
        "references/coordinates/" + str(coord["id"]),
        "PUT",
        {"x": 5, "y": 1, "version": coord["version"]},
    )
    a.call("references/coordinates/" + str(coord["id"]), "PUT", {"x": 6, "y": 1, "version": 0}, 409)
    base = {
        "name": prefix,
        "coordinatesId": coord["id"],
        "budget": 100,
        "totalBoxOffice": 200,
        "mpaaRating": "PG",
        "directorId": person["id"],
        "screenwriterId": person["id"],
        "length": 100,
        "genre": "ACTION",
        "tagline": prefix,
    }
    m = a.call("movies", "POST", dict(base, oscarsCount=5))
    assert m["id"] > 0 and m["creationDate"]
    assert m["director"]["location"]["name"] == prefix
    large = a.call(
        "movies", "POST", dict(base, name=prefix + " large", oscarsCount=9223372036854775807)
    )
    assert large["oscarsCount"] == 9223372036854775807
    a.call(
        "movies/" + str(large["id"]) + "?version=" + str(large["version"]), "DELETE", expected=204
    )
    assert b.call("movies/revision")["revision"] > revision
    assert b.call("movies/" + str(m["id"]))["name"] == prefix
    updated = a.call(
        "movies/" + str(m["id"]),
        "PUT",
        dict(base, version=m["version"], name=prefix + " updated", oscarsCount=5),
    )
    a.call("movies/" + str(m["id"]), "PUT", dict(base, version=m["version"]), 409)
    for field, value in [
        ("budget", 0),
        ("totalBoxOffice", -1),
        ("length", 0),
        ("name", "  "),
        ("mpaaRating", None),
        ("oscarsCount", 0),
        ("directorId", None),
        ("coordinatesId", None),
        ("goldenPalmCount", -1),
    ]:
        a.call("movies", "POST", dict(base, **{field: value}), 400)
    a.call("references/coordinates", "POST", {"x": 1, "y": 2.1}, 400)
    a.call("references/locations", "POST", {"x": 1, "y": 2.5, "z": 1, "name": ""}, 400)
    a.call(
        "references/persons",
        "POST",
        {"name": "", "eyeColor": "RED", "hairColor": "WHITE", "height": 1},
        400,
    )
    a.call("movies?sort=wrong", expected=400)
    # Все строковые колонки реально допускают поиск и сортировку, включая nullable связи.
    for column in (
        "name",
        "tagline",
        "genre",
        "mpaaRating",
        "director",
        "screenwriter",
        "operator",
    ):
        a.call("movies?column=" + column + "&sort=" + column + "&q=a&size=2")
    page = a.call("movies?size=2")
    assert len(page["items"]) <= 2
    from urllib.parse import quote

    found = a.call("movies?column=name&q=" + quote(prefix.lower()))
    assert found["total"] == 1
    assert len(a.call("special/tagline?text=" + quote(prefix))) == 1
    less = a.call("special/genre-less?genre=COMEDY")
    assert any(x["id"] == m["id"] for x in less)
    a.call("special/redistribute", "POST", {"source": "ACTION", "target": "ACTION"}, 400)
    # Возврат пустого результата при буквальном %, а не SQL wildcard.
    assert not a.call("special/tagline?text=" + quote(prefix + "%"))
    result = a.call("special/delete-tagline", "POST", {"text": prefix})
    assert result["id"] == m["id"]
    a.call("movies/" + str(m["id"]), expected=404)
    assert not a.call("special/delete-tagline", "POST", {"text": prefix})["deleted"]
    # Сценарист с фильмами без наград включён, без единого фильма не включён.
    a.call("movies", "POST", base)
    assert any(p["id"] == person["id"] for p in a.call("special/writers"))
    # Проверяем каскад location -> person -> movies.
    a.call(
        "references/locations/" + str(location["id"]) + "?version=" + str(location["version"]),
        "DELETE",
        expected=204,
    )
    a.call("references/persons/" + str(person["id"]), expected=404)
    assert a.call("movies?column=name&q=" + quote(prefix))["total"] == 0
    a.call(
        "references/coordinates/" + str(coord["id"]) + "?version=" + str(coord["version"]),
        "DELETE",
        expected=204,
    )
    a.call("auth/logout", "POST", {}, 204)
    a.call("movies", expected=401)
    print(
        "PASS:",
        checks,
        "HTTP-проверок; CRUD, валидация, конфликты, фильтры, функции, каскад, 2 сессии",
    )


if __name__ == "__main__":
    main()
