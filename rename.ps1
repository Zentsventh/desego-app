$dir = "c:\Users\ESTUDIANTE\Downloads\DemoApiAvicola\DemoApiAvicola"
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False

$files = Get-ChildItem -Path $dir -Recurse -File -Include *.kt, *.xml, *.kts, *.properties | Where-Object { $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\.gradle\\" -and $_.FullName -notmatch "\\.idea\\" -and $_.FullName -notmatch "\\.git\\" }

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -cmatch "avicola" -or $content -cmatch "Avicola") {
        $newContent = $content -creplace "avicola", "desego"
        $newContent = $newContent -creplace "Avicola", "Desego"
        [IO.File]::WriteAllText($file.FullName, $newContent, $Utf8NoBomEncoding)
    }
}

$dirsToRename = @(
    "$dir\app\src\main\java\com\avicola",
    "$dir\app\src\test\java\com\avicola",
    "$dir\app\src\androidTest\java\com\avicola"
)

foreach ($d in $dirsToRename) {
    if (Test-Path $d) {
        Rename-Item -Path $d -NewName "desego"
    }
}
