-- For an existing MySQL schema only. Review and back up the database first.
-- Apply manually before starting the updated application; this is not run by the app.

ALTER TABLE rooms
    DROP INDEX uk_rooms_room_number,
    ADD CONSTRAINT uk_rooms_hostel_room_number
        UNIQUE (hostel_name, room_number);

ALTER TABLE students
    DROP INDEX uk_students_student_code,
    ADD CONSTRAINT uk_students_hostel_student_code
        UNIQUE (hostel_name, student_code);
