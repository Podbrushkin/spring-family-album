# https://www.pexels.com/collections/family-at-home-during-quarantine-time-nfgce65/
function Get-RandomSequence {
    param (
        [int]$length = 32
    )

    $characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    $randomSequence = ""
    $random = New-Object -TypeName System.Random

    for ($i = 0; $i -lt $length; $i++) {
        $randomIndex = $random.Next(0, $characters.Length)
        $randomCharacter = $characters[$randomIndex]
        $randomSequence += $randomCharacter
    }

    return $randomSequence
}

function Split-Basename {
    param (
        [string]$basename
    )

    $words = $basename -split '\s'
    $filteredWords = $words | Where-Object { $_ -notmatch '\d' }

    return $filteredWords
}

$files = Get-ChildItem *.jpg
$imageDetails = @()

$files | ForEach-Object {
    $uniqueHash = Get-RandomSequence -length 32
    $imageHash = Get-RandomSequence -length 64

    $tags = Split-Basename -basename $_.BaseName

    $creationDate = Get-Date -Year (Get-Random -Minimum 1993 -Maximum (Get-Date).Year) `
                            -Month (Get-Random -Minimum 1 -Maximum 13) `
                            -Day (Get-Random -Minimum 1 -Maximum 29) `
                            -Hour (Get-Random -Minimum 0 -Maximum 24) `
                            -Minute (Get-Random -Minimum 0 -Maximum 60) `
                            -Second (Get-Random -Minimum 0 -Maximum 60) `
                            -Millisecond (Get-Random -Minimum 0 -Maximum 1000)
    $formattedCreationDate = Get-Date -Date $creationDate -Format "yyyy-MM-ddTHH:mm:ss.fff"

    $imageObject = [PSCustomObject]@{
        fullName = $_.FullName
        imgId = $_.BaseName -replace '.*(\d{7})$', '$1'
        uniqueHash = $uniqueHash
        imageHash = $imageHash
        tags = @($tags)
        creationDate = $formattedCreationDate
    }

    $imageDetails += $imageObject
}

$imageDetails | ConvertTo-Json