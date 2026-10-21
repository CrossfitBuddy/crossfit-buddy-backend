create table roles (
    id uuid primary key default gen_random_uuid(),
    name user_role not null unique,
    description text not null
);

insert into roles (name, description) values
    ('ATHLETE', 'Выполнение тренировок и сохранение собственных результатов'),
    ('TRAINER', 'Подготовка тренировок и сопровождение атлетов'),
    ('ADMIN', 'Администрирование системы');
