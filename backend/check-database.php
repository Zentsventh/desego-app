<?php
declare(strict_types=1);
if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}
require __DIR__ . '/src/database.php';
try {
    $pdo = desegoDatabase();
    echo json_encode([
        'database' => $pdo->query('SELECT DATABASE()')->fetchColumn(),
        'tables' => $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN),
    ], JSON_PRETTY_PRINT), PHP_EOL;
} catch (Throwable $error) {
    fwrite(STDERR, 'MySQL connection failed; code=' . $error->getCode() . PHP_EOL);
    exit(1);
}
