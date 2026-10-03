UPDATE plans
SET limits = limits || '{"websites":3}'::jsonb
WHERE code = 'FREE';
