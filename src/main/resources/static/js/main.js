function runNetcat() {
    const host = document.getElementById('host').value;
    const port = document.getElementById('port').value;
    executeCommand(`nc -zv ${host} ${port}`);
}

function runNslookup() {
    const domain = document.getElementById('domain').value;
    executeCommand(`nslookup ${domain}`);
}

function runCurl() {
    const url = document.getElementById('url').value;
    executeCommand(`curl -v ${url}`);
}

function executeCommand(command) {
    fetch('/checkutil/execute', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ command: command })
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('output').textContent = data.output;
    })
    .catch(error => {
        document.getElementById('output').textContent = 'Error: ' + error.message;
    });
} 