"""Проверка фильтрации и сортировки. Запускать на отдельной тестовой базе.
python3 tests/filtering.py http://localhost:8080/movie-lab
"""

import uuid
from urllib.parse import urlencode
from smoke import Client

client = Client()
client.login("student1")
prefix = "FILTER-" + uuid.uuid4().hex[:8]
movies = []
coordinate = person = None
fields = [
    "id", "name", "coordinates", "coordinatesX", "coordinatesY", "creationDate",
    "oscarsCount", "budget", "totalBoxOffice", "mpaaRating", "director",
    "screenwriter", "operator", "length", "goldenPalmCount", "tagline", "genre",
]


def page(**params):
    return client.call("movies?" + urlencode(params))


def value(movie, field):
    if field == "coordinates":
        return movie["coordinates"]["id"]
    if field in ("coordinatesX", "coordinatesY"):
        return movie["coordinates"][field[-1].lower()]
    if field in ("director", "screenwriter", "operator"):
        return movie[field]["name"] if movie[field] else None
    return movie[field]


try:
    coordinate = client.call("references/coordinates", "POST", {"x": -37, "y": 1.2})
    person = client.call("references/persons", "POST", {
        "name": prefix, "eyeColor": "RED", "hairColor": "WHITE", "height": 180,
    })
    for number in (2, 10, 3):
        movies.append(client.call("movies", "POST", {
            "name": prefix + str(number), "tagline": prefix,
            "coordinatesId": coordinate["id"], "directorId": person["id"],
            "screenwriterId": person["id"], "operatorId": person["id"],
            "budget": number, "totalBoxOffice": number,
            "length": number, "mpaaRating": "PG", "genre": "COMEDY",
            "oscarsCount": 9007199254740993 if number == 10 else number,
            "goldenPalmCount": None if number == 3 else number,
        }))
    # Каждое поле доступно для поиска. Текст ищется по подстроке без учёта регистра.
    for field in fields:
        expected = value(movies[0], field)
        query = str(expected)
        if field == "name":
            query = prefix.lower()
        result = page(column=field, q=query, size=100)
        assert movies[0]["id"] in [item["id"] for item in result["items"]], field
    # Большие Long сравниваются точно, а не через double.
    exact = page(column="oscarsCount", q="9007199254740993")
    assert [item["id"] for item in exact["items"]] == [movies[1]["id"]]
    assert page(column="oscarsCount", q="9007199254740992")["total"] == 0
    # Сортировка каждого поля в обе стороны; пустые значения всегда в конце.
    # Числа 2, 3, 10 позволяют заметить ошибочную сортировку как строк.
    for field in fields:
        for descending in (False, True):
            result = page(column="tagline", q=prefix, sort=field,
                          desc=str(descending).lower(), size=100)
            values = [value(item, field) for item in result["items"]]
            filled = [item for item in values if item is not None]
            expected = sorted(filled, reverse=descending) + [None] * (len(values) - len(filled))
            assert values == expected, (field, descending, values)
    # Сортировка применяется до пагинации, порядок сохраняется между страницами.
    first = page(column="tagline", q=prefix, sort="budget", size=2)
    second = page(column="tagline", q=prefix, sort="budget", size=2, page=1)
    assert [item["budget"] for item in first["items"] + second["items"]] == [2, 3, 10]
    assert first["total"] == second["total"] == 3
    # Пустой фильтр не исключает NULL и не требует разбора числа или даты.
    for field in fields:
        assert page(column=field, q="")["total"] >= 3
    for params in (
        {"column": "wrong"}, {"sort": "budget desc; delete from movies"},
        {"column": "id", "q": "1 OR 1=1"},
        {"column": "id", "q": "9223372036854775808"},
        {"column": "length", "q": "2.5"},
        {"column": "budget", "q": "NaN"},
        {"column": "totalBoxOffice", "q": "Infinity"},
        {"column": "creationDate", "q": "2026-02-30"},
    ):
        client.call("movies?" + urlencode(params), expected=400)
    print("PASS: все 17 полей, оба направления, NULL, пагинация, Long и ошибочный ввод")
finally:
    for movie in movies:
        client.call(f"movies/{movie['id']}?version={movie['version']}", "DELETE", expected=204)
    for kind, item in (("persons", person), ("coordinates", coordinate)):
        if item:
            client.call(f"references/{kind}/{item['id']}?version={item['version']}",
                        "DELETE", expected=204)
