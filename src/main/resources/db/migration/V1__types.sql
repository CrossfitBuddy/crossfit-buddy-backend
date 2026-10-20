create extension if not exists pgcrypto;

create type user_role as enum ('ATHLETE', 'TRAINER', 'ADMIN');
create type workout_type as enum ('OWN', 'ASSIGNED', 'SHARED');
create type workout_status as enum ('DRAFT', 'READY', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
create type subscription_status as enum ('PENDING', 'ACTIVE', 'REJECTED', 'CANCELLED');

