// 네트워크 진단 도구 JavaScript 함수

// Netcat 테스트 실행
function runNetcat() {
    const ip = document.getElementById('nc-ip').value;
    const port = document.getElementById('nc-port').value;
    
    if (!ip || !port) {
        alert('IP 주소와 포트를 입력해주세요.');
        return;
    }
    
    // 로딩 표시 보이기
    document.getElementById('netcat-loading').style.display = 'block';
    document.getElementById('netcat-btn').disabled = true;
    document.getElementById('netcat-result').textContent = '';
    
    fetch('/checkutil/netcat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ ip: ip, port: port })
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('netcat-result').textContent = data.output.replace(/\\n/g, '\n');
    })
    .catch(error => {
        document.getElementById('netcat-result').textContent = '오류: ' + error.message;
    })
    .finally(() => {
        // 로딩 표시 숨기기
        document.getElementById('netcat-loading').style.display = 'none';
        document.getElementById('netcat-btn').disabled = false;
    });
}

// DNS 조회 테스트 실행
function runNslookup() {
    const url = document.getElementById('nslookup-url').value;
    
    if (!url) {
        alert('도메인 이름을 입력해주세요.');
        return;
    }
    
    // 로딩 표시 보이기
    document.getElementById('nslookup-loading').style.display = 'block';
    document.getElementById('nslookup-btn').disabled = true;
    document.getElementById('nslookup-result').textContent = '';
    
    fetch('/checkutil/nslookup', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ url: url })
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('nslookup-result').textContent = data.output.replace(/\\n/g, '\n');
    })
    .catch(error => {
        document.getElementById('nslookup-result').textContent = '오류: ' + error.message;
    })
    .finally(() => {
        // 로딩 표시 숨기기
        document.getElementById('nslookup-loading').style.display = 'none';
        document.getElementById('nslookup-btn').disabled = false;
    });
}

// Curl 테스트 실행
function runCurl() {
    const url = document.getElementById('curl-url').value;
    
    if (!url) {
        alert('URL을 입력해주세요.');
        return;
    }
    
    // 로딩 표시 보이기
    document.getElementById('curl-loading').style.display = 'block';
    document.getElementById('curl-btn').disabled = true;
    document.getElementById('curl-result').textContent = '';
    
    fetch('/checkutil/curl', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ url: url })
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('curl-result').textContent = data.output.replace(/\\n/g, '\n');
    })
    .catch(error => {
        document.getElementById('curl-result').textContent = '오류: ' + error.message;
    })
    .finally(() => {
        // 로딩 표시 숨기기
        document.getElementById('curl-loading').style.display = 'none';
        document.getElementById('curl-btn').disabled = false;
    });
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('네트워크 진단 도구가 로드되었습니다');
}); 