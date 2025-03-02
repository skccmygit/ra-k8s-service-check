// 네트워크 진단 도구 JavaScript 함수

// 공통 실행 함수
async function executeCommand(command, params) {
    const btnId = `${command}-btn`;
    const loadingId = `${command}-loading`;
    const resultId = `${command}-result`;
    
    try {
        document.getElementById(btnId).disabled = true;
        document.getElementById(loadingId).style.display = 'block';
        document.getElementById(resultId).textContent = '';
        document.getElementById(resultId).style.color = ''; // 색상 초기화
        
        // 타임아웃 설정
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        const response = await fetch(`/checkutil/${command}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(params),
            signal: controller.signal
        });

        clearTimeout(timeoutId);
        
        if (!response.ok) {
            throw new Error(`HTTP 오류! 상태: ${response.status}`);
        }
        
        const result = await response.json();
        if (result.error) {
            document.getElementById(resultId).style.color = 'red'; // 오류는 적색으로
            document.getElementById(resultId).textContent = result.error;
        } else {
            document.getElementById(resultId).style.color = 'green'; // 정상 출력은 녹색으로
            document.getElementById(resultId).textContent = result.output || '';
        }
    } catch (error) {
        document.getElementById(resultId).style.color = 'red'; // 오류는 적색으로
        if (error.name === 'AbortError') {
            document.getElementById(resultId).textContent = '요청이 10초 후 시간 초과되었습니다';
        } else {
            document.getElementById(resultId).textContent = '오류: ' + error.message;
        }
    } finally {
        // UI 상태 복원
        document.getElementById(btnId).disabled = false;
        document.getElementById(loadingId).style.display = 'none';
    }
}

// Netcat 테스트 실행
function runNetcat() {
    const ip = document.getElementById('nc-ip').value;
    const port = document.getElementById('nc-port').value;
    
    if (!ip || !port) {
        alert('IP 주소와 포트를 입력해주세요.');
        return;
    }
    
    executeCommand('netcat', { ip, port });
}

// DNS 조회 테스트 실행
function runNslookup() {
    const url = document.getElementById('nslookup-url').value;
    
    if (!url) {
        alert('도메인 이름을 입력해주세요.');
        return;
    }
    
    executeCommand('nslookup', { url });
}

// Curl 테스트 실행
function runCurl() {
    const url = document.getElementById('curl-url').value;
    
    if (!url) {
        alert('URL을 입력해주세요.');
        return;
    }
    
    executeCommand('curl', { url });
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('네트워크 진단 도구가 로드되었습니다');
}); 