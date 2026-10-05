-- В psql используйте -v ON_ERROR_STOP=1. Скрипт выполняется один раз в пустой схеме.
BEGIN;

CREATE TABLE app_revision (
    id integer PRIMARY KEY CHECK (id = 1),
    value bigint NOT NULL DEFAULT 0
);

INSERT INTO
    app_revision
VALUES
    (1, 0);

CREATE TABLE app_users (
    login text PRIMARY KEY,
    salt text NOT NULL,
    password_hash text NOT NULL
);

CREATE TABLE coordinates (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    version bigint NOT NULL DEFAULT 0,
    x integer NOT NULL,
    y real NOT NULL CHECK (
        y <= 2
        AND y > '-Infinity'::real
    )
);

CREATE TABLE locations (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    version bigint NOT NULL DEFAULT 0,
    x integer NOT NULL,
    y integer NOT NULL,
    z real NOT NULL CHECK (
        z > '-Infinity'::real
        AND z < 'Infinity'::real
    ),
    name text NOT NULL
);

CREATE TABLE persons (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    version bigint NOT NULL DEFAULT 0,
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    eye_color varchar(255) NOT NULL CHECK (eye_color IN ('RED', 'YELLOW', 'WHITE')),
    hair_color varchar(255) NOT NULL CHECK (hair_color IN ('RED', 'YELLOW', 'WHITE')),
    location_id bigint REFERENCES locations (id) ON DELETE CASCADE,
    height double precision NOT NULL CHECK (
        height > 0
        AND height < 'Infinity'::double precision
    ),
    nationality varchar(255) CHECK (
        nationality IN (
            'UNITED_KINGDOM',
            'GERMANY',
            'THAILAND',
            'SOUTH_KOREA'
        )
    )
);

CREATE TABLE movies (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY CHECK (id > 0),
    version bigint NOT NULL DEFAULT 0,
    name text NOT NULL CHECK (name ~ '[^[:space:]]'),
    coordinates_id bigint NOT NULL REFERENCES coordinates (id) ON DELETE CASCADE,
    creation_date date NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,
    oscars_count bigint CHECK (oscars_count > 0),
    budget double precision NOT NULL CHECK (
        budget > 0
        AND budget < 'Infinity'::double precision
    ),
    total_box_office real NOT NULL CHECK (
        total_box_office > 0
        AND total_box_office < 'Infinity'::real
    ),
    mpaa_rating varchar(255) NOT NULL CHECK (mpaa_rating IN ('G', 'PG', 'R', 'NC_17')),
    director_id bigint NOT NULL REFERENCES persons (id) ON DELETE CASCADE,
    screenwriter_id bigint REFERENCES persons (id) ON DELETE CASCADE,
    operator_id bigint REFERENCES persons (id) ON DELETE CASCADE,
    length integer NOT NULL CHECK (length > 0),
    golden_palm_count bigint CHECK (golden_palm_count > 0),
    tagline text,
    genre varchar(255) CHECK (
        genre IN ('ACTION', 'WESTERN', 'COMEDY', 'FANTASY')
    )
);

CREATE INDEX movies_coordinates_idx ON movies (coordinates_id);

CREATE INDEX movies_director_idx ON movies (director_id);

CREATE INDEX movies_screenwriter_idx ON movies (screenwriter_id);

CREATE INDEX movies_operator_idx ON movies (operator_id);

CREATE INDEX persons_location_idx ON persons (location_id);

CREATE INDEX movies_genre_idx ON movies (genre);

CREATE FUNCTION movie_created_date () RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='INSERT' THEN NEW.creation_date := (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date;
 ELSE NEW.creation_date := OLD.creation_date;
 END IF;
 RETURN NEW;
END $$;

CREATE TRIGGER movie_date
BEFORE INSERT OR UPDATE ON movies FOR EACH ROW
EXECUTE FUNCTION movie_created_date ();

-- Версия общей модели меняется только вместе с успешной транзакцией.
CREATE FUNCTION bump_revision () RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 UPDATE app_revision SET value=value+1 WHERE id=1;
 RETURN NULL;
END $$;

CREATE TRIGGER movies_revision
AFTER INSERT OR UPDATE OR DELETE ON movies FOR EACH STATEMENT
EXECUTE FUNCTION bump_revision ();

CREATE TRIGGER coordinates_revision
AFTER INSERT OR UPDATE OR DELETE ON coordinates FOR EACH STATEMENT
EXECUTE FUNCTION bump_revision ();

CREATE TRIGGER persons_revision
AFTER INSERT OR UPDATE OR DELETE ON persons FOR EACH STATEMENT
EXECUTE FUNCTION bump_revision ();

CREATE TRIGGER locations_revision
AFTER INSERT OR UPDATE OR DELETE ON locations FOR EACH STATEMENT
EXECUTE FUNCTION bump_revision ();

-- Порядок из задания, а не алфавитный порядок строк.
CREATE FUNCTION genre_rank (g text) RETURNS integer LANGUAGE sql IMMUTABLE AS $$
 SELECT CASE g WHEN 'ACTION' THEN 0 WHEN 'WESTERN' THEN 1 WHEN 'COMEDY' THEN 2 WHEN 'FANTASY' THEN 3 END
$$;

CREATE FUNCTION delete_one_by_tagline (p_text text) RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE selected_id bigint;
BEGIN
 IF p_text IS NULL THEN RAISE EXCEPTION 'tagline должен быть строкой'; END IF;
 LOCK TABLE movies IN SHARE ROW EXCLUSIVE MODE;
 SELECT id INTO selected_id FROM movies WHERE tagline=p_text ORDER BY id LIMIT 1;
 IF selected_id IS NOT NULL THEN DELETE FROM movies WHERE id=selected_id; END IF;
 RETURN selected_id;
END $$;

CREATE FUNCTION movies_by_tagline (p_text text) RETURNS SETOF movies LANGUAGE sql STABLE AS $$
 SELECT * FROM movies WHERE position(p_text IN tagline)>0 ORDER BY id
$$;

CREATE FUNCTION movies_genre_less (p_genre text) RETURNS SETOF movies LANGUAGE plpgsql STABLE AS $$
BEGIN
 IF genre_rank(p_genre) IS NULL THEN RAISE EXCEPTION 'Неизвестный жанр'; END IF;
 RETURN QUERY SELECT * FROM movies WHERE genre_rank(genre)<genre_rank(p_genre) ORDER BY id;
END $$;

CREATE FUNCTION screenwriters_without_oscars () RETURNS SETOF persons LANGUAGE sql STABLE AS $$
 SELECT p.* FROM persons p
 WHERE EXISTS (SELECT 1 FROM movies m WHERE m.screenwriter_id=p.id)
 AND NOT EXISTS (SELECT 1 FROM movies m WHERE m.screenwriter_id=p.id AND m.oscars_count IS NOT NULL)
 ORDER BY p.id
$$;

CREATE FUNCTION redistribute_oscars (p_source text, p_target text) RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE total numeric; count_targets bigint; base_share numeric; remainder bigint;
BEGIN
 IF genre_rank(p_source) IS NULL OR genre_rank(p_target) IS NULL THEN RAISE EXCEPTION 'Укажите корректные жанры'; END IF;
 IF p_source=p_target THEN RAISE EXCEPTION 'Жанры должны отличаться'; END IF;
 -- Блокирует конкурирующие INSERT/UPDATE/DELETE до конца транзакции.
 LOCK TABLE movies IN SHARE ROW EXCLUSIVE MODE;
 SELECT count(*) INTO count_targets FROM movies WHERE genre=p_target;
 IF count_targets=0 THEN RAISE EXCEPTION 'В жанре-получателе нет фильмов'; END IF;
 SELECT coalesce(sum(oscars_count),0) INTO total FROM movies WHERE genre=p_source;
 IF total=0 THEN RETURN 0; END IF;
 base_share:=trunc(total/count_targets); remainder:=mod(total,count_targets)::bigint;
 IF EXISTS (
  SELECT 1 FROM (
   SELECT coalesce(oscars_count,0)::numeric + base_share + CASE WHEN row_number() OVER (ORDER BY id)<=remainder THEN 1 ELSE 0 END AS next_value
   FROM movies WHERE genre=p_target
  ) t WHERE next_value>9223372036854775807::numeric
 ) THEN RAISE EXCEPTION 'Переполнение Long: распределение отменено'; END IF;
 UPDATE movies SET oscars_count=NULL, version=version+1 WHERE genre=p_source AND oscars_count IS NOT NULL;
 WITH shares AS (
  SELECT id,base_share + CASE WHEN row_number() OVER (ORDER BY id)<=remainder THEN 1 ELSE 0 END AS amount
  FROM movies WHERE genre=p_target
 )
 UPDATE movies m SET oscars_count=nullif((coalesce(m.oscars_count,0)::numeric+s.amount)::bigint,0),version=m.version+1
 FROM shares s WHERE m.id=s.id;
 RETURN total;
END $$;

COMMIT;
