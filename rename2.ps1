$dir = "c:\Users\ESTUDIANTE\Downloads\DemoApiAvicola\DemoApiAvicola"

$files = Get-ChildItem -Path $dir -Recurse -File -Include *.kt, *.xml, *.kts, *.properties | Where-Object { $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\.gradle\\" -and $_.FullName -notmatch "\\.idea\\" -and $_.FullName -notmatch "\\.git\\" }

foreach ($f in $files) {
    $path = $f.FullName
    $text = [IO.File]::ReadAllText($path)
    $modified = $false

    if ($text -cmatch "avicola") {
        $text = $text -creplace "avicola", "desego"
        $modified = $true
    }
    if ($text -cmatch "Avicola") {
        $text = $text -creplace "Avicola", "Desego"
        $modified = $true
    }

    if ($modified) {
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($path, $text, $utf8NoBom)
    }
}

$folders = @(
    "$dir\app\src\main\java\com\avicola",
    "$dir\app\src\test\java\com\avicola",
    "$dir\app\src\androidTest\java\com\avicola"
)

foreach ($folder in $folders) {
    if (Test-Path $folder) {
        Rename-Item -Path $folder -NewName "desego"
    }
}
