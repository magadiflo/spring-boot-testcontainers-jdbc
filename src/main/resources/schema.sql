CREATE TABLE IF NOT EXISTS posts(
    id INT NOT NULL PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(250) NOT NULL,
    body TEXT NOT NULL,
    version INT
);