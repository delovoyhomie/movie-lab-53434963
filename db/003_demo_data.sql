-- Начальный каталог реальных фильмов. Источники: раздел «Данные и поиск» в README.md.
-- Для пустой БД или замены старых неизменённых записей «Учебный фильм N».
-- Повторный запуск не перезаписывает фильмы, которые уже есть в каталоге.
BEGIN;

LOCK TABLE movies IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE demo_movies (
    number integer,
    name text,
    budget double precision,
    total_box_office real,
    mpaa_rating text,
    length integer,
    oscars_count bigint,
    director text,
    screenwriter text,
    operator text,
    tagline text,
    genre text
) ON COMMIT DROP;

INSERT INTO demo_movies VALUES
    (1, 'Терминатор 2: Судный день (1991)', 100000000, 517971391, 'R', 137, 4,
     'Джеймс Кэмерон', 'Джеймс Кэмерон', 'Адам Гринберг',
     'It''s nothing personal.', 'ACTION'),
    (2, 'Крепкий орешек (1988)', 28000000, 143651650, 'R', 132, NULL,
     'Джон Мактирнан', 'Стивен Э. де Соуза', 'Ян де Бонт',
     '40 stories of sheer adventure!', 'ACTION'),
    (3, 'Безумный Макс: Дорога ярости (2015)', 150000000, 380463726, 'R', 120, 6,
     'Джордж Миллер', 'Джордж Миллер', 'Джон Сил',
     'Only the mad survive.', 'ACTION'),
    (4, 'Непрощённый (1992)', 14400000, 159157447, 'R', 130, 4,
     'Клинт Иствуд', 'Дэвид Уэбб Пиплз', 'Джек Н. Грин',
     'Some legends will never be forgotten. Some wrongs can never be forgiven.', 'WESTERN'),
    (5, 'Джанго освобождённый (2012)', 100000000, 449841566, 'R', 165, 2,
     'Квентин Тарантино', 'Квентин Тарантино', 'Роберт Ричардсон',
     'Life, liberty and the pursuit of vengeance.', 'WESTERN'),
    (6, 'Омерзительная восьмёрка (2015)', 62000000, 151813358, 'R', 167, 1,
     'Квентин Тарантино', 'Квентин Тарантино', 'Роберт Ричардсон',
     'No one comes up here without a damn good reason.', 'WESTERN'),
    (7, 'Один дома (1990)', 15000000, 476678838, 'PG', 103, NULL,
     'Крис Коламбус', 'Джон Хьюз', 'Хулио Макат',
     'A family comedy without the family.', 'COMEDY'),
    (8, 'Шоу Трумана (1998)', 60000000, 264118201, 'PG', 104, NULL,
     'Питер Уир', 'Эндрю Никкол', 'Питер Бизиу',
     'On the air. Unaware.', 'COMEDY'),
    (9, 'Назад в будущее (1985)', 19000000, 398511663, 'PG', 116, 1,
     'Роберт Земекис', 'Боб Гейл', 'Дин Канди',
     NULL, 'COMEDY'),
    (10, 'Гарри Поттер и философский камень (2001)', 125000000, 966637830, 'PG', 152, NULL,
     'Крис Коламбус', 'Стив Кловис', 'Джон Сил',
     'Let the magic begin.', 'FANTASY'),
    (11, 'Волшебник страны Оз (1939)', 2777000, 35883549, 'G', 101, 2,
     'Виктор Флеминг', 'Ноэл Лэнгли', 'Гарольд Россон',
     NULL, 'FANTASY'),
    (12, 'Лабиринт Фавна (2006)', 16000000, 89367582, 'R', 119, 3,
     'Гильермо дель Торо', 'Гильермо дель Торо', 'Гильермо Наварро',
     'What happens when make-believe believes it''s real?', 'FANTASY');

-- Вариант не допускает обычные цвета глаз и волос. Эти поля и рост условные,
-- гражданство и местоположение не заполняются неподтверждёнными значениями.
INSERT INTO persons (name, eye_color, hair_color, height)
SELECT name, 'WHITE', 'WHITE', 180
FROM (
    SELECT director AS name FROM demo_movies
    UNION
    SELECT screenwriter FROM demo_movies
    UNION
    SELECT operator FROM demo_movies
) authors
WHERE NOT EXISTS (SELECT 1 FROM persons p WHERE p.name = authors.name)
ORDER BY name;

-- Сохраняем ID, дату добавления и координаты старого учебного набора.
-- Изменённые пользователем записи (version > 0) не трогаем.
UPDATE movies m
SET name = d.name,
    budget = d.budget,
    total_box_office = d.total_box_office,
    mpaa_rating = d.mpaa_rating,
    length = d.length,
    oscars_count = d.oscars_count,
    golden_palm_count = NULL,
    director_id = (SELECT min(id) FROM persons WHERE name = d.director),
    screenwriter_id = (SELECT min(id) FROM persons WHERE name = d.screenwriter),
    operator_id = (SELECT min(id) FROM persons WHERE name = d.operator),
    tagline = d.tagline,
    genre = d.genre,
    version = m.version + 1
FROM demo_movies d
WHERE m.name = 'Учебный фильм ' || d.number
  AND m.version = 0
  AND m.budget = 100000 * d.number
  AND EXISTS (SELECT 1 FROM persons p WHERE p.id = m.director_id AND p.name = 'Режиссёр Иванов');

-- Координаты — служебное поле модели из задания, а не место съёмок фильма.
INSERT INTO coordinates (x, y)
SELECT 0, 0
WHERE NOT EXISTS (SELECT 1 FROM coordinates WHERE x = 0 AND y = 0)
  AND EXISTS (
      SELECT 1 FROM demo_movies d
      WHERE NOT EXISTS (SELECT 1 FROM movies WHERE name = d.name OR name = 'Учебный фильм ' || d.number)
  );

INSERT INTO movies (
    name, coordinates_id, budget, total_box_office, mpaa_rating, length,
    oscars_count, director_id, screenwriter_id, operator_id, tagline, genre
)
SELECT d.name,
    (SELECT min(id) FROM coordinates WHERE x = 0 AND y = 0),
    d.budget, d.total_box_office, d.mpaa_rating, d.length, d.oscars_count,
    (SELECT min(id) FROM persons WHERE name = d.director),
    (SELECT min(id) FROM persons WHERE name = d.screenwriter),
    (SELECT min(id) FROM persons WHERE name = d.operator),
    d.tagline, d.genre
FROM demo_movies d
WHERE NOT EXISTS (SELECT 1 FROM movies WHERE name = d.name OR name = 'Учебный фильм ' || d.number)
ORDER BY d.number;

-- Удаляем старые учебные имена, только если на них больше нет ссылок.
DELETE FROM persons p
WHERE p.name IN ('Режиссёр Иванов', 'Сценарист без наград', 'Сценарист с наградами')
  AND p.version = 0
  AND NOT EXISTS (
      SELECT 1 FROM movies m
      WHERE m.director_id = p.id OR m.screenwriter_id = p.id OR m.operator_id = p.id
  );

DELETE FROM locations l
WHERE l.name = 'Студия' AND l.x = 1 AND l.y = 2 AND l.z = 3 AND l.version = 0
  AND NOT EXISTS (SELECT 1 FROM persons WHERE location_id = l.id);

COMMIT;
