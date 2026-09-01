ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_name (name) WITH PARSER ngram;
ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_title (title) WITH PARSER ngram;
ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_skills (skills) WITH PARSER ngram;
ALTER TABLE resume ADD FULLTEXT INDEX ft_resume_intro (introduction) WITH PARSER ngram;
SHOW INDEX FROM resume WHERE Key_name LIKE 'ft%';
