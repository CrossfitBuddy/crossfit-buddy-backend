create table if not exists workout_format
(
    type                varchar(32) primary key,
    name                varchar(255) not null,
    description         text not null,
    duration_required   boolean not null,
    interval_required   boolean not null
);

insert into format_type (type, name, description, duration_required, interval_required)
values
    (
        'EMOM',
        'Every Minute on the Minute',
        'В начале каждой минуты (включая нулевую) атлет выполняет заданное количество повторений. Оставшееся до конца минуты время используется для отдыха. Комплекс продолжается заданное количество минут. Допускается, что заданное количество выполнение должно выполняться на произвольную минуту.',
        true,
        true
    ),
    (
        'FOR_TIME',
        'For Time',
        'Атлет выполняет заданный объем работы за максимально короткое время. Может состоять из нескольких раундов, раунды могут быть как одинаковые, так и разные',
        true,
        false
    ),
    (
        'AMRAP',
        'As Many Rounds As Possible',
        'Атлет выполняет максимальное количество раундов (или повторений) за строго фиксированное время. Раунды могут быть как одинаковые, так и разные по структуре.',
        true,
        false
    ),
    (
        'TABATA',
        'Tabata',
        'Комплекс, основанный на строгом чередовании работы и отдыха. Состоит из нескольких раундов, между раундами есть фиксированное время отдыха. Каждый раунд выполняется максимальное количество раз за отведенное время.',
        true,
        true
    )
on conflict (type) do nothing;
