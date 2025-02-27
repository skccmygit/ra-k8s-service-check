async function executeCommand(command, params) {
    const btnId = `${command}-btn`;
    const loadingId = `${command}-loading`;
    const resultId = `${command}-result`;
    
    try {
        // Disable button and show loading
        document.getElementById(btnId).disabled = true;
        document.getElementById(loadingId).style.display = 'block';
        document.getElementById(resultId).textContent = '';
        
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        const response = await fetch(`/checkutil/${command}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(params),
            signal: controller.signal
        });

        clearTimeout(timeoutId);
        
        const result = await response.json();
        document.getElementById(resultId).textContent = result.output;
    } catch (error) {
        if (error.name === 'AbortError') {
            document.getElementById(resultId).textContent = 'Request timed out after 10 seconds';
        } else {
            document.getElementById(resultId).textContent = 'Error executing command';
        }
    } finally {
        // Re-enable button and hide loading
        document.getElementById(btnId).disabled = false;
        document.getElementById(loadingId).style.display = 'none';
    }
}

function runNetcat() {
    const ip = document.getElementById('nc-ip').value;
    const port = document.getElementById('nc-port').value;
    executeCommand('netcat', {ip, port});
}

function runNslookup() {
    const url = document.getElementById('nslookup-url').value;
    executeCommand('nslookup', {url});
}

function runCurl() {
    const url = document.getElementById('curl-url').value;
    executeCommand('curl', {url});
} 