begin;
lock table stage, complex_format in access exclusive mode;

alter table stage
    drop constraint if exists stage_complex_format_id_fkey;

alter table stage
    drop column if exists complex_format_id;

drop table if exists complex_format;

create table if not exists complex_format
(
    type                varchar(32) primary key,
    name                varchar(255) not null,
    description         text not null
);

insert into complex_format (type, name, description)
values
    (
        'EMOM',
        'Every Minute on the Minute',
        'В начале каждой минуты (включая нулевую) атлет выполняет заданное количество повторений. Оставшееся до конца минуты время используется для отдыха. Комплекс продолжается заданное количество минут. Допускается, что заданное количество выполнение должно выполняться на произвольную минуту.'
    ),
    (
        'FOR_TIME',
        'For Time',
        'Атлет выполняет заданный объем работы за максимально короткое время. Может состоять из нескольких раундов, раунды могут быть как одинаковые, так и разные'
    ),
    (
        'AMRAP',
        'As Many Rounds As Possible',
        'Атлет выполняет максимальное количество раундов (или повторений) за строго фиксированное время. Раунды могут быть как одинаковые, так и разные по структуре.'
    )
on conflict (type) do nothing;

alter table stage
    add column complex_format_type varchar(32);

alter table stage
    alter column complex_format_type set not null;

alter table stage
    add constraint stage_complex_format_type_fkey
        foreign key (complex_format_type)
            references complex_format(type);
commit;