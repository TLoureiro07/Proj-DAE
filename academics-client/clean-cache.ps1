# Limpar cache do Nuxt
Remove-Item -Recurse -Force .nuxt -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .output -ErrorAction SilentlyContinue
Write-Host "Cache limpo!"

