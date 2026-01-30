$ofile = "ENDPOINTS_CHECKS.md"
"# Endpoint Smoke Test Results" | Out-File -FilePath $ofile -Encoding UTF8
"Generated: $(Get-Date)" | Out-File -FilePath $ofile -Append -Encoding UTF8

function Append($title, $method, $url, $body){
  "### $title" | Out-File -FilePath $ofile -Append -Encoding UTF8
  try{
    if($method -in @('GET','DELETE')){
      $r = Invoke-WebRequest -Uri $url -Method $method -TimeoutSec 10 -UseBasicParsing
    } else {
      $r = Invoke-WebRequest -Uri $url -Method $method -Body $body -ContentType 'application/json' -TimeoutSec 10 -UseBasicParsing
    }
    "Status: $($r.StatusCode)" | Out-File -FilePath $ofile -Append -Encoding UTF8
    "Headers:" | Out-File -FilePath $ofile -Append -Encoding UTF8
    ($r.Headers | Out-String) | Out-File -FilePath $ofile -Append -Encoding UTF8
    "Body:" | Out-File -FilePath $ofile -Append -Encoding UTF8
    if($r.Content){
      $len = $r.Content.Length; $max=1000; $snippet = $r.Content.Substring(0,[Math]::Min($len,$max))
      $snippet | Out-File -FilePath $ofile -Append -Encoding UTF8
    }
  } catch {
    "ERROR: $($_.Exception.Message)" | Out-File -FilePath $ofile -Append -Encoding UTF8
  }
  "" | Out-File -FilePath $ofile -Append -Encoding UTF8
}

# Actuator
Append 'GET /actuator/health (management:8081)' 'GET' 'http://localhost:8081/actuator/health' $null

# Auth
Append 'POST /api/auth/register' 'POST' 'http://localhost:8080/api/auth/register' '{}' 
Append 'POST /api/auth/login' 'POST' 'http://localhost:8080/api/auth/login' '{}' 
Append 'GET /api/auth/me' 'GET' 'http://localhost:8080/api/auth/me' $null

# Users
Append 'POST /api/users' 'POST' 'http://localhost:8080/api/users' '{}' 
Append 'GET /api/users/1' 'GET' 'http://localhost:8080/api/users/1' $null
Append 'PUT /api/users/1' 'PUT' 'http://localhost:8080/api/users/1' '{}' 

# Trips
Append 'POST /api/trips/create' 'POST' 'http://localhost:8080/api/trips/create' '{}' 
Append 'GET /api/trips/1' 'GET' 'http://localhost:8080/api/trips/1' $null
Append 'GET /api/trips/user/1' 'GET' 'http://localhost:8080/api/trips/user/1' $null

# Routes
Append 'GET /api/routes/1' 'GET' 'http://localhost:8080/api/routes/1' $null

# Groups
Append 'GET /api/groups/1' 'GET' 'http://localhost:8080/api/groups/1' $null
Append 'GET /api/groups/1/members' 'GET' 'http://localhost:8080/api/groups/1/members' $null

# Share
Append 'GET /api/share/token123' 'GET' 'http://localhost:8080/api/share/token123' $null

# Travel
Append 'POST /api/travel/assign' 'POST' 'http://localhost:8080/api/travel/assign' '{}' 
Append 'GET /api/travel/1' 'GET' 'http://localhost:8080/api/travel/1' $null
Append 'PUT /api/travel/1' 'PUT' 'http://localhost:8080/api/travel/1' '{}' 
Append 'PUT /api/travel/1/confirm' 'PUT' 'http://localhost:8080/api/travel/1/confirm' '{}' 

# Places
Append 'GET /api/places/nearby' 'GET' 'http://localhost:8080/api/places/nearby' $null

# Reviews
Append 'POST /api/reviews' 'POST' 'http://localhost:8080/api/reviews' '{}' 
Append 'GET /api/reviews/1' 'GET' 'http://localhost:8080/api/reviews/1' $null

# Payment
Append 'POST /api/payment/create' 'POST' 'http://localhost:8080/api/payment/create' '{}' 
Append 'POST /api/payment/verify' 'POST' 'http://localhost:8080/api/payment/verify' '{}' 
Append 'POST /api/payment/webhook' 'POST' 'http://localhost:8080/api/payment/webhook' '{}' 

# Admin
Append 'GET /api/admin/places' 'GET' 'http://localhost:8080/api/admin/places' $null
Append 'POST /api/admin/places' 'POST' 'http://localhost:8080/api/admin/places' '{}' 
Append 'DELETE /api/admin/places/1' 'DELETE' 'http://localhost:8080/api/admin/places/1' $null
Append 'GET /api/admin/vehicles' 'GET' 'http://localhost:8080/api/admin/vehicles' $null
Append 'POST /api/admin/vehicles' 'POST' 'http://localhost:8080/api/admin/vehicles' '{}' 
Append 'DELETE /api/admin/vehicles/1' 'DELETE' 'http://localhost:8080/api/admin/vehicles/1' $null

Write-Output 'Completed checks'
