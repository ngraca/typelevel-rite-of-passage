CREATE TABLE users (
  email text NOT NULL,
  hashedPassword text NOT NULL,
  firstName text,
  lastName text,
  company text,
  role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users (
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'daniel@rockthejvm.com',
  '$2a$10$fTtSkEzG7/OrqJdqx3N.Su/yLrQ7MPaoS6A/m8WS/KdA/.1ESZ5jO',
  'Daniel',
  'Ciocirlan',
  'Rock the JVM',
  'ADMIN'
);

INSERT INTO users (
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'riccardo@rockthejvm.com',
  '$2a$10$Mgvd7UROYFj9W4hjMFp.EeCs3yVAcD/MiXislIj5Mt9RWQ1q3VedS',
  'Riccardo',
  'Cardin',
  'Rock the JVM',
  'RECRUITER'
);