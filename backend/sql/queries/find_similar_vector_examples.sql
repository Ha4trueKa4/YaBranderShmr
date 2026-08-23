SELECT svg_data, (embedding <=> $1::vector) AS dist
FROM vector_examples
WHERE embedding <=> $1::vector < $3
ORDER BY dist
LIMIT $2;
