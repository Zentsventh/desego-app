<?php
declare(strict_types=1);

function desegoDatabase(): PDO
{
    return new PDO(
        'mysql:host=' . (getenv('DESEGO_DB_HOST') ?: '127.0.0.1')
        . ';port=' . (getenv('DESEGO_DB_PORT') ?: '3306')
        . ';dbname=' . (getenv('DESEGO_DB_NAME') ?: 'bd_desego') . ';charset=utf8mb4',
        getenv('DESEGO_DB_USER') ?: 'root',
        getenv('DESEGO_DB_PASSWORD') ?: '',
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
         PDO::ATTR_EMULATE_PREPARES => false,
         PDO::ATTR_TIMEOUT => 5]
    );
}
