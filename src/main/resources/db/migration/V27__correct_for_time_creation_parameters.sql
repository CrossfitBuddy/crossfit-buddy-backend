update workout_format
set duration_required = false, interval_required = false
where type = 'FOR_TIME';
