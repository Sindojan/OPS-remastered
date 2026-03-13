-- Fix: System-Admin E-Mail fehlte '.com' Endung in V7 Migration
UPDATE users SET email = 'philipp.ebert@strate-software.com'
WHERE email = 'philipp.ebert@strate-software';
