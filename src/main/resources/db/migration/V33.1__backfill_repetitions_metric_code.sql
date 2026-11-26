UPDATE exercise_metric
SET metric_code = 'repetitions'
WHERE lower(trim(name)) IN ('повторения', 'количество повторений')
  AND metrics_value_type = 'INTEGER'
  AND lower(trim(unit)) = 'раз'
  AND (metric_code IS NULL OR trim(metric_code) = '');
