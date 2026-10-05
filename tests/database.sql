-- Только тестовая база. Все изменения откатываются в конце.
BEGIN;
DELETE FROM movies;
DELETE FROM persons;
DELETE FROM coordinates;
DELETE FROM locations;
CREATE FUNCTION pg_temp.assert_true(ok boolean, description text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN IF ok IS DISTINCT FROM TRUE THEN RAISE EXCEPTION 'FAIL: %',description; END IF; END $$;
CREATE FUNCTION pg_temp.reject(command text) RETURNS void LANGUAGE plpgsql AS $$
DECLARE failed boolean := false;
BEGIN
 BEGIN EXECUTE command; EXCEPTION WHEN integrity_constraint_violation OR raise_exception THEN failed:=true; END;
 IF NOT failed THEN RAISE EXCEPTION 'Expected rejection: %',command; END IF;
END $$;
INSERT INTO coordinates(x,y) VALUES(1,2);
INSERT INTO locations(x,y,z,name) VALUES(1,2,3,'');
INSERT INTO persons(name,eye_color,hair_color,height,location_id)
 SELECT 'writer','RED','WHITE',180,id FROM locations;
INSERT INTO persons(name,eye_color,hair_color,height) VALUES('no films','RED','WHITE',180);
INSERT INTO movies(name,coordinates_id,budget,total_box_office,mpaa_rating,director_id,screenwriter_id,length,genre,oscars_count,tagline)
SELECT 'donor '||n,(SELECT min(id) FROM coordinates),1,1,'G',(SELECT min(id) FROM persons),(SELECT min(id) FROM persons),1,'ACTION',CASE WHEN n=1 THEN 3 ELSE 2 END,'same' FROM generate_series(1,2) n;
INSERT INTO movies(name,coordinates_id,budget,total_box_office,mpaa_rating,director_id,screenwriter_id,length,genre,tagline)
SELECT 'target '||n,(SELECT min(id) FROM coordinates),1,1,'G',(SELECT min(id) FROM persons),(SELECT min(id) FROM persons),1,'COMEDY','a%b_c' FROM generate_series(1,3) n;
SELECT pg_temp.assert_true((SELECT count(*) FROM screenwriters_without_oscars())=0,'writer with Oscars excluded');
SELECT pg_temp.assert_true((SELECT redistribute_oscars('ACTION','COMEDY'))=5,'sum 5 transferred');
SELECT pg_temp.assert_true((SELECT array_agg(oscars_count ORDER BY id) FROM movies WHERE genre='COMEDY')=ARRAY[2,2,1]::bigint[],'remainder 2 2 1');
SELECT pg_temp.assert_true((SELECT sum(oscars_count) FROM movies)=5,'conservation');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies WHERE genre='ACTION' AND oscars_count IS NULL)=2,'donors cleared to NULL');
SELECT pg_temp.assert_true((SELECT min(version) FROM movies)=1,'SQL updates versions');
SELECT pg_temp.assert_true((SELECT redistribute_oscars('ACTION','COMEDY'))=0,'zero donor total');
SELECT pg_temp.reject($q$SELECT redistribute_oscars('COMEDY','COMEDY')$q$);
SELECT pg_temp.reject($q$SELECT redistribute_oscars('COMEDY','FANTASY')$q$);
SELECT pg_temp.reject($q$SELECT redistribute_oscars('INVALID','COMEDY')$q$);
SELECT pg_temp.assert_true((SELECT count(*) FROM movies_by_tagline('%'))=3,'literal percent');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies_by_tagline('_'))=3,'literal underscore');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies_by_tagline('SAME'))=0,'special case sensitivity');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies_genre_less('COMEDY'))=2,'genre declaration order');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies_genre_less('ACTION'))=0,'smallest genre');
SELECT pg_temp.assert_true((SELECT delete_one_by_tagline('same')) IS NOT NULL,'delete found');
SELECT pg_temp.assert_true((SELECT count(*) FROM movies WHERE tagline='same')=1,'delete exactly one');
SELECT pg_temp.assert_true((SELECT delete_one_by_tagline('missing')) IS NULL,'missing tagline');
-- Проверка отсутствия переполнения и атомарности.
UPDATE movies SET oscars_count=9223372036854775807 WHERE genre='ACTION';
SELECT pg_temp.reject($q$SELECT redistribute_oscars('COMEDY','ACTION')$q$);
SELECT pg_temp.assert_true((SELECT sum(oscars_count) FROM movies WHERE genre='COMEDY')=5,'overflow leaves donors unchanged');
SELECT pg_temp.assert_true((SELECT oscars_count FROM movies WHERE genre='ACTION')=9223372036854775807,'overflow leaves recipient unchanged');
-- Меньше наград, чем получателей: 1 0 0, нули хранятся как NULL.
UPDATE movies SET oscars_count=NULL WHERE genre='COMEDY';
UPDATE movies SET oscars_count=1 WHERE genre='ACTION';
SELECT redistribute_oscars('ACTION','COMEDY');
SELECT pg_temp.assert_true((SELECT array_agg(oscars_count ORDER BY id) FROM movies WHERE genre='COMEDY')=ARRAY[1,NULL,NULL]::bigint[],'sparse awards');
UPDATE movies SET oscars_count=NULL;
SELECT pg_temp.assert_true((SELECT count(*) FROM screenwriters_without_oscars())=1,'only writer who has films');
-- Существующие награды получателей сохраняются и увеличиваются.
UPDATE movies SET oscars_count=2 WHERE genre='ACTION';
UPDATE movies SET oscars_count=10 WHERE id=(SELECT min(id) FROM movies WHERE genre='COMEDY');
SELECT redistribute_oscars('ACTION','COMEDY');
SELECT pg_temp.assert_true((SELECT array_agg(oscars_count ORDER BY id) FROM movies WHERE genre='COMEDY')=ARRAY[11,1,NULL]::bigint[],'additive redistribution');
-- Ограничения полей на уровне БД.
SELECT pg_temp.reject($q$UPDATE movies SET oscars_count=0$q$);
SELECT pg_temp.reject($q$UPDATE movies SET golden_palm_count=0$q$);
SELECT pg_temp.reject($q$UPDATE movies SET budget=0$q$);
SELECT pg_temp.reject($q$UPDATE movies SET budget='NaN'::float8$q$);
SELECT pg_temp.reject($q$UPDATE movies SET total_box_office='Infinity'::real$q$);
SELECT pg_temp.reject($q$UPDATE movies SET length=NULL$q$);
SELECT pg_temp.reject($q$UPDATE movies SET length=-1$q$);
SELECT pg_temp.reject($q$UPDATE movies SET name='  '$q$);
SELECT pg_temp.reject($q$UPDATE movies SET mpaa_rating='BAD'$q$);
SELECT pg_temp.reject($q$UPDATE movies SET coordinates_id=NULL$q$);
SELECT pg_temp.reject($q$UPDATE movies SET director_id=NULL$q$);
SELECT pg_temp.reject($q$UPDATE movies SET genre='BAD'$q$);
SELECT pg_temp.reject($q$UPDATE coordinates SET y=2.01$q$);
SELECT pg_temp.reject($q$UPDATE coordinates SET y='NaN'::real$q$);
SELECT pg_temp.reject($q$UPDATE coordinates SET y='-Infinity'::real$q$);
SELECT pg_temp.reject($q$UPDATE coordinates SET x=NULL$q$);
SELECT pg_temp.reject($q$UPDATE persons SET height=0$q$);
SELECT pg_temp.reject($q$UPDATE persons SET eye_color=NULL$q$);
SELECT pg_temp.reject($q$UPDATE persons SET hair_color='BLUE'$q$);
SELECT pg_temp.reject($q$UPDATE persons SET nationality='BAD'$q$);
SELECT pg_temp.reject($q$UPDATE locations SET z='NaN'::real$q$);
SELECT pg_temp.reject($q$UPDATE locations SET name=NULL$q$);
UPDATE movies SET creation_date='2000-01-01';
SELECT pg_temp.assert_true((SELECT min(creation_date) FROM movies)=(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,'immutable generated date');
-- Связанные зависимые объекты удаляются каскадно.
DELETE FROM locations;
SELECT pg_temp.assert_true((SELECT count(*) FROM movies)=0,'location cascade movies');
SELECT pg_temp.assert_true((SELECT count(*) FROM persons)=1,'unrelated person survives');
ROLLBACK;
