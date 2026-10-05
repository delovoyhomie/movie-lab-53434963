-- Необязательно. Данные для демонстрации функций и пагинации.
BEGIN;

INSERT INTO
    coordinates (x, y)
VALUES
    (10, 1.5),
    (20, 2);

INSERT INTO
    locations (x, y, z, name)
VALUES
    (1, 2, 3, 'Студия');

INSERT INTO
    persons (
        name,
        eye_color,
        hair_color,
        location_id,
        height,
        nationality
    )
SELECT
    'Режиссёр Иванов',
    'RED',
    'WHITE',
    min(id),
    180,
    'GERMANY'
FROM
    locations;

INSERT INTO
    persons (name, eye_color, hair_color, height)
VALUES
    ('Сценарист без наград', 'YELLOW', 'RED', 175),
    ('Сценарист с наградами', 'WHITE', 'YELLOW', 182);

INSERT INTO
    movies (
        name,
        coordinates_id,
        oscars_count,
        budget,
        total_box_office,
        mpaa_rating,
        director_id,
        screenwriter_id,
        length,
        tagline,
        genre
    )
SELECT
    'Учебный фильм ' || n,
    (
        SELECT
            min(id)
        FROM
            coordinates
    ),
    CASE
        WHEN n IN (1, 2) THEN n + 1
        ELSE NULL
    END,
    100000 * n,
    150000 * n,
    'PG',
    (
        SELECT
            min(id)
        FROM
            persons
    ),
    (
        SELECT
            id
        FROM
            persons
        WHERE
            name = CASE
                WHEN n IN (1, 2) THEN 'Сценарист с наградами'
                ELSE 'Сценарист без наград'
            END
    ),
    90 + n,
    CASE
        WHEN n <= 2 THEN 'Дорога домой'
        ELSE 'История ' || n
    END,
    CASE
        WHEN n <= 2 THEN 'ACTION'
        WHEN n <= 5 THEN 'COMEDY'
        WHEN n <= 8 THEN 'WESTERN'
        ELSE 'FANTASY'
    END
FROM
    generate_series(1, 12) n;

COMMIT;
