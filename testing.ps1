# test-ollama-api.ps1 - Updated for your specific routes
function Test-ApiEndpoint {
    param (
        [Parameter(Mandatory=$true)]
        [string]$Uri,
        [Parameter(Mandatory=$true)]
        [string]$Method,
        [Parameter(Mandatory=$false)]
        [string]$Body,
        [Parameter(Mandatory=$false)]
        [string]$ContentType = "application/json"
    )

    Write-Host "Testing $Method $Uri" -ForegroundColor Cyan
    
    try {
        # Display request info
        if ($Method -ne "GET" -and $Body) {
            Write-Host "Sending body: $Body" -ForegroundColor Gray
            Write-Host "Content type: $ContentType" -ForegroundColor Gray
        }
        
        # Set parameters for Invoke-WebRequest
        $params = @{
            Uri = $Uri
            Method = $Method
            ContentType = $ContentType
            ErrorAction = "Stop"
        }
        
        # Add body if provided
        if ($Method -ne "GET" -and $Body) {
            $params.Body = $Body
        }
        
        # Execute the request
        $response = Invoke-WebRequest @params
        
        # Process the response
        Write-Host "Status: $($response.StatusCode) $($response.StatusDescription)" -ForegroundColor Green
        
        # Handle content directly as string
        if ($response.Content) {
            $content = $response.Content
            try {
                # Try to parse as JSON for pretty printing
                $jsonContent = $content | ConvertFrom-Json
                $jsonContent | ConvertTo-Json -Depth 5
            } catch {
                # Just return content as is if not JSON
                $content
            }
        } else {
            "[No content]"
        }
    }
    catch {
        # Handle errors
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            Write-Host "Status: $($_.Exception.Response.StatusCode) ($statusCode)" -ForegroundColor Red
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $reader.ReadToEnd()
                $reader.Close()
            } catch {
                Write-Host "Couldn't read error response: $_" -ForegroundColor Red
            }
        }
    }
}

# Define test data
$jsonBody = @{
    model = "phi4-mini"
    prompt = "What is Scala programming language?"
} | ConvertTo-Json

# Test our endpoints
Write-Host "=== Testing Ollama API Integration ===" -ForegroundColor Yellow
Write-Host ""

# Test 1: List models (OllamaController)
Write-Host "Test 1: List Models via OllamaController" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/ollama/models" -Method "GET"
Write-Host ""

# Test 2: Generate with JSON (OllamaController)
Write-Host "Test 2: Generate Text via OllamaController" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/ollama/generate" -Method "POST" -Body $jsonBody
Write-Host ""

# Test 3: Generate text with plain text (OllamaController)
Write-Host "Test 3: Generate Text with Plain Text via OllamaController" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/ollama/generate-text" -Method "POST" -Body "Tell me about Scala programming language" -ContentType "text/plain"
Write-Host ""

# Test 4: Debug endpoint (JSON)
Write-Host "Test 4: Debug JSON Request" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/debug/json" -Method "POST" -Body $jsonBody
Write-Host ""

# Test 5: Debug endpoint (Raw)
Write-Host "Test 5: Debug Raw Request" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/debug/raw" -Method "POST" -Body $jsonBody
Write-Host ""

# Test 6: Pekko controller
Write-Host "Test 6: Pekko Ollama List Models" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/pekko/models" -Method "GET"
Write-Host ""

# Test 7: Generate with Pekko
Write-Host "Test 7: Generate Text via Pekko Controller" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:9000/api/pekko/generate" -Method "POST" -Body $jsonBody
Write-Host ""

# Direct test to Ollama API for verification
Write-Host "Direct Test to Ollama API" -ForegroundColor Magenta
Test-ApiEndpoint -Uri "http://localhost:11434/api/tags" -Method "GET"
Write-Host ""

Write-Host "=== Testing Complete ===" -ForegroundColor Yellow