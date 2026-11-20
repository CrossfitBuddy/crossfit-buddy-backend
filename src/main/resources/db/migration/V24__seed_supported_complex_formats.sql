lock table complex_format in share row exclusive mode;

with supported(code, name, description) as (
    values
        ('AMRAP', 'AMRAP', 'As many rounds as possible within the specified duration.'),
        ('EMOM', 'EMOM', 'Start the prescribed round at each interval within the specified duration.'),
        ('FOR_TIME', 'For Time', 'Complete the prescribed rounds within the time limit.'),
        ('TABATA', 'Tabata', 'Perform work and rest intervals for the specified duration.')
), missing as (
    select supported.* from supported
    where not exists (select 1 from complex_format existing where existing.code = supported.code)
)
insert into complex_format(id, code, name, description)
select (select coalesce(max(id), 0) from complex_format) + row_number() over (order by code)::integer,
       code, name, description
from missing;
