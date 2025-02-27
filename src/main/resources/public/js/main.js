async function executeCommand(command, params) {
    const response = await fetch(`/${command}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
    });
    const result = await response.json();
    document.getElementById(`${command}-result`).textContent = result.output;
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