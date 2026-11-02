<?php

declare(strict_types=1);
ini_set('display_errors', '0');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    http_response_code(405);
    header('Allow: GET');
    echo json_encode(['service' => 'desego', 'database' => 'unknown']);
    exit;
}

try {
    require dirname(__DIR__).'/src/database.php';
    desegoDatabase()->query('SELECT 1')->fetchColumn();
    echo json_encode(['service' => 'desego', 'database' => 'connected']);
} catch (Throwable $error) {
    http_response_code(503);
    error_log('Desego: MySQL unavailable; code='.$error->getCode());
    echo json_encode(['service' => 'desego', 'database' => 'unavailable']);
}
