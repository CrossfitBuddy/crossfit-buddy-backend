ALTER TABLE exercise
    ADD COLUMN video_id uuid,
    ADD COLUMN image_id uuid,
    ADD COLUMN category varchar(255) NOT NULL DEFAULT 'OTHER'
        CHECK (category IN (
            'STRENGTH',
            'CARDIO',
            'GYMNASTICS',
            'WEIGHTLIFTING',
            'OTHER'
        ));

ALTER TABLE exercise
    ALTER COLUMN description SET NOT NULL;
