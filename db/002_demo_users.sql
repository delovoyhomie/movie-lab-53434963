BEGIN;

-- Только демонстрационные пользователи. Пароль обоих: student123
INSERT INTO
    app_users
VALUES
    (
        'student1',
        'bW92aWUtbGFiLWRlbW8tMDE=',
        'do2o3thF7zUkuPxuoO+XH2vdewBtTKpGNTO4L3HSrrk='
    );

INSERT INTO
    app_users
VALUES
    (
        'student2',
        'bW92aWUtbGFiLWRlbW8tMDI=',
        '7KQbsB2d1J7tvj35cku/zUlJ9uAmTsBFfuVTSuVBlho='
    );

COMMIT;
